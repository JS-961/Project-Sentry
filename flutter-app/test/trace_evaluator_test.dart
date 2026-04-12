import "package:flutter_test/flutter_test.dart";

import "package:sentry/models/recorded_trace.dart";
import "package:sentry/models/sensor_sample.dart";
import "package:sentry/services/trace_evaluator.dart";

void main() {
  test("TraceEvaluator computes peaks and pass state", () {
    final trace = RecordedTrace(
      label: "hard-brake-1",
      phonePlacement: "dashboard mount",
      deviceLabel: "Pixel test device",
      expectedTrigger: false,
      samples: [
        SensorSample(
          timestamp: DateTime.utc(2026, 4, 11, 12, 0, 0),
          ax: 0.0,
          ay: 0.0,
          az: 1.0,
          speedMps: 12.0,
        ),
        SensorSample(
          timestamp: DateTime.utc(2026, 4, 11, 12, 0, 0, 100),
          ax: 0.5,
          ay: 0.2,
          az: 1.3,
          speedMps: 14.0,
        ),
        SensorSample(
          timestamp: DateTime.utc(2026, 4, 11, 12, 0, 0, 200),
          ax: 0.2,
          ay: 0.1,
          az: 1.1,
          speedMps: 10.0,
        ),
      ],
    );

    final evaluation = TraceEvaluator.evaluate(
      trace: trace,
      triggered: false,
    );

    expect(evaluation.traceName, "hard-brake-1");
    expect(evaluation.sampleCount, 3);
    expect(evaluation.durationSeconds, closeTo(0.2, 0.0001));
    expect(evaluation.peakAccelerationG, closeTo(1.4071, 0.001));
    expect(evaluation.peakJerkGPerS, greaterThan(3.5));
    expect(evaluation.peakSpeedMps, 14.0);
    expect(evaluation.passed, isTrue);
    expect(evaluation.outcomeLabel, "PASS");
  });

  test("TraceEvaluator reports observed when expectation is missing", () {
    final trace = RecordedTrace(
      label: "unknown-trace",
      phonePlacement: "",
      deviceLabel: "",
      samples: [
        SensorSample(
          timestamp: DateTime.utc(2026, 4, 11, 12, 0, 0),
          ax: 0.0,
          ay: 0.0,
          az: 1.0,
          speedMps: 0.0,
        ),
      ],
    );

    final evaluation = TraceEvaluator.evaluate(
      trace: trace,
      triggered: true,
    );

    expect(evaluation.expectedTrigger, isNull);
    expect(evaluation.passed, isNull);
    expect(evaluation.outcomeLabel, "Observed");
  });
}
