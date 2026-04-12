# Evaluation Plan

## Goals

Evaluate whether the MVP provides useful, timely, and reasonably stable safety signals without overwhelming users with false alarms.

## Primary Metrics

- False alarms per driving hour (crash flow and high-severity alerts).
- Detection latency (event/crash trigger to UI alert and action start).
- Event detection quality (precision/recall/F1 for harsh braking, acceleration, cornering, speeding).
- Alert rate per hour and per trip.
- User intervention rate during countdown (cancel vs timeout).

## Secondary Metrics

- Battery impact during continuous foreground operation.
- Sensor sampling stability at target rates.
- Location update continuity and speed estimate reliability.

## Test Scenarios

1. Controlled normal driving baseline (low event rate expected).
2. Controlled risky maneuvers in safe environments (event detection sensitivity).
3. Speed-limit boundary cases (speeding detector behavior).
4. Crash-flow simulation via "Simulate Crash" button.
5. Permission denied/revoked scenarios for SMS, call, location, and notifications.

## Acceptance Targets (Initial)

- Crash simulation reliably starts countdown and timeout path.
- End-to-end simulated alert latency within demo-acceptable bounds.
- Event detectors produce interpretable counters and non-static risk score trends.
- No hardcoded personal or emergency contact data in repository.

## Data Collection and Analysis Notes

- Keep logs minimal and local.
- Use anonymized IDs for sessions/trips.
- Export aggregate metrics for analysis rather than raw personal traces.
