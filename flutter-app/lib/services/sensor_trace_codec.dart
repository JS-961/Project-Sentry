import "dart:convert";

import "../models/recorded_trace.dart";
import "../models/sensor_sample.dart";

class SensorTraceCodec {
  static const String versionMarker = "sentry_trace_v1";

  static String encode(RecordedTrace trace) {
    final buffer = StringBuffer()
      ..writeln("# $versionMarker")
      ..writeln("# label=${_metaValue(trace.label)}")
      ..writeln("# phone_placement=${_metaValue(trace.phonePlacement)}")
      ..writeln("# device_label=${_metaValue(trace.deviceLabel)}")
      ..writeln(
        "# expected_trigger=${trace.expectedTrigger ?? ""}",
      )
      ..writeln("timestamp,ax_g,ay_g,az_g,speed_mps");

    for (final sample in trace.samples) {
      buffer.writeln(
        "${sample.timestamp.toIso8601String()},"
        "${sample.ax},"
        "${sample.ay},"
        "${sample.az},"
        "${sample.speedMps}",
      );
    }

    return buffer.toString();
  }

  static RecordedTrace decode(
    String content, {
    String? fileName,
  }) {
    var label = "";
    var phonePlacement = "";
    var deviceLabel = "";
    bool? expectedTrigger;
    final samples = <SensorSample>[];

    for (final rawLine in const LineSplitter().convert(content)) {
      final line = rawLine.trim();
      if (line.isEmpty) {
        continue;
      }
      if (line.startsWith("#")) {
        final meta = line.replaceFirst("#", "").trim();
        if (meta == versionMarker) {
          continue;
        }
        final separator = meta.indexOf("=");
        if (separator <= 0) {
          continue;
        }
        final key = meta.substring(0, separator).trim();
        final value = meta.substring(separator + 1).trim();
        switch (key) {
          case "label":
            label = value;
            break;
          case "phone_placement":
            phonePlacement = value;
            break;
          case "device_label":
            deviceLabel = value;
            break;
          case "expected_trigger":
            if (value == "true") {
              expectedTrigger = true;
            } else if (value == "false") {
              expectedTrigger = false;
            }
            break;
        }
        continue;
      }
      if (line.startsWith("timestamp,")) {
        continue;
      }

      final columns = line.split(",");
      if (columns.length < 5) {
        throw const FormatException("Invalid sensor trace row.");
      }

      samples.add(
        SensorSample(
          timestamp: DateTime.parse(columns[0]),
          ax: double.parse(columns[1]),
          ay: double.parse(columns[2]),
          az: double.parse(columns[3]),
          speedMps: double.parse(columns[4]),
        ),
      );
    }

    return RecordedTrace(
      label: label.isNotEmpty ? label : _fallbackLabel(fileName),
      phonePlacement: phonePlacement,
      deviceLabel: deviceLabel,
      samples: List<SensorSample>.unmodifiable(samples),
      expectedTrigger: expectedTrigger,
      fileName: fileName,
    );
  }

  static String _metaValue(String value) {
    return value.replaceAll("\r", " ").replaceAll("\n", " ").trim();
  }

  static String _fallbackLabel(String? fileName) {
    if (fileName == null || fileName.trim().isEmpty) {
      return "Recorded Trace";
    }
    return fileName.replaceFirst(RegExp(r"\.csv$", caseSensitive: false), "");
  }
}
