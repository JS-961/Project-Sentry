# Project Sentry Architecture

Project Sentry is intentionally split into two app layers that serve different
capstone goals while sharing one repository, documentation set, and advisory ML
workspace.

## Design Principle

Do not force one stack to do everything.

- `flutter-app/` owns polished demo, simulation, validation replay, results,
  report exports, and presentation flows.
- `android-native/` owns real Android runtime behavior, foreground monitoring,
  permissions, persistence, advisory ML display, and crash escalation.

This keeps the repository honest:

- the Flutter app is strong for demos and evidence packaging
- the native app is strong for realistic device behavior

## Repository Responsibilities

| Area | Primary Responsibility | Source of Truth |
| --- | --- | --- |
| `flutter-app/` | Scenario lab, live validation capture, replay, results dashboard, report exports, presentation mode | original Project-Sentry |
| `android-native/` | Foreground service, permissions, sensors, location, Room, countdown, SMS/call/TTS crash flow, advisory ML display | original SafeDrive-project |
| `docs/` | Unified capstone architecture, evaluation, ethics, references, figure plan, and repository guidance | merged |
| `demo/` | Demo script and class/demo support material | merged |
| `ml/` | Advisory training pipeline, diagnostics, evaluation outputs, and Android JSON model exports | shared model work |

## Diagram Sources

The canonical Mermaid source lives in `docs/Diagrams.md`. The Flutter report
assets mirror the same content in `flutter-app/assets/diagrams.md` so exported
report packs do not carry stale architecture diagrams.

The final report currently uses these rendered diagrams:

| Final Report Figure | Mermaid Section | Purpose |
| --- | --- | --- |
| Figure 1 | `Figure 1: Project Sentry Use-Case Diagram` | Maps Driver/Tester, Phone Sensors, Emergency Contact, and the ten major report use cases |
| Figure 2 | `Figure 2: Project Sentry System Architecture` | Shows the two-app repository architecture plus `docs/`, `demo/`, and `ml/` evidence layers |
| Figure 3 | `Figure 3: Crash-Detection And Escalation Runtime Flow` | Shows permission readiness, monitoring, rule-based crash flow, advisory ML display, escalation, and Room persistence |

Supporting Mermaid diagrams are also provided for Android runtime architecture,
advisory ML training/export, Flutter validation/report-pack flow, permission
sequence, and the core persistence model.

## Android Native Runtime Boundary

The native Android app is the real MVP runtime and owns:

- `MainActivity` permission explanations and runtime permission requests
- `HomeScreen` Drive, History, and Status tabs
- `SettingsScreen` emergency contacts, demo call number, TTS template, and crash
  readiness checks
- `DrivingModeService` foreground operation
- accelerometer, gyroscope, and fused location collection
- risk scoring and crash-like event gating
- advisory JSON model inference for driver-risk and road-condition labels
- Room persistence for trips, events, and crash alerts
- `CrashCountdownActivity` countdown confirmation
- SMS, call intent, and TTS escalation actions

The crash countdown remains rule-based and human-in-the-loop. The ML output is
displayed as advisory context only; it does not decide whether emergency
escalation begins.

## Android Runtime Flow

The Android flow is:

1. The driver starts Driving Mode from the Drive tab.
2. `MainActivity` checks required permissions and configuration.
3. Android shows permission prompts only after the app explanation dialog.
4. `DrivingModeService` starts as a foreground service.
5. The service collects accelerometer, gyroscope, and fused location samples.
6. Rule-based logic updates risk score, event counters, crash candidates, and
   cooldown state.
7. `AdvisoryRiskClassifier` publishes ML label, score, confidence, and source
   into the shared UI state.
8. If staged crash validation passes, `CrashCountdownActivity` opens.
9. The driver can cancel, press `Call Now`, or let the timer expire.
10. The service persists trips, risk events, and crash-alert outcomes to Room.
11. The History and Status tabs expose the persisted data and current readiness.

## Flutter App Boundary

The Flutter app is not just a mock UI. It supports:

- deterministic scenario playback
- live accelerometer capture from a real Android device
- optional GPS speed for trace recording
- saved trace replay and PASS/CHECK evaluation
- results visualization and report-pack generation
- presentation mode for capstone demos

However, Flutter is still not the primary runtime for:

- long-running background monitoring
- Android foreground service reliability
- native SMS/call/TTS crash escalation

## Advisory ML Boundary

The ML workspace under `ml/` provides a deployed advisory layer, not a
production crash classifier. It supports:

- public smartphone-sensor dataset processing
- conservative label mapping and skipped unverifiable folders
- window-level feature extraction
- model training and diagnostics
- JSON model export for Android assets
- Android-side inference through `AdvisoryRiskClassifier`

Current ML labels are appropriate for advisory driver-risk and road-condition
context such as `NORMAL`, `AGGRESSIVE`, and `ROAD_ANOMALY`. They are not
validated as emergency crash truth labels.

## Shared Artifacts

The two apps are currently connected at the repository level rather than through
an in-process bridge.

Shared capstone artifacts include:

- evaluation methodology
- demo script
- design and ethics documentation
- Mermaid diagrams
- figure capture plan
- advisory ML workspace
- export and reporting conventions

## Why There Is No Heavy Bridge Yet

No full Flutter-native bridge was added during this merge because it would be
high-risk and would blur responsibilities before the repository structure is
stable.

Low-risk coordination paths for later:

- shared CSV or JSON trace formats
- shared report inputs and outputs
- launching native demos separately during capstone presentation
- gradual extraction of common evaluation logic

High-risk paths deliberately deferred:

- rewriting the Android service into Flutter
- large MethodChannel integrations without a stable contract
- collapsing both apps into one runtime prematurely
