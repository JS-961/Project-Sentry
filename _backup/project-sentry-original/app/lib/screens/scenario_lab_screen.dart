import "dart:async";

import "package:flutter/material.dart";

import "../models/crash_event.dart";
import "../models/detection_config.dart";
import "../models/recorded_trace.dart";
import "../models/scenario.dart";
import "../models/sensor_sample.dart";
import "../models/trace_evaluation.dart";
import "../services/detection_engine.dart";
import "../services/event_log.dart";
import "../services/live_sensor_service.dart";
import "../services/recorded_sensor_service.dart";
import "../services/scenario_library.dart";
import "../services/scenario_sensor_service.dart";
import "../services/sensor_source.dart";
import "../services/sensor_trace_recorder.dart";
import "../services/sensor_trace_store.dart";
import "../services/trace_evaluator.dart";
import "../services/trip_controller.dart";
import "../widgets/brand_logo.dart";
import "../widgets/primary_button.dart";
import "../widgets/reveal_on_build.dart";
import "alert_screen.dart";

enum LabRunType {
  scenario,
  liveCapture,
  recordedTrace,
}

enum TriggerExpectation {
  unknown,
  shouldNotTrigger,
  shouldTrigger,
}

class ScenarioLabScreen extends StatefulWidget {
  const ScenarioLabScreen({
    super.key,
    required this.eventLog,
    required this.config,
  });

  final EventLog eventLog;
  final DetectionConfig config;

  @override
  State<ScenarioLabScreen> createState() => _ScenarioLabScreenState();
}

class _ScenarioLabScreenState extends State<ScenarioLabScreen> {
  final SensorTraceStore _traceStore = SensorTraceStore();

  late final List<Scenario> _scenarios;
  late Scenario _selected;
  late final TextEditingController _traceLabelController;
  late final TextEditingController _placementController;

  TripController? _controller;
  StreamSubscription<DetectionDecision>? _decisionSubscription;
  StreamSubscription<SensorSample>? _recordingSubscription;
  Completer<void>? _scenarioCompleter;
  SensorTraceRecorder? _activeRecorder;
  Scenario? _activeScenario;
  RecordedTrace? _activeTrace;
  TraceEvaluation? _latestTraceEvaluation;
  LabRunType? _runType;
  List<SensorTraceFile> _traceFiles = const [];
  SensorTraceFile? _selectedTraceFile;
  TriggerExpectation _traceExpectation = TriggerExpectation.shouldNotTrigger;
  bool _alertVisible = false;
  bool _triggered = false;
  bool _running = false;
  bool _batchRunning = false;
  bool _liveCaptureRunning = false;
  bool _traceReplayRunning = false;
  bool _loadingTraces = false;
  int _batchIndex = 0;
  List<Scenario> _batchQueue = const [];

  @override
  void initState() {
    super.initState();
    _scenarios = ScenarioLibrary.all();
    _selected = _scenarios.first;
    _traceLabelController = TextEditingController(text: _selected.id);
    _placementController = TextEditingController(text: "dashboard mount");
    unawaited(_loadTraces());
  }

  @override
  void dispose() {
    _recordingSubscription?.cancel();
    _decisionSubscription?.cancel();
    _controller?.removeListener(_onControllerUpdate);
    _controller?.dispose();
    _traceLabelController.dispose();
    _placementController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final latest = _controller?.latestSample;
    final accel = latest?.magnitude ?? 0.0;
    final speedKmh = latest == null ? 0.0 : latest.speedMps * 3.6;
    final decision = _controller?.lastDecision;
    final batchLabel = _batchRunning
        ? "Batch: ${_batchIndex + 1} / ${_batchQueue.length}"
        : null;
    final validationLabel = _liveCaptureRunning
        ? "Live capture: ${_activeRecorder?.sampleCount ?? 0} samples"
        : _traceReplayRunning
            ? "Replay: ${_activeTrace?.displayName ?? _selectedTraceFile?.displayName ?? "Recorded Trace"}"
            : null;

    return Scaffold(
      appBar: AppBar(
        title: const Text("Scenario Lab"),
        leading: const BrandLogo(),
        leadingWidth: 48,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          RevealOnBuild(
            delay: const Duration(milliseconds: 40),
            child: _buildScenarioPicker(),
          ),
          const SizedBox(height: 12),
          RevealOnBuild(
            delay: const Duration(milliseconds: 120),
            child: _buildScenarioDetails(),
          ),
          const SizedBox(height: 12),
          RevealOnBuild(
            delay: const Duration(milliseconds: 200),
            child: _buildValidationToolsCard(),
          ),
          const SizedBox(height: 12),
          RevealOnBuild(
            delay: const Duration(milliseconds: 280),
            child: _buildMetricsCard(accel, speedKmh, decision),
          ),
          if (_latestTraceEvaluation != null) ...[
            const SizedBox(height: 12),
            RevealOnBuild(
              delay: const Duration(milliseconds: 320),
              child: _buildTraceResultsCard(_latestTraceEvaluation!),
            ),
          ],
          if (batchLabel != null) ...[
            const SizedBox(height: 12),
            Text(
              batchLabel,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
          ],
          if (validationLabel != null) ...[
            const SizedBox(height: 12),
            Text(
              validationLabel,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
          ],
          const SizedBox(height: 12),
          RevealOnBuild(
            delay: const Duration(milliseconds: 360),
            child: _buildActionCard(),
          ),
        ],
      ),
    );
  }

  Widget _buildScenarioPicker() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: DropdownButtonFormField<Scenario>(
          initialValue: _selected,
          decoration: const InputDecoration(labelText: "Scenario"),
          items: _scenarios
              .map(
                (scenario) => DropdownMenuItem(
                  value: scenario,
                  child: Text(scenario.name),
                ),
              )
              .toList(),
          onChanged: _running || _batchRunning
              ? null
              : (scenario) {
                  if (scenario == null) return;
                  setState(() {
                    _selected = scenario;
                    _traceLabelController.text = scenario.id;
                    _traceExpectation =
                        _expectationFromBool(scenario.expectedTrigger);
                  });
                },
        ),
      ),
    );
  }

  Widget _buildScenarioDetails() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              _selected.description,
              style: const TextStyle(fontSize: 14),
            ),
            const SizedBox(height: 8),
            Text(
              "Expected trigger: ${_selected.expectedTrigger ? "Yes" : "No"}",
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildValidationToolsCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              "Validation Tools",
              style: Theme.of(context)
                  .textTheme
                  .titleMedium
                  ?.copyWith(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 8),
            const Text(
              "Live capture uses real device accelerometer values. GPS speed is added when location permission is available.",
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _traceLabelController,
              enabled: !_running,
              decoration: const InputDecoration(
                labelText: "Trace label",
                hintText: "city-drive-1",
              ),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _placementController,
              enabled: !_running,
              decoration: const InputDecoration(
                labelText: "Phone placement",
                hintText: "dashboard mount",
              ),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<TriggerExpectation>(
              initialValue: _traceExpectation,
              decoration: const InputDecoration(
                labelText: "Expected replay result",
              ),
              items: const [
                DropdownMenuItem(
                  value: TriggerExpectation.unknown,
                  child: Text("Not set"),
                ),
                DropdownMenuItem(
                  value: TriggerExpectation.shouldNotTrigger,
                  child: Text("Should not trigger"),
                ),
                DropdownMenuItem(
                  value: TriggerExpectation.shouldTrigger,
                  child: Text("Should trigger"),
                ),
              ],
              onChanged: _running
                  ? null
                  : (value) {
                      if (value == null) return;
                      setState(() {
                        _traceExpectation = value;
                      });
                    },
            ),
            const SizedBox(height: 8),
            const Text(
              "Safe negative-test proxies: phone pickup, set-down on a seat or cushion, backpack drop, or passenger handling. No literal phone-drop test is required.",
            ),
            const SizedBox(height: 12),
            if (_traceFiles.isEmpty)
              Text(
                _loadingTraces
                    ? "Loading recorded traces..."
                    : "No saved traces yet. Record a live trace and it will appear here.",
              )
            else
              DropdownButtonFormField<SensorTraceFile>(
                initialValue: _selectedTraceFile,
                decoration: const InputDecoration(
                  labelText: "Recorded trace",
                ),
                items: _traceFiles
                    .map(
                      (trace) => DropdownMenuItem(
                        value: trace,
                        child: Text(trace.displayName),
                      ),
                    )
                    .toList(),
                onChanged: _running
                    ? null
                    : (trace) {
                        setState(() {
                          _selectedTraceFile = trace;
                        });
                        if (trace != null) {
                          _syncExpectationFromFile(trace);
                        }
                      },
              ),
            if (_selectedTraceFile != null) ...[
              const SizedBox(height: 8),
              Text(
                "Selected file: ${_selectedTraceFile!.fileName} (${_selectedTraceFile!.byteSize} bytes)",
              ),
            ],
            const SizedBox(height: 8),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: _loadingTraces ? null : _refreshTraces,
                icon: const Icon(Icons.refresh),
                label:
                    Text(_loadingTraces ? "Refreshing..." : "Refresh Traces"),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTraceResultsCard(TraceEvaluation evaluation) {
    final scheme = Theme.of(context).colorScheme;
    final passed = evaluation.passed;
    final resultColor = passed == null
        ? scheme.primary
        : passed
            ? Colors.green
            : Colors.orange;
    final speedKmh = evaluation.peakSpeedMps * 3.6;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  "Trace Results",
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                ),
                Text(
                  evaluation.outcomeLabel,
                  style: TextStyle(
                    fontWeight: FontWeight.w700,
                    color: resultColor,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text("Trace: ${evaluation.traceName}"),
            Text(
              "Expected trigger: ${_yesNoUnknown(evaluation.expectedTrigger)}",
            ),
            Text("Actual trigger: ${evaluation.triggered ? "Yes" : "No"}"),
            Text(
              "Peak acceleration: ${evaluation.peakAccelerationG.toStringAsFixed(2)} g",
            ),
            Text(
              "Peak jerk: ${evaluation.peakJerkGPerS.toStringAsFixed(2)} g/s",
            ),
            Text("Peak speed: ${speedKmh.toStringAsFixed(1)} km/h"),
            Text("Samples: ${evaluation.sampleCount}"),
            Text(
              "Duration: ${evaluation.durationSeconds.toStringAsFixed(2)} s",
            ),
            if (evaluation.phonePlacement.trim().isNotEmpty)
              Text("Placement: ${evaluation.phonePlacement}"),
            if (evaluation.deviceLabel.trim().isNotEmpty)
              Text("Device: ${evaluation.deviceLabel}"),
          ],
        ),
      ),
    );
  }

  Widget _buildMetricsCard(
    double accel,
    double speedKmh,
    DetectionDecision? decision,
  ) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              "Live Metrics",
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 8),
            Text("Acceleration: ${accel.toStringAsFixed(2)} g"),
            Text("Speed: ${speedKmh.toStringAsFixed(1)} km/h"),
            if (decision != null) ...[
              const SizedBox(height: 8),
              Text(
                "Last decision: ${decision.reason} "
                "(${decision.severity.toStringAsFixed(2)} g)",
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildActionCard() {
    final scheme = Theme.of(context).colorScheme;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              "Scenario Actions",
              style: Theme.of(context)
                  .textTheme
                  .titleMedium
                  ?.copyWith(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 12),
            PrimaryButton(
              label: _running ? "Running..." : "Run Scenario",
              onPressed: _running || _batchRunning ? null : _runScenario,
              background: Colors.blueAccent,
            ),
            const SizedBox(height: 8),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton(
                onPressed: _running || _batchRunning ? null : _runBatch,
                style: OutlinedButton.styleFrom(
                  foregroundColor: scheme.primary,
                  side: BorderSide(color: scheme.primary),
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  textStyle: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                child: Text(
                  _batchRunning ? "Running Batch..." : "Run All Scenarios",
                ),
              ),
            ),
            const SizedBox(height: 16),
            const Divider(height: 1),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: _batchRunning ||
                        _traceReplayRunning ||
                        (_running && !_liveCaptureRunning)
                    ? null
                    : _toggleLiveCapture,
                icon: Icon(_liveCaptureRunning ? Icons.save : Icons.sensors),
                style: OutlinedButton.styleFrom(
                  foregroundColor: scheme.secondary,
                  side: BorderSide(color: scheme.secondary),
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  textStyle: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                label: Text(
                  _liveCaptureRunning
                      ? "Stop & Save Live Trace"
                      : "Start Live Capture",
                ),
              ),
            ),
            const SizedBox(height: 8),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed:
                    _selectedTraceFile == null || _running || _batchRunning
                        ? null
                        : _replaySelectedTrace,
                icon: const Icon(Icons.play_arrow_rounded),
                style: OutlinedButton.styleFrom(
                  foregroundColor: scheme.primary,
                  side: BorderSide(color: scheme.primary),
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  textStyle: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                label: Text(
                  _traceReplayRunning
                      ? "Replaying Trace..."
                      : "Replay Selected Trace",
                ),
              ),
            ),
            const SizedBox(height: 8),
            SizedBox(
              width: double.infinity,
              child: TextButton(
                onPressed:
                    _running || _batchRunning ? _handleStopPressed : null,
                style: TextButton.styleFrom(
                  foregroundColor: Colors.redAccent,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  textStyle: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                child: const Text("Stop"),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _runScenario() async {
    await _runScenarioInternal(_selected);
  }

  Future<void> _runScenarioInternal(Scenario scenario) async {
    await _stopScenario(resetBatch: false);
    _triggered = false;
    _alertVisible = false;
    _scenarioCompleter = Completer<void>();

    setState(() {
      _selected = scenario;
      _traceLabelController.text = scenario.id;
      _traceExpectation = _expectationFromBool(scenario.expectedTrigger);
      _activeScenario = scenario;
      _activeTrace = null;
      _latestTraceEvaluation = null;
      _runType = LabRunType.scenario;
      _running = true;
      _liveCaptureRunning = false;
      _traceReplayRunning = false;
    });

    final sensor = ScenarioSensorService(
      frames: scenario.frames,
      sampleRateHz: scenario.sampleRateHz,
      onComplete: _onPlaybackComplete,
    );

    try {
      await _startController(sensor);
      await _scenarioCompleter?.future;
    } catch (error) {
      await _stopScenario();
      if (!mounted) return;
      _showSnackBar("Could not start scenario: $error");
    }
  }

  Future<void> _runBatch() async {
    _batchQueue = List<Scenario>.from(_scenarios);
    _batchIndex = 0;
    setState(() {
      _batchRunning = true;
    });
    await _runScenarioInternal(_batchQueue[_batchIndex]);
  }

  Future<void> _toggleLiveCapture() async {
    if (_liveCaptureRunning) {
      await _finishLiveCapture(save: true);
      return;
    }
    await _startLiveCapture();
  }

  Future<void> _startLiveCapture() async {
    await _stopScenario();
    _triggered = false;
    _alertVisible = false;

    final sensor = LiveSensorService();
    final recorder = SensorTraceRecorder(
      label: _traceLabelController.text.trim().isEmpty
          ? _selected.id
          : _traceLabelController.text.trim(),
      phonePlacement: _placementController.text.trim().isEmpty
          ? "dashboard mount"
          : _placementController.text.trim(),
      deviceLabel: "Flutter capture",
      expectedTrigger: _boolFromExpectation(_traceExpectation),
    );

    await _recordingSubscription?.cancel();
    _recordingSubscription = sensor.stream.listen(recorder.addSample);

    setState(() {
      _activeRecorder = recorder;
      _activeScenario = null;
      _activeTrace = null;
      _latestTraceEvaluation = null;
      _runType = LabRunType.liveCapture;
      _running = true;
      _liveCaptureRunning = true;
      _traceReplayRunning = false;
    });

    try {
      await _startController(sensor);
    } catch (error) {
      await _stopScenario();
      if (!mounted) return;
      _showSnackBar("Could not start live capture: $error");
    }
  }

  Future<void> _finishLiveCapture({required bool save}) async {
    final recorder = _activeRecorder;
    final trace =
        recorder == null || recorder.isEmpty ? null : recorder.buildTrace();
    final sampleCount = recorder?.sampleCount ?? 0;

    await _stopScenario(resetBatch: false);
    if (!mounted) return;

    if (!save) {
      if (sampleCount > 0) {
        _showSnackBar("Live capture discarded.");
      }
      return;
    }

    if (trace == null) {
      _showSnackBar("No live samples were captured.");
      return;
    }

    try {
      final path = await _traceStore.saveTrace(trace);
      await _loadTraces(preferredPath: path);
      unawaited(
        widget.eventLog.add(
          CrashEvent(
            timestamp: DateTime.now(),
            source: "Scenario Lab",
            outcome: "Live trace saved",
            notes:
                "Saved ${trace.displayName} with ${trace.sampleCount} samples.",
            eventType: "trace",
          ),
        ),
      );
      if (!mounted) return;
      _showSnackBar(
        "Saved live trace with ${trace.sampleCount} samples.",
      );
    } catch (error) {
      if (!mounted) return;
      _showSnackBar("Could not save live trace: $error");
    }
  }

  Future<void> _replaySelectedTrace() async {
    final traceFile = _selectedTraceFile;
    if (traceFile == null) {
      return;
    }

    await _stopScenario(resetBatch: false);
    _triggered = false;
    _alertVisible = false;

    try {
      final trace = await _traceStore.loadTrace(traceFile);
      if (!mounted) return;

      setState(() {
        _activeScenario = null;
        _activeTrace = _traceWithExpectedFallback(trace);
        _traceExpectation = _expectationFromBool(
            _traceWithExpectedFallback(trace).expectedTrigger);
        _latestTraceEvaluation = null;
        _runType = LabRunType.recordedTrace;
        _running = true;
        _liveCaptureRunning = false;
        _traceReplayRunning = true;
      });

      final sensor = RecordedSensorService(
        samples: _activeTrace!.samples,
        onComplete: _onPlaybackComplete,
      );
      await _startController(sensor);
    } catch (error) {
      await _stopScenario(resetBatch: false);
      if (!mounted) return;
      _showSnackBar("Could not replay trace: $error");
    }
  }

  Future<void> _stopScenario({bool resetBatch = true}) async {
    if (resetBatch) {
      _batchRunning = false;
      _batchQueue = const [];
      _batchIndex = 0;
    }
    await _recordingSubscription?.cancel();
    _recordingSubscription = null;
    if (_controller != null) {
      _controller!.removeListener(_onControllerUpdate);
      await _controller!.stop();
      _controller!.dispose();
      _controller = null;
    }
    _decisionSubscription?.cancel();
    _decisionSubscription = null;
    _completeScenarioCompleter();
    _activeRecorder = null;
    _activeScenario = null;
    _activeTrace = null;
    _runType = null;

    if (mounted) {
      setState(() {
        _running = false;
        _liveCaptureRunning = false;
        _traceReplayRunning = false;
      });
      return;
    }

    _running = false;
    _liveCaptureRunning = false;
    _traceReplayRunning = false;
  }

  void _onPlaybackComplete() {
    unawaited(_finalizePlayback());
  }

  Future<void> _finalizePlayback() async {
    final runType = _runType;
    final scenario = _activeScenario;
    final trace = _activeTrace;

    await _controller?.stop();
    if (!mounted) return;
    setState(() {
      _running = false;
      _traceReplayRunning = false;
    });

    if (runType == LabRunType.scenario && scenario != null) {
      final expected = scenario.expectedTrigger;
      final passed = _triggered == expected;
      final event = CrashEvent(
        timestamp: DateTime.now(),
        source: "Scenario Lab",
        outcome: passed ? "Scenario PASS" : "Scenario CHECK",
        notes:
            "Scenario ${scenario.name}. Expected trigger: ${expected ? "Yes" : "No"}. "
            "Triggered: ${_triggered ? "Yes" : "No"}.",
        eventType: "scenario",
        scenarioId: scenario.id,
        expectedTrigger: expected,
        triggered: _triggered,
      );
      unawaited(widget.eventLog.add(event));
      _showSnackBar(
        "Scenario complete. Triggered: ${_triggered ? "Yes" : "No"} "
        "- Expected: ${expected ? "Yes" : "No"} "
        "- ${passed ? "PASS" : "CHECK"}",
      );

      _completeScenarioCompleter();
      _activeScenario = null;
      _runType = null;

      if (_batchRunning) {
        unawaited(_advanceBatch());
      }
      return;
    }

    if (runType == LabRunType.recordedTrace && trace != null) {
      final evaluation = TraceEvaluator.evaluate(
        trace: trace,
        triggered: _triggered,
      );
      if (mounted) {
        setState(() {
          _latestTraceEvaluation = evaluation;
        });
      } else {
        _latestTraceEvaluation = evaluation;
      }
      unawaited(
        widget.eventLog.add(
          CrashEvent(
            timestamp: DateTime.now(),
            source: "Scenario Lab",
            outcome: "Trace ${evaluation.outcomeLabel}",
            notes:
                "Replay ${trace.displayName}. Expected trigger: ${_yesNoUnknown(evaluation.expectedTrigger)}. "
                "Triggered: ${_triggered ? "Yes" : "No"}. "
                "Peak accel: ${evaluation.peakAccelerationG.toStringAsFixed(2)} g. "
                "Peak jerk: ${evaluation.peakJerkGPerS.toStringAsFixed(2)} g/s. "
                "Peak speed: ${(evaluation.peakSpeedMps * 3.6).toStringAsFixed(1)} km/h. "
                "Samples: ${trace.sampleCount}.",
            eventType: "trace",
            expectedTrigger: evaluation.expectedTrigger,
            triggered: _triggered,
          ),
        ),
      );
      _showSnackBar(
        "Trace replay complete. Result: ${evaluation.outcomeLabel}.",
      );
    }

    _activeTrace = null;
    _runType = null;
  }

  Future<void> _advanceBatch() async {
    if (!_batchRunning) return;
    _batchIndex += 1;
    if (_batchIndex >= _batchQueue.length) {
      if (!mounted) return;
      setState(() {
        _batchRunning = false;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Batch run complete.")),
      );
      return;
    }
    await _runScenarioInternal(_batchQueue[_batchIndex]);
  }

  Future<void> _refreshTraces() async {
    await _loadTraces(preferredPath: _selectedTraceFile?.path);
  }

  Future<void> _loadTraces({String? preferredPath}) async {
    if (mounted) {
      setState(() {
        _loadingTraces = true;
      });
    } else {
      _loadingTraces = true;
    }

    try {
      final traces = await _traceStore.listTraces();
      final selected =
          _selectTrace(traces, preferredPath ?? _selectedTraceFile?.path);
      if (!mounted) {
        _traceFiles = traces;
        _selectedTraceFile = selected;
        _loadingTraces = false;
        return;
      }
      setState(() {
        _traceFiles = traces;
        _selectedTraceFile = selected;
        _loadingTraces = false;
      });
      if (selected != null) {
        _syncExpectationFromFile(selected);
      }
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _loadingTraces = false;
      });
      _showSnackBar("Could not load trace list: $error");
    }
  }

  SensorTraceFile? _selectTrace(
    List<SensorTraceFile> traces,
    String? preferredPath,
  ) {
    if (traces.isEmpty) {
      return null;
    }
    if (preferredPath == null) {
      return traces.first;
    }
    for (final trace in traces) {
      if (trace.path == preferredPath) {
        return trace;
      }
    }
    return traces.first;
  }

  Future<void> _startController(SensorSource sensor) async {
    final controller = TripController(
      sensorSource: sensor,
      detector: DetectionEngine(config: widget.config),
    );

    _controller = controller;
    _decisionSubscription?.cancel();
    _decisionSubscription = controller.decisions.listen(_handleDecision);
    controller.addListener(_onControllerUpdate);
    await controller.start();
  }

  void _onControllerUpdate() {
    if (!mounted) return;
    setState(() {});
  }

  Future<void> _handleStopPressed() async {
    if (_liveCaptureRunning) {
      await _finishLiveCapture(save: false);
      return;
    }
    await _stopScenario();
    if (!mounted) return;
    _showSnackBar("Run stopped.");
  }

  Future<void> _handleDecision(DetectionDecision decision) async {
    if (_alertVisible) return;
    _alertVisible = true;
    _triggered = true;
    final latest = _controller?.latestSample;
    final peak = latest?.magnitude.toStringAsFixed(2) ?? "?";
    final details = "${decision.reason} - peak $peak g";
    await _showCrashAlert(
      source: _alertSource(),
      details: details,
      reason: decision.reason,
      severity: decision.severity,
    );
    _alertVisible = false;
  }

  String _alertSource() {
    switch (_runType) {
      case LabRunType.liveCapture:
        return "Live Capture";
      case LabRunType.recordedTrace:
        return "Trace: ${_activeTrace?.displayName ?? _selectedTraceFile?.displayName ?? "Recorded Trace"}";
      case LabRunType.scenario:
      case null:
        return "Scenario: ${_activeScenario?.name ?? _selected.name}";
    }
  }

  Future<void> _showCrashAlert({
    required String source,
    String? details,
    String? reason,
    double? severity,
  }) async {
    final result = await Navigator.of(context).push<CrashEvent>(
      MaterialPageRoute(
        builder: (_) => AlertScreen(
          eventLog: widget.eventLog,
          source: source,
          details: details,
          reason: reason,
          severity: severity,
        ),
      ),
    );

    if (result != null && mounted) {
      setState(() {});
    }
  }

  void _completeScenarioCompleter() {
    if (_scenarioCompleter != null && !_scenarioCompleter!.isCompleted) {
      _scenarioCompleter!.complete();
    }
    _scenarioCompleter = null;
  }

  void _showSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  RecordedTrace _traceWithExpectedFallback(RecordedTrace trace) {
    if (trace.expectedTrigger != null) {
      return trace;
    }
    return trace.copyWith(
      expectedTrigger: _boolFromExpectation(_traceExpectation),
    );
  }

  TriggerExpectation _expectationFromBool(bool? value) {
    if (value == null) {
      return TriggerExpectation.unknown;
    }
    return value
        ? TriggerExpectation.shouldTrigger
        : TriggerExpectation.shouldNotTrigger;
  }

  bool? _boolFromExpectation(TriggerExpectation value) {
    switch (value) {
      case TriggerExpectation.unknown:
        return null;
      case TriggerExpectation.shouldNotTrigger:
        return false;
      case TriggerExpectation.shouldTrigger:
        return true;
    }
  }

  String _yesNoUnknown(bool? value) {
    if (value == null) {
      return "Not set";
    }
    return value ? "Yes" : "No";
  }

  void _syncExpectationFromFile(SensorTraceFile traceFile) {
    unawaited(_loadExpectationFromFile(traceFile));
  }

  Future<void> _loadExpectationFromFile(SensorTraceFile traceFile) async {
    try {
      final trace = await _traceStore.loadTrace(traceFile);
      final expectation = _expectationFromBool(trace.expectedTrigger);
      if (!mounted || _selectedTraceFile?.path != traceFile.path) {
        return;
      }
      setState(() {
        _traceExpectation = expectation;
      });
    } catch (_) {
      // Keep the current selection if metadata cannot be read.
    }
  }
}
