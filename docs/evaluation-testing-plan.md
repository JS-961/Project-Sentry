# Evaluation And Testing Plan

Project Sentry now has two evaluation tracks because the repository contains two
purpose-built apps.

## Evaluation Goals

- demonstrate believable crash-detection and driving-risk workflows
- show repeatable evidence for the capstone
- validate that the native Android runtime behaves safely under permissions and
  service constraints
- avoid overstating performance beyond what was actually tested

## Track A: Flutter Demo And Validation App

Use `flutter-app/` for:

- deterministic scenario playback
- repeatable PASS/CHECK scenario batches
- live Android trace capture
- replay of recorded traces
- export of JSON, CSV, Markdown, HTML, and ZIP report artifacts

Recommended checks:

- run all scripted scenarios in Scenario Lab
- record safe live traces on Android devices
- replay saved traces and compare expected vs actual trigger result
- verify results dashboard and report-pack generation

Key metrics:

- scenario PASS/CHECK counts
- false positives during safe traces
- peak acceleration and jerk values
- replay consistency
- export completeness

## Track B: Native Android MVP Runtime

Use `android-native/` for:

- foreground service startup and shutdown
- permission request behavior
- sensor and location monitoring
- crash countdown behavior
- SMS, call, and TTS fallback/status handling
- Room persistence of trips and events

Recommended checks:

- start and stop Driving Mode cleanly
- verify notification and foreground-service behavior
- test simulate-crash flow
- test permission denied and permission revoked scenarios
- verify event and crash alert rows are written to Room

Key metrics:

- crash countdown activation latency
- permission-denied handling quality
- event counters and risk score responsiveness
- background reliability during a short monitored session
- alert outcome logging

## Shared Capstone Evidence

Collect and preserve:

- screenshots from Flutter presentation mode and results screens
- screenshots or recordings from the native Android countdown and settings flow
- exported report packs from Flutter
- native Android run notes for service, permissions, and persistence

## Suggested Demo-Ready Acceptance Targets

- Flutter scenario lab produces repeatable PASS/CHECK evidence.
- Flutter report pack can be generated in a few clicks.
- Native Android app can start Driving Mode, simulate a crash, and show the
  countdown/escalation path.
- The repository documentation explains clearly which app owns which behavior.

## Non-Negotiable Testing Ethics

- no real crash recreation
- no real emergency calls
- no dangerous driving
- no misleading claims about ML or production readiness
