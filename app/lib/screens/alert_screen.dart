import "dart:async";

import "package:flutter/material.dart";

import "../models/crash_event.dart";
import "../services/event_log.dart";
import "../widgets/primary_button.dart";

class AlertScreen extends StatefulWidget {
  const AlertScreen({
    super.key,
    required this.eventLog,
    required this.source,
    this.details,
    this.reason,
    this.severity,
    this.countdownSeconds = 10,
  });

  final EventLog eventLog;
  final String source;
  final String? details;
  final String? reason;
  final double? severity;
  final int countdownSeconds;

  @override
  State<AlertScreen> createState() => _AlertScreenState();
}

class _AlertScreenState extends State<AlertScreen> {
  late int _secondsLeft;
  Timer? _timer;
  bool _resolved = false;

  @override
  void initState() {
    super.initState();
    _secondsLeft = widget.countdownSeconds;
    _timer = Timer.periodic(const Duration(seconds: 1), _tick);
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.red.shade50,
      appBar: AppBar(
        title: const Text("Crash Detected"),
        backgroundColor: Colors.red.shade200,
      ),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              "Are you OK?",
              style: TextStyle(fontSize: 28, fontWeight: FontWeight.bold),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 12),
            Text(
              "Sending alert in $_secondsLeft seconds",
              style: const TextStyle(fontSize: 16),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            PrimaryButton(
              label: "I'm OK",
              onPressed: () => _resolve("User canceled"),
              background: Colors.green,
            ),
            const SizedBox(height: 12),
            PrimaryButton(
              label: "Get Help",
              onPressed: () => _resolve("User requested help"),
              background: Colors.redAccent,
            ),
            const Spacer(),
            if (widget.details != null)
              Text(
                widget.details!,
                textAlign: TextAlign.center,
                style: const TextStyle(color: Colors.black54),
              ),
            if (widget.details != null) const SizedBox(height: 8),
            Text(
              "Source: ${widget.source}",
              textAlign: TextAlign.center,
              style: const TextStyle(color: Colors.grey),
            ),
          ],
        ),
      ),
    );
  }

  void _tick(Timer timer) {
    if (_secondsLeft <= 1) {
      _resolve("No response (auto-alert)");
      return;
    }

    setState(() {
      _secondsLeft -= 1;
    });
  }

  Future<void> _resolve(String outcome) async {
    if (_resolved) return;
    _resolved = true;
    _timer?.cancel();

    final event = CrashEvent(
      timestamp: DateTime.now(),
      source: widget.source,
      outcome: outcome,
      notes: widget.details ??
          "Location capture and contact alert not yet wired.",
      eventType: "alert",
      reason: widget.reason,
      severity: widget.severity,
    );

    await widget.eventLog.add(event);
    if (!mounted) return;
    Navigator.of(context).pop(event);
  }
}
