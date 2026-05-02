# Android Native MVP

This app is the real Android runtime prototype inside the merged
`Project-Sentry/` repository.

Use it for:

- foreground driving mode demonstrations
- real sensor and location monitoring
- permission handling walkthroughs
- Room-backed persistence of trips, risk events, and crash alerts
- crash countdown plus SMS, call, and TTS escalation

Core features:

- Jetpack Compose UI for Home, Settings, and Crash Countdown
- `DrivingModeService` foreground service
- accelerometer, gyroscope, and fused location monitoring
- threshold-based risk scoring and crash gating
- advisory ML inference from bundled JSON models
- Room database for local persistence
- configurable demo contacts, demo call number, and TTS template

## Open In Android Studio

Open `android-native/` directly as its own Gradle project. The repository root
can remain `Project-Sentry/` on disk even if Windows blocks casing-only rename;
the product name in docs remains **Project Sentry**.

## Run

From `Project-Sentry/android-native/`:

```powershell
.\gradlew.bat assembleDebug
```

If the Android SDK is installed through Android Studio but Gradle cannot find
it, set `ANDROID_HOME` for the current PowerShell session:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat assembleDebug
```

Or on macOS/Linux:

```bash
./gradlew assembleDebug
```

Android Studio flow:

1. Open `android-native/`.
2. Let Gradle sync dependencies through the committed wrapper.
3. Run on an Android device or emulator with API 26+.
4. Grant location, SMS, phone, and notification permissions.
5. Configure demo-only numbers in Settings before testing crash flow.
6. Start Driving Mode, capture Drive/Status screenshots, then use `Simulate
   Crash` for the countdown screenshot.

If Gradle reports that the Android SDK location is missing, define
`ANDROID_HOME` or create `local.properties` from `local.properties.example`.

## Screenshot Flow

Use this app for the runtime screenshots:

1. Settings: show demo contacts, demo call number, TTS text, and readiness.
2. Drive: start Driving Mode and show live speed, counters, rule risk, and ML
   advisory label/confidence.
3. Countdown: tap `Simulate Crash` and capture the confirmation screen.
4. History: return after a session and show persisted trips/events/alerts.
5. Status: show permission readiness and ML advisory status.

## Notes

- This app is the source of truth for real Android MVP behavior.
- Crash escalation remains rule-based and human-in-the-loop. ML is displayed as
  advisory risk context only.
- Export/report UI is still lighter than the Flutter demo app.
- User-facing branding now uses Project Sentry, while the internal Android
  package name remains `com.safedrive.ai` to avoid a risky namespace refactor.
