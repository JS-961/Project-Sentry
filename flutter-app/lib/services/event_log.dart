import "dart:convert";

import "package:shared_preferences/shared_preferences.dart";

import "../models/crash_event.dart";

class EventLog {
  static const String _storageKey = "sentry_event_log_v1";

  final List<CrashEvent> _events = <CrashEvent>[];
  bool _loaded = false;

  List<CrashEvent> get events => List<CrashEvent>.unmodifiable(_events);
  bool get isLoaded => _loaded;

  Future<void> load() async {
    if (_loaded) return;
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_storageKey);
    if (raw != null && raw.isNotEmpty) {
      try {
        final decoded = jsonDecode(raw) as List<dynamic>;
        _events
          ..clear()
          ..addAll(
            decoded
                .whereType<Map<String, dynamic>>()
                .map(CrashEvent.fromJson),
          );
      } catch (_) {
        _events.clear();
      }
    }
    _loaded = true;
  }

  Future<void> add(CrashEvent event) async {
    _events.insert(0, event);
    await _persist();
  }

  Future<void> clear() async {
    _events.clear();
    await _persist();
  }

  String exportJson() {
    return jsonEncode(_events.map((event) => event.toJson()).toList());
  }

  String exportCsv() {
    final buffer = StringBuffer();
    buffer.writeln(
      "timestamp,source,outcome,notes,eventType,reason,severity,latitude,longitude,scenarioId,expectedTrigger,triggered",
    );
    for (final event in _events) {
      buffer.writeln(
        [
          _csvField(event.timestamp.toIso8601String()),
          _csvField(event.source),
          _csvField(event.outcome),
          _csvField(event.notes),
          _csvField(event.eventType),
          _csvField(event.reason),
          _csvField(event.severity?.toString()),
          _csvField(event.latitude?.toString()),
          _csvField(event.longitude?.toString()),
          _csvField(event.scenarioId),
          _csvField(event.expectedTrigger?.toString()),
          _csvField(event.triggered?.toString()),
        ].join(","),
      );
    }
    return buffer.toString();
  }

  String _csvField(String? value) {
    final raw = value ?? "";
    final needsQuotes =
        raw.contains(",") || raw.contains("\n") || raw.contains("\"");
    if (!needsQuotes) return raw;
    final escaped = raw.replaceAll("\"", "\"\"");
    return "\"$escaped\"";
  }

  Future<void> _persist() async {
    final prefs = await SharedPreferences.getInstance();
    final payload = jsonEncode(_events.map((event) => event.toJson()).toList());
    await prefs.setString(_storageKey, payload);
  }
}
