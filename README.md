# Project Sentry
Mobile Crash Detection & Response (Capstone)

This project focuses on detecting a potential vehicle crash using phone sensors,
confirming the user's status, and alerting emergency contacts with location.
The goal is a reliable, demo-ready MVP within a short timeline.

## MVP Goals
- Detect a suspected crash event from motion/location signals
- Prompt the user with a short "I'm OK / Get Help" timer
- Alert emergency contacts with GPS location if no response
- Log events locally for review and reporting (exportable JSON)
- Privacy-first: data stays on device unless user opts to share

## Non-Goals (for MVP)
- Active crash prevention or vehicle control
- Guaranteed detection of all crashes
- Direct emergency service dispatch
- Insurance integration

## Repo Layout
- docs/ - scope, architecture, evaluation, report outline
- app/ - mobile app source (Flutter)
- scripts/ - small utilities for logs or data processing

## Docs
- docs/MVP.md
- docs/Architecture.md
- docs/Testing-Evaluation.md
- docs/Differentiators.md
- docs/Validation-Research-Plan.md
- docs/Validation-Workflow.md
- docs/Survey-Market-Research.md

## Timeline (Target: under 4 weeks)
- Week 1: scope, UX flow, architecture, app skeleton
- Week 2: sensor capture + rule-based detection
- Week 3: alert flow + logging + polish
- Week 4: evaluation, demo prep, report writing
