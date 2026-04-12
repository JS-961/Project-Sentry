import argparse
import json
from pathlib import Path


def summarize(events):
    scenario_events = [e for e in events if e.get("eventType") == "scenario"]
    alert_events = [e for e in events if e.get("eventType") == "alert"]
    passed = 0
    checked = 0
    for e in scenario_events:
        expected = e.get("expectedTrigger")
        triggered = e.get("triggered")
        if expected is None or triggered is None:
            continue
        if expected == triggered:
            passed += 1
        else:
            checked += 1
    total = len(scenario_events)
    pass_rate = "N/A" if total == 0 else f"{(passed / total) * 100:.1f}%"
    return {
        "scenario_total": total,
        "scenario_pass": passed,
        "scenario_check": checked,
        "pass_rate": pass_rate,
        "alert_total": len(alert_events),
    }


def scenario_outcomes(events):
    outcomes = {}
    for e in events:
        if e.get("eventType") != "scenario":
            continue
        scenario_id = e.get("scenarioId") or "scenario"
        name = pretty_name(scenario_id)
        entry = outcomes.setdefault(
            scenario_id,
            {"id": scenario_id, "name": name, "pass": 0, "check": 0},
        )
        expected = e.get("expectedTrigger")
        triggered = e.get("triggered")
        if expected is None or triggered is None:
            entry["check"] += 1
        elif expected == triggered:
            entry["pass"] += 1
        else:
            entry["check"] += 1
    return sorted(outcomes.values(), key=lambda row: row["name"])


def pretty_name(scenario_id):
    if not scenario_id:
        return "Scenario"
    return " ".join(part.capitalize() for part in scenario_id.split("_"))


def generate_report(events):
    summary = summarize(events)
    outcomes = scenario_outcomes(events)
    lines = []
    lines.append("# Testing & Evaluation (Auto-Generated)")
    lines.append("")
    lines.append("## Overview")
    lines.append(f"- Total scenario runs: {summary['scenario_total']}")
    lines.append(f"- Scenario PASS: {summary['scenario_pass']}")
    lines.append(f"- Scenario CHECK: {summary['scenario_check']}")
    lines.append(f"- Scenario pass rate: {summary['pass_rate']}")
    lines.append(f"- Alert events recorded: {summary['alert_total']}")
    lines.append("")
    lines.append("## Methodology")
    lines.append("- Deterministic scenario playback using scripted sensor traces.")
    lines.append("- Rule-based detection with acceleration + jerk thresholds.")
    lines.append("- Manual confirmation flow captured during simulated alerts.")
    lines.append("")
    lines.append("## Scenario Results (Most Recent)")
    scenario_events = [e for e in events if e.get("eventType") == "scenario"]
    if not scenario_events:
        lines.append("- No scenario events recorded yet.")
    else:
        for e in scenario_events[:8]:
            expected = "Yes" if e.get("expectedTrigger") else "No"
            triggered = "Yes" if e.get("triggered") else "No"
            scenario_id = e.get("scenarioId") or "scenario"
            lines.append(
                f"- {e.get('timestamp')} | {scenario_id} | "
                f"Expected: {expected} | Triggered: {triggered} | {e.get('outcome')}"
            )
    lines.append("")
    lines.append("## Scenario Outcome Table")
    if not outcomes:
        lines.append("- No scenario events recorded yet.")
    else:
        lines.append("| Scenario | PASS | CHECK | Total | Pass Rate |")
        lines.append("| --- | --- | --- | --- | --- |")
        for row in outcomes:
            total = row["pass"] + row["check"]
            pass_rate = "N/A" if total == 0 else f"{(row['pass'] / total) * 100:.1f}%"
            lines.append(
                f"| {row['name']} | {row['pass']} | {row['check']} | {total} | {pass_rate} |"
            )
    lines.append("")
    lines.append("## Limitations")
    lines.append("- Scenarios are simulated; real-world variability is not fully captured.")
    lines.append("- Additional validation on real devices is needed once hardware is available.")
    lines.append("")
    return "\n".join(lines)


def generate_report_html(events):
    summary = summarize(events)
    outcomes = scenario_outcomes(events)
    scenario_events = [e for e in events if e.get("eventType") == "scenario"]
    if not scenario_events:
        rows = "<tr><td colspan='5'>No scenario events recorded yet.</td></tr>"
    else:
        rows = []
        for e in scenario_events[:8]:
            expected = "Yes" if e.get("expectedTrigger") else "No"
            triggered = "Yes" if e.get("triggered") else "No"
            scenario_id = e.get("scenarioId") or "scenario"
            rows.append(
                "<tr>"
                f"<td>{e.get('timestamp')}</td>"
                f"<td>{scenario_id}</td>"
                f"<td>{expected}</td>"
                f"<td>{triggered}</td>"
                f"<td>{e.get('outcome')}</td>"
                "</tr>"
            )
        rows = "".join(rows)

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Sentry Testing Report</title>
  <style>
    @page {{ size: A4; margin: 20mm; }}
    body {{ font-family: Arial, sans-serif; color: #111; line-height: 1.5; }}
    h1 {{ font-size: 22px; margin-bottom: 0; }}
    h2 {{ font-size: 16px; margin-top: 20px; }}
    .meta {{ color: #555; font-size: 12px; }}
    table {{ width: 100%; border-collapse: collapse; margin-top: 8px; }}
    th, td {{ border: 1px solid #ccc; padding: 6px; font-size: 12px; }}
    th {{ background: #f2f2f2; text-align: left; }}
    .summary {{ display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }}
    .summary div {{ background: #f9f9f9; padding: 8px; border-radius: 4px; }}
  </style>
</head>
<body>
  <h1>Testing & Evaluation (Auto-Generated)</h1>
  <div class="meta">Generated by Sentry</div>

  <h2>Overview</h2>
  <div class="summary">
    <div>Total scenario runs: {summary['scenario_total']}</div>
    <div>Scenario PASS: {summary['scenario_pass']}</div>
    <div>Scenario CHECK: {summary['scenario_check']}</div>
    <div>Scenario pass rate: {summary['pass_rate']}</div>
    <div>Alert events recorded: {summary['alert_total']}</div>
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
      {rows}
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
      {render_outcome_rows(outcomes)}
    </tbody>
  </table>

  <h2>Limitations</h2>
  <ul>
    <li>Scenarios are simulated; real-world variability is not fully captured.</li>
    <li>Additional validation on real devices is needed once hardware is available.</li>
  </ul>
</body>
</html>
"""


def render_outcome_rows(outcomes):
    if not outcomes:
        return "<tr><td colspan='5'>No scenario events recorded yet.</td></tr>"
    rows = []
    for row in outcomes:
        total = row["pass"] + row["check"]
        pass_rate = "N/A" if total == 0 else f"{(row['pass'] / total) * 100:.1f}%"
        rows.append(
            "<tr>"
            f"<td>{row['name']}</td>"
            f"<td>{row['pass']}</td>"
            f"<td>{row['check']}</td>"
            f"<td>{total}</td>"
            f"<td>{pass_rate}</td>"
            "</tr>"
        )
    return "".join(rows)


def main():
    parser = argparse.ArgumentParser(description="Generate a test report from Sentry JSON logs.")
    parser.add_argument("input", type=str, help="Path to JSON log exported by the app")
    parser.add_argument(
        "-o",
        "--output",
        type=str,
        default="sentry-report.md",
        help="Output file (.md or .html)",
    )
    args = parser.parse_args()

    input_path = Path(args.input)
    if not input_path.exists():
        raise SystemExit(f"Input file not found: {input_path}")

    events = json.loads(input_path.read_text(encoding="utf-8"))
    output_path = Path(args.output)
    if output_path.suffix.lower() == ".html":
        content = generate_report_html(events)
    else:
        content = generate_report(events)
    output_path.write_text(content, encoding="utf-8")
    print(f"Wrote report to {output_path.resolve()}")


if __name__ == "__main__":
    main()
