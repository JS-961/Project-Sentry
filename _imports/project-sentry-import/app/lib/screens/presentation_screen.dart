import "dart:async";

import "package:flutter/material.dart";

import "../models/crash_event.dart";
import "../models/detection_config.dart";
import "../services/detection_engine.dart";
import "../services/event_log.dart";
import "../services/simulated_sensor_service.dart";
import "../services/trip_controller.dart";
import "../widgets/primary_button.dart";
import "alert_screen.dart";

class PresentationScreen extends StatefulWidget {
  const PresentationScreen({
    super.key,
    required this.eventLog,
    required this.config,
  });

  final EventLog eventLog;
  final DetectionConfig config;

  @override
  State<PresentationScreen> createState() => _PresentationScreenState();
}

class _PresentationScreenState extends State<PresentationScreen> {
  late final TripController _controller;
  StreamSubscription<DetectionDecision>? _decisionSubscription;

  @override
  void initState() {
    super.initState();
    _controller = TripController(
      sensorSource: SimulatedSensorService(sampleRateHz: 12),
      detector: DetectionEngine(config: widget.config),
    );
    _controller.addListener(_onControllerUpdate);
    _decisionSubscription = _controller.decisions.listen(_handleDecision);
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
    final scheme = Theme.of(context).colorScheme;
    final latest = _controller.latestSample;
    final speedKmh = latest == null ? 0.0 : (latest.speedMps * 3.6);
    final accel = latest?.magnitude ?? 0.0;
    final active = _controller.isActive;
    final decision = _controller.lastDecision;

    return Scaffold(
      backgroundColor: scheme.primary,
      appBar: AppBar(
        title: const Text("Presentation Mode"),
        backgroundColor: scheme.primary,
        foregroundColor: scheme.secondary,
        actions: [
          IconButton(
            tooltip: "Demo Checklist",
            icon: const Icon(Icons.checklist),
            onPressed: _showDemoChecklist,
          ),
          IconButton(
            tooltip: "Reset Event Log",
            icon: const Icon(Icons.delete_sweep),
            onPressed: _confirmClearLog,
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          Center(
            child: Column(
              children: [
                Image.asset(
                  "assets/logo.png",
                  width: 92,
                  height: 92,
                  fit: BoxFit.contain,
                ),
                const SizedBox(height: 12),
                Text(
                  "Project Sentry",
                  style: Theme.of(context).textTheme.titleLarge?.copyWith(
                        color: scheme.secondary,
                        fontWeight: FontWeight.w700,
                      ),
                ),
                const SizedBox(height: 4),
                Text(
                  "Live Demo View",
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: Colors.white70,
                      ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),
          _buildStatusCard(active),
          const SizedBox(height: 16),
          _buildMetricsCard(accel, speedKmh),
          const SizedBox(height: 16),
          _buildDecisionCard(decision, scheme),
          const SizedBox(height: 20),
          PrimaryButton(
            label: active ? "Stop Demo Trip" : "Start Demo Trip",
            onPressed: _toggleSession,
            background: active ? Colors.white.withOpacity(0.24) : scheme.secondary,
            foreground: active ? Colors.white : Colors.black,
          ),
          const SizedBox(height: 12),
          PrimaryButton(
            label: "Simulate Crash",
            onPressed: _simulateCrash,
            background: Colors.redAccent,
            foreground: Colors.white,
          ),
          const SizedBox(height: 12),
          Text(
            "Tip: Use this screen for live presentations and screen recordings.",
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: Colors.white70,
                ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatusCard(bool active) {
    return _darkCard(
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            "Trip Status",
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  color: Colors.white,
                  fontWeight: FontWeight.w600,
                ),
          ),
          Text(
            active ? "Active" : "Idle",
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  color: active ? Colors.greenAccent : Colors.white54,
                  fontWeight: FontWeight.w700,
                ),
          ),
        ],
      ),
    );
  }

  Widget _buildMetricsCard(
    double accel,
    double speedKmh,
  ) {
    return _darkCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            "Live Metrics",
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  color: Colors.white,
                  fontWeight: FontWeight.w600,
                ),
          ),
          const SizedBox(height: 12),
          _metricRow("Acceleration", "${accel.toStringAsFixed(2)} g"),
          const SizedBox(height: 8),
          _metricRow("Speed", "${speedKmh.toStringAsFixed(1)} km/h"),
        ],
      ),
    );
  }

  Widget _buildDecisionCard(
    DetectionDecision? decision,
    ColorScheme scheme,
  ) {
    return _darkCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            "Last Decision",
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  color: Colors.white,
                  fontWeight: FontWeight.w600,
                ),
          ),
          const SizedBox(height: 12),
          Text(
            decision == null ? "No trigger yet." : decision.reason,
            style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                  color: decision == null ? Colors.white70 : scheme.secondary,
                  fontWeight: FontWeight.w700,
                ),
          ),
          if (decision != null) ...[
            const SizedBox(height: 6),
            Text(
              "Severity: ${decision.severity.toStringAsFixed(2)} g",
              style: Theme.of(context)
                  .textTheme
                  .bodyMedium
                  ?.copyWith(color: Colors.white70),
            ),
          ],
        ],
      ),
    );
  }

  Widget _metricRow(String label, String value) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: Theme.of(context)
              .textTheme
              .bodyMedium
              ?.copyWith(color: Colors.white70),
        ),
        Text(
          value,
          style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                color: Colors.white,
                fontWeight: FontWeight.w600,
              ),
        ),
      ],
    );
  }

  Widget _darkCard({required Widget child}) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.08),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withOpacity(0.12)),
      ),
      child: child,
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
        const SnackBar(content: Text("Injected crash spike into demo stream")),
      );
      return;
    }
    await _showCrashAlert(
      source: "Presentation demo",
      details: "Manual simulation from demo screen.",
    );
  }

  Future<void> _handleDecision(DetectionDecision decision) async {
    if (_controller.alertVisible) return;
    _controller.setAlertVisible(true);
    final latest = _controller.latestSample;
    final peak = latest?.magnitude.toStringAsFixed(2) ?? "?";
    final details = "${decision.reason} - peak $peak g";
    await _showCrashAlert(
      source: "Presentation demo",
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

    if (result != null && mounted) {
      setState(() {});
    }
  }

  Future<void> _showDemoChecklist() async {
    await showModalBottomSheet<void>(
      context: context,
      backgroundColor: Theme.of(context).colorScheme.primary,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Icon(Icons.checklist, color: Colors.amber),
                    const SizedBox(width: 8),
                    Text(
                      "Demo Checklist",
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                            color: Colors.white,
                            fontWeight: FontWeight.w700,
                          ),
                    ),
                    const Spacer(),
                    IconButton(
                      onPressed: () => Navigator.of(context).pop(),
                      icon: const Icon(Icons.close, color: Colors.white70),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                const _ChecklistItem(
                  index: 1,
                  text: "Tap Start Demo Trip to begin sensor streaming.",
                ),
                const _ChecklistItem(
                  index: 2,
                  text: "Point out live metrics updating in real time.",
                ),
                const _ChecklistItem(
                  index: 3,
                  text: "Tap Simulate Crash to trigger the alert flow.",
                ),
                const _ChecklistItem(
                  index: 4,
                  text: "Show countdown + user response buttons.",
                ),
                const _ChecklistItem(
                  index: 5,
                  text: "Open Results to show reports and exports.",
                ),
                const SizedBox(height: 12),
                Text(
                  "Tip: Use Reset Event Log before a new demo run.",
                  style: Theme.of(context)
                      .textTheme
                      .bodySmall
                      ?.copyWith(color: Colors.white70),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Future<void> _confirmClearLog() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text("Clear event log?"),
        content: const Text(
          "This will remove all scenario and alert events from the log.",
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text("Cancel"),
          ),
          ElevatedButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text("Clear"),
          ),
        ],
      ),
    );

    if (confirmed != true) return;
    await widget.eventLog.clear();
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text("Event log cleared")),
    );
  }
}

class _ChecklistItem extends StatelessWidget {
  const _ChecklistItem({required this.index, required this.text});

  final int index;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 24,
            height: 24,
            decoration: BoxDecoration(
              color: Colors.white12,
              borderRadius: BorderRadius.circular(8),
            ),
            alignment: Alignment.center,
            child: Text(
              "$index",
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: Colors.white70,
                    fontWeight: FontWeight.w700,
                  ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              text,
              style: Theme.of(context)
                  .textTheme
                  .bodyMedium
                  ?.copyWith(color: Colors.white),
            ),
          ),
        ],
      ),
    );
  }
}
