Feature Priorities (Rubric-Driven)

Goal: emphasize features that map directly to grading criteria across the
rubrics, proposal, design report, and final report templates.

Must-Emphasize Features (Product)
1) Detection Core
- Real-time crash detection pipeline (sensor -> features -> decision)
- Transparent decision reason + severity shown to user
- Configurable thresholds to demonstrate design flexibility

2) Safety + Response Flow
- Confirmation UI with countdown ("I'm OK" / "Get Help")
- Emergency contact alert (SMS placeholder or in-app mock)
- Clear safety disclaimers and manual cancel

3) Evidence & Evaluation
- Local event logging persisted on device
- Exportable JSON for testing/results tables
- Simulation mode + scenario playback for repeatable tests

4) Accessibility & Usability
- Large buttons, minimal steps, readable status
- Offline-first behavior where possible

5) Privacy & Security
- On-device storage by default
- Explicit permissions and opt-in sharing

Why These Matter (Rubric Mapping)
- CLO1 / Proposal: Problem analysis + gap + justification -> transparency, privacy-first,
  exportable logs (market gap).
- CLO2: Feasibility + innovation -> simulation for safe testing; configurable thresholds.
- CLO3: Design + implementation + testing -> modular sensor source, detection engine,
  test suite, metrics.
- CLO4: Communication -> clean UI + exportable evidence for reports and demos.
- CLO5: Project management -> measurable milestones and testing artifacts.

Evidence to Capture as You Build
- Screenshots of UI and workflow
- Test cases and results (unit + simulated scenarios)
- Sample exported event logs (JSON)
- Architecture diagram + data flow
- Risk list and mitigation notes
