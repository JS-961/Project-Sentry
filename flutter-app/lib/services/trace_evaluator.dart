import "../models/recorded_trace.dart";
import "../models/trace_evaluation.dart";

class TraceEvaluator {
  static TraceEvaluation evaluate({
    required RecordedTrace trace,
    required bool triggered,
  }) {
    var peakAccelerationG = 0.0;
    var peakJerkGPerS = 0.0;
    var peakSpeedMps = 0.0;

    for (var i = 0; i < trace.samples.length; i += 1) {
      final sample = trace.samples[i];
      final magnitude = sample.magnitude;
      if (magnitude > peakAccelerationG) {
        peakAccelerationG = magnitude;
      }
      if (sample.speedMps > peakSpeedMps) {
        peakSpeedMps = sample.speedMps;
      }

      if (i == 0) {
        continue;
      }

      final previous = trace.samples[i - 1];
      final dtMs =
          sample.timestamp.difference(previous.timestamp).inMilliseconds;
      if (dtMs <= 0) {
        continue;
      }
      final jerk = (sample.magnitude - previous.magnitude) / (dtMs / 1000.0);
      if (jerk > peakJerkGPerS) {
        peakJerkGPerS = jerk;
      }
    }

    final durationSeconds = trace.samples.length < 2
        ? 0.0
        : trace.samples.last.timestamp
                .difference(trace.samples.first.timestamp)
                .inMilliseconds /
            1000.0;

    return TraceEvaluation(
      traceName: trace.displayName,
      sampleCount: trace.sampleCount,
      durationSeconds: durationSeconds,
      peakAccelerationG: peakAccelerationG,
      peakJerkGPerS: peakJerkGPerS,
      peakSpeedMps: peakSpeedMps,
      triggered: triggered,
      expectedTrigger: trace.expectedTrigger,
      phonePlacement: trace.phonePlacement,
      deviceLabel: trace.deviceLabel,
    );
  }
}
