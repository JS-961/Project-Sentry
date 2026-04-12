import "package:flutter_test/flutter_test.dart";

import "package:sentry/models/crash_event.dart";
import "package:sentry/services/report_generator.dart";

void main() {
  test("ReportGenerator includes scenario counts", () {
    final events = [
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
      CrashEvent(
        timestamp: DateTime(2026, 4, 10, 10, 5, 0),
        source: "Scenario Lab",
        outcome: "Scenario CHECK",
        notes: "Scenario Collision.",
        eventType: "scenario",
        scenarioId: "collision",
        expectedTrigger: true,
        triggered: false,
      ),
    ];

    final report = ReportGenerator().generateTestingReport(events);
    expect(report, contains("Total scenario runs: 2"));
    expect(report, contains("Scenario PASS: 1"));
    expect(report, contains("Scenario CHECK: 1"));
    expect(report, contains("Scenario Outcome Table"));
  });

  test("ReportGenerator HTML includes summary", () {
    final events = [
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
    ];

    final html = ReportGenerator().generateTestingReportHtml(events);
    expect(html, contains("Testing & Evaluation"));
    expect(html, contains("Total scenario runs"));
    expect(html, contains("Scenario Outcome Table"));
  });
}
