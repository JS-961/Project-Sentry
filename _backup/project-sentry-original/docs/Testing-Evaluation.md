Testing & Evaluation

Metrics
- Detection latency: time from trigger to alert UI
- False positives: non-crash events that trigger alerts
- False negatives: simulated crash events missed
- User response time: time to cancel an alert
- Battery impact during a 30-minute session

Test Plan (MVP)
- No special hardware required
- Use simulated sensor streams and safe everyday movements
- Scenario playback with expected outcomes (pass/fail)
- Batch scenario runs for repeatable evaluations
- Simulated crash triggers
  - Controlled phone shake with high acceleration
  - Sudden braking in a vehicle at low speed
- False-positive tests
  - Walking with phone
  - Dropping phone on a couch
  - Normal driving with bumps

Data Collection
- Log sensor metrics locally for each test
- Record whether the alert was correct or not
- Summarize results in report tables/graphs
- Export logs as JSON/CSV for reporting
- Auto-generate a testing report section from logs
- Visual summary chart for pass/check counts
- Report pack generator script (MD + CSV + JSON + diagrams)
- HTML report export (print to PDF)
- Chart image export for report figures

Ethics & Safety
- No real crash testing
- Avoid dangerous driving behavior
