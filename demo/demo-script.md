# Project Sentry Demo Script

This repository supports two demo tracks.

## Pre-Demo Setup

- Use placeholder or demo-only phone numbers.
- Do not use real emergency contacts or emergency services.
- Keep permissions ready on the Android native app if you plan to show the real
  runtime.
- If using Flutter live capture, use a safe Android device and controlled test
  motion only.

## Recommended Class Demo: Flutter First

Use `flutter-app/` when time is limited and you want the strongest overall
capstone story.

1. Open the Flutter app and explain that it is the presentation, simulation, and
   reporting layer.
2. Show Home, Lab, and Results tabs.
3. Enter Scenario Lab and run a scripted scenario.
4. If relevant, show the live trace capture tooling and explain that it supports
   safe validation on real Android hardware.   
5. Open Presentation Mode for a clean walkthrough.
6. Open Results and show report/export options.
7. Close by explaining that this app is optimized for reproducible validation
   and capstone evidence.

## Technical Follow-Up Demo: Native Android

Use `android-native/` when you want to prove the real MVP runtime exists.

1. Open the native Android app and explain that it is the runtime source of
   truth.
2. Start Driving Mode.
3. Show live risk score and counters.
4. Point out the ML advisory label/confidence as context, while explaining that
   crash escalation remains rule-based.
5. Trigger `Simulate Crash`.
6. Show the countdown UI and the response options.
7. Explain the timeout path:
   - SMS with a location link
   - call flow intent
   - local TTS intro
8. Explain that Room persists trip, risk, and crash-alert data locally.

## Suggested Talking Points

- We intentionally kept two apps because each one solves a different capstone
  problem well.
- Flutter gives polished simulation, validation, and reporting.
- Native Android gives real service-based monitoring and crash escalation.
- Advisory ML provides driver-risk and road-condition context without owning
  emergency escalation.
- The repository is privacy-first and local-only for the MVP.
- The system is still a capstone MVP, not a certified emergency product.

## Fallback Plan

- If Android permissions are denied, explain the graceful fallback behavior.
- If location is unavailable, explain that demos must tolerate missing speed or
  location context.
- If time is short, demo Flutter only and explain that native Android is the
  deeper technical runtime.
