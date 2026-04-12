# SafeDrive AI

SafeDrive AI is an Android-first capstone concept focused on:

1. Crash response assistance (countdown, SMS location alert, and demo call flow).
2. Driving-risk prevention (live risk scoring from phone sensors plus GPS speed).

This repository now includes an Android MVP implementation in `android-app/` plus docs and ML scaffolding.

## Implemented Android MVP

- Crash response flow
  - Crash-like detection gate using acceleration/jerk with optional speed-drop gate
  - Simulate Crash button to trigger the same flow
  - 12s "Are you OK?" full-screen countdown with Cancel, I'm OK, and Call Now
  - Timeout or Call Now path: SMS with maps link, then call flow intent, then local TTS intro
- Risk prevention flow
  - Foreground Driving Mode service with persistent notification
  - Accelerometer + gyroscope sampling with circular buffer
  - Location/speed updates and threshold-based event detection
  - Live risk score (0-100) and event counters on Home screen
- Privacy-first design
  - No cloud backend required for MVP
  - Room database for trips, risk events, and crash alert attempts
  - Placeholder contact values only; no real personal data in repo

## Repository Structure

```text
SafeDrive-project/
|-- android-app/
|-- ml/
|   |-- data/
|   |   `-- README.md
|   |-- notebooks/
|   |-- src/
|   |-- models/
|   |-- requirements.txt
|   `-- README.md
|-- docs/
|   |-- system_architecture.md
|   |-- ethics_and_limitations.md
|   |-- evaluation_plan.md
|   `-- references.md
|-- demo/
|   `-- demo_script.md
`-- README.md
```

## Disclaimers

- This project is for research/demo purposes and is not a certified emergency system.
- Do not hardcode or auto-dial real emergency numbers in demos.
- Android may restrict emergency-related calling behavior and can require user confirmation.
- Call/TTS behavior depends on OS, dialer, and permission state.
- Sensor-based crash/risk detection can produce false positives and false negatives.

## Run (Android)

1. Open `android-app/` in Android Studio.
2. Let Gradle sync dependencies.
3. Run the app on an emulator or physical Android device (API 26+).
4. Grant runtime permissions when prompted (location, SMS, call, notifications).
5. Set demo contacts/numbers in Settings before running crash flow tests.

## Next Steps

1. Add end-of-trip summary UI backed by Room queries.
2. Add TensorFlow Lite wrapper + placeholder model integration in Android.
3. Expand `ml/src/` training/export scripts and connect model output to risk engine.
