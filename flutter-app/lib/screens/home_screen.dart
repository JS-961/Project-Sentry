import "dart:async";

import "package:flutter/material.dart";
import "package:intl/intl.dart";

import "../models/crash_event.dart";
import "../models/detection_config.dart";
import "../services/detection_engine.dart";
import "../services/event_log.dart";
import "../services/simulated_sensor_service.dart";
import "../services/trip_controller.dart";
import "../widgets/brand_logo.dart";
import "../widgets/primary_button.dart";
import "../widgets/reveal_on_build.dart";
import "alert_screen.dart";
import "presentation_screen.dart";
import "settings_screen.dart";

class HomeScreen extends StatefulWidget {
  const HomeScreen({
    super.key,
    required this.eventLog,
    required this.config,
    required this.logLoaded,
    required this.onConfigChanged,
  });

  final EventLog eventLog;
  final DetectionConfig config;
  final bool logLoaded;
  final ValueChanged<DetectionConfig> onConfigChanged;

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  late final TripController _controller;
  StreamSubscription<DetectionDecision>? _decisionSubscription;
  late DetectionConfig _config;

  @override
  void initState() {
    super.initState();
    _config = widget.config;
    _controller = TripController(
      sensorSource: SimulatedSensorService(sampleRateHz: 10),
      detector: DetectionEngine(config: _config),
    );
    _controller.addListener(_onControllerUpdate);
    _decisionSubscription = _controller.decisions.listen(_handleDecision);
  }

  @override
  void didUpdateWidget(HomeScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.config != oldWidget.config) {
      _config = widget.config;
      _controller.updateConfig(_config);
    }
  }

  @override
  void dispose() {
    _decisionSubscription?.cancel();
    _controller.removeListener(_onControllerUpdate);
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final events = widget.eventLog.events;
    final timeFormat = DateFormat("HH:mm:ss");
    final latest = _controller.latestSample;
    final speedKmh = latest == null ? 0.0 : (latest.speedMps * 3.6);
    final accel = latest?.magnitude ?? 0.0;
    final active = _controller.isActive;

    return Scaffold(
      appBar: AppBar(
        title: const Text("Project Sentry"),
        leading: const BrandLogo(),
        leadingWidth: 48,
        actions: [
          IconButton(
            tooltip: "Presentation Mode",
            icon: const Icon(Icons.present_to_all),
            onPressed: _openPresentation,
          ),
          IconButton(
            tooltip: "Settings",
            icon: const Icon(Icons.tune),
            onPressed: _openSettings,
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          RevealOnBuild(
            delay: const Duration(milliseconds: 40),
            child: _buildStatusCard(active),
          ),
          const SizedBox(height: 12),
          RevealOnBuild(
            delay: const Duration(milliseconds: 120),
            child: _buildMetricsCard(accel, speedKmh),
          ),
          const SizedBox(height: 12),
          RevealOnBuild(
            delay: const Duration(milliseconds: 200),
            child: _buildDetectionTile(_controller.lastDecision),
          ),
          const SizedBox(height: 12),
          RevealOnBuild(
            delay: const Duration(milliseconds: 280),
            child: _buildActionCard(active),
          ),
          const SizedBox(height: 16),
          _sectionTitle("Recent Events"),
          const SizedBox(height: 8),
          RevealOnBuild(
            delay: const Duration(milliseconds: 360),
            child: _buildEventsCard(events, timeFormat, widget.logLoaded),
          ),
        ],
      ),
    );
  }

  Widget _buildStatusCard(bool active) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text(
              "Trip Status",
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
            ),
            Text(
              active ? "Active" : "Idle",
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: active ? Colors.green : Colors.grey,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMetricsCard(double accel, double speedKmh) {
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
          ],
        ),
      ),
    );
  }

  Widget _buildDetectionTile(DetectionDecision? decision) {
    return Card(
      child: ExpansionTile(
        title: const Text(
          "Detection Details",
          style: TextStyle(fontWeight: FontWeight.w600),
        ),
        childrenPadding: const EdgeInsets.only(left: 16, right: 16, bottom: 16),
        children: [
          Text("Accel threshold: ${_config.accelThresholdG} g"),
          Text("Jerk threshold: ${_config.jerkThresholdGPerS} g/s"),
          Text("Min speed: ${_config.minSpeedMps} m/s"),
          if (decision != null) ...[
            const SizedBox(height: 8),
            Text(
              "Last decision: ${decision.reason} "
              "(${decision.severity.toStringAsFixed(2)} g)",
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildActionCard(bool active) {
    final scheme = Theme.of(context).colorScheme;
    final label = active ? "Stop Trip" : "Start Trip";
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _sectionTitle("Quick Actions"),
            const SizedBox(height: 12),
            PrimaryButton(
              label: label,
              onPressed: _toggleSession,
              background: active ? Colors.grey : scheme.secondary,
              foreground: active ? Colors.white : Colors.black,
            ),
            const SizedBox(height: 8),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                icon: const Icon(Icons.warning_amber_rounded),
                label: Text(
                  active ? "Inject Crash Spike" : "Simulate Crash (Manual)",
                ),
                onPressed: _simulateCrash,
                style: OutlinedButton.styleFrom(
                  foregroundColor: Colors.redAccent,
                  side: const BorderSide(color: Colors.redAccent),
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  textStyle: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEventsCard(
    List<CrashEvent> events,
    DateFormat timeFormat,
    bool logLoaded,
  ) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 8),
        child: _buildEventsList(events, timeFormat, logLoaded),
      ),
    );
  }

  Widget _buildEventsList(
    List<CrashEvent> events,
    DateFormat timeFormat,
    bool logLoaded,
  ) {
    if (!logLoaded) {
      return const Padding(
        padding: EdgeInsets.all(12),
        child: Text("Loading log..."),
      );
    }
    if (events.isEmpty) {
      return const Padding(
        padding: EdgeInsets.all(12),
        child: Text("No events yet."),
      );
    }
    return ListView.separated(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: events.length,
      separatorBuilder: (_, __) => const Divider(height: 1),
      itemBuilder: (context, index) {
        final event = events[index];
        return ListTile(
          title: Text(event.outcome),
          subtitle: Text(
            "${timeFormat.format(event.timestamp)} - ${event.source}\n${event.notes}",
          ),
          contentPadding:
              const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
        );
      },
    );
  }

  Widget _sectionTitle(String label) {
    return Text(
      label,
      style: Theme.of(context)
          .textTheme
          .titleMedium
          ?.copyWith(fontWeight: FontWeight.w700),
    );
  }

  void _onControllerUpdate() {
    if (!mounted) return;
    setState(() {});
  }

  Future<void> _toggleSession() async {
    if (_controller.isActive) {
      await _controller.stop();
    } else {
      await _controller.start();
    }
  }

  Future<void> _simulateCrash() async {
    if (_controller.isActive) {
      _controller.injectCrash();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Injected crash spike into sensor stream")),
      );
      return;
    }
    await _showCrashAlert(
      source: "Manual simulation",
      details: "Triggered from UI without sensor detection.",
    );
  }

  Future<void> _openSettings() async {
    final updated = await Navigator.of(context).push<DetectionConfig>(
      MaterialPageRoute(
        builder: (_) => SettingsScreen(initial: _config),
      ),
    );
    if (updated == null) return;
    setState(() {
      _config = updated;
    });
    _controller.updateConfig(updated);
    widget.onConfigChanged(updated);
  }

  Future<void> _openPresentation() async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute(
        builder: (_) => PresentationScreen(
          eventLog: widget.eventLog,
          config: _config,
        ),
      ),
    );
  }

  Future<void> _handleDecision(DetectionDecision decision) async {
    if (_controller.alertVisible) return;
    _controller.setAlertVisible(true);
    final latest = _controller.latestSample;
    final peak = latest?.magnitude.toStringAsFixed(2) ?? "?";
    final details = "${decision.reason} - peak $peak g";
    await _showCrashAlert(
      source: "Rule-based detection",
      details: details,
      reason: decision.reason,
      severity: decision.severity,
    );
    _controller.setAlertVisible(false);
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

    if (result != null) {
      setState(() {});
    }
  }
}
