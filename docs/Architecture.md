# Project Sentry Architecture

Project Sentry is intentionally split into two app layers that serve different
capstone goals while sharing the same repository, documentation, and future ML
workspace.

## Design Principle

Do not force one stack to do everything.

- `flutter-app/` owns polished demo, simulation, validation replay, results,
  and presentation flows.
- `android-native/` owns real Android runtime behavior, foreground monitoring,
  permissions, persistence, and crash escalation.

This keeps the repository honest:

- the Flutter app is strong for demos and evidence packaging
- the native app is strong for realistic device behavior

## Repository Responsibilities

| Area | Primary Responsibility | Source of Truth |
| --- | --- | --- |
| `flutter-app/` | Scenario lab, live validation capture, replay, results dashboard, report exports, presentation mode | original Project-Sentry |
| `android-native/` | Foreground service, permissions, sensors, location, Room, countdown, SMS/call/TTS crash flow | original SafeDrive-project |
| `docs/` | Unified capstone architecture, evaluation, ethics, references, and repository guidance | merged |
| `demo/` | Demo script and class/demo support material | merged |
| `ml/` | Training and export scaffold for future model work | original SafeDrive-project scaffold plus shared future work |

## Flutter App Boundary

The Flutter app is not just a mock UI. It now supports:

- deterministic scenario playback
- live accelerometer capture from a real Android device
- optional GPS speed for trace recording
- saved trace replay and PASS/CHECK evaluation
- results visualization and report-pack generation

However, Flutter is still not the primary runtime for:

- long-running background monitoring
- Android foreground service reliability
- native SMS/call/TTS crash escalation

## Native Android Boundary

The native Android app is the real MVP runtime and owns:

- `DrivingModeService` foreground operation
- runtime permission flow
- accelerometer, gyroscope, and fused location collection
- risk scoring and crash-like event gating
- Room persistence for trips, events, and crash alerts
- countdown UI and escalation actions

It is the best place to continue technical development for a realistic mobile
prototype.

## Shared Artifacts

The two apps are currently connected at the repository level rather than through
an in-process bridge.

Shared capstone artifacts include:

- evaluation methodology
- demo script
- design and ethics documentation
- future ML workspace
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
