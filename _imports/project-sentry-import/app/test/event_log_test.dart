import "package:flutter_test/flutter_test.dart";
import "package:shared_preferences/shared_preferences.dart";

import "package:sentry/models/crash_event.dart";
import "package:sentry/services/event_log.dart";

void main() {
  test("EventLog exports JSON and CSV", () async {
    SharedPreferences.setMockInitialValues({});
    final log = EventLog();
    await log.add(
      CrashEvent(
        timestamp: DateTime(2026, 4, 10, 10, 0, 0),
        source: "Scenario Lab",
        outcome: "Scenario PASS",
        notes: "Scenario Normal Drive.",
        eventType: "scenario",
        scenarioId: "normal_drive",
        expectedTrigger: false,
        triggered: false,
      ),
    );

    final json = log.exportJson();
    final csv = log.exportCsv();

    expect(json, contains("\"eventType\":\"scenario\""));
    expect(csv, contains("Scenario PASS"));
    expect(csv, contains("normal_drive"));
  });
}
