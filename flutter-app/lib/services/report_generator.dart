import "../models/crash_event.dart";

class ReportGenerator {
  String generateTestingReport(List<CrashEvent> events) {
    final summary = _summarize(events);
    final outcomes = _scenarioOutcomes(events);
    final buffer = StringBuffer();
    buffer.writeln("# Testing & Evaluation (Auto-Generated)");
    buffer.writeln();
    buffer.writeln("## Overview");
    buffer.writeln("- Total scenario runs: ${summary.total}");
    buffer.writeln("- Scenario PASS: ${summary.pass}");
    buffer.writeln("- Scenario CHECK: ${summary.check}");
    buffer.writeln("- Scenario pass rate: ${summary.passRate}");
    buffer.writeln("- Alert events recorded: ${summary.alerts}");
    buffer.writeln();
    buffer.writeln("## Methodology");
    buffer.writeln("- Deterministic scenario playback using scripted sensor traces.");
    buffer.writeln("- Rule-based detection with acceleration + jerk thresholds.");
    buffer.writeln("- Manual confirmation flow captured during simulated alerts.");
    buffer.writeln();
    buffer.writeln("## Scenario Results (Most Recent)");
    if (summary.scenarios.isEmpty) {
      buffer.writeln("- No scenario events recorded yet.");
    } else {
      for (final event in summary.scenarios.take(8)) {
        final expected = event.expectedTrigger == true ? "Yes" : "No";
        final triggered = event.triggered == true ? "Yes" : "No";
        buffer.writeln(
          "- ${event.timestamp.toIso8601String()} | ${event.scenarioId ?? "scenario"} "
          "| Expected: $expected | Triggered: $triggered | ${event.outcome}",
        );
      }
    }
    buffer.writeln();
    buffer.writeln("## Scenario Outcome Table");
    if (outcomes.isEmpty) {
      buffer.writeln("- No scenario events recorded yet.");
    } else {
      buffer.writeln("| Scenario | PASS | CHECK | Total | Pass Rate |");
      buffer.writeln("| --- | --- | --- | --- | --- |");
      for (final outcome in outcomes) {
        buffer.writeln(
          "| ${outcome.name} | ${outcome.pass} | ${outcome.check} | ${outcome.total} | ${outcome.passRate} |",
        );
      }
    }
    buffer.writeln();
    buffer.writeln("## Limitations");
    buffer.writeln("- Scenarios are simulated; real-world variability is not fully captured.");
    buffer.writeln("- Additional validation on real devices is needed once hardware is available.");

    return buffer.toString();
  }

  String generateTestingReportHtml(
    List<CrashEvent> events, {
    String? logoDataUri,
  }) {
    final summary = _summarize(events);
    final outcomes = _scenarioOutcomes(events);
    final scenarioRows = summary.scenarios.isEmpty
        ? "<tr><td colspan='5'>No scenario events recorded yet.</td></tr>"
        : summary.scenarios.take(8).map((event) {
            final expected = event.expectedTrigger == true ? "Yes" : "No";
            final triggered = event.triggered == true ? "Yes" : "No";
            final scenarioId = event.scenarioId ?? "scenario";
            return "<tr>"
                "<td>${event.timestamp.toIso8601String()}</td>"
                "<td>$scenarioId</td>"
                "<td>$expected</td>"
                "<td>$triggered</td>"
                "<td>${event.outcome}</td>"
                "</tr>";
          }).join();

    final logoBlock = logoDataUri == null
        ? "<div class='logo-text'>SENTRY</div>"
        : "<img class='logo-img' src='$logoDataUri' alt='Sentry Logo' />";

    final outcomeRows = outcomes.isEmpty
        ? "<tr><td colspan='5'>No scenario events recorded yet.</td></tr>"
        : outcomes
            .map(
              (outcome) => "<tr>"
                  "<td>${outcome.name}</td>"
                  "<td>${outcome.pass}</td>"
                  "<td>${outcome.check}</td>"
                  "<td>${outcome.total}</td>"
                  "<td>${outcome.passRate}</td>"
                  "</tr>",
            )
            .join();

    return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Sentry Testing Report</title>
  <style>
    @page { size: A4; margin: 20mm; }
    body { font-family: Arial, sans-serif; color: #111; line-height: 1.5; }
    h1 { font-size: 22px; margin-bottom: 0; }
    h2 { font-size: 16px; margin-top: 20px; }
    .meta { color: #555; font-size: 12px; }
    .header { display: flex; align-items: center; gap: 12px; }
    .logo-img { width: 52px; height: 52px; object-fit: contain; }
    .logo-text { font-weight: 800; font-size: 20px; letter-spacing: 2px; }
    table { width: 100%; border-collapse: collapse; margin-top: 8px; }
    th, td { border: 1px solid #ccc; padding: 6px; font-size: 12px; }
    th { background: #f2f2f2; text-align: left; }
    .summary { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
    .summary div { background: #f9f9f9; padding: 8px; border-radius: 4px; }
  </style>
</head>
<body>
  <div class="header">
    $logoBlock
    <div>
      <h1>Testing & Evaluation (Auto-Generated)</h1>
      <div class="meta">Generated by Sentry</div>
    </div>
  </div>

  <h2>Overview</h2>
  <div class="summary">
    <div>Total scenario runs: ${summary.total}</div>
    <div>Scenario PASS: ${summary.pass}</div>
    <div>Scenario CHECK: ${summary.check}</div>
    <div>Scenario pass rate: ${summary.passRate}</div>
    <div>Alert events recorded: ${summary.alerts}</div>
  </div>

  <h2>Methodology</h2>
  <ul>
    <li>Deterministic scenario playback using scripted sensor traces.</li>
    <li>Rule-based detection with acceleration + jerk thresholds.</li>
    <li>Manual confirmation flow captured during simulated alerts.</li>
  </ul>

  <h2>Scenario Results (Most Recent)</h2>
  <table>
    <thead>
      <tr>
        <th>Timestamp</th>
        <th>Scenario</th>
        <th>Expected Trigger</th>
        <th>Triggered</th>
        <th>Outcome</th>
      </tr>
    </thead>
    <tbody>
      $scenarioRows
    </tbody>
  </table>

  <h2>Scenario Outcome Table</h2>
  <table>
    <thead>
      <tr>
        <th>Scenario</th>
        <th>PASS</th>
        <th>CHECK</th>
        <th>Total</th>
        <th>Pass Rate</th>
      </tr>
    </thead>
    <tbody>
      $outcomeRows
    </tbody>
  </table>

  <h2>Limitations</h2>
  <ul>
    <li>Scenarios are simulated; real-world variability is not fully captured.</li>
    <li>Additional validation on real devices is needed once hardware is available.</li>
  </ul>
</body>
</html>
""";
  }

  _Summary _summarize(List<CrashEvent> events) {
    final scenarioEvents =
        events.where((event) => event.eventType == "scenario").toList();
    final alertEvents =
        events.where((event) => event.eventType == "alert").toList();

    var pass = 0;
    var check = 0;
    for (final event in scenarioEvents) {
      if (event.expectedTrigger != null && event.triggered != null) {
        if (event.expectedTrigger == event.triggered) {
          pass += 1;
        } else {
          check += 1;
        }
      }
    }

    final total = scenarioEvents.length;
    final passRate =
        total == 0 ? "N/A" : "${(pass / total * 100).toStringAsFixed(1)}%";

    return _Summary(
      total: total,
      pass: pass,
      check: check,
      passRate: passRate,
      alerts: alertEvents.length,
      scenarios: scenarioEvents,
    );
  }

  List<_ScenarioOutcome> _scenarioOutcomes(List<CrashEvent> events) {
    final outcomes = <String, _ScenarioOutcome>{};
    for (final event in events) {
      if (event.eventType != "scenario") continue;
      final id = event.scenarioId ?? "scenario";
      final name = _prettyScenarioName(id);
      final outcome = outcomes.putIfAbsent(
        id,
        () => _ScenarioOutcome(id: id, name: name),
      );
      final expected = event.expectedTrigger;
      final triggered = event.triggered;
      if (expected != null && triggered != null) {
        if (expected == triggered) {
          outcome.pass += 1;
        } else {
          outcome.check += 1;
        }
      } else {
        outcome.check += 1;
      }
    }
    final list = outcomes.values.toList()
      ..sort((a, b) => a.name.compareTo(b.name));
    return list;
  }

  String _prettyScenarioName(String scenarioId) {
    if (scenarioId.isEmpty) return "Scenario";
    final parts = scenarioId.split("_");
    return parts
        .map((part) =>
            part.isEmpty ? part : part[0].toUpperCase() + part.substring(1))
        .join(" ");
  }
}

class _Summary {
  _Summary({
    required this.total,
    required this.pass,
    required this.check,
    required this.passRate,
    required this.alerts,
    required this.scenarios,
  });

  final int total;
  final int pass;
  final int check;
  final String passRate;
  final int alerts;
  final List<CrashEvent> scenarios;
}

class _ScenarioOutcome {
  _ScenarioOutcome({
    required this.id,
    required this.name,
  });

  final String id;
  final String name;
  int pass = 0;
  int check = 0;

  int get total => pass + check;
  String get passRate =>
      total == 0 ? "N/A" : "${(pass / total * 100).toStringAsFixed(1)}%";
}
