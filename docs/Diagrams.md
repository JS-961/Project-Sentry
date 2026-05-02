# Diagrams (Mermaid)

The first three diagrams below map directly to the final report figure list:

- Figure 1. Project Sentry use-case diagram.
- Figure 2. Project Sentry system architecture.
- Figure 3. Crash-detection and escalation runtime flow.

Use these Mermaid blocks in the final report, slides, or exported report pack.
Render them as high-resolution PNG/SVG before inserting them into Word or PDF.

## Figure 1: Project Sentry Use-Case Diagram

```mermaid
flowchart LR
  Driver["Driver / Tester"]:::actor
  Sensors["Phone Sensors<br/>Accelerometer / Gyroscope / GPS"]:::actor
  Contact["Emergency Contact"]:::actor

  subgraph Sentry["Project Sentry"]
    UC1["UC1 Configure emergency contacts,<br/>demo call number, and TTS message"]:::usecase
    UC2["UC2 Start driving mode<br/>and grant permissions"]:::usecase
    UC3["UC3 Monitor motion and<br/>location during trip"]:::usecase
    UC4["UC4 Detect risky driving events<br/>such as braking, acceleration,<br/>cornering, and speeding"]:::usecase
    UC5["UC5 Detect suspected crash-like<br/>event and show countdown"]:::usecase
    UC6["UC6 Cancel alert if<br/>driver confirms safe"]:::usecase
    UC7["UC7 Escalate by SMS,<br/>call flow, and TTS"]:::usecase
    UC8["UC8 Review stored trips,<br/>risk events, and alerts"]:::usecase
    UC9["UC9 Run deterministic<br/>Flutter scenarios"]:::usecase
    UC10["UC10 Capture, replay,<br/>and export validation evidence"]:::usecase
  end

  Driver --> UC1
  Driver --> UC2
  Driver --> UC6
  Driver --> UC8
  Driver --> UC9
  Driver --> UC10
  Sensors --> UC3
  UC3 --> UC4
  UC4 --> UC5
  UC5 --> UC6
  UC5 --> UC7
  UC7 --> Contact
  UC9 --> UC10

  classDef actor fill:#eff6ff,stroke:#2563eb,color:#111827,stroke-width:2px
  classDef usecase fill:#f8fafc,stroke:#475569,color:#111827,stroke-width:1px
```

## Figure 2: Project Sentry System Architecture

```mermaid
flowchart TB
  Driver["Driver / Tester"]:::actor
  Reviewer["Instructor / Reviewer"]:::actor

  subgraph Repo["Project Sentry Repository"]
    subgraph Android["android-native/ - real Android MVP"]
      AndroidUI["Jetpack Compose UI<br/>Drive / History / Status / Settings"]:::component
      Service["DrivingModeService<br/>foreground monitoring"]:::component
      Countdown["CrashCountdownActivity<br/>cancel / call now / timeout"]:::component
      Escalation["SMS / call intent / TTS<br/>demo-safe crash flow"]:::component
      Room["Room database<br/>trips / risk events / crash alerts"]:::data
      AndroidML["AdvisoryRiskClassifier<br/>JSON assets in app"]:::ml
    end

    subgraph Flutter["flutter-app/ - demo and validation layer"]
      ScenarioLab["Scenario Lab<br/>deterministic playback"]:::component
      LiveCapture["Live capture and trace replay"]:::component
      Results["Results Summary<br/>charts and metrics"]:::component
      Presentation["Presentation Mode"]:::component
      ReportPack["Report pack export<br/>JSON / CSV / MD / HTML / ZIP"]:::data
    end

    Docs["docs/<br/>architecture, ethics, report, figures"]:::doc
    Demo["demo/<br/>script and presentation support"]:::doc
    MLWorkspace["ml/<br/>training, diagnostics,<br/>evaluation, Android JSON export"]:::ml
  end

  PhoneSensors["Phone sensors and GPS"]:::external
  Driver --> AndroidUI
  Driver --> ScenarioLab
  PhoneSensors --> Service
  AndroidUI --> Service
  Service --> Room
  Service --> Countdown
  Countdown --> Escalation
  AndroidML --> Service
  MLWorkspace --> AndroidML

  ScenarioLab --> Results
  LiveCapture --> Results
  Results --> ReportPack
  Presentation --> Reviewer
  ReportPack --> Reviewer
  Docs --> Reviewer
  Demo --> Reviewer
  AndroidUI --> Reviewer

  classDef actor fill:#eff6ff,stroke:#2563eb,color:#111827,stroke-width:2px
  classDef external fill:#fef3c7,stroke:#d97706,color:#111827
  classDef component fill:#f8fafc,stroke:#475569,color:#111827
  classDef data fill:#ecfdf5,stroke:#059669,color:#111827
  classDef doc fill:#f5f3ff,stroke:#7c3aed,color:#111827
  classDef ml fill:#eef2ff,stroke:#4f46e5,color:#111827
```

## Figure 3: Crash-Detection And Escalation Runtime Flow

```mermaid
flowchart TD
  Start(["Driver taps Start Drive"]) --> Ready{"Permissions and demo<br/>configuration ready?"}
  Ready -- No --> Explain["Before Permissions dialog<br/>or Settings readiness message"]
  Explain --> Request["Grant location, notifications,<br/>SMS, phone, and save demo contacts"]
  Request --> Ready

  Ready -- Yes --> Foreground["DrivingModeService starts<br/>as foreground service"]
  Foreground --> Stream["Collect accelerometer,<br/>gyroscope, and fused location"]
  Stream --> Features["Compute acceleration magnitude,<br/>jerk, gyro magnitude, and speed context"]
  Features --> Risk["Update rule risk score,<br/>event counters, and latest event"]
  Features --> Advisory["Run advisory ML window inference"]
  Advisory -. advisory output only .-> UI["Drive and Status show<br/>ML advisory result"]

  Risk --> Candidate{"Crash-like candidate?"}
  Candidate -- No --> PersistEvent["Persist trip and risk-event data<br/>when relevant"]
  PersistEvent --> Foreground

  Candidate -- Yes --> Validate["Apply staged validation,<br/>speed context, and cooldown gates"]
  Validate --> Countdown["Launch CrashCountdownActivity<br/>and crash notification"]
  Countdown --> Cancel{"Driver taps<br/>I'm OK?"}
  Cancel -- Yes --> Cancelled["Record cancelled alert outcome"]
  Cancel -- No --> Timeout{"Timer expires<br/>or Call Now pressed?"}
  Timeout -- Yes --> Escalate["Send SMS, open call flow,<br/>and play TTS intro"]

  Cancelled --> PersistAlert["Persist crash-alert outcome<br/>in Room"]
  Escalate --> PersistAlert
  PersistAlert --> History["History tab displays<br/>trips, events, and alerts"]
```

## Android Runtime Architecture

```mermaid
flowchart LR
  Main["MainActivity"] --> Permissions["Permission explanation<br/>and Android runtime requests"]
  Main --> Home["HomeScreen<br/>Drive / History / Status"]
  Main --> Settings["SettingsScreen<br/>contacts / call number / TTS"]
  Home --> Service["DrivingModeService"]
  Settings --> SettingsRepo["SettingsRepository<br/>SharedPreferences"]
  SettingsRepo --> Service
  Service --> Sensors["SensorManager<br/>accelerometer + gyroscope"]
  Service --> Location["FusedLocationProviderClient"]
  Service --> ML["AdvisoryRiskClassifier<br/>trained JSON models"]
  Service --> Store["DrivingStateStore<br/>live UI state"]
  Store --> Home
  Service --> Room["SafeDriveDatabase<br/>Trip / RiskEvent / CrashAlert"]
  Service --> Countdown["CrashCountdownActivity"]
  Countdown --> Service
```

## Advisory ML Training And Android Export

```mermaid
flowchart LR
  PublicData["Public smartphone-sensor recordings"] --> LabelCheck["Conservative label mapping<br/>skip unverifiable folders"]
  LabelCheck --> Windows["Window feature extraction<br/>accel / jerk / gyro / combined motion"]
  Windows --> Train["Train and evaluate<br/>tabular advisory classifiers"]
  Train --> Metrics["Diagnostics, summaries,<br/>confusion matrices"]
  Train --> Json["Export Android-readable<br/>JSON models"]
  Json --> Assets["android-native app assets"]
  Assets --> Classifier["AdvisoryRiskClassifier"]
  Classifier --> State["DrivingStateStore"]
  State --> UI["Drive and Status<br/>label / score / confidence"]
  UI -. advisory only .-> Countdown["Crash countdown<br/>rule-based authority"]
```

## Flutter Validation And Report-Pack Flow

```mermaid
flowchart TB
  Lab["Scenario Lab"] --> Scripted["Deterministic scenario playback"]
  Lab --> Live["Live capture<br/>accelerometer plus optional GPS speed"]
  Lab --> Replay["Replay selected trace"]
  Scripted --> Eval["PASS / CHECK evaluation"]
  Live --> TraceFiles["Saved trace files"]
  TraceFiles --> Replay
  Replay --> Eval
  Eval --> EventLog["Event Log"]
  EventLog --> Results["Results Summary<br/>counts, chart, scenario events"]
  Results --> Exports["Export MD / HTML / CSV / JSON / PNG"]
  Exports --> Pack["Final report pack ZIP"]
```

## Permission And Crash Flow Sequence

```mermaid
sequenceDiagram
  participant User as Driver or Tester
  participant UI as Android UI
  participant OS as Android Permission System
  participant Service as DrivingModeService
  participant Countdown as CrashCountdownActivity
  participant Room as Room Database

  User->>UI: Tap Start Drive
  UI->>UI: Show Before Permissions explanation if needed
  UI->>OS: Request location, notifications, SMS, phone
  OS-->>UI: Permission results
  UI->>Service: Start foreground monitoring
  Service->>Service: Detect crash-like candidate
  Service->>Countdown: Launch countdown
  alt Driver confirms safe
    User->>Countdown: I'm OK - Cancel Alert
    Countdown->>Service: ACTION_CRASH_CANCELLED
    Service->>Room: Store cancelled alert outcome
  else Timer expires or Call Now
    Countdown->>Service: ACTION_CRASH_TIMEOUT or ACTION_CRASH_CALL_NOW
    Service->>Service: Send SMS, open call flow, play TTS
    Service->>Room: Store escalation outcome
  end
```

## Core Persistence Model

```mermaid
classDiagram
  class TripEntity {
    startedAtEpochMs
    endedAtEpochMs
    maxRiskScore
    avgRiskScore
    totalEvents
  }
  class RiskEventEntity {
    timestampEpochMs
    eventType
    value
    speedMps
    riskScore
  }
  class CrashAlertEntity {
    timestampEpochMs
    outcome
    smsStatus
    callStatus
    ttsStatus
  }
  class AppSettings {
    emergencyContacts
    demoCallNumber
    ttsTemplate
  }
  TripEntity "1" --> "*" RiskEventEntity
  TripEntity "1" --> "*" CrashAlertEntity
  AppSettings --> CrashAlertEntity
```
