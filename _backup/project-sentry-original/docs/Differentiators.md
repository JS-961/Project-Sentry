Differentiators (Positioning Notes)

Goal: Fill a practical gap by making crash detection transparent, privacy-first,
and research-friendly rather than a black-box alert.

Proposed Differentiators
- Transparency: show detection reason + severity to the user.
- Privacy-first: local storage by default, no cloud required.
- Data ownership: exportable JSON logs for reports or audits.
- Calibration-friendly: thresholds visible and configurable.
- Cross-platform: one codebase for Android + web + Windows demos.

How the Current MVP Supports This
- Detection engine exposes reason + severity.
- Event log is persisted locally and can be exported.
- Modular sensor source (simulated now, real sensors later).
