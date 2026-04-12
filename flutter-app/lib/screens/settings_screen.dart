import "package:flutter/material.dart";

import "../models/detection_config.dart";
import "../widgets/brand_logo.dart";

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key, required this.initial});

  final DetectionConfig initial;

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  late double _accelThresholdG;
  late double _jerkThreshold;
  late double _minSpeed;
  late double _cooldown;

  @override
  void initState() {
    super.initState();
    _accelThresholdG = widget.initial.accelThresholdG;
    _jerkThreshold = widget.initial.jerkThresholdGPerS;
    _minSpeed = widget.initial.minSpeedMps;
    _cooldown = widget.initial.cooldownSeconds.toDouble();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Detection Settings"),
        leading: const BrandLogo(),
        leadingWidth: 48,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _buildSlider(
            label: "Accel Threshold (g)",
            value: _accelThresholdG,
            min: 1.5,
            max: 6.0,
            divisions: 18,
            onChanged: (value) => setState(() => _accelThresholdG = value),
          ),
          _buildSlider(
            label: "Jerk Threshold (g/s)",
            value: _jerkThreshold,
            min: 5.0,
            max: 30.0,
            divisions: 25,
            onChanged: (value) => setState(() => _jerkThreshold = value),
          ),
          _buildSlider(
            label: "Minimum Speed (m/s)",
            value: _minSpeed,
            min: 0.0,
            max: 25.0,
            divisions: 25,
            onChanged: (value) => setState(() => _minSpeed = value),
          ),
          _buildSlider(
            label: "Cooldown (seconds)",
            value: _cooldown,
            min: 3.0,
            max: 30.0,
            divisions: 27,
            onChanged: (value) => setState(() => _cooldown = value),
          ),
          const SizedBox(height: 16),
          ElevatedButton(
            onPressed: _save,
            child: const Text("Save Settings"),
          ),
          TextButton(
            onPressed: _resetDefaults,
            child: const Text("Reset to Defaults"),
          ),
        ],
      ),
    );
  }

  Widget _buildSlider({
    required String label,
    required double value,
    required double min,
    required double max,
    required int divisions,
    required ValueChanged<double> onChanged,
  }) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label,
              style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 8),
            Text(value.toStringAsFixed(2)),
            Slider(
              value: value,
              min: min,
              max: max,
              divisions: divisions,
              label: value.toStringAsFixed(2),
              onChanged: onChanged,
            ),
          ],
        ),
      ),
    );
  }

  void _save() {
    Navigator.of(context).pop(
      DetectionConfig(
        accelThresholdG: _accelThresholdG,
        jerkThresholdGPerS: _jerkThreshold,
        minSpeedMps: _minSpeed,
        cooldownSeconds: _cooldown.round(),
      ),
    );
  }

  void _resetDefaults() {
    setState(() {
      _accelThresholdG = 3.0;
      _jerkThreshold = 15.0;
      _minSpeed = 5.0;
      _cooldown = 10.0;
    });
  }
}
