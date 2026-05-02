# Setup And Screenshot Guide

Use this guide when preparing final report screenshots. The full figure-by-figure
capture runbook lives in `docs/Final_Report/figures-to-capture.md`.

Keep demo data clean: no real phone numbers, real emergency contacts, sensitive
locations, or real emergency-service targets.

## Flutter Demo App

Use Flutter for the polished capstone demo, scenario lab, validation tools,
results screens, report exports, and presentation mode.

Prerequisites:

- Flutter SDK
- Chrome or Windows desktop target for quick screenshots
- Android SDK and Android device if using live sensor capture

Run from the repository root:

```powershell
cd flutter-app
flutter pub get
flutter run -d chrome
```

Useful alternatives:

```powershell
flutter run -d windows
flutter run -d android
flutter test
```

Screenshot order:

1. Scenario Lab with a deterministic playback result.
2. Validation Tools/live capture card with the location-permission note.
3. Results Summary with metrics and export actions.
4. Presentation Mode for the class-demo view.

## Android Native MVP

Use Android native for the real runtime MVP: foreground service monitoring,
permissions, sensors, fused location, Room persistence, advisory ML, countdown,
SMS/call/TTS escalation, and status/history views.

Prerequisites:

- Android Studio or Android SDK command-line setup
- JDK 17
- Android device or emulator with API 26+
- demo-only contact numbers and call target

Run from the repository root:

```powershell
cd android-native
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat assembleDebug
```

Then open `android-native/` in Android Studio, run the app on an emulator or
phone, grant required permissions, and configure demo-only numbers in Settings.

Verification commands:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

Screenshot order:

1. Settings with demo contacts, demo call number, TTS, and readiness.
2. Drive tab during active monitoring with speed, counters, rule risk, ML
   advisory label/confidence, and `Simulate Crash`.
3. Crash countdown after tapping `Simulate Crash`.
4. History after a monitored session.
5. Status with permissions, crash-flow readiness, and ML advisory status.

## Survey Screenshots

Use the survey form/results tool used by the team. Capture:

1. response overview or total-response count,
2. charts for willingness/trust/privacy questions,
3. any free-response theme summary if it is safe to show,
4. a cropped or redacted view if names, emails, phone numbers, or private
   identifiers appear.

Suggested filenames:

- `fig-16-survey-response-overview.png`
- `fig-17-survey-response-charts.png`
- `fig-18-survey-open-ended-themes.png`
