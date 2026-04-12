import "package:flutter_test/flutter_test.dart";

import "package:sentry/models/recorded_trace.dart";
import "package:sentry/models/sensor_sample.dart";
import "package:sentry/services/sensor_trace_codec.dart";

void main() {
  test("SensorTraceCodec preserves metadata and samples", () {
    final trace = RecordedTrace(
      label: "city-drive-1",
      phonePlacement: "dashboard mount",
      deviceLabel: "Pixel test device",
      expectedTrigger: false,
      samples: [
        SensorSample(
          timestamp: DateTime.utc(2026, 4, 11, 12, 0, 0),
          ax: 0.1,
          ay: -0.2,
          az: 1.0,
          speedMps: 12.5,
        ),
        SensorSample(
          timestamp: DateTime.utc(2026, 4, 11, 12, 0, 0, 100),
          ax: 0.4,
          ay: 0.0,
          az: 1.2,
          speedMps: 11.8,
        ),
      ],
      fileName: "ignored.csv",
    );

    final encoded = SensorTraceCodec.encode(trace);
    final decoded = SensorTraceCodec.decode(encoded, fileName: "trace.csv");

    expect(decoded.label, "city-drive-1");
    expect(decoded.phonePlacement, "dashboard mount");
    expect(decoded.deviceLabel, "Pixel test device");
    expect(decoded.expectedTrigger, isFalse);
    expect(decoded.samples, hasLength(2));
    expect(decoded.samples.first.ax, closeTo(0.1, 0.0001));
    expect(decoded.samples.last.az, closeTo(1.2, 0.0001));
    expect(decoded.samples.last.speedMps, closeTo(11.8, 0.0001));
  });

  test("SensorTraceCodec falls back to file name when label is missing", () {
    const content = """
# sentry_trace_v1
timestamp,ax_g,ay_g,az_g,speed_mps
2026-04-11T12:00:00.000Z,0.0,0.0,1.0,0.0
""";

    final decoded =
        SensorTraceCodec.decode(content, fileName: "sample-trace.csv");

    expect(decoded.label, "sample-trace");
    expect(decoded.displayName, "sample-trace");
    expect(decoded.samples, hasLength(1));
  });
}
