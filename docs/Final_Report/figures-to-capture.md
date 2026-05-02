# Figures to Capture

This checklist is cross-referenced against `docs/Final_Report/final-report-draft.md`.
The report preserves Figure 1 through Figure 18 numbering, but Figure 10 is
combined into Figure 9 and Figure 15 is intentionally omitted as redundant.
Normalized assets now live in `docs/Final_Report/figures/`.

Keep all screenshots free of personal phone numbers, real contacts, sensitive
locations, private survey identifiers, and real emergency-service targets. Use
demo-safe numbers only.

## Fast Setup

Android native MVP:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
& "$env:ANDROID_HOME\emulator\emulator.exe" -avd Medium_Phone_API_35
```

In a second terminal:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
& "$env:ANDROID_HOME\platform-tools\adb.exe" devices
cd android-native
.\gradlew.bat installDebug
```

Flutter app:

```powershell
cd flutter-app
flutter pub get
flutter run -d chrome
```

Useful Flutter alternatives:

```powershell
flutter run -d windows
flutter run -d android
```

Mermaid diagrams:

- Open `docs/Diagrams.md`.
- Render the Mermaid block for the matching figure using GitHub, a VS Code
  Mermaid preview extension, Obsidian, Mermaid Live Editor, or another renderer.
- Export PNG/SVG at high resolution so labels stay readable in the final PDF.

## Capture Order

Capture in this order for efficiency:

1. Render Mermaid Figures 1-3 from `docs/Diagrams.md`.
2. Capture Android setup/status/settings first, then active monitoring, countdown,
   and history.
3. Capture Flutter Lab, Validation Tools, Results, Presentation Mode, then the
   generated report-pack files in VS Code.
4. Capture survey overview, survey charts, and open-ended response themes.

## Current Capture Status

| Figure | Status | Normalized Asset(s) |
| --- | --- | --- |
| Figure 1 | Captured | `figures/fig-01-use-case-diagram.png` |
| Figure 2 | Captured | `figures/fig-02-system-architecture.png` |
| Figure 3 | Captured | `figures/fig-03-runtime-flow.png` |
| Figure 4 | Captured, two parts | `figures/fig-04a-android-drive-status-risk.png`, `figures/fig-04b-android-drive-counters-crash-test.png` |
| Figure 5 | Captured, two parts | `figures/fig-05a-android-settings-readiness-contacts.png`, `figures/fig-05b-android-settings-call-tts-tools.png` |
| Figure 6 | Captured | `figures/fig-06-android-crash-countdown.png` |
| Figure 7 | Captured, two parts | `figures/fig-07a-android-history-trip-risk.png`, `figures/fig-07b-android-history-alerts.png` |
| Figure 8 | Captured, two parts | `figures/fig-08a-android-status-preflight-runtime.png`, `figures/fig-08b-android-status-local-data-checklist.png` |
| Figure 9 | Captured, combined with Figure 10 | `figures/fig-09-flutter-lab-validation-combined.png` |
| Figure 10 | Combined/omitted | Covered by Figure 9 because the same screenshot includes the Validation Tools card and location-permission note |
| Figure 11 | Captured, two parts | `figures/fig-11a-flutter-results-summary-chart.png`, `figures/fig-11b-flutter-results-exports-preview.png` |
| Figure 12 | Captured | `figures/fig-12-flutter-presentation-mode.png` |
| Figure 13 | Captured | `figures/fig-13-vscode-report-pack-artifacts.png` |
| Figure 14 | Captured | `figures/fig-14-android-first-run-permissions.png` |
| Figure 15 | Omitted/redundant | Permission-readiness evidence is already visible in Figures 8 and 14 |
| Figure 16 | Captured | `figures/fig-16-survey-overview.png` |
| Figure 17 | Captured, nine parts | `figures/fig-17a-survey-chart-01.png` through `figures/fig-17i-survey-chart-09.png` |
| Figure 18 | Captured | `figures/fig-18-survey-open-ended-themes.png` |

## Master Screenshot Checklist

### Figure 1. Project Sentry use-case diagram.

- Output file: `fig-01-use-case-diagram.png`
- Report location: Chapter 3.2, Requirements and Use Cases.
- Source: `docs/Diagrams.md`, section `Figure 1: Project Sentry Use-Case Diagram`.
- How to capture:
  1. Open `docs/Diagrams.md`.
  2. Render the first Mermaid block under Figure 1.
  3. Export the rendered diagram as PNG/SVG.
- Must be visible:
  - `Driver / Tester`
  - `Phone Sensors`
  - `Emergency Contact`
  - the ten use cases from the report, including configuration, driving mode,
    sensor monitoring, risk events, countdown, cancel, escalation, history,
    Flutter scenario lab, and trace/report export.
- Do not capture the raw Mermaid text unless the renderer is unavailable.

### Figure 2. Project Sentry system architecture.

- Output file: `fig-02-system-architecture.png`
- Report location: Chapter 3.3, System Architecture.
- Source: `docs/Diagrams.md`, section `Figure 2: Project Sentry System Architecture`.
- How to capture:
  1. Open `docs/Diagrams.md`.
  2. Render the Figure 2 Mermaid block.
  3. Export the rendered diagram.
- Must be visible:
  - `android-native/`
  - `flutter-app/`
  - `docs/`
  - `demo/`
  - `ml/`
  - Android runtime responsibilities: foreground service, permissions, sensors,
    location, Room, crash countdown, SMS/call/TTS, advisory ML.
  - Flutter responsibilities: scenario lab, live capture, replay, results,
    presentation mode, report pack.
  - Reviewer/instructor evidence path.

### Figure 3. Crash-detection and escalation runtime flow.

- Output file: `fig-03-runtime-flow.png`
- Report location: Chapter 3.3, System Architecture.
- Source: `docs/Diagrams.md`, section `Figure 3: Crash-Detection And Escalation Runtime Flow`.
- How to capture:
  1. Open `docs/Diagrams.md`.
  2. Render the Figure 3 Mermaid block.
  3. Export the rendered diagram.
- Must be visible:
  - Start Driving Mode.
  - Permission readiness.
  - Foreground `DrivingModeService`.
  - accelerometer, gyroscope, and fused location samples.
  - risk scoring and candidate validation.
  - advisory ML shown as context only.
  - countdown, cancel, timeout or `Call Now`.
  - SMS/call/TTS escalation.
  - Room persistence for trips, risk events, and crash alerts.

### Figure 4. Android Drive tab during active monitoring.

- Output files: `fig-04a-android-drive-status-risk.png`, `fig-04b-android-drive-counters-crash-test.png`
- Report location: Chapter 4.6, Screenshots and Figure Placeholders.
- Source: Android native app.
- How to activate:
  1. Launch the native Android app on the emulator or device.
  2. Grant permissions if prompted.
  3. Tap `Settings` in the top bar and save demo-safe values in:
     - `Emergency Contacts`
     - `Call Escalation`
     - `TTS Message`
  4. Return to the app.
  5. Open the bottom `Drive` tab.
  6. Tap `Start Drive`.
  7. Wait until the `Drive Status` card says `Monitoring` or until GPS/sensor
     status begins updating.
- Must be visible:
  - top title `Sentry`
  - bottom tab `Drive` selected
  - `Drive Status`
  - `Monitoring` or active driving state
  - trip duration and speed
  - `Location` and `Sensors` status
  - `Live Risk`
  - rule risk score
  - `ML advisory`, label/score/confidence, and model source
  - `Event Counters`
  - `Simulate Crash Countdown` button
- If the whole page does not fit, prioritize the upper Drive status plus the
  `Live Risk` card, then take a second crop including `Event Counters` and
  `Simulate Crash Countdown` if needed.

### Figure 5. Android Settings screen with crash-flow configuration.

- Output files: `fig-05a-android-settings-readiness-contacts.png`, `fig-05b-android-settings-call-tts-tools.png`
- Report location: Chapter 4.6, Screenshots and Figure Placeholders.
- Source: Android native app.
- How to activate:
  1. From the Android app, tap `Settings` in the top bar.
  2. Enter demo-safe phone numbers only.
  3. Save the configuration with `Save Configuration`.
  4. Stay on the Settings page for the screenshot.
- Must be visible:
  - screen title `Settings`
  - `Crash Flow Readiness`
  - `Location`, `Notifications`, `SMS`, and `Phone call`
  - `Emergency Contacts`
  - `Call Escalation`
  - `TTS Message`
  - if possible, `Flow Tools`, `Save Configuration`, or `Test Full Crash Flow`
- Redact or replace any real number before the screenshot. Prefer a demo number
  such as `+96170123456`.

### Figure 6. Android crash countdown and escalation confirmation screen.

- Output file: `fig-06-android-crash-countdown.png`
- Report location: Chapter 4.6, Screenshots and Figure Placeholders.
- Source: Android native app, `CrashCountdownActivity`.
- How to activate:
  1. Finish Settings with demo contacts and a demo call number.
  2. Open the `Drive` tab.
  3. Tap `Start Drive`.
  4. Scroll to `Crash Flow Test`.
  5. Tap `Simulate Crash Countdown`.
  6. Capture immediately before the timer expires.
- Must be visible:
  - `Potential Crash Detected`
  - `Are you OK?`
  - numeric countdown timer
  - progress bar
  - escalation explanation mentioning SMS, call flow, and TTS
  - `Escalation target`
  - `SMS contacts`
  - `Call number`
  - `I'm OK - Cancel Alert`
  - `Call Now`
- Use only demo-safe numbers.

### Figure 7. Android History tab with persisted trips and alert logs.

- Output files: `fig-07a-android-history-trip-risk.png`, `fig-07b-android-history-alerts.png`
- Report location: Chapter 4.6, Screenshots and Figure Placeholders.
- Source: Android native app.
- How to activate:
  1. Start and stop Driving Mode once so a trip record exists.
  2. Trigger `Simulate Crash Countdown`.
  3. Press `I'm OK - Cancel Alert` or let the test flow escalate with demo data.
  4. Return to the app.
  5. Open the bottom `History` tab.
- Must be visible:
  - bottom tab `History` selected
  - `Trip Summary`
  - `Recent Trips`
  - `Recent Risk Events`, if any exist
  - `Crash Alerts`
  - at least one persisted trip or alert outcome
  - any SMS/call/TTS status row if available
- If no risk events appear, the screenshot is still usable if it shows persisted
  trips and crash alerts.

### Figure 8. Android Status tab showing permissions and readiness.

- Output files: `fig-08a-android-status-preflight-runtime.png`, `fig-08b-android-status-local-data-checklist.png`
- Report location: Chapter 4.6, Screenshots and Figure Placeholders.
- Source: Android native app.
- How to activate:
  1. Configure demo contacts and call number in Settings.
  2. Grant all requested permissions.
  3. Open the bottom `Status` tab.
  4. If possible, start Driving Mode first so ML values are populated, then
     return to `Status`.
- Must be visible:
  - bottom tab `Status` selected
  - `Preflight Checklist`
  - `Location permission`
  - `Notifications`
  - `SMS permission + contact`
  - `Phone permission + number`
  - `Runtime Health`
  - `ML Advisory`
  - `ML result`
  - `ML confidence`
  - `Local Data`
- This is the best screenshot for proving permissions plus advisory ML readiness.

### Figure 9. Flutter Scenario Lab with deterministic playback controls.

- Output file: `fig-09-flutter-lab-validation-combined.png`
- Report location: Chapter 4.6, Screenshots and Figure Placeholders.
- Source: Flutter app.
- How to activate:
  1. Run the Flutter app.
  2. Use the bottom navigation bar and tap `Lab`.
  3. On `Scenario Lab`, choose any predefined scenario from the `Scenario`
     dropdown.
  4. Tap `Run Scenario` or `Run All Scenarios`.
  5. Wait until the result is shown/logged.
- Must be visible:
  - screen title `Scenario Lab`
  - `Scenario` dropdown
  - selected scenario description
  - `Expected trigger`
  - `Scenario Actions`
  - `Run Scenario` or `Run All Scenarios`
  - a visible PASS/CHECK-style result if available, such as `Scenario PASS`,
    `Scenario CHECK`, or a populated `Trace Results` card.

### Figure 10. Flutter live-capture validation card with location-permission notice.

- Status: Combined into Figure 9.
- Output file: none.
- Report location: Chapter 4.6, Screenshots and Figure Placeholders.
- Source: Flutter app.
- How to activate:
  1. Run the Flutter app.
  2. Tap bottom navigation `Lab`.
  3. Scroll until `Validation Tools` is visible.
- Must be visible:
  - `Validation Tools`
  - the note: `Live capture uses real device accelerometer values. GPS speed is added when location permission is available.`
  - `Trace label`
  - `Phone placement`
  - `Expected replay result`
  - `Recorded trace` or `No saved traces yet`
  - `Start Live Capture`
  - `Replay Selected Trace`, if a trace is selected.
- If capturing from Chrome/Windows, the live accelerometer may not behave like a
  phone. The figure is about the validation UI and permission note, not proof of
  field sensor accuracy.

### Figure 11. Flutter Results Summary screen with evaluation metrics.

- Output files: `fig-11a-flutter-results-summary-chart.png`, `fig-11b-flutter-results-exports-preview.png`
- Report location: Chapter 4.6, Screenshots and Figure Placeholders.
- Source: Flutter app.
- How to activate:
  1. Run one or more scenarios in the `Lab` tab.
  2. Tap bottom navigation `Results`.
  3. Scroll only as needed to keep the summary and export controls visible.
- Must be visible:
  - screen title `Results Summary`
  - `Scenario Results`
  - total scenarios
  - `Scenario PASS`
  - `Scenario CHECK`
  - pass rate
  - `Scenario Results Chart`
  - `Reports & Charts`
  - `Save Chart Image (PNG)`
  - `Save Report (Markdown)`
  - `Save Report (HTML)`
  - `Save Scenario Outcomes (CSV)`
  - `Save Final Report Pack`

### Figure 12. Flutter Presentation Mode screen used in capstone demos.

- Output file: `fig-12-flutter-presentation-mode.png`
- Report location: Chapter 4.6, Screenshots and Figure Placeholders.
- Source: Flutter app.
- How to activate:
  1. Open the Flutter app.
  2. Stay on the `Home` tab.
  3. Tap the presentation icon in the top app bar. Tooltip: `Presentation Mode`.
  4. On the Presentation Mode screen, optionally tap `Start Demo Trip`.
- Must be visible:
  - screen title `Presentation Mode`
  - `Project Sentry`
  - `Live Demo View`
  - `Trip Status`
  - `Live Metrics`
  - decision card or no-decision state
  - `Start Demo Trip` or `Stop Demo Trip`
  - `Simulate Crash`
- This screenshot supports the class-demo story, not the Android production path.

### Figure 13. VS Code view of exported report-pack artifacts.

- Output file: `fig-13-vscode-report-pack-artifacts.png`
- Report location: Chapter 4.6, Screenshots and Figure Placeholders.
- Source: VS Code Explorer after Flutter export.
- How to activate:
  1. In Flutter, run at least one scenario.
  2. Open `Results`.
  3. Tap `Save Final Report Pack`.
  4. Note the output folder shown by the app.
  5. In VS Code Explorer, open or reveal the generated output folder.
- Must be visible:
  - generated Markdown report
  - generated HTML report
  - generated CSV output
  - generated JSON output
  - generated chart PNG, if present
  - generated ZIP or final report pack folder
  - VS Code folder context so the files are clearly project artifacts.
- If the browser saves files into Downloads, move only the screenshot focus to
  the exported folder view. Do not expose unrelated personal files.

### Figure 14. Android first-run permission request sequence.

- Output file: `fig-14-android-first-run-permissions.png`
- Report location: Chapter 5.5, Permission-Flow Testing.
- Source: Android native app plus Android system permission prompts.
- How to activate:
  1. Use a clean install or clear app data.
  2. Launch the Android app.
  3. Tap `Start Drive` or otherwise start the flow that requires permissions.
  4. Capture the app explanation dialog titled `Before Permissions`.
  5. Continue and capture Android system prompts for location, notifications,
     SMS, and phone call as they appear.
  6. After granting, capture the app showing the ready state.
- Must be visible across the sequence or collage:
  - `Before Permissions`
  - explanation text for Location, SMS, Phone call, and Notifications
  - Android permission prompts
  - a granted or ready state after approval.
- Best format: one collage containing 3-5 narrow mobile screenshots.

### Figure 15. Android permissions checklist for crash-flow readiness.

- Status: Omitted as redundant with Figures 8 and 14.
- Output file: none.
- Report location: Chapter 5.5, Permission-Flow Testing.
- Source: Android native app.
- How to activate option A:
  1. Open `Settings`.
  2. Capture the `Crash Flow Readiness` card.
- How to activate option B:
  1. Open bottom tab `Status`.
  2. Capture `Preflight Checklist`.
- Must be visible:
  - `Location`
  - `Notifications`
  - `SMS`
  - `Phone call`
  - or the Status equivalents `Location permission`, `SMS permission + contact`,
    and `Phone permission + number`.
- Figure 15 is not inserted separately in the report. Use Figure 8 as the broad
  Status and ML-readiness proof, and Figure 14 as the permission-flow proof.

### Figure 16. Survey response overview for Project Sentry.

- Output file: `fig-16-survey-overview.png`
- Report location: Chapter 6.4, Survey Evidence Placeholders.
- Source: the team's survey tool/results dashboard.
- How to activate:
  1. Open the survey form or survey dashboard.
  2. Go to the responses/results area.
     - Google Forms: open the form, then `Responses`, then `Summary`.
     - Microsoft Forms: open the form, then `Responses`.
     - SurveyMonkey or similar: open `Analyze Results` or the response overview.
  3. Capture the top overview area.
- Must be visible:
  - survey title or enough context to identify Project Sentry
  - total response count
  - responses/results tab or overview context
  - no names, emails, phone numbers, or private respondent identifiers.
- Crop browser bookmarks/account details if they reveal personal info.

### Figure 17. Survey response charts for trust, privacy, and adoption questions.

- Output files: `fig-17a-survey-chart-01.png` through `fig-17i-survey-chart-09.png`
- Report location: Chapter 6.4, Survey Evidence Placeholders.
- Source: the team's survey tool/results dashboard.
- How to activate:
  1. Open survey responses/results.
  2. Stay in the summary/chart view.
  3. Scroll to the strongest chart questions.
  4. Capture one combined screenshot or a clean collage.
- Must include charts for at least two of these, ideally three or four:
  - willingness to use a crash-alert app
  - trust in countdown confirmation before escalation
  - privacy comfort with location/contact use
  - perceived usefulness for local road-safety context
  - willingness to share location with emergency contacts
- Must be visible:
  - question text
  - chart bars/pie/percentages
  - response counts or percentages
  - no private identifiers.
- If chart labels are too small, use browser zoom at 110-125 percent before
  screenshotting.

### Figure 18. Survey open-ended response themes.

- Output file: `fig-18-survey-open-ended-themes.png`
- Report location: Chapter 6.4, Survey Evidence Placeholders.
- Source: survey results, exported CSV, or a manually synthesized theme chart.
- How to activate option A, direct survey screenshot:
  1. Open the survey results.
  2. Scroll to the open-ended question results.
  3. Capture a redacted view of responses.
- How to activate option B, safer theme chart:
  1. Export survey responses to CSV.
  2. Create a small chart or table summarizing themes.
  3. Use themes such as emergency-contact trust, phone-placement concern,
     false-alarm tolerance, privacy concern, and local road-safety concern.
- Must be visible:
  - either redacted open-ended responses or a synthesized theme chart
  - no names, emails, phone numbers, or identifiable anecdotes
  - theme labels and counts/mentions if using a synthesized chart.
- Prefer option B if raw comments include personal details.

## Suggested Table List

These are not figures, but they should likely appear as numbered tables in the
final report.

| Proposed Table | Purpose | Status |
| --- | --- | --- |
| Table 1 Requirements traceability summary | Map major requirements to implementation | Drafted |
| Advisory ML evaluation summary | Summarize strict grouped and controlled random model metrics | Drafted |
| Evaluation metrics and current status | Show what is measured vs future validation work | Drafted |
| Android module summary | Summarize service, sensors, Room, countdown, escalation, and advisory ML | Drafted in prose |
| Flutter prototype contribution summary | Explain why Flutter remains in the repository | Drafted in prose |
| Device and testing matrix | Distinguish emulator vs physical-device validation if the team adds one later | Future validation |
| Competitor comparison matrix | Support entrepreneurial chapter | Drafted |

## Final Asset Naming List

- `fig-01-use-case-diagram.png`
- `fig-02-system-architecture.png`
- `fig-03-runtime-flow.png`
- `fig-04a-android-drive-status-risk.png`
- `fig-04b-android-drive-counters-crash-test.png`
- `fig-05a-android-settings-readiness-contacts.png`
- `fig-05b-android-settings-call-tts-tools.png`
- `fig-06-android-crash-countdown.png`
- `fig-07a-android-history-trip-risk.png`
- `fig-07b-android-history-alerts.png`
- `fig-08a-android-status-preflight-runtime.png`
- `fig-08b-android-status-local-data-checklist.png`
- `fig-09-flutter-lab-validation-combined.png`
- Figure 10 has no separate file; it is combined into Figure 9.
- `fig-11a-flutter-results-summary-chart.png`
- `fig-11b-flutter-results-exports-preview.png`
- `fig-12-flutter-presentation-mode.png`
- `fig-13-vscode-report-pack-artifacts.png`
- `fig-14-android-first-run-permissions.png`
- Figure 15 has no separate file; it is omitted as redundant with Figures 8 and 14.
- `fig-16-survey-overview.png`
- `fig-17a-survey-chart-01.png`
- `fig-17b-survey-chart-02.png`
- `fig-17c-survey-chart-03.png`
- `fig-17d-survey-chart-04.png`
- `fig-17e-survey-chart-05.png`
- `fig-17f-survey-chart-06.png`
- `fig-17g-survey-chart-07.png`
- `fig-17h-survey-chart-08.png`
- `fig-17i-survey-chart-09.png`
- `fig-18-survey-open-ended-themes.png`

## Capture Notes

- Hicham can focus on Android rows.
- Jawad can focus on Flutter, VS Code, Mermaid, and survey rows.
- Prefer portrait screenshots for Android app figures.
- Prefer desktop-width screenshots for Flutter web/Windows figures.
- Capture screenshots after the app is configured, except Figure 14, which is
  explicitly about first-run permission behavior.
- For survey screenshots, aggregate or redacted evidence is enough. Do not show
  respondent identity.
- For diagrams, export high-resolution images rather than screenshotting tiny
  Markdown previews.
