class TraceEvaluation {
  const TraceEvaluation({
    required this.traceName,
    required this.sampleCount,
    required this.durationSeconds,
    required this.peakAccelerationG,
    required this.peakJerkGPerS,
    required this.peakSpeedMps,
    required this.triggered,
    required this.expectedTrigger,
    required this.phonePlacement,
    required this.deviceLabel,
  });

  final String traceName;
  final int sampleCount;
  final double durationSeconds;
  final double peakAccelerationG;
  final double peakJerkGPerS;
  final double peakSpeedMps;
  final bool triggered;
  final bool? expectedTrigger;
  final String phonePlacement;
  final String deviceLabel;

  bool? get passed {
    if (expectedTrigger == null) {
      return null;
    }
    return expectedTrigger == triggered;
  }

  String get outcomeLabel {
    final result = passed;
    if (result == null) {
      return "Observed";
    }
    return result ? "PASS" : "CHECK";
  }
}
