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

If Gradle reports that the Android SDK location is missing, define
`ANDROID_HOME` or create `local.properties` from `local.properties.example`.

## Notes

- This app is the source of truth for real Android MVP behavior.
- Export/report UI is still lighter than the Flutter demo app.
- User-facing branding now uses Project Sentry, while the internal Android
  package name remains `com.safedrive.ai` to avoid a risky namespace refactor.
