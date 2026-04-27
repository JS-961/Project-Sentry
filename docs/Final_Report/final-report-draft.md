# Project Sentry Final Report Draft

This draft follows the CSC599 final report structure extracted from `CSC599 - Grading Rubric (1).pdf` on April 26, 2026. It is written from repository evidence only. Unsupported claims are intentionally marked with `TODO`.

## Preliminary Pages

### Title Page

- Project Title: **Project Sentry**
- Student Name(s): **TODO**
- Student ID(s): **TODO**
- Supervisor/Advisor: **TODO**
- Department: **TODO**
- Course: **CSC599 Capstone Project**
- Semester: **TODO**
- Submission Date: **TODO**

### Signature Page

- Student signatures: **TODO**
- Supervisor/advisor signature: **TODO**
- Coordinator signature: **TODO**

### Acknowledgments

**TODO:** Add acknowledgments if required by the department or supervisor.

### Abstract

Project Sentry is a smartphone-based road-safety prototype that aims to detect crash-like events and risky driving behavior using onboard sensors, speed context, and a human-in-the-loop emergency escalation flow. The project began with a Flutter-based approach because Flutter enabled rapid UI prototyping, deterministic scenario simulation, report-oriented presentation screens, and reusable validation tooling. During technical exploration, the team determined that the safety-critical runtime responsibilities of the system were better suited to a native Android implementation. In particular, foreground/background driving monitoring, sensor reliability, Android runtime permissions, fused location updates, telephony-based emergency actions, text-to-speech escalation, and local persistence are all tightly coupled to Android platform behavior. As a result, the final capstone direction treats the Android native application as the main technical MVP, while retaining the Flutter application as an early prototype, simulation environment, and documentation/presentation layer.

The current repository demonstrates both sides of that architecture. The Android native MVP includes a foreground driving service, accelerometer and gyroscope monitoring, fused location tracking, staged crash validation, a countdown confirmation screen, SMS/call/TTS escalation, and Room-backed storage of trips, risk events, and crash alert outcomes. The Flutter application provides scenario playback, live trace capture, replay-based validation, automated tests for several core modules, and report-pack export features useful for capstone evidence generation. The project therefore contributes a realistic, engineering-driven foundation for mobile crash detection while staying honest about present limitations. Final quantitative evaluation, full physical-device testing evidence, user study results, screenshots, and production-grade machine-learning integration remain future work. `TODO: Update the abstract with final measured testing results before submission.`

### Table of Contents

**TODO:** Generate automatically after the report is finalized in Word or LaTeX.

### List of Figures

**TODO:** Populate after screenshots and exported diagrams are finalized.

### List of Tables

**TODO:** Populate after table numbering is finalized.

## 1. Introduction

### 1.1 Background and Motivation

Road traffic injuries remain a serious public-safety problem. The research notes already preserved in this repository cite the World Health Organization's road-safety material and U.S. NHTSA traffic-safety releases as evidence that crashes, distracted driving, and delayed response remain important practical concerns. At the same time, smartphones are widely available and contain accelerometers, gyroscopes, location services, storage, and communication features that make them attractive as low-cost safety-monitoring platforms. The project's research notes also cite high smartphone ownership rates and the existence of commercial crash-detection features from major consumer platforms, which together suggest that mobile-device-based incident detection is both socially relevant and technically plausible. `TODO: Convert these source mentions into full IEEE or APA citations in the final version.`

### 1.2 Problem Statement

The problem addressed by Project Sentry is the need for an accessible mobile system that can monitor driving behavior, detect potentially severe impact events, and initiate a controlled emergency-response workflow without requiring dedicated in-vehicle hardware. The challenge is not only to detect abnormal motion, but also to do so in a way that is transparent, testable, privacy-conscious, and practical on real Android devices. Crash-like motion can be confused with harsh braking, potholes, phone handling, or cornering, so the system must balance sensitivity with false-alarm reduction. In addition, the application must operate within Android's foreground-service, permission, and notification constraints.

### 1.3 Practical and Research Gap

The repository evidence suggests two distinct gaps.

First, there is a practical product gap. Commercial crash-detection features exist, but they are often tied to particular hardware ecosystems, closed implementations, or premium devices. For an educational capstone, that makes it difficult to demonstrate the underlying logic, inspect the tradeoffs, or adapt the workflow to a different mobile context.

Second, there is a research and engineering gap. The project's validation notes show that public smartphone datasets are far stronger for aggressive-driving and road-anomaly analysis than for true severe-collision ground truth. This means that a capstone team cannot simply download one complete dataset and claim full validation. A more defensible path is to combine transparent heuristics, safe scenario simulation, controlled trace collection, and careful limitation reporting.

There is also a software-architecture gap specific to this project. Cross-platform UI technology is useful for prototyping and presentation, but the final system also requires Android-native capabilities such as foreground/background service behavior, reliable sensor monitoring, Android permission handling, fused location updates, SMS/call/TTS escalation, and local persistence. This gap directly motivated the final pivot in technical direction.

### 1.4 Project Objectives

The current project objectives are:

1. Design a smartphone-based system that can monitor driving-related signals and flag crash-like events or risky driving conditions.
2. Provide a native Android MVP capable of running foreground monitoring, handling permissions, and performing local emergency escalation actions.
3. Provide a separate validation and presentation layer that supports scenario playback, trace capture, replay, and report generation.
4. Keep the system privacy-conscious by storing data locally and avoiding unnecessary cloud dependency in the MVP stage.
5. Document limitations honestly and avoid overstating production readiness, machine-learning maturity, or emergency-service reliability.
6. Leave a clear path for future machine-learning integration, richer reporting, and broader validation.

### 1.5 Significance and Potential Impact

Project Sentry is significant in three ways. From an educational perspective, it demonstrates how mobile sensing, event detection, UI workflow design, local persistence, and platform constraints interact in one capstone project. From an engineering perspective, it shows that a safety-oriented prototype can be structured honestly by separating simulation/reporting concerns from runtime-critical Android behavior. From a practical perspective, it offers a low-cost foundation that could be extended into a stronger telematics, safe-driving, or crash-response research platform, especially in environments where specialized vehicle hardware is unavailable.

## 2. Literature Review / Related Work

This section is intentionally conservative. It uses only source-backed notes already present in the repository and therefore should be expanded with more peer-reviewed citations before final submission.

### 2.1 Existing Crash Detection Systems

The research notes preserved in `docs/research/validation-research-plan.md` identify Apple Crash Detection and Google Pixel crash-detection features as important commercial reference points. Their relevance is not that Project Sentry reproduces those proprietary systems, but that they demonstrate the real-world viability of mobile-device crash detection. The same notes also mention Life360 as an example of a large-scale consumer safety platform that combines sensing, family safety, and emergency workflow ideas. Together, these systems establish that the application domain is real, commercially valuable, and technically feasible. `TODO: Add full citation entries from official Apple, Google, and Life360 sources.`

### 2.2 Smartphone-Based Crash Detection

Smartphone-based crash detection typically relies on some combination of inertial sensing, location context, speed change, and event thresholds. The Flutter prototype in this repository already implements a simple threshold detector based on acceleration magnitude, jerk, minimum speed, and cooldown. The Android native MVP takes a more defensive approach by combining sensor impact evidence with speed-based validation before launching the crash countdown. This aligns with the broader idea in the literature that raw sensor spikes alone are not enough to distinguish true collisions from normal driving anomalies.

The repository's research notes also cite Google's official product material, which reportedly uses multiple modalities and data sources. That comparison is useful because it highlights a limitation of capstone-scale work: the final system here is still a heuristic prototype, not a production classifier trained on large proprietary datasets.

### 2.3 Driver Behavior and Risk Prediction

The preserved research notes identify aggressive-driving datasets and telematics-oriented datasets as more available than true open crash datasets. For example, the notes mention `jair-jr/driverBehaviorDataset` and a 2025 Scientific Data paper on smartphone-sensor road-safety data. These sources are valuable because they support risk-event analysis such as harsh braking, acceleration, turning, and anomaly handling, even if they do not provide sufficient severe-collision ground truth on their own. This is directly relevant to Project Sentry because the Android MVP already tracks harsh braking, harsh acceleration, sharp cornering, and speeding as risk events in addition to crash-like events.

### 2.4 Telematics and Safe-Driving Apps

The repository references several telematics demo applications and benchmark repositories. Their value is mostly architectural and UX-oriented: trip tracking, permission handling, event logging, and safe-driving dashboards are recurring themes. Project Sentry does not currently implement a full telematics product, but it shares several of the same core concerns: continuous sensing, event interpretation, device permissions, user trust, and evidence reporting.

### 2.5 Gap Analysis

Based on the current sources, the main gaps are:

- Public open data is much stronger for non-crash driving behavior than for real crash validation.
- Many existing consumer systems are closed, making them unsuitable as transparent teaching artifacts.
- A cross-platform prototype can present the product story well, but runtime-critical Android behavior still requires native handling.
- There is room for a local-first, educationally transparent prototype that combines crash response, driver-risk monitoring, and reproducible validation support.

### 2.6 Positioning of This Project

Project Sentry is positioned as an engineering capstone rather than a finished commercial emergency product. Its novelty lies in how it combines:

- an Android-native runtime MVP for realistic platform-level safety behavior,
- a Flutter-based validation and presentation layer for demos and documentation,
- a transparent threshold-based baseline that can later support machine-learning experimentation,
- and explicit privacy, ethics, and limitation reporting.

The project does not claim to outperform commercial crash-detection systems. Instead, it aims to provide a defensible, extensible, and well-documented prototype that demonstrates core computer-science theory and software-engineering decision-making.

## 3. System Analysis and Design

### 3.1 Requirements Analysis

The repository already contains a formal traceability file in `docs/requirements-traceability.md`. Table 1 consolidates the most relevant requirements for the final report.

| ID | Requirement Summary | Main Owner | Status |
| --- | --- | --- | --- |
| FL-01 | Provide a polished demo and presentation experience | Flutter prototype | Implemented |
| FL-02 | Support deterministic scenario playback | Flutter prototype | Implemented |
| FL-03 | Support live device trace capture for validation | Flutter prototype | Implemented |
| FL-04 | Support replay and PASS/CHECK evaluation of saved traces | Flutter prototype | Implemented |
| FL-05 | Export capstone evidence and report assets | Flutter prototype | Implemented |
| AN-01 | Run continuous driving monitoring in a foreground service | Android native MVP | Implemented |
| AN-02 | Request and use Android runtime permissions safely | Android native MVP | Implemented |
| AN-03 | Persist trips, risk events, and crash alert attempts locally | Android native MVP | Implemented |
| AN-04 | Provide crash countdown and escalation flow | Android native MVP | Implemented |
| AN-05 | Provide settings for contacts, demo number, and TTS | Android native MVP | Implemented |
| AN-06 | Provide native history/export UI comparable to Flutter reporting | Android native MVP | Partial |
| ML-01 | Maintain a shared ML training/export workspace | Shared | Scaffold only |

Functional requirements can be summarized as follows:

- start and stop a monitored driving session,
- collect motion and location data while driving mode is active,
- calculate risky-event indicators and crash-like-event candidates,
- present a countdown so the user can cancel or escalate,
- send SMS/open call flow/play TTS for demo-safe escalation,
- persist operational data locally,
- support scenario-based validation and evidence export,
- and maintain configurable emergency settings.

Non-functional requirements include:

- reliability under Android foreground-service rules,
- privacy-first local storage,
- transparent and explainable decision logic,
- maintainability through repository separation of concerns,
- safe demo behavior using placeholder or demo-only numbers,
- and reportability for capstone grading and presentation.

### 3.2 Use Cases

The main actors are the driver/tester, the mobile application, the phone sensors, and the emergency contact workflow. Key use cases are:

1. Configure emergency contacts, demo call number, and TTS message.
2. Start driving mode and grant required permissions.
3. Monitor accelerometer, gyroscope, and location data during a trip.
4. Detect risky driving events such as harsh braking or speeding.
5. Detect a suspected crash-like event and show a countdown confirmation UI.
6. Cancel the alert if the driver is safe.
7. Escalate through SMS, call intent, and TTS if the driver does not respond.
8. Review recent trips, risk events, and alert outcomes stored in Room.
9. Run deterministic scenarios in the Flutter lab.
10. Capture, replay, and export validation traces and report artifacts.

`TODO: Add a use-case diagram or UML diagram generated from these use cases.`

### 3.3 System Architecture

Project Sentry is intentionally split into two application layers that share one repository:

| Repository Area | Primary Responsibility |
| --- | --- |
| `android-native/` | Real Android runtime, foreground service, permissions, sensors, fused location, Room persistence, crash countdown, SMS/call/TTS |
| `flutter-app/` | UI prototype, scenario simulation, live trace capture, replay-based validation, presentation mode, report exports |
| `docs/` | Architecture, ethics, evaluation, references, research notes, traceability |
| `demo/` | Demo script and class-presentation support |
| `ml/` | Future machine-learning workspace and dataset/model scaffold |

This architecture is a deliberate design choice. Rather than forcing one stack to do everything poorly, the repository preserves the strongest role of each implementation.

`TODO: Export the Mermaid system-context and runtime-flow diagrams from docs/diagrams.md as report figures.`

### 3.4 Android-Native Architecture

The Android native MVP is the main final product. It is built with Kotlin, Jetpack Compose, Room, and Google Play Services Location. The core runtime responsibilities are handled by `DrivingModeService`, which:

- runs as a foreground service,
- registers accelerometer and gyroscope listeners,
- requests fused location updates,
- calculates risk events and crash candidates,
- publishes live state updates,
- starts the countdown and escalation flow,
- and writes results to local Room tables.

The Android UI layer provides:

- a Drive tab for live monitoring and crash simulation,
- a History tab for locally stored trips/events/alerts,
- a Status tab for preflight readiness and demo checks,
- a Settings screen for crash-flow configuration,
- and a dedicated full-screen countdown activity for suspected crashes.

### 3.5 Flutter Prototype Role

The Flutter application should be described as an early prototype and a continuing support tool, not as the final runtime product. Its role in the final capstone is to provide:

- rapid and polished UI exploration,
- deterministic scenario playback,
- live trace capture from a real Android device,
- replay-based validation,
- automated testing for core prototype logic,
- presentation mode,
- and report-pack generation for class demonstrations.

This role remains valuable even after the technical pivot, because it strengthens communication, evidence generation, and validation repeatability.

### 3.6 Feasibility and Risk Analysis

The project demonstrates partial feasibility across several dimensions.

Technical feasibility:

- Strong for Android-native sensing, permissions, local persistence, and countdown/escalation flow.
- Strong for scenario playback, validation tooling, and export-oriented UI in Flutter.
- Partial for broad crash-validation claims because final measured field data is not yet complete.

Operational feasibility:

- The MVP can be demonstrated on Android devices or emulators.
- The Flutter prototype can be used for class presentation and validation evidence.
- Real-world usage remains constrained by permissions, phone placement, OS behavior, and testing safety.

Resource feasibility:

- The project uses common student-accessible tools such as Android Studio, JDK 17, Flutter, and local storage.
- No special vehicle hardware is required for the MVP.

Key risks include false positives, false negatives, device placement variability, Android platform restrictions, missing field-validation data, and incomplete market/user validation. The repository already documents mitigation ideas such as safe simulated testing, countdown confirmation before escalation, local-only data handling, and staged crash validation instead of immediate triggering.

### 3.7 Ethical and Privacy Considerations

The ethical posture of Project Sentry is explicitly documented in `docs/ethics-and-limitations.md`. The most important design principles are:

- the system is a prototype, not a certified emergency-response product;
- no real crash recreation or dangerous driving should be used for testing;
- no real emergency services should be contacted during demos;
- demo-safe numbers should be used for call/SMS testing;
- data should remain local by default;
- and limitations should be visible to users and reviewers.

The Android countdown flow reflects a human-in-the-loop philosophy by giving the driver a visible cancellation window before escalation. This is an important ethical and UX decision because false positives are possible in any sensor-based prototype.

## 4. Implementation

### 4.1 Development Methodology

The repository structure suggests an iterative and prototype-driven development approach rather than a single linear waterfall process. This is an inference from the preserved research notes, the separate Flutter and Android workstreams, the bug-fixing report, and the final architectural merge. A reasonable description for the report is that the team followed an agile-style process with repeated implementation, testing, refinement, and scope adjustment.

The most important process decision was the technical pivot from "Flutter as the main application" to "Android native as the main final product, with Flutter retained as an earlier prototype and support layer." This should be framed as an engineering response to platform requirements, not as a failed approach.

`TODO: Add actual sprint names, dates, or milestone references if the team kept them.`

### 4.2 Tools and Technologies

| Category | Technologies Observed in Repository |
| --- | --- |
| Native mobile stack | Kotlin, Jetpack Compose, Android SDK, Google Play Services Location |
| Native persistence | Room |
| Native communication features | `SmsManager`, call intents, `TextToSpeech` |
| Cross-platform prototype | Flutter, Dart |
| Flutter validation/reporting | `sensors_plus`, `geolocator`, `permission_handler`, `shared_preferences`, `archive` |
| Build/runtime tooling | Gradle, JDK 17 |
| Documentation support | Markdown, Mermaid diagrams |
| ML scaffold | Python requirements scaffold, TensorFlow Lite references in docs |

### 4.3 Android Native Modules

#### 4.3.1 Foreground Driving Service

The core Android runtime is implemented in `DrivingModeService.kt`. This service starts and stops monitored driving sessions, maintains a foreground notification, registers sensor listeners, requests location updates, and tracks the active trip state. Keeping this logic inside a foreground service is essential because the target use case requires continued monitoring beyond a simple foreground screen.

#### 4.3.2 Sensor and Location Monitoring

The Android app reads the accelerometer and gyroscope through `SensorManager` and speed/location context through fused location updates. The service maintains recent sensor samples, speed history, and runtime state such as latest speed, last significant event times, and notification health indicators. This allows the app to distinguish between general motion, risk events, and possible crash evidence.

#### 4.3.3 Crash Detection Flow

The Android implementation does not immediately escalate on a single spike. Instead, it stages a crash candidate when linear acceleration or jerk crosses a high threshold, waits through a short validation window, and then checks additional evidence such as speed drop, repeated impact samples, or hard-stop behavior before launching the countdown. This is an important design improvement because it reduces the chance that bumps or sharp turns will trigger the full crash flow.

#### 4.3.4 Countdown Screen

The `CrashCountdownActivity` provides the user-facing confirmation step after a suspected crash. It shows the remaining seconds, explains the escalation path, displays the configured number of SMS contacts and call target, and offers clear actions for cancellation or immediate escalation. This screen is central to both user safety and demo clarity.

#### 4.3.5 SMS, Call, and TTS Escalation

If the countdown expires, the Android app attempts three local escalation steps:

1. send SMS messages to configured contacts,
2. open a call or dial intent using the configured number,
3. play a short TTS message locally.

Each alert outcome is recorded in the local database so the demo can show whether each step was attempted or blocked by permissions or missing configuration.

#### 4.3.6 Room Database

The Android native app uses Room to persist three main entities:

- `TripEntity` for session-level trip summaries,
- `RiskEventEntity` for harsh-braking, acceleration, cornering, and speeding events,
- `CrashAlertEntity` for countdown resolution and escalation outcomes.

This persistence layer strengthens the MVP by making results inspectable after the live demo instead of keeping everything in memory only.

#### 4.3.7 Settings and Preferences

The Android settings flow uses shared preferences through `SettingsRepository`. It stores emergency contacts, a demo call number, and a TTS template. The UI validates phone-number formatting and exposes readiness checks for permissions and crash-flow configuration.

### 4.4 Flutter Prototype

#### 4.4.1 Why Flutter Was Initially Used

Flutter was initially attractive because it enabled fast interface development, scenario-based demonstrations, presentation-friendly screens, and simple cross-target demos. For an early capstone phase, this was valuable because it reduced friction when exploring product flow and helped the team communicate the concept.

#### 4.4.2 What the Flutter Prototype Contributed

The Flutter prototype remains important because it contributed several meaningful assets:

- Scenario Lab for deterministic playback,
- live sensor capture with optional speed context,
- replay-based validation of stored traces,
- event logging and report generation,
- results dashboards and presentation screens,
- and automated tests for parts of the prototype logic.

These contributions are not wasted work. They now serve the documentation, validation, and presentation needs of the capstone.

#### 4.4.3 Why the Final Implementation Moved to Android Native

The final implementation moved to Android native because the real system requirements were increasingly platform-specific. Reliable foreground/background driving monitoring, permission handling, fused location, telephony escalation, TTS behavior, and local persistence all depend heavily on Android runtime behavior. Building those responsibilities natively is the more defensible engineering decision for a safety-critical MVP.

The report should therefore state this pivot clearly:

> The team initially explored Flutter for rapid UI prototyping, then pivoted to native Android to better support platform-level safety features.

### 4.5 ML and Risk Prevention

The repository does not currently contain deployed machine-learning inference in either application. The `ml/` directory is explicitly documented as scaffold only. The honest description of the present system is therefore:

- Flutter prototype: threshold-based crash detection using acceleration magnitude, jerk, minimum speed, and cooldown.
- Android native MVP: threshold-based risk scoring plus staged crash-candidate validation using inertial and speed context.

The current risk-prevention contribution is therefore heuristic rather than learned. However, the repository does preserve a future ML direction:

- collect anonymized local traces,
- reuse public non-crash or aggressive-driving datasets where appropriate,
- build preprocessing and model-export pipelines in `ml/`,
- and target future offline TensorFlow Lite integration only after real evidence is available.

The research notes also correctly identify an important data limitation: public smartphone datasets are more useful for negative cases and aggressive maneuvers than for true open severe-collision data. That means the future ML path should likely combine public data with controlled local collection rather than relying on a single external crash dataset.

### 4.6 Screenshots and Figure Placeholders

`TODO: Insert final screenshots for the following items:`

- Android native Drive tab
- Android native Settings screen
- Android native crash countdown screen
- Android native History tab
- Android native Status tab
- Flutter Scenario Lab
- Flutter Results Summary
- Flutter Presentation Mode
- exported report-pack artifacts

See `docs/Final_Report/figures-to-capture.md` for a recommended capture list.

## 5. Testing and Evaluation

### 5.1 Test Plan

Project Sentry now has two testing tracks because the repository contains two purpose-built applications.

Track A focuses on the Flutter prototype:

- deterministic scenario playback,
- repeatable PASS/CHECK scenario batches,
- live sensor capture,
- replay of recorded traces,
- and export/report generation.

Track B focuses on the Android native MVP:

- foreground-service startup and shutdown,
- permission-request behavior,
- sensor and location monitoring,
- crash countdown behavior,
- SMS/call/TTS escalation,
- and Room persistence.

This two-track evaluation strategy is consistent with the actual repository structure and should be explained explicitly in the final report.

### 5.2 Unit Testing Plan

The Flutter application already contains automated test files for:

- detection engine behavior,
- report generation,
- sensor trace encoding/decoding,
- trace evaluation,
- event-log export,
- and configuration storage.

The repository currently contains at least 13 explicit Flutter test cases across those modules. However, Flutter execution was not rerun on this documentation workstation on April 26, 2026 because the `flutter` CLI is not installed here. `TODO: Rerun Flutter tests on a machine with the Flutter SDK and add the results.`

For Android, the Gradle unit-test task is configured, but as of April 26, 2026 it reported `NO-SOURCE`, which indicates that Android JVM unit tests have not yet been added. This should be reported honestly as a testing gap rather than hidden.

### 5.3 Integration Testing Plan

Important integration scenarios include:

- permissions plus foreground-service startup,
- sensors plus location plus risk-event logic,
- crash-detection validation plus countdown launch,
- countdown resolution plus SMS/call/TTS escalation,
- and Room insert/query behavior plus UI presentation.

The repository's Android bug-fixing report documents manual QA attention in these areas, especially around permissions, false positives, notification behavior, history display, and countdown reliability.

### 5.4 System Testing Plan

System testing should cover complete user journeys rather than isolated functions:

1. Configure crash-flow settings.
2. Start driving mode.
3. Observe live risk indicators and event counters.
4. Trigger a simulated crash or validated test condition.
5. Confirm that the countdown appears and can be cancelled.
6. Allow timeout during a demo-safe run and verify escalation behavior.
7. Confirm that alert outcomes and trip data are visible in history/status screens.

For Flutter, system testing should include:

1. Run scripted scenarios from the Scenario Lab.
2. Verify PASS/CHECK outcomes.
3. Capture a safe live trace.
4. Replay the saved trace.
5. Generate the results summary and final report pack.

### 5.5 Permission-Flow Testing

Permission-flow testing is especially important because the Android MVP depends on location, notifications, SMS, and phone permissions. The repository's QA notes indicate that the permission flow was refined to avoid first-launch crashes and to delay foreground-service startup until required permissions are granted. The final report should include screenshots and a short narrative walkthrough of:

- first launch,
- permission prompts,
- missing-permission fallback messaging,
- and preflight readiness indicators.

`TODO: Capture this flow on a clean Android install and include screenshots.`

### 5.6 Crash Simulation Testing

The safest current crash-demo path is the Android native "Simulate Crash" flow and the Flutter deterministic scenario flow. Both approaches avoid unsafe physical testing while still demonstrating the product behavior. The countdown screen, cancellation action, and escalation outcome logging should all be documented as part of the crash-simulation evidence.

`TODO: Add a table summarizing each crash-simulation run, device used, permissions state, and observed outcome.`

### 5.7 Risk-Event Testing

The Android MVP also supports non-crash risk-event logging, including harsh braking, harsh acceleration, sharp cornering, and speeding. This is valuable because it broadens the system from pure crash response into driver-risk monitoring. However, the repository does not yet contain a finalized measured dataset or summary table for false alarms, event rates, or threshold calibration. `TODO: Collect repeatable safe runs and summarize event behavior before final submission.`

### 5.8 Emulator vs Physical Device Testing

The project can be partially demonstrated on emulators, especially for UI and flow review, but physical-device testing is more credible for:

- real sensor sampling,
- location and speed behavior,
- notification behavior,
- background activity restrictions,
- and telephony/TTS handling.

The Flutter validation workflow already anticipates physical-device trace capture, and the Android native MVP is clearly designed with real device behavior in mind. What is still missing is a finalized evidence package that shows which tests were completed on emulators versus which were completed on actual phones. `TODO: Add a device matrix with OS version, device model, and tested features.`

### 5.9 Current Verified Evidence

The following items were directly verified on April 26, 2026 in the local environment:

- `android-native\\gradlew.bat :app:assembleDebug` completed successfully.
- `android-native\\gradlew.bat :app:testDebugUnitTest` completed successfully, but the task reported `NO-SOURCE`.
- `android-native\\gradlew.bat :app:lintDebug` completed successfully.
- The Android build emitted a non-blocking warning that Android Gradle Plugin `8.3.2` is not formally tested up to `compileSdk = 35`.
- The local environment has JDK 17 installed.
- The local environment does not have the Flutter CLI installed, so Flutter app execution and Flutter test reruns were not performed here.

These are the only build-verification claims that should be treated as freshly confirmed in this draft.

### 5.10 Evaluation Metrics and TODO Results

The rubric expects actual evaluation, so the final report should include measured results where possible. Table 2 captures the current state honestly.

| Metric | Current Status | Evidence Available Now | What Is Still Needed |
| --- | --- | --- | --- |
| Detection latency | TODO | No final measured table in repo | Time countdown launch after trigger under controlled runs |
| False alarms per hour | TODO | Risk/collision logic exists, but no finalized field dataset | Collect repeated safe-drive traces and summarize false positives |
| SMS success | Partial | Code path exists and alert status is persisted | Demo-run table showing permission state and observed result |
| Call escalation success | Partial | Code path exists and alert status is persisted | Demo-run table showing `CALL_PHONE` or dialer behavior on device |
| TTS success | Partial | Code path exists and alert status is persisted | Device-level confirmation during timed crash demo |
| Risk-event accuracy | TODO | Heuristic logic exists | Controlled trace set with expected event labels |
| Scenario PASS/CHECK rate | Partial | Flutter report/export code exists | Regenerate a final report pack and archive the outputs |

## 6. Entrepreneurial / Innovation Aspects

### 6.1 Market Relevance

The repository's research notes already frame the market relevance well. Road safety remains a persistent public issue, smartphone ownership is widespread, and major consumer platforms already invest in crash-related safety features. This means the problem area is not hypothetical.

### 6.2 Existing Market Systems

The existing-market discussion should compare Project Sentry against:

- Apple Crash Detection,
- Google Pixel crash-detection features,
- Life360 safety features,
- and relevant telematics applications where appropriate.

The point of the comparison is not to claim that this capstone outperforms those systems, but to show how the project differs:

- it is prototype-level and transparent rather than proprietary,
- it is local-first,
- it is educational and extensible,
- and it is not limited to one premium hardware line in concept.

`TODO: Build a one-page competitor matrix from official product pages only.`

### 6.3 Why This Project Is Still Valuable

Project Sentry remains valuable even in the presence of established consumer products because it targets a different outcome:

- it exposes design tradeoffs clearly,
- it can be adapted for research or teaching,
- it supports reproducible scenario-based validation,
- and it demonstrates how to build a mobile sensing MVP under realistic Android constraints.

Its value is therefore strongest as an engineering and experimentation platform rather than a finished mass-market product.

### 6.4 Possible Commercialization

Potential commercialization paths could include:

- a safe-driving companion app with local risk scoring,
- a family safety tool focused on emergency-contact workflows,
- a telematics add-on for student or fleet safety studies,
- or a white-label prototype for further industry collaboration.

However, any commercialization discussion must remain conditional. The current prototype would still need stronger calibration, legal review, device testing, policy review, and likely backend/service architecture before deployment beyond capstone scope.

### 6.5 Deployment and Scalability

From a deployment perspective, the Android native MVP is the more scalable foundation because it already owns the platform-specific runtime behavior. The Flutter layer can continue to serve as a supporting interface for demos, validation, or future cross-platform reporting. Long-term scalability would require:

- modularization of detector logic,
- stronger test coverage,
- more robust device compatibility validation,
- and likely a clearer strategy for updates, data retention, and user support.

### 6.6 Societal and Ethical Impact

Societal value comes from the possibility of faster awareness and safer incident response, but societal risk comes from false reassurance or false escalation. For that reason, the final report should keep emphasizing that Project Sentry is a prototype and not a certified emergency system. This is not a weakness in the report; it is an ethical strength.

## 7. Project Management and Teamwork

### 7.1 Team Roles

The repository confirms the technical workstreams, but not the final named role distribution. A safe draft structure is shown below.

| Role Area | Suggested Description | Final Owner |
| --- | --- | --- |
| Android native runtime | Foreground service, sensors, location, permissions, Room, crash flow | TODO |
| Flutter prototype and validation | Scenario Lab, trace capture/replay, presentation mode, report exports | TODO |
| Documentation and report integration | Architecture docs, ethics, evaluation, final report assembly | TODO |
| Research and market analysis | Related work, competitor notes, feasibility framing | TODO |

### 7.2 Timeline Placeholder

The report should include a timeline or Gantt chart. The repository currently supports a milestone-oriented narrative, but not a finalized chart. A reasonable milestone structure is:

1. Problem framing and early prototype exploration.
2. Flutter-based simulation and UI prototyping.
3. Android-native MVP development for runtime-critical features.
4. Validation tooling, bug fixing, and documentation integration.
5. Final testing, evidence capture, and report assembly.

`TODO: Add actual dates and team assignments.`

### 7.3 Milestones

Milestones that are already evident from repository artifacts include:

- creation of the Flutter simulation and reporting layer,
- preservation of research and validation notes,
- integration of the Android native runtime,
- creation of shared documentation and traceability,
- and stabilization work described in the Android bug-fixing report.

`TODO: Add milestone dates, decision dates, and meeting checkpoints.`

### 7.4 GitHub Collaboration

The unified repository itself is evidence of a structured collaboration approach. It separates responsibilities cleanly, preserves documentation alongside code, and keeps future ML work isolated from unsupported claims. The final report can state that GitHub served as the shared collaboration and version-control platform, but it should not invent commit counts, branch strategies, or reviewer details unless those records are actually available.

`TODO: Add screenshots or logs of branch structure, pull requests, or commit history if desired.`

### 7.5 Resource Management

The main project resources are:

- Android Studio and Android SDK,
- JDK 17,
- Flutter SDK,
- Android devices or emulators,
- documentation time for evidence capture,
- and safe testing conditions.

The final report should also note the project constraint that some validation depends on the availability of physical Android hardware and safe conditions for motion capture.

### 7.6 Advisor Interaction Placeholders

`TODO: Add meeting dates, key advisor recommendations, and how they affected scope or implementation decisions.`

## 8. Results and Discussion

### 8.1 Current Achieved Outcomes

Based on the repository as it exists today, the project has already achieved the following:

- a working Android native MVP with service-based monitoring and local crash workflow,
- a working Flutter prototype for simulation, validation, and evidence export,
- a Room-backed local data model for trips, events, and crash outcomes,
- a privacy-conscious architecture that keeps data local in the MVP stage,
- and a documentation set that explains architecture, evaluation, ethics, and future work.

### 8.2 Comparison to Objectives

| Objective | Current Status | Evidence |
| --- | --- | --- |
| Smartphone-based monitoring and crash workflow | Largely achieved | Android native service, countdown, escalation code |
| Native Android MVP for platform-critical features | Achieved | `android-native/` structure and verified Gradle checks |
| Validation and presentation layer | Achieved | Flutter Scenario Lab, Results Summary, report exports |
| Privacy-first local MVP | Achieved | Local Room storage and ethics documentation |
| Honest limitation reporting | Achieved | `docs/ethics-and-limitations.md`, ML scaffold notes |
| Final quantitative evaluation package | Partial | Test plans exist; final metrics package still missing |

### 8.3 Strengths

The strongest aspects of the project are:

- honest separation of concerns between prototype UI/validation and runtime-critical Android behavior,
- a defensible engineering pivot rather than forced architectural compromise,
- local persistence and visible operational history in the Android MVP,
- clear human-in-the-loop crash escalation through a countdown,
- and good capstone communication support through the Flutter evidence layer.

### 8.4 Limitations

Important limitations that should remain explicit in the final report include:

- final field metrics are not yet complete,
- Android unit tests are still missing,
- Flutter/native are intentionally not bridged into one runtime,
- machine learning is not yet deployed,
- phone placement and device variation remain important unknowns,
- and emergency behavior is demo-oriented rather than production-certified.

### 8.5 Lessons Learned

Several lessons emerge from the current repository:

1. Cross-platform prototyping is valuable, but it does not remove platform realities.
2. Safety-critical mobile behavior requires attention to OS rules, permissions, and lifecycle behavior.
3. Transparent heuristics and honest limitation reporting are preferable to overstated "AI" claims.
4. Validation tooling and documentation are not secondary work; they materially improve capstone quality.

### 8.6 Discussion of the Flutter-to-Android Pivot

The Flutter-to-Android pivot is one of the most important discussion points in this report. It should be presented positively and professionally:

- Flutter was useful for rapid prototyping, scenario simulation, results visualization, and capstone storytelling.
- The team learned through technical exploration that the project's most safety-critical features depended on Android-native lifecycle and permission behavior.
- Instead of forcing an unstable cross-platform workaround, the team preserved Flutter where it added the most value and moved the final runtime MVP to the stack that best fit the problem.

This is a mature engineering decision because it prioritizes system fit, reliability, and honest scope management over attachment to an earlier implementation choice.

## 9. Conclusion and Future Work

### 9.1 Summary of Contributions

Project Sentry delivers a two-layer capstone outcome with clear division of responsibility. The final product focus is a native Android MVP that performs foreground driving monitoring, sensor and location collection, staged crash validation, countdown-based user confirmation, SMS/call/TTS escalation, and Room-backed persistence. Supporting that final product is a Flutter prototype that remains useful for scenario-driven validation, trace replay, presentation, and evidence export. The project therefore demonstrates not only technical implementation, but also sound architectural judgment and responsible documentation practice.

### 9.2 Future Improvements

Priority future work includes:

1. integrate a real ML model only after collecting sufficient anonymized evaluation data,
2. improve the Android UI/UX and add richer native export/history features,
3. add trip history filtering, summaries, and export options,
4. expand physical-device testing across multiple Android phones and OS versions,
5. review Play Store and policy implications for permissions, foreground services, SMS, and calling,
6. consider a future Flutter-native bridge only after stable interfaces are defined,
7. refactor Android detector logic into more testable classes,
8. add Android unit and instrumentation tests,
9. formalize market validation through surveys, interviews, and competitor analysis.

## References

The repository currently contains reference leads but not a finalized academic reference list. The following items should be converted to one consistent style, preferably IEEE or APA.

- Android foreground services. Android Developers. `TODO: full citation formatting.` <https://developer.android.com/develop/background-work/services/foreground-services>
- Android runtime permissions. Android Developers. `TODO: full citation formatting.` <https://developer.android.com/training/permissions/requesting>
- Android sensors overview. Android Developers. `TODO: full citation formatting.` <https://developer.android.com/guide/topics/sensors/sensors_overview>
- Android location documentation. Android Developers. `TODO: full citation formatting.` <https://developer.android.com/develop/sensors-and-location/location>
- `SmsManager` reference. Android Developers. `TODO: full citation formatting.` <https://developer.android.com/reference/android/telephony/SmsManager>
- `TextToSpeech` reference. Android Developers. `TODO: full citation formatting.` <https://developer.android.com/reference/android/speech/tts/TextToSpeech>
- Room documentation. Android Developers. `TODO: full citation formatting.` <https://developer.android.com/training/data-storage/room>
- TensorFlow Lite Android documentation. `TODO: full citation formatting.` <https://www.tensorflow.org/lite/android>
- TensorFlow Lite conversion documentation. `TODO: full citation formatting.` <https://www.tensorflow.org/lite/models/convert>
- WHO road-safety overview / road-traffic-injuries fact sheet. `TODO: choose one final official citation and format it consistently.`
- NHTSA research/data and April 1, 2026 traffic-safety release. `TODO: full citation formatting.`
- Apple crash-detection support pages cited in research notes. `TODO: full citation formatting.`
- Google Pixel crash-detection product/support pages cited in research notes. `TODO: full citation formatting.`
- Life360 Q2 2025 results / safety-positioning source cited in research notes. `TODO: full citation formatting.`
- `Harnessing Smartphone Sensors for Enhanced Road Safety: A Comprehensive Dataset and Review` (Scientific Data, 2025). `TODO: add complete bibliographic entry.`
- Additional benchmark repositories listed in `docs/references.md` and `docs/research/github-links.md`. `TODO: decide which should remain as implementation inspirations versus formal academic references.`

## Appendices

### Appendix A. Installation Guide Placeholder

`TODO: Consolidate setup steps from README.md, android-native/README.md, and flutter-app/README.md into one clean appendix.`

### Appendix B. User Manual Placeholder

`TODO: Add short operating instructions for both the Android MVP and the Flutter prototype.`

### Appendix C. Test Cases Placeholder

`TODO: Add structured test cases for permissions, monitoring, simulated crash flow, trace replay, and export generation.`

### Appendix D. Dataset and Model Details Placeholder

`TODO: Add dataset sources, preprocessing plan, feature definitions, and model-integration notes if ML work progresses.`

### Appendix E. GitHub and Repository Structure Placeholder

`TODO: Add the final repository tree, collaboration workflow notes, and any selected commit/PR evidence.`
