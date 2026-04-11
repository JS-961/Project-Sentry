# Architecture (MVP)

Sentry is a Flutter-based crash detection prototype that uses phone sensors and
a rule-based detector to identify high-impact events. It provides a confirmation
UI, logs outcomes locally, and packages results into a report ZIP for evaluation.

## High-Level Flow
1. Sensor samples stream from the device (or simulation) into the app.
2. The Trip Controller forwards samples to the Detection Engine.
3. The Detection Engine applies thresholds and speed gating.
4. A suspected crash triggers the Alert UI with a countdown.
5. User response (or timeout) is recorded in the Event Log.
6. Reports and exports are generated from the Event Log.
7. A ZIP report pack is built with a manifest for documentation.

## Core Modules
- **UI Layer**: Splash, Home, Scenario Lab, Results, Settings, Presentation Mode, Alert.
- **Trip Controller**: Coordinates sensor streaming and detection lifecycle.
- **Sensor Sources**: SimulatedSensorService for live demos, ScenarioSensorService for tests.
- **Detection Engine**: Rule-based thresholds on acceleration magnitude and jerk.
- **Event Log**: Persists scenario results and alert outcomes locally.
- **Config Store**: Saves detection thresholds and cooldown values.
- **Report Generator**: Produces Markdown and HTML summaries for reporting.
- **File Exporter**: Writes CSV/JSON/HTML/MD and ZIP report packs.
- **Asset Loader**: Loads report templates and diagrams used in the pack.

## Data & Storage
All data is stored locally on-device for the MVP. Events are saved as a list of
`CrashEvent` objects and exported as JSON/CSV. Configuration is stored separately
as `DetectionConfig`. No remote servers are required for the prototype.

## Evaluation & Scenario Lab
Scenario Lab plays deterministic sensor traces so results are reproducible.
Each scenario logs PASS/CHECK based on expected vs. observed detection. A batch
runner executes all scenarios to generate a full evaluation log quickly.

## Reporting & Evidence
The Results screen generates:
- Markdown and HTML reports with a scenario outcome table.
- Chart captures for visualization.
- A ZIP report pack containing logs, templates, diagrams, and a manifest.

See `docs/Requirements.md` for the full requirements traceability matrix
linking features to evidence and tests.

## Security & Privacy (MVP)
User data remains local to the device. The alert flow does not transmit messages
in the prototype. Location and contact integration are planned extensions.

## Limitations & Next Steps
- Real-world validation is not completed (simulated data only).
- Background detection and SMS integration are not enabled in the MVP.
- Accuracy depends on phone placement and sensor variability.
- Next steps include real-device testing and emergency contact integration.
