# Evidence Status and Future Validation Notes

This file records manual submission items and future validation opportunities. The current final draft includes screenshots, diagrams, survey evidence, preliminary lists, and the generated Word report.

## High Priority

### 1. Administrative and Team Details

- Missing:
  - final student signatures
  - final supervisor/advisor signature
  - coordinator signature or department-approved equivalent
- Needed for:
  - signature page
  - final submission formatting
- Action:
  - keep the filled title-page metadata as is
  - replace the blank signature lines with final approvals before submission

### 2. Future Testing Results and Metrics

- Future validation opportunities:
  - detection latency measurements
  - false alarms per hour
  - SMS/call/TTS success summary table
  - risk-event accuracy or threshold-behavior summary
  - physical-device comparison between advisory ML labels and safe observed traces
  - final scenario PASS/CHECK results from Flutter
- Current treatment:
  - the report documents current evidence honestly and frames broader measured field validation as future work

### 3. Final Screenshots and Figures

- Current state:
  - Android, Flutter, Mermaid, VS Code, and survey screenshots are captured under `docs/Final_Report/figures/`
  - Figure 10 is intentionally combined into Figure 9
  - Figure 15 is intentionally omitted as redundant with Figures 8 and 14
  - the complete 11-part survey screenshot sequence is preserved in Appendix F
- Action:
  - review the generated Word output for image sizing and page breaks
  - keep screenshots free of real personal numbers or sensitive location data

### 4. Reference Formatting

- Manual check:
  - final consistency pass after the last Word/PDF formatting step
- Action:
  - keep APA as the chosen style
  - verify that any newly inserted figure captions or text additions use the same in-text citation style

## Medium Priority

### 5. Entrepreneurial Validation Evidence

- Current state:
  - survey screenshots are inserted as Figures 16-18
  - the full 11-part survey sequence is inserted in Appendix F
- Optional strengthening:
  - response count and distribution note, if the team wants a stronger survey-method paragraph
  - larger survey results, if the team wants stronger user validation
- Action:
  - keep the current competitor matrix and lightweight interview evidence
  - summarize the survey only as strongly as the response count supports

### 6. Timeline, Milestones, and Teamwork Proof

- Optional:
  - final visual Gantt chart if the team wants one in the PDF
  - advisor meeting records
- Action:
  - use the repository-backed timeline and role split already present in the draft
  - add advisor checkpoints only if they can be supported

### 7. Flutter Execution Evidence

- Current state:
  - Flutter test files exist in the repository
  - Flutter CLI was not found on this documentation machine during the May 2, 2026 pass
- Optional/future:
  - fresh `flutter test` run result
  - fresh `flutter run` or demo verification notes
- Action:
  - rerun on a machine with Flutter SDK installed
  - archive terminal output or summarize pass/fail status in the report

### 8. Android Device Matrix

- Optional/future:
  - exact device model(s)
  - Android version(s)
  - which features were tested on emulator vs phone
- Action:
  - create a small table with columns:
    - device/emulator
    - Android version
    - tested features
    - observed issues

## Lower Priority but Valuable

### 9. Appendix Content

- Optional:
  - supporting logs for the appendices, if desired
- Action:
  - core appendix content is now drafted in the final-report draft; add visuals or logs only if they strengthen submission quality

### 10. Polished Figure Exports

- Current state:
  - rendered Mermaid diagrams are inserted as Figures 1-3
  - figure numbering is preserved, with Figure 10 combined into Figure 9 and Figure 15 omitted as redundant
- Action:
  - review final Word image sizing and page breaks
  - regenerate the final PDF after Word review

### 11. Optional Supporting Artifacts

- Optional:
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
- advisory ML training/export artifacts under `ml/models/advisory-risk/`
- Android advisory JSON model assets under `android-native/app/src/main/assets/`
- Android Gradle verification on May 2, 2026:
  - `:app:assembleDebug` succeeded
  - `:app:testDebugUnitTest` succeeded with `NO-SOURCE`
  - `:app:lintDebug` succeeded

## Recommended Final Submission Steps

1. Review the final Word report for image sizing and page breaks.
2. Add final signatures or department approval equivalents.
3. Add optional measured tables only if the team wants stronger quantitative evidence.
4. Export one clean final PDF.
