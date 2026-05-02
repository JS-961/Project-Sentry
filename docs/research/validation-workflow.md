# Validation Workflow

Date: 2026-04-11

## What Was Added

The Flutter app now supports three validation paths inside the Lab tab:

- scripted scenario playback
- live accelerometer capture from a real Android phone
- replay of saved sensor traces with automatic evaluation

This work is centered in:

- `flutter-app/lib/screens/scenario_lab_screen.dart`
- `flutter-app/lib/services/live_sensor_service.dart`
- `flutter-app/lib/services/recorded_sensor_service.dart`
- `flutter-app/lib/services/sensor_trace_codec.dart`
- `flutter-app/lib/services/trace_evaluator.dart`

## How To Use It

1. Open the app on an Android phone.
2. Go to `Lab`.
3. In `Validation Tools`, enter:
   - `Trace label`
   - `Phone placement`
   - `Expected replay result`
4. Tap `Start Live Capture`.
5. Perform a safe test activity.
6. Tap `Stop & Save Live Trace`.
7. Select the saved trace from the dropdown.
8. Tap `Replay Selected Trace`.
9. Read the `Trace Results` card.

The replay result shows:

- expected trigger
- actual trigger
- peak acceleration in `g`
- peak jerk in `g/s`
- peak speed in `km/h`
- sample count
- duration

## Safe Test Scenarios

Use these first:

- `normal_drive_1`
- `hard_brake_1`
- `rough_road_1`
- `phone_handling_1`
- `seat_setdown_1`
- `passenger_handling_1`

Do not do deliberate crash recreation.
Do not literally drop an unprotected phone.

Safer proxy negatives:

- pick up the mounted phone and set it back down
- place the phone on a cushioned seat
- place the phone inside a padded bag and drop the bag a short distance
- have a passenger handle the phone while the car is stationary

## Trace File Notes

Saved traces are stored in the app documents directory under:

- `sensor_traces/`

Each trace file stores:

- trace label
- phone placement
- device label
- expected trigger
- timestamped sensor samples

## Current Limitations

- live speed depends on location permission and GPS availability
- thresholds are still heuristic and need calibration against real traces
- Flutter replay evaluation remains rule-based; Android now also exposes an
  advisory ML risk label that should be compared against safe captured traces
- trace management is local-only for now

## Recommended Next Steps For Handoff

1. Collect 10 safe baseline traces with one consistent phone placement.
2. Replay each trace and record whether the current thresholds behaved correctly.
3. Tune thresholds using false-positive data before attempting any stronger trigger cases.
4. Build a simple exported summary table from replay results if the team needs cleaner reporting.
