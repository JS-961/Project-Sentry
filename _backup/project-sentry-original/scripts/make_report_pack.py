import argparse
import json
from pathlib import Path
import zipfile

from generate_report import generate_report, generate_report_html, summarize


def csv_field(value):
    raw = "" if value is None else str(value)
    if "," in raw or "\n" in raw or "\"" in raw:
        raw = raw.replace("\"", "\"\"")
        return f"\"{raw}\""
    return raw


def export_csv(events):
    header = [
        "timestamp",
        "source",
        "outcome",
        "notes",
        "eventType",
        "reason",
        "severity",
        "latitude",
        "longitude",
        "scenarioId",
        "expectedTrigger",
        "triggered",
    ]
    lines = [",".join(header)]
    for e in events:
        row = [
            csv_field(e.get("timestamp")),
            csv_field(e.get("source")),
            csv_field(e.get("outcome")),
            csv_field(e.get("notes")),
            csv_field(e.get("eventType")),
            csv_field(e.get("reason")),
            csv_field(e.get("severity")),
            csv_field(e.get("latitude")),
            csv_field(e.get("longitude")),
            csv_field(e.get("scenarioId")),
            csv_field(e.get("expectedTrigger")),
            csv_field(e.get("triggered")),
        ]
        lines.append(",".join(row))
    return "\n".join(lines) + "\n"


def write_pack(events, output_dir: Path):
    output_dir.mkdir(parents=True, exist_ok=True)
    report = generate_report(events)
    report_html = generate_report_html(events)
    csv_text = export_csv(events)
    json_text = json.dumps(events, indent=2)
    summary = summarize(events)

    (output_dir / "report.md").write_text(report, encoding="utf-8")
    (output_dir / "report.html").write_text(report_html, encoding="utf-8")
    (output_dir / "log.csv").write_text(csv_text, encoding="utf-8")
    (output_dir / "log.json").write_text(json_text, encoding="utf-8")
    (output_dir / "summary.txt").write_text(
        "\n".join(
            [
                f"Total scenario runs: {summary['scenario_total']}",
                f"Scenario PASS: {summary['scenario_pass']}",
                f"Scenario CHECK: {summary['scenario_check']}",
                f"Pass rate: {summary['pass_rate']}",
                f"Alert events recorded: {summary['alert_total']}",
            ]
        )
        + "\n",
        encoding="utf-8",
    )

    diagrams = Path("docs/Diagrams.md")
    if diagrams.exists():
        (output_dir / "diagrams.md").write_text(
            diagrams.read_text(encoding="utf-8"),
            encoding="utf-8",
        )

    template = Path("docs/Report-Template-Printable.md")
    if template.exists():
        (output_dir / "report-template.md").write_text(
            template.read_text(encoding="utf-8"),
            encoding="utf-8",
        )


def zip_pack(output_dir: Path, zip_path: Path):
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zf:
        for path in output_dir.rglob("*"):
            if path.is_file():
                zf.write(path, path.relative_to(output_dir))


def main():
    parser = argparse.ArgumentParser(
        description="Create a report pack folder (and optional zip) from a Sentry JSON log."
    )
    parser.add_argument("input", type=str, help="Path to JSON log exported by the app")
    parser.add_argument(
        "-o",
        "--output",
        type=str,
        default=None,
        help="Output directory for the report pack",
    )
    parser.add_argument("--zip", action="store_true", help="Also create a zip archive")
    args = parser.parse_args()

    input_path = Path(args.input)
    if not input_path.exists():
        raise SystemExit(f"Input file not found: {input_path}")

    events = json.loads(input_path.read_text(encoding="utf-8"))
    stamp = (
        input_path.stem.replace(":", "-").replace(" ", "-")
        if args.output is None
        else None
    )
    output_dir = Path(args.output) if args.output else Path(f"report-pack-{stamp}")
    write_pack(events, output_dir)
    print(f"Report pack written to {output_dir.resolve()}")

    if args.zip:
        zip_path = output_dir.with_suffix(".zip")
        zip_pack(output_dir, zip_path)
        print(f"Zip written to {zip_path.resolve()}")


if __name__ == "__main__":
    main()
