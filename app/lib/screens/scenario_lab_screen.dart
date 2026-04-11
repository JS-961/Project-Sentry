import "dart:async";

import "package:flutter/material.dart";

import "../models/crash_event.dart";
import "../models/detection_config.dart";
import "../models/scenario.dart";
import "../services/detection_engine.dart";
import "../services/event_log.dart";
import "../services/scenario_library.dart";
import "../services/scenario_sensor_service.dart";
import "../services/trip_controller.dart";
import "../widgets/brand_logo.dart";
import "../widgets/primary_button.dart";
import "../widgets/reveal_on_build.dart";
import "alert_screen.dart";

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
  late final List<Scenario> _scenarios;
  late Scenario _selected;

  TripController? _controller;
  StreamSubscription<DetectionDecision>? _decisionSubscription;
  Completer<void>? _scenarioCompleter;
  bool _alertVisible = false;
  bool _triggered = false;
  bool _running = false;
  bool _batchRunning = false;
  int _batchIndex = 0;
  List<Scenario> _batchQueue = const [];

  @override
  void initState() {
    super.initState();
    _scenarios = ScenarioLibrary.all();
    _selected = _scenarios.first;
  }

  @override
  void dispose() {
    _decisionSubscription?.cancel();
    _controller?.dispose();
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
            child: _buildMetricsCard(accel, speedKmh, decision),
          ),
          if (batchLabel != null) ...[
            const SizedBox(height: 12),
            Text(
              batchLabel,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
          ],
          const SizedBox(height: 12),
          RevealOnBuild(
            delay: const Duration(milliseconds: 280),
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
            const SizedBox(height: 8),
            SizedBox(
              width: double.infinity,
              child: TextButton(
                onPressed: _running || _batchRunning ? _stopScenario : null,
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
      _running = true;
    });

    final sensor = ScenarioSensorService(
      frames: scenario.frames,
      sampleRateHz: scenario.sampleRateHz,
      onComplete: _onScenarioComplete,
    );

    final controller = TripController(
      sensorSource: sensor,
      detector: DetectionEngine(config: widget.config),
    );

    _controller = controller;
    _decisionSubscription?.cancel();
    _decisionSubscription = controller.decisions.listen(_handleDecision);
    controller.addListener(_onControllerUpdate);

    await controller.start();
    await _scenarioCompleter?.future;
  }

  Future<void> _runBatch() async {
    _batchQueue = List<Scenario>.from(_scenarios);
    _batchIndex = 0;
    setState(() {
      _batchRunning = true;
    });
    await _runScenarioInternal(_batchQueue[_batchIndex]);
  }

  Future<void> _stopScenario({bool resetBatch = true}) async {
    if (resetBatch) {
      _batchRunning = false;
      _batchQueue = const [];
      _batchIndex = 0;
    }
    if (_controller != null) {
      await _controller!.stop();
      _controller!.dispose();
      _controller = null;
    }
    _decisionSubscription?.cancel();
    _decisionSubscription = null;
    _scenarioCompleter?.complete();
    _scenarioCompleter = null;

    if (mounted) {
      setState(() {
        _running = false;
      });
    }
  }

  void _onScenarioComplete() {
    _controller?.stop();
    if (!mounted) return;
    setState(() {
      _running = false;
    });
    final expected = _selected.expectedTrigger;
    final passed = _triggered == expected;
    final event = CrashEvent(
      timestamp: DateTime.now(),
      source: "Scenario Lab",
      outcome: passed ? "Scenario PASS" : "Scenario CHECK",
      notes:
          "Scenario ${_selected.name}. Expected trigger: ${expected ? "Yes" : "No"}. "
          "Triggered: ${_triggered ? "Yes" : "No"}.",
      eventType: "scenario",
      scenarioId: _selected.id,
      expectedTrigger: expected,
      triggered: _triggered,
    );
    unawaited(widget.eventLog.add(event));
    _scenarioCompleter?.complete();
    _scenarioCompleter = null;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          "Scenario complete. Triggered: ${_triggered ? "Yes" : "No"} "
          "- Expected: ${expected ? "Yes" : "No"} "
          "- ${passed ? "PASS" : "CHECK"}",
        ),
      ),
    );

    if (_batchRunning) {
      unawaited(_advanceBatch());
    }
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

  void _onControllerUpdate() {
    if (!mounted) return;
    setState(() {});
  }

  Future<void> _handleDecision(DetectionDecision decision) async {
    if (_alertVisible) return;
    _alertVisible = true;
    _triggered = true;
    final latest = _controller?.latestSample;
    final peak = latest?.magnitude.toStringAsFixed(2) ?? "?";
    final details = "${decision.reason} - peak $peak g";
    await _showCrashAlert(
      source: "Scenario: ${_selected.name}",
      details: details,
      reason: decision.reason,
      severity: decision.severity,
    );
    _alertVisible = false;
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
}
