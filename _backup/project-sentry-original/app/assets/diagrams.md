# Diagrams (Mermaid)

Use these Mermaid diagrams in your report or slides. They render in many Markdown
viewers (GitHub, VS Code extensions, Obsidian).

## System Context
```mermaid
flowchart LR
  Driver[Driver / Tester] -->|Start Trip, Simulate| App[Sentry Mobile App]
  Sensors[Phone Sensors<br/>Accel / Gyro / GPS] --> App
  App -->|Alert Flow| Contact[Emergency Contact]
  App -->|Write| Storage[Local Event Log]
  App -->|Export ZIP| ReportPack[Final Report Pack]
  ReportPack --> Reviewer[Instructor / Reviewer]
```

## App Navigation Map
```mermaid
flowchart TB
  Splash[Splash] --> Shell[App Shell]
  Shell --> Home[Home]
  Shell --> Lab[Scenario Lab]
  Shell --> Results[Results Summary]
  Home --> Settings[Detection Settings]
  Home --> Presentation[Presentation Mode]
  Home --> Alert[Crash Alert]
  Lab --> Alert
```

## Runtime Data Flow
```mermaid
flowchart LR
  SRC[Sensor Source<br/>Real or Simulated] --> TC[Trip Controller]
  TC --> DET[Detection Engine]
  DET -->|Suspected Crash| ALERT[Confirmation UI]
  ALERT -->|User Response| LOG[Event Log]
  DET -->|Decision| LOG
  LOG --> REP[Report Generator]
  REP --> EXP[File Exporter]
  EXP --> ZIP[Final Report Pack ZIP]
```

## Detection Algorithm (Decision Logic)
```mermaid
flowchart TD
  S[Sensor Sample] --> A[Compute magnitude |a|]
  A --> B[Compute jerk = (|a|-|a_prev|)/dt]
  B --> C{Cooldown active?}
  C -- Yes --> X[Ignore]
  C -- No --> D{Speed >= minSpeed?}
  D -- No --> X
  D -- Yes --> E{Magnitude >= accelThreshold?}
  E -- No --> X
  E -- Yes --> F{Jerk >= jerkThreshold?}
  F -- No --> X
  F -- Yes --> G[Emit crash decision]
  G --> H[Show Alert UI]
  H --> I[Log event]
```

## Signal Processing (Magnitude + Jerk)
```mermaid
flowchart LR
  AX[ax] --> MAG[|a| = sqrt(ax^2 + ay^2 + az^2)]
  AY[ay] --> MAG
  AZ[az] --> MAG
  MAG --> DELTA[delta = |a| - |a_prev|]
  DT[dt seconds] --> JERK[jerk = delta / dt]
  MAG --> THRESH[Threshold gates]
  JERK --> THRESH
```

## Sequence: Crash Alert
```mermaid
sequenceDiagram
  participant Sensor as Sensor Source
  participant TC as Trip Controller
  participant DET as Detection Engine
  participant UI as Alert Screen
  participant Log as Event Log
  Sensor->>TC: Stream sensor samples
  TC->>DET: Add sample
  DET-->>UI: Suspected crash decision
  UI->>Log: Record alert event
  UI-->>TC: Continue or stop session
```

## Sequence: Scenario Lab Run
```mermaid
sequenceDiagram
  participant User as Tester
  participant Lab as Scenario Lab
  participant Sensor as Scenario Sensor Service
  participant TC as Trip Controller
  participant DET as Detection Engine
  participant Log as Event Log
  User->>Lab: Run scenario
  Lab->>Sensor: Start scripted frames
  Sensor->>TC: Frame stream
  TC->>DET: Add sample
  DET-->>Lab: Decision (if any)
  Lab->>Log: Record PASS/CHECK outcome
```

## State Machine: Trip Controller
```mermaid
stateDiagram-v2
  [*] --> Idle
  Idle --> Streaming: Start
  Streaming --> Idle: Stop
  Streaming --> Alerting: Suspected crash
  Alerting --> Streaming: User OK
  Alerting --> Idle: Session ended
```

## Data Model (Core)
```mermaid
classDiagram
  class CrashEvent {
    DateTime timestamp
    String source
    String outcome
    String notes
    String eventType
    String scenarioId
    bool expectedTrigger
    bool triggered
  }
  class DetectionConfig {
    double accelThresholdG
    double jerkThresholdGPerS
    double minSpeedMps
    int cooldownSeconds
  }
  class Scenario {
    String id
    String name
    String description
    bool expectedTrigger
    int sampleRateHz
  }
  class ScenarioFrame {
    double ax
    double ay
    double az
    double speedMps
  }
  Scenario "1" --> "*" ScenarioFrame
```

## Report Pack Pipeline
```mermaid
flowchart LR
  LOG[Event Log] --> REP[Report Generator]
  REP -->|MD/HTML| PACK[Pack Builder]
  LOG -->|CSV/JSON| PACK
  CHART[Chart Capture] --> PACK
  PACK --> ZIP[ZIP + manifest.txt]
```

## Deployment Targets
```mermaid
flowchart LR
  Flutter[Flutter App] --> Web[Web (Chrome)]
  Flutter --> Windows[Windows Desktop]
  Flutter --> Android[Android Device / Emulator]
```
