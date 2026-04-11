import "sensor_sample.dart";

class RecordedTrace {
  const RecordedTrace({
    required this.label,
    required this.phonePlacement,
    required this.deviceLabel,
    required this.samples,
    this.expectedTrigger,
    this.fileName,
  });

  final String label;
  final String phonePlacement;
  final String deviceLabel;
  final List<SensorSample> samples;
  final bool? expectedTrigger;
  final String? fileName;

  String get displayName {
    if (label.trim().isNotEmpty) {
      return label.trim();
    }
    if (fileName != null && fileName!.trim().isNotEmpty) {
      return fileName!
          .replaceFirst(RegExp(r"\.csv$", caseSensitive: false), "");
    }
    return "Recorded Trace";
  }

  int get sampleCount => samples.length;

  RecordedTrace copyWith({
    String? label,
    String? phonePlacement,
    String? deviceLabel,
    List<SensorSample>? samples,
    bool? expectedTrigger,
    String? fileName,
  }) {
    return RecordedTrace(
      label: label ?? this.label,
      phonePlacement: phonePlacement ?? this.phonePlacement,
      deviceLabel: deviceLabel ?? this.deviceLabel,
      samples: samples ?? this.samples,
      expectedTrigger: expectedTrigger ?? this.expectedTrigger,
      fileName: fileName ?? this.fileName,
    );
  }
}

class SensorTraceFile {
  const SensorTraceFile({
    required this.path,
    required this.fileName,
    required this.modifiedAt,
    required this.byteSize,
  });

  final String path;
  final String fileName;
  final DateTime modifiedAt;
  final int byteSize;

  String get displayName =>
      fileName.replaceFirst(RegExp(r"\.csv$", caseSensitive: false), "");
}
