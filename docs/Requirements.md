# Requirements Traceability

This table maps project requirements to implementation evidence and tests.

| ID | Requirement | Implementation | Evidence / Test | Status |
| --- | --- | --- | --- | --- |
| REQ-01 | Detect high-impact events from sensor data. | `DetectionEngine`, `TripController` | `app/test/detection_engine_test.dart` | Implemented |
| REQ-02 | Reduce false positives at low speeds. | `DetectionConfig.minSpeedMps` speed gate | Scenario: Phone Drop, Stop & Go | Implemented |
| REQ-03 | Provide user confirmation with countdown. | `AlertScreen` | UI demo + alert flow | Implemented |
| REQ-04 | Log alerts and scenario outcomes locally. | `EventLog` + Shared Preferences | `app/test/event_log_test.dart` | Implemented |
| REQ-05 | Provide deterministic evaluation scenarios. | `ScenarioLabScreen`, `ScenarioSensorService` | Batch run “Run All Scenarios” | Implemented |
| REQ-06 | Export reports and logs for documentation. | `ReportGenerator`, `FileExporter` | `app/test/report_generator_test.dart` | Implemented |
| REQ-07 | Package deliverables into a ZIP report pack. | Results Summary → Final Report Pack | ZIP output + `manifest.txt` | Implemented |
| REQ-08 | Allow threshold configuration. | `SettingsScreen`, `ConfigStore` | UI sliders + persisted config | Implemented |
| REQ-09 | Provide a presentation-ready demo mode. | `PresentationScreen` + checklist | Presentation Mode UI | Implemented |
| REQ-10 | Keep data local (privacy-first MVP). | No backend; local storage only | Architecture docs | Implemented |
| REQ-11 | Cross-platform demo support. | Flutter targets web/windows/android | `flutter doctor` devices list | Implemented |
