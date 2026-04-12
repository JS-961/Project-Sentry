# Validation + Research Plan

Date: 2026-04-11

## Current App Reality

- This note started as a planning document before live capture landed in the
  Flutter app.
- `flutter-app/lib/services/simulated_sensor_service.dart` still supports
  deterministic and injected validation flows.
- `flutter-app/lib/services/scenario_library.dart` still provides hand-authored
  scenarios such as `collision`, `rear_end`, `pothole_hit`, and `phone_drop`.
- Live capture and replay are now implemented under `flutter-app/lib/services/`
  and documented in `docs/research/validation-workflow.md`.
- Important implementation detail: the current detection thresholds behave like
  `g` units, while `sensors_plus` accelerometer streams are documented in
  `m/s^2`. The current Flutter implementation explicitly converts to `g`.

## 1. Real Accelerometer Data

### Best External Sources Found

1. `jair-jr/driverBehaviorDataset`
- Public GitHub dataset with labeled aggressive and non-aggressive maneuvers.
- Includes accelerometer, linear acceleration, gyroscope, magnetometer, and ground-truth event windows.
- The repository states the phone was fixed on the windshield and labels were created from synchronized video review.
- Useful for: false-positive analysis, aggressive braking/turning baselines, and CSV import experiments.
- Limitation: this is not a true crash dataset.

2. `Harnessing Smartphone Sensors for Enhanced Road Safety: A Comprehensive Dataset and Review` (Scientific Data, published March 10, 2025)
- Open road-safety dataset collected from smartphone sensors.
- Covers road anomalies and driving behavior.
- Uses accelerometer, gyroscope, magnetometer, GPS, gravity, orientation, and uncalibrated sensors.
- Reports an average sampling rate of `89.82 Hz`, with observed rates from `60 Hz` to `99 Hz`.
- Useful for: realistic sampling assumptions, pothole/bump discrimination, CSV schema ideas, and non-crash driving traces.
- Limitation: still not a direct public severe-collision dataset.

3. Damoov / Mobile-Telematics demo apps
- Open-source telematics sample apps and SDK demos for Android and React Native.
- Good benchmark for trip tracking, permissions, telematics UX, and feature scope.
- Useful for: implementation reference and market benchmarking.
- Limitation: not a raw open crash dataset for model training.

### What The External Research Suggests

- Public smartphone datasets are much stronger for `aggressive driving`, `road anomaly detection`, and `trip telemetry` than for true severe-collision ground truth.
- Google Pixel's official crash-detection material says the system compares accelerometer, GPS, and microphone data against real and simulated crash data.
- Apple and Google both ship crash detection on consumer devices, which is strong evidence that the product category is technically feasible.

Inference from the sources:
- We should not expect a clean, open, public smartphone crash dataset that is good enough to fully validate this app by itself.
- The practical path is a hybrid one:
  - use public datasets for non-crash negatives and aggressive-maneuver edge cases
  - collect our own controlled traces on Android devices for the exact phone placement and sampling setup we support

### Recommended Technical Plan

#### Phase A: Plug In Real Sensor Values

- Add a `LiveSensorService` that reads from `sensors_plus`.
- Convert raw accelerometer values from `m/s^2` to `g` before creating `SensorSample`, or refactor the full detection pipeline to SI units.
- Keep the simulator and scenario library in place for deterministic tests.

Recommended first live stream:
- Start with `accelerometerEventStream()` plus speed input.
- Keep gravity included at first so it matches the current `SensorSample` shape, which assumes a baseline near `1.0 g` on the vertical axis.
- Consider `userAccelerometerEventStream()` later if we decide to redesign the feature set around gravity-removed motion.

#### Phase B: Record Real Trips

- Add a local trace recorder that exports timestamped sensor rows to CSV.
- Record these fields at minimum:
  - timestamp
  - ax
  - ay
  - az
  - magnitude
  - speed
  - scenario label
  - phone placement
  - device model
- Make the recorder usable from a hidden dev screen or scenario lab action.

#### Phase C: Replay Recorded Traces

- Add a `RecordedSensorService` that can replay a CSV trace as `SensorSample` values.
- This lets the app test real-world traces with the same `TripController` and `DetectionEngine` already used by the simulator.
- This is the cleanest bridge from "live testing" to repeatable regression tests.

### Controlled Data Collection Matrix

Collect safe, non-collision traces first:

- Stationary engine idle
- Normal city driving
- Highway cruising
- Speed bump
- Pothole / rough road
- Hard brake
- Sharp turn / lane change
- Phone pickup / passenger handling
- Phone drop while not driving

Recommended capture constraints:

- Start with one supported placement: windshield/dashboard mount.
- Start with two Android phones if available.
- Record at least 10 runs per scenario.
- Never attempt deliberate crash recreation.

Metrics to log per run:

- Peak acceleration magnitude
- Peak jerk
- Speed at peak
- Whether current thresholds would trigger
- False positive / false negative outcome
- Battery drain over session

### Which Local GitHub Links Matter Most

From `docs/research/github-links.md`, the most useful items for this workstream are:

- `jair-jr/driverBehaviorDataset`
- `Mobile-Telematics/telematicsSDK-demoapp-Android-java`
- `Mobile-Telematics/TelematicsApp-Android`

Working assumption:
- The accident-alert app repos in the list are more likely to help with workflow and UX ideas than with reusable raw sensor datasets.

## 2. Feasibility, Practicality, and Market Demand

### Why The Problem Is Real

- WHO reports that road traffic crashes cause about `1.19 million` deaths per year worldwide, with `20 to 50 million` more non-fatal injuries.
- WHO also states road crashes cost many countries about `3%` of GDP.
- NHTSA reported `39,254` traffic deaths in the United States in `2024`.
- In the same April 1, 2026 NHTSA release, the agency states that distracted-driving crashes injure `18 people every half hour` and kill `one person about every 2.5 hours`.

### Why A Smartphone App Is Practical

- Pew Research Center reported on November 20, 2025 that `91%` of U.S. adults own a smartphone.
- `sensors_plus` already exposes accelerometer and user-accelerometer streams in Flutter, with configurable sampling periods.
- Google and Apple both deploy crash-detection features on mainstream consumer devices, which is strong evidence that phone-based safety sensing is commercially practical.

### Why There Is Market Demand

- Life360 described itself as the market-leading family safety and connection app in its August 11, 2025 earnings release.
- In that same release, Life360 reported approximately `88.0 million` global monthly active users and `2.5 million` paying circles in Q2 2025.

Inference from the sources:
- There is already proven user demand for mobile safety products, especially when they combine sensing, emergency workflows, and family notification.
- The harder part is not "is there a market?" but "what trustworthy niche can this project own that big platforms do not already cover?"

## 3. Research Data We Should Collect Ourselves

### A. Feasibility Data

Collect this from app testing:

- sensor sampling stability by phone model
- battery drain over 15, 30, and 60 minutes
- effect of phone placement on false triggers
- effect of road anomalies on false triggers
- effect of low-speed handling events like drops and pickups
- threshold ranges that separate hard braking from obvious non-crash events

### B. Practicality Data

Collect this from short field sessions and interviews:

- whether users are willing to keep the phone mounted while driving
- whether they are comfortable granting motion and location permissions
- whether they trust automatic alerts
- whether they prefer countdown confirmation before notifying contacts
- what failure is worse to them: missing an event or false alarms

### C. Market Validation Data

Run a lightweight survey with drivers:

- target sample: 30-50 respondents
- primary segment: students, commuters, and parents who drive regularly
- optional secondary segment: taxi/delivery drivers

Suggested survey questions:

- How often do you drive each week?
- Have you ever been in a crash or near-miss?
- Would you install a phone app that detects severe collisions and starts an emergency countdown?
- Would you allow continuous motion sensing while driving?
- Would you allow location sharing only during a detected emergency?
- What concerns you most: battery, privacy, false alarms, or reliability?
- Would you trust alerts to emergency contacts more than direct emergency-service calling?
- Would you pay for this feature, or only use it if free?

### D. Competitor Snapshot To Build

Make a simple comparison sheet with:

- Google Pixel Crash Detection
- Apple Crash Detection
- Life360 safety features
- any Lebanon- or MENA-relevant emergency/safety apps we can identify later

Compare:

- hardware dependency
- automatic calling support
- emergency-contact workflow
- privacy posture
- sensor usage
- pricing
- platform coverage

## 4. Recommended Near-Term Deliverables

This is the most efficient order for the next sprint:

1. Add `LiveSensorService` with proper unit conversion.
2. Add CSV recording for real trips.
3. Add CSV replay into the scenario lab.
4. Collect safe baseline traces for potholes, hard braking, phone drops, and normal driving.
5. Run a small survey/interview round for privacy, trust, and adoption signals.
6. Build a one-page competitor matrix from official product pages.

## Sources

- `sensors_plus` Flutter docs:
  - https://pub.dev/documentation/sensors_plus/latest/sensors_plus/AccelerometerEvent-class.html
  - https://pub.dev/documentation/sensors_plus/latest/sensors_plus/accelerometerEventStream.html
  - https://pub.dev/documentation/sensors_plus/latest/sensors_plus/userAccelerometerEventStream.html
- `driverBehaviorDataset`:
  - https://github.com/jair-jr/driverBehaviorDataset
- Scientific Data dataset paper:
  - https://www.nature.com/articles/s41597-024-04193-0
  - https://pubmed.ncbi.nlm.nih.gov/40064898/
- Google Pixel crash detection:
  - https://store.google.com/us/magazine/car-crash-detection?hl=en-US
  - https://support.google.com/googlepixelwatch/answer/12663810
- Apple crash detection:
  - https://support.apple.com/en-afri/guide/iphone/iph948a628e9/ios
- WHO road traffic injuries fact sheet:
  - https://www.who.int/news-room/fact-sheets/detail/road-traffic-injuries/
- NHTSA traffic deaths / distracted driving release:
  - https://www.nhtsa.gov/press-releases/traffic-deaths-2025-early-estimates-2024-annual
- Pew mobile fact sheet:
  - https://www.pewresearch.org/internet/fact-sheet/mobile/
- Life360 Q2 2025 results:
  - https://life360.gcs-web.com/news-releases/news-release-details/life360-reports-record-q2-2025-results
- Damoov / Mobile-Telematics repositories:
  - https://github.com/Mobile-Telematics/TelematicsApp-Android
  - https://github.com/orgs/Mobile-Telematics/repositories
