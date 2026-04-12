import "../models/recorded_trace.dart";

class SensorTraceStore {
  Future<List<SensorTraceFile>> listTraces() async {
    return const <SensorTraceFile>[];
  }

  Future<RecordedTrace> loadTrace(SensorTraceFile traceFile) {
    throw UnsupportedError("Recorded trace loading is not supported on web.");
  }

  Future<String> saveTrace(RecordedTrace trace) {
    throw UnsupportedError("Recorded trace storage is not supported on web.");
  }
}
