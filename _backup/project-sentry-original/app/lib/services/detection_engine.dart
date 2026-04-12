import "../models/detection_config.dart";
import "../models/sensor_sample.dart";

class DetectionDecision {
  DetectionDecision({required this.reason, required this.severity});

  final String reason;
  final double severity;
}

class DetectionEngine {
  DetectionEngine({DetectionConfig? config})
      : config = config ?? const DetectionConfig();

  final DetectionConfig config;
  SensorSample? _previous;
  DateTime? _lastDecisionAt;

  DetectionDecision? addSample(SensorSample sample) {
    if (_previous == null) {
      _previous = sample;
      return null;
    }

    final dtMs = sample.timestamp.difference(_previous!.timestamp).inMilliseconds;
    if (dtMs <= 0) {
      _previous = sample;
      return null;
    }

    final dtSeconds = dtMs / 1000.0;
    final jerk = (sample.magnitude - _previous!.magnitude) / dtSeconds;
    _previous = sample;

    if (_lastDecisionAt != null) {
      final cooldown = sample.timestamp.difference(_lastDecisionAt!).inSeconds;
      if (cooldown < config.cooldownSeconds) {
        return null;
      }
    }

    if (sample.speedMps < config.minSpeedMps) {
      return null;
    }

    if (sample.magnitude >= config.accelThresholdG &&
        jerk >= config.jerkThresholdGPerS) {
      _lastDecisionAt = sample.timestamp;
      return DetectionDecision(
        reason: "High acceleration + jerk",
        severity: sample.magnitude,
      );
    }

    return null;
  }
}
