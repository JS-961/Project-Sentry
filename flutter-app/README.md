# Flutter Demo App

This app is the polished capstone-facing layer inside the merged
`Project-Sentry/` repository.

Use it for:

- scenario playback and deterministic validation
- live Android accelerometer capture
- saved trace replay with PASS/CHECK evaluation
- presentation mode and polished UI walkthroughs
- results dashboards and report-pack exports

Core features:

- Home, Lab, Results, Settings, and Presentation screens
- scenario lab with repeatable sensor playback
- live sensor capture with optional GPS speed when permission is available
- saved trace storage and replay
- adjustable detection thresholds
- event log plus JSON, CSV, Markdown, HTML, and ZIP report outputs
- automated tests for detector logic, log export, codec, and trace evaluation

## Run

From `Project-Sentry/flutter-app/`:

```powershell
flutter pub get
flutter run
```

Useful targets:

- `flutter run -d chrome` for a quick browser demo
- `flutter run -d windows` for a local desktop presentation
- `flutter run -d android` for live sensor capture on an Android device

If Android-target Gradle setup complains about missing SDK paths, create
`flutter-app/android/local.properties` using `local.properties.example` and set
your local `flutter.sdk` and `sdk.dir` values.

The Android subproject stays under `flutter-app/android/` and is meant to be
used through Flutter tooling, not as the primary Android Studio project.

## Demo Entry Flow

For class presentation, the smoothest flow is:

1. Home
2. Lab
3. Results
4. Presentation Mode

That sequence shows the capstone story clearly: simulate, validate, summarize,
then present.

## Screenshot Flow

Use this app for the report/presentation screenshots:

1. Home: show the polished entry state if needed for presentation context.
2. Lab: run a deterministic scenario and capture the PASS/CHECK result.
3. Validation Tools: show the live-capture card and location-permission note.
4. Results: show aggregate counts, metrics, and export/report actions.
5. Presentation Mode: capture the class-demo walkthrough screen.

For the cleanest screenshot pass, use `flutter run -d chrome` or
`flutter run -d windows`. Use `flutter run -d android` only when you need live
sensor capture from a physical phone.

## Notes

- This app is the source of truth for demo, presentation, simulation, and
  validation workflows.
- It is not the source of truth for the production-style Android service stack;
  use `../android-native/` for real runtime behavior.
