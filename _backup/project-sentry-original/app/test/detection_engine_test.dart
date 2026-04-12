import "package:flutter_test/flutter_test.dart";

import "package:sentry/models/detection_config.dart";
import "package:sentry/models/sensor_sample.dart";
import "package:sentry/services/detection_engine.dart";

void main() {
  group("DetectionEngine", () {
    test("does not trigger below thresholds", () {
      final engine = DetectionEngine(
        config: const DetectionConfig(
          accelThresholdG: 3.0,
          jerkThresholdGPerS: 15.0,
          minSpeedMps: 5.0,
          cooldownSeconds: 10,
        ),
      );

      final base = DateTime(2026, 4, 10, 12, 0, 0);
      engine.addSample(_sample(base, az: 1.0, speedMps: 10));
      final decision = engine.addSample(
        _sample(base.add(const Duration(milliseconds: 100)), az: 1.2, speedMps: 10),
      );

      expect(decision, isNull);
    });

    test("triggers on high accel and jerk", () {
      final engine = DetectionEngine(
        config: const DetectionConfig(
          accelThresholdG: 3.0,
          jerkThresholdGPerS: 15.0,
          minSpeedMps: 5.0,
          cooldownSeconds: 10,
        ),
      );

      final base = DateTime(2026, 4, 10, 12, 0, 0);
      engine.addSample(_sample(base, az: 1.0, speedMps: 12));
      final decision = engine.addSample(
        _sample(base.add(const Duration(milliseconds: 100)), az: 4.0, speedMps: 12),
      );

      expect(decision, isNotNull);
      expect(decision!.reason, isNotEmpty);
    });

    test("respects cooldown between triggers", () {
      final engine = DetectionEngine(
        config: const DetectionConfig(
          accelThresholdG: 3.0,
          jerkThresholdGPerS: 15.0,
          minSpeedMps: 5.0,
          cooldownSeconds: 10,
        ),
      );

      final base = DateTime(2026, 4, 10, 12, 0, 0);
      engine.addSample(_sample(base, az: 1.0, speedMps: 12));
      final first = engine.addSample(
        _sample(base.add(const Duration(milliseconds: 100)), az: 4.0, speedMps: 12),
      );

      expect(first, isNotNull);

      final second = engine.addSample(
        _sample(base.add(const Duration(milliseconds: 300)), az: 4.5, speedMps: 12),
      );

      expect(second, isNull);
    });

    test("does not trigger when below min speed", () {
      final engine = DetectionEngine(
        config: const DetectionConfig(
          accelThresholdG: 3.0,
          jerkThresholdGPerS: 15.0,
          minSpeedMps: 5.0,
          cooldownSeconds: 10,
        ),
      );

      final base = DateTime(2026, 4, 10, 12, 0, 0);
      engine.addSample(_sample(base, az: 1.0, speedMps: 2));
      final decision = engine.addSample(
        _sample(base.add(const Duration(milliseconds: 100)), az: 4.0, speedMps: 2),
      );

      expect(decision, isNull);
    });
  });
}

SensorSample _sample(
  DateTime timestamp, {
  double ax = 0.0,
  double ay = 0.0,
  double az = 1.0,
  required double speedMps,
}) {
  return SensorSample(
    timestamp: timestamp,
    ax: ax,
    ay: ay,
    az: az,
    speedMps: speedMps,
  );
}
