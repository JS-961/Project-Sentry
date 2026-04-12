# System Architecture (MVP)

## Overview

SafeDrive AI is designed as an Android-first, on-device safety assistant with two core capabilities:

1. Crash response escalation flow.
2. Continuous driving-risk estimation and event tracking.

The MVP is privacy-first and local-only, with no cloud dependency.

## Core Components

- UI Layer (Jetpack Compose)
  - Home screen: start/stop driving mode, live score, counters, simulate crash.
  - Settings screen: emergency contacts, demo call number, TTS message template.
  - Crash countdown screen: cancel, "I'm OK", or call-now actions.
- DrivingMode Foreground Service
  - Maintains persistent notification.
  - Manages sensor + location collection.
  - Computes rolling risk score and event detections.
- Sensor Pipeline
  - Accelerometer and gyroscope sampling.
  - Circular buffer for recent high-frequency motion windows.
  - Speed updates from location provider.
- Risk Engine
  - Threshold-based detector for harsh braking/accel, sharp cornering, speeding.
  - Live risk score output in 0-100 range.
- Crash Detection and Escalation Coordinator
  - Crash-like gate from peak acceleration/jerk (optional speed delta).
  - Starts countdown.
  - Timeout path: SMS with location + map link, then demo call intent, then short TTS intro.
- Data Layer (Room)
  - Persists trips, events, risk snapshots, and alert attempts.

## Data Flow

1. User starts Driving Mode from Home screen.
2. Foreground service starts and begins sensor/location collection.
3. Risk engine processes streaming windows and updates live score + event counters.
4. Detected events are persisted to Room and reflected in UI.
5. Crash-like event (or simulate button) triggers countdown UI.
6. If no user response before timeout:
   - Send SMS to configured contacts with latest location and maps link.
   - Launch call intent to configured demo number.
   - Speak short TTS intro and leave call flow to OS.

## Local Data Model (Initial)

- AppSettings: contacts, demo number, TTS template, thresholds.
- Trip: start/end time, distance, average speed, score summary.
- RiskEvent: type, timestamp, severity, contextual speed/location.
- CrashAlertAttempt: countdown outcome, SMS/call/TTS status.

## Security and Privacy Notes

- Only minimum data required for local operation is stored.
- No location/phone secrets should be committed to version control.
- The app is demo-focused and must avoid real emergency numbers in defaults.
