# Repository Guide

This repository is intentionally split by responsibility.

## Use `flutter-app/` When

- you need a polished class demo
- you want deterministic scenario playback
- you want live trace capture and replay for validation
- you need results dashboards, exports, or report packs
- you want the most presentation-friendly UI

## Use `android-native/` When

- you need real Android foreground monitoring
- you need to show permissions, sensors, and location behavior
- you need Room-backed persistence
- you need the countdown plus SMS, call, and TTS crash flow
- you are continuing technical MVP development

## Recommended Class Demo Order

1. Start with `flutter-app/` for a smooth overview and scenario-based story.
2. Show Presentation Mode or Results to make the capstone evidence clear.
3. If time allows, switch to `android-native/` to demonstrate the real Android
   runtime and explain why it remains separate.

## Recommended Engineering Focus

- continue product and runtime work in `android-native/`
- continue capstone evidence, validation tooling, and polished demo work in
  `flutter-app/`
- keep shared methodology and future ML notes in `docs/` and `ml/`
