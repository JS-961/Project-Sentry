Scripts

Utilities for reporting and analysis.

Generate report from JSON log (Markdown or HTML):
python scripts/generate_report.py path\\to\\sentry-log.json -o sentry-report.md
python scripts/generate_report.py path\\to\\sentry-log.json -o sentry-report.html

Generate a full report pack (report + HTML + CSV + JSON + diagrams + template):
python scripts/make_report_pack.py path\\to\\sentry-log.json --zip
