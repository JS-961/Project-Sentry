# Project Sentry - Crash Detection & Response (Capstone)

> Draft report scaffold with auto-filled sections from the latest report pack.
> Update highlighted placeholders before submission.

## Title Page
- Project Title: **Project Sentry**
- Student Names / IDs: **[Fill]**
- Supervisor: **[Fill]**
- Course / Semester: **[Fill]**

## Abstract (<= 1 page)
- Problem summary: Detect severe crash-like events using phone sensors and prompt the user before sending alerts.
- Objectives: Demonstrate a transparent, privacy-first crash detection MVP with reproducible testing.
- Methodology: Rule-based thresholds on acceleration magnitude and jerk, validated with deterministic scenarios.
- Key findings: **[Update after final batch run]**
- Impact: Low-cost, cross-platform prototype for safety and research documentation.

---

## 1. Introduction
- Background and motivation: Smartphone sensors enable low-cost safety monitoring without vehicle hardware.
- Problem statement: Existing solutions are opaque, expensive, or inaccessible for everyday drivers.
- Research/practical gap: Need a transparent, privacy-first crash detection workflow with exportable evidence.
- Objectives: See **docs/MVP.md** and **docs/Feature-Priorities.md**.
- Significance and impact: Enables reproducible evaluation and educational demonstration.

## 2. Related Work
- Academic references summary: **[Fill with citations]**
- Industry references summary: **[Fill with citations]**
- Identified gaps: See **docs/Differentiators.md**.

## 3. Requirements
- Functional requirements: See **docs/Requirements.md**.
- Non-functional requirements: Privacy-first, reproducibility, local-only storage.
- Constraints: Short timeline, simulated data, limited hardware access.

## 4. System Design
- Architecture overview: See **docs/Architecture.md**.
- Key components: Trip Controller, Detection Engine, Scenario Lab, Event Log.
- Data flow and algorithm logic: See **docs/Diagrams.md**.

## 5. Implementation
- Tech stack: Flutter/Dart, sensors_plus, shared_preferences, geolocator, permission_handler, archive.
- Modules summary:
  - `TripController` and `DetectionEngine` for detection lifecycle
  - `ScenarioLabScreen` for deterministic evaluation
  - `ResultsSummaryScreen` for exports and report pack
- Screenshots: **[Add final screenshots from emulator/device]**

## 6. Testing & Evaluation
### Methodology
- Deterministic scenario playback using scripted sensor traces.
- Rule-based detection with acceleration + jerk thresholds.
- Manual confirmation flow captured during simulated alerts.

### Results Snapshot (from latest run)
- Latest pack generated: 2026-04-11T16:34:36.384676
- Total scenario runs: 1
- Scenario PASS: 1
- Scenario CHECK: 0
- Scenario pass rate: 100.0%
- Alert events recorded: 1

### Scenario Outcome Table
| Scenario | PASS | CHECK | Total | Pass Rate |
| --- | --- | --- | --- | --- |
| Normal Drive | 1 | 0 | 1 | 100.0% |

### Chart Evidence
![Scenario Chart](reports/2026-04-11/sentry-report-pack-2026-04-11/sentry-chart-2026-04-11T16-34-36.384840.png)

> Note: Re-run **Scenario Lab -> Run All Scenarios** before final submission to capture full results.

## 7. Results & Discussion
- Outcomes vs objectives: **[Update after final batch run]**
- Strengths: Transparent thresholds, reproducible scenarios, exportable evidence.
- Limitations: Simulated data only, no background service, no real SMS integration.

## 8. Project Management
- Timeline / milestones: See **docs/Timeline.md**.
- Risks and mitigation: See **docs/Risks-Ethics.md**.
- Team roles: **[Fill]**

## 9. Conclusion & Future Work
- Summary of contributions: **[Fill]**
- Future enhancements: Real device testing, background detection, emergency contact integration.

## References
- **[Add APA/IEEE citations]**

---

### Appendix: Latest Report Pack
- Source: `reports/2026-04-11/sentry-report-pack-2026-04-11/`
- Manifest: `reports/2026-04-11/sentry-report-pack-2026-04-11/manifest.txt`
