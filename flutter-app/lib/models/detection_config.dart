class DetectionConfig {
  const DetectionConfig({
    this.accelThresholdG = 3.0,
    this.jerkThresholdGPerS = 15.0,
    this.minSpeedMps = 5.0,
    this.cooldownSeconds = 10,
  });

  final double accelThresholdG;
  final double jerkThresholdGPerS;
  final double minSpeedMps;
  final int cooldownSeconds;

  DetectionConfig copyWith({
    double? accelThresholdG,
    double? jerkThresholdGPerS,
    double? minSpeedMps,
    int? cooldownSeconds,
  }) {
    return DetectionConfig(
      accelThresholdG: accelThresholdG ?? this.accelThresholdG,
      jerkThresholdGPerS: jerkThresholdGPerS ?? this.jerkThresholdGPerS,
      minSpeedMps: minSpeedMps ?? this.minSpeedMps,
      cooldownSeconds: cooldownSeconds ?? this.cooldownSeconds,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      "accelThresholdG": accelThresholdG,
      "jerkThresholdGPerS": jerkThresholdGPerS,
      "minSpeedMps": minSpeedMps,
      "cooldownSeconds": cooldownSeconds,
    };
  }

  static DetectionConfig fromJson(Map<String, dynamic>? json) {
    if (json == null) {
      return const DetectionConfig();
    }
    return DetectionConfig(
      accelThresholdG: (json["accelThresholdG"] as num?)?.toDouble() ?? 3.0,
      jerkThresholdGPerS: (json["jerkThresholdGPerS"] as num?)?.toDouble() ?? 15.0,
      minSpeedMps: (json["minSpeedMps"] as num?)?.toDouble() ?? 5.0,
      cooldownSeconds: (json["cooldownSeconds"] as num?)?.toInt() ?? 10,
    );
  }
}
