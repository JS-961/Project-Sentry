# Missing Evidence and TODOs

This file tracks what is still missing before `final-report-draft.md` can become a strong 40-60 page final submission.

## High Priority

### 1. Administrative and Team Details

- Missing:
  - student names
  - student IDs
  - supervisor/advisor name
  - department/semester/date
  - signature-page details
- Needed for:
  - title page
  - signature page
  - final submission formatting
- Action:
  - fill in all official course metadata from the department template

### 2. Final Testing Results and Metrics

- Missing:
  - detection latency measurements
  - false alarms per hour
  - SMS/call/TTS success summary table
  - risk-event accuracy or threshold-behavior summary
  - final scenario PASS/CHECK results from Flutter
- Needed for:
  - Chapter 5: Testing and Evaluation
  - Chapter 8: Results and Discussion
- Action:
  - run the final Flutter scenario batch
  - collect Android simulated crash runs on device
  - summarize results in tables with date, device, permissions, outcome

### 3. Final Screenshots and Figures

- Missing:
  - Android Drive tab screenshot
  - Android Settings screenshot
  - Android crash countdown screenshot
  - Android History screenshot
  - Android Status screenshot
  - Flutter Scenario Lab screenshot
  - Flutter Results Summary screenshot
  - Flutter Presentation Mode screenshot
  - exported report pack screenshot or figure
- Needed for:
  - Chapters 3, 4, and 5
  - List of Figures
- Action:
  - follow `figures-to-capture.md`
  - capture on clean demo data
  - avoid real personal numbers or sensitive location data in screenshots

### 4. Reference Formatting

- Missing:
  - final IEEE or APA entries for all cited sources
  - consistent in-text citation style
- Needed for:
  - Chapter 2
  - Chapter 6
  - References section
- Action:
  - convert the URLs and source names already listed in `docs/references.md` and `docs/research/validation-research-plan.md` into a final bibliography
  - do not add any citation that cannot be verified

## Medium Priority

### 5. Entrepreneurial Validation Evidence

- Missing:
  - survey results
  - interview summary
  - competitor comparison matrix
- Needed for:
  - Chapter 6: Entrepreneurial / Innovation Aspects
- Action:
  - if time allows, collect at least a lightweight response set
  - if not collected, keep Chapter 6 literature-driven and clearly label the missing user-validation evidence

### 6. Timeline, Milestones, and Teamwork Proof

- Missing:
  - actual timeline or Gantt chart
  - milestone dates
  - advisor meeting records
  - exact team-role assignments
- Needed for:
  - Chapter 7: Project Management and Teamwork
  - CLO5 alignment
- Action:
  - rebuild a simple milestone chart from meeting notes, commit history, or team memory
  - add advisor checkpoints only if they can be supported

### 7. Flutter Execution Evidence

- Current state:
  - Flutter test files exist in the repository
  - Flutter CLI was not installed on this documentation machine on April 26, 2026
- Missing:
  - fresh `flutter test` run result
  - fresh `flutter run` or demo verification notes
- Needed for:
  - stronger Chapter 5 evidence
- Action:
  - rerun on a machine with Flutter SDK installed
  - archive terminal output or summarize pass/fail status in the report

### 8. Android Device Matrix

- Missing:
  - exact device model(s)
  - Android version(s)
  - which features were tested on emulator vs phone
- Needed for:
  - Chapter 5 credibility
  - limitations discussion
- Action:
  - create a small table with columns:
    - device/emulator
    - Android version
    - tested features
    - observed issues

## Lower Priority but Valuable

### 9. Appendix Content

- Missing:
  - consolidated installation guide
  - user manual
  - structured test-case appendix
  - dataset/model appendix
  - repository-structure appendix
- Action:
  - assemble from existing READMEs and docs once main chapters are stable

### 10. Polished Figure Exports

- Missing:
  - rendered Mermaid diagrams as PNG/SVG
  - consistent figure numbering and captions
- Action:
  - export the Mermaid diagrams from `docs/diagrams.md`
  - name the files consistently for the final document

### 11. Optional Supporting Artifacts

- Missing:
  - meeting logs
  - commit or PR screenshots
  - report pack ZIP artifact
  - lint/test report screenshots
- Action:
  - include only if they strengthen the appendix and can be verified easily

## Already Supported by Current Repository Evidence

- official report structure and rubric categories
- Android-native/Flutter split and rationale
- Android foreground service, permissions, sensors, location, countdown, SMS/call/TTS, Room
- Flutter scenario lab, live capture, replay, results summary, report exports
- ethics, limitations, and future-work documentation
- Android Gradle verification on April 26, 2026:
  - `:app:assembleDebug` succeeded
  - `:app:testDebugUnitTest` succeeded with `NO-SOURCE`
  - `:app:lintDebug` succeeded

## Recommended Next Evidence to Collect

1. Final screenshots for both apps.
2. One clean Android simulated-crash test table.
3. One final Flutter scenario-batch export or report pack.
4. Team/admin metadata and timeline.
5. Final formatted bibliography.
