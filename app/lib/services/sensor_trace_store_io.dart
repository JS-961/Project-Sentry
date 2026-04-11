import "dart:io";

import "package:path_provider/path_provider.dart";

import "../models/recorded_trace.dart";
import "sensor_trace_codec.dart";

class SensorTraceStore {
  Future<List<SensorTraceFile>> listTraces() async {
    final directory = await _ensureTraceDirectory();
    final files = directory
        .listSync()
        .whereType<File>()
        .where((file) => file.path.toLowerCase().endsWith(".csv"))
        .map((file) {
      final stat = file.statSync();
      return SensorTraceFile(
        path: file.path,
        fileName: _fileName(file),
        modifiedAt: stat.modified,
        byteSize: stat.size,
      );
    }).toList()
      ..sort((left, right) => right.modifiedAt.compareTo(left.modifiedAt));

    return files;
  }

  Future<RecordedTrace> loadTrace(SensorTraceFile traceFile) async {
    final file = File(traceFile.path);
    final content = await file.readAsString();
    return SensorTraceCodec.decode(content, fileName: traceFile.fileName);
  }

  Future<String> saveTrace(RecordedTrace trace) async {
    final directory = await _ensureTraceDirectory();
    final fileName = _buildFileName(trace);
    final file = File("${directory.path}${Platform.pathSeparator}$fileName");
    await file.writeAsString(SensorTraceCodec.encode(trace), flush: true);
    return file.path;
  }

  Future<Directory> _ensureTraceDirectory() async {
    final baseDirectory = await getApplicationDocumentsDirectory();
    final directory = Directory(
      "${baseDirectory.path}${Platform.pathSeparator}sensor_traces",
    );
    if (!directory.existsSync()) {
      await directory.create(recursive: true);
    }
    return directory;
  }

  String _buildFileName(RecordedTrace trace) {
    final label = _slug(trace.label.isNotEmpty ? trace.label : "trace");
    final stamp = DateTime.now()
        .toIso8601String()
        .replaceAll(":", "-")
        .replaceAll(".", "-");
    return "$stamp-$label.csv";
  }

  String _slug(String value) {
    final normalized = value
        .toLowerCase()
        .replaceAll(RegExp(r"[^a-z0-9]+"), "-")
        .replaceAll(RegExp(r"(^-+|-+$)"), "");
    return normalized.isEmpty ? "trace" : normalized;
  }

  String _fileName(File file) {
    final segments = file.uri.pathSegments;
    if (segments.isNotEmpty) {
      return segments.last;
    }
    return file.path.split(Platform.pathSeparator).last;
  }
}
