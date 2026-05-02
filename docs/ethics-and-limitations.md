# Ethics And Limitations

## Prototype Status

Project Sentry is an educational capstone prototype. It is not a certified
emergency-response system and must not be presented as one.

## Core Limitations

- False positives can trigger unnecessary alerts.
- False negatives can miss real incidents.
- Phone placement and sensor quality materially affect results.
- Flutter and native Android are intentionally separate layers today.
- The ML model is deployed only as an advisory risk signal and should not be
  described as production-grade crash detection.

## Safety Rules For Testing

- Never perform real crash tests.
- Never encourage dangerous driving.
- Use simulation, controlled low-risk maneuvers, and safe validation traces.
- Use placeholder or demo-only phone numbers.
- Do not auto-dial or contact real emergency services during demos.

## Privacy Principles

- Keep data local by default.
- Collect only the minimum data needed for validation and runtime behavior.
- Do not commit real phone numbers, personal identifiers, or sensitive location
  traces.
- Explain clearly when local databases or trace files contain location data.

## Android-Specific Constraints

- Android and carriers may restrict or alter call placement behavior.
- SMS, call, notification, and location behavior depends on runtime permission
  state.
- Foreground service behavior can vary by OS version and vendor policies.

## Repository Communication Rules

When presenting or documenting this project:

- describe `flutter-app/` as the demo, simulation, validation, and reporting
  layer
- describe `android-native/` as the real Android MVP runtime
- describe `ml/` as the advisory training/export workspace, with Android
  crash escalation still controlled by rule-based validation

## Human Factors

- Keep the countdown and cancellation options visible.
- Prefer human-in-the-loop confirmation before any outreach action.
- Make limitations explicit during demos to avoid overstating system safety.
