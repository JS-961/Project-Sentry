import "../models/recorded_trace.dart";
import "../models/sensor_sample.dart";

class SensorTraceRecorder {
  SensorTraceRecorder({
    required this.label,
    required this.phonePlacement,
    this.deviceLabel = "Unknown device",
    this.expectedTrigger,
  });

  final String label;
  final String phonePlacement;
  final String deviceLabel;
  final bool? expectedTrigger;
  final List<SensorSample> _samples = <SensorSample>[];

  int get sampleCount => _samples.length;
  bool get isEmpty => _samples.isEmpty;

  void addSample(SensorSample sample) {
    _samples.add(sample);
  }

  RecordedTrace buildTrace() {
    return RecordedTrace(
      label: label.trim(),
      phonePlacement: phonePlacement.trim(),
      deviceLabel: deviceLabel.trim(),
      samples: List<SensorSample>.unmodifiable(_samples),
      expectedTrigger: expectedTrigger,
    );
  }
}
