# Requirements Traceability

| ID | Requirement | Primary Owner | Evidence | Status |
| --- | --- | --- | --- | --- |
| HYB-01 | Keep both original project strengths in one clean repository | Shared | top-level structure, README, `_backup/`, `_imports/` | Implemented |
| FL-01 | Provide a polished demo and presentation experience | `flutter-app/` | `flutter-app/lib/screens/presentation_screen.dart`, `flutter-app/lib/screens/home_screen.dart` | Implemented |
| FL-02 | Support deterministic scenario playback | `flutter-app/` | `flutter-app/lib/screens/scenario_lab_screen.dart`, `flutter-app/lib/services/scenario_sensor_service.dart` | Implemented |
| FL-03 | Support live device trace capture for validation | `flutter-app/` | `flutter-app/lib/services/live_sensor_service.dart`, `flutter-app/lib/services/sensor_trace_recorder.dart` | Implemented |
| FL-04 | Support replay of saved traces with PASS/CHECK evaluation | `flutter-app/` | `flutter-app/lib/services/recorded_sensor_service.dart`, `flutter-app/lib/services/trace_evaluator.dart` | Implemented |
| FL-05 | Export capstone evidence and report assets | `flutter-app/` | `flutter-app/lib/screens/results_summary_screen.dart`, `flutter-app/lib/services/report_generator.dart`, `flutter-app/scripts/` | Implemented |
| FL-06 | Keep automated test coverage for core demo logic | `flutter-app/` | `flutter-app/test/` | Implemented |
| AN-01 | Run continuous driving monitoring in a foreground service | `android-native/` | `android-native/app/src/main/java/com/safedrive/ai/service/DrivingModeService.kt` | Implemented |
| AN-02 | Request and use Android runtime permissions safely | `android-native/` | `android-native/app/src/main/AndroidManifest.xml`, `android-native/app/src/main/java/com/safedrive/ai/MainActivity.kt` | Implemented |
| AN-03 | Persist trips, risk events, and crash alert attempts locally | `android-native/` | `android-native/app/src/main/java/com/safedrive/ai/data/local/`, `android-native/app/src/main/java/com/safedrive/ai/data/local/SafeDriveDatabase.kt` | Implemented |
| AN-04 | Provide crash countdown and escalation flow | `android-native/` | `android-native/app/src/main/java/com/safedrive/ai/ui/CrashCountdownActivity.kt`, `android-native/app/src/main/java/com/safedrive/ai/service/DrivingModeService.kt` | Implemented |
| AN-05 | Provide native settings for contacts, demo number, and TTS | `android-native/` | `android-native/app/src/main/java/com/safedrive/ai/ui/SettingsScreen.kt`, `android-native/app/src/main/java/com/safedrive/ai/data/SettingsRepository.kt` | Implemented |
| AN-06 | Provide native history/export UI comparable to Flutter reports | `android-native/` | not yet present | Partial |
| SH-01 | Keep unified architecture, ethics, evaluation, references, and demo docs | Shared | `docs/`, `demo/` | Implemented |
| ML-01 | Maintain a shared ML training/export workspace | `ml/` | `ml/README.md`, `ml/requirements.txt`, folder scaffold | Scaffold only |
| FUT-01 | Add a direct Flutter-native runtime bridge | Deferred | intentionally not implemented in this merge | Deferred |
