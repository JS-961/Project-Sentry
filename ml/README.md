# ML Workspace

This folder is the shared machine-learning scaffold for Project Sentry.

Current role:

- collect anonymized training and evaluation data
- prototype preprocessing and model experiments
- export future TensorFlow Lite artifacts for Android integration

Current status:

- scaffold only
- not wired into either app as a production feature yet

Suggested workflow:

1. Place anonymized datasets under `ml/data/`.
2. Implement reusable preprocessing and export code in `ml/src/`.
3. Use `ml/notebooks/` for exploratory work only.
4. Save trained or converted artifacts under `ml/models/`.
5. Document any app integration before claiming ML functionality in demos.

Privacy rules:

- never commit personal identifiers
- never commit raw sensitive location traces
- keep only the minimum data needed for validation and model development
