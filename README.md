# Project Sentry

Project Sentry is a hybrid capstone repository that intentionally keeps two
complementary apps in one place:

- `flutter-app/` is the polished simulation, validation, presentation, and
  reporting layer.
- `android-native/` is the real Android MVP runtime with foreground service,
  permissions, sensors, Room persistence, and crash escalation flow.

This repository does not try to force a risky full bridge between them yet.
Instead, it preserves the strongest part of each original project in a clean,
defensible structure for capstone delivery and continued technical development.

On disk, the folder may remain `Project-Sentry/` if Windows prevents a
casing-only rename. In documentation and presentation, the product name is
**Project Sentry**.

## Why This Repo Contains Two Apps

The two codebases solve different problems well:

- Flutter is better for fast demos, deterministic scenario playback, results
  dashboards, presentation mode, and capstone evidence generation.
- Native Android is better for real device monitoring, background reliability,
  runtime permissions, local persistence, and crash response behavior.

Keeping both makes the capstone stronger:

- one app is easy to present in class
- one app is closer to a realistic mobile MVP

## Repository Layout

- `flutter-app/` - simulation, scenario lab, live trace capture, replay,
  presentation mode, and export/report tooling
- `android-native/` - real Android MVP runtime with service-based monitoring
- `docs/` - unified capstone architecture, evaluation, ethics, references, and
  repository guidance
- `demo/` - demo script and demo asset staging area
- `ml/` - shared model training/export scaffold
- `_imports/` - imported working copies of the two original projects
- `_backup/` - backup copies of both originals before restructuring

## Which App To Use

Use `flutter-app/` when you need:

- a polished class demo
- deterministic scenario playback
- live trace capture and replay for validation
- results dashboards and report-pack outputs
- presentation mode and demo-friendly UI

Use `android-native/` when you need:

- real Android sensor and location monitoring
- foreground service behavior
- runtime permissions
- Room-backed trip, event, and crash alert persistence
- SMS, call, and TTS crash escalation flow

## Run The Flutter Demo App

Prerequisites:

- Flutter SDK
- an emulator, desktop target, or Android device

Typical flow:

```bash
cd flutter-app
flutter pub get
flutter run
```

Demo-first walkthrough:

1. Home
2. Lab
3. Results
4. Presentation Mode

Best use cases:

- Scenario Lab demos
- presentation mode
- results/report export walkthrough
- controlled validation with saved traces

Notes:

- Flutter includes live sensor capture and replay tooling for validation, but it
  is not the source of truth for background crash monitoring or native Android
  reliability.
- if Android-target Gradle setup cannot find the Flutter SDK or Android SDK,
  use `flutter-app/android/local.properties.example` as the local template

## Run The Native Android MVP

Prerequisites:

- Android Studio
- JDK 17
- Android SDK
- Android device or emulator, API 26+

Typical flow:

```powershell
cd android-native
.\gradlew.bat assembleDebug
```

Android Studio flow:

1. Open `android-native/` in Android Studio.
2. Let Gradle sync dependencies through the committed wrapper.
3. Run the app.
4. Grant runtime permissions for location, SMS, phone, and notifications.
5. Set demo contacts and demo call number in Settings before showing crash flow.

Notes:

- The native app is the real functional MVP.
- The internal Android package name remains `com.safedrive.ai` for stability,
  but user-facing branding is normalized to Project Sentry.
- if Gradle cannot find the Android SDK, use
  `android-native/local.properties.example` as the local template

## Shared Docs And Demo Material

Start here:

- `docs/architecture.md`
- `docs/repository-guide.md`
- `docs/requirements-traceability.md`
- `docs/evaluation-testing-plan.md`
- `demo/demo-script.md`

## Current Limitations

- There is no production Flutter-to-native bridge yet by design.
- `flutter-app/` is still demo-first, even though it now includes live capture
  and trace replay for validation.
- `android-native/` is the runtime MVP, but it still needs richer history and
  export UI.
- `ml/` is scaffold only and should not be presented as deployed intelligence.
- local setup still requires Flutter SDK and Android SDK installation on the
  developer machine

## Privacy And Safety

- This is a prototype, not a certified emergency system.
- Use placeholder or non-emergency numbers only during demos.
- Do not perform dangerous driving or real crash recreation.
- Keep data local and collect only what is necessary for testing.

## Merge Provenance

Merged from:

- original `Project-Sentry/` for Flutter UI, scenario lab, validation tooling,
  presentation mode, exports, tests, and report-oriented docs
- original `SafeDrive-project/` for the native Android runtime, service layer,
  Room, permissions, and crash escalation flow
