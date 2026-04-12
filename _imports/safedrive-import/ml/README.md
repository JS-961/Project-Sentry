# ML Workspace (SafeDrive AI)

This folder contains the machine-learning pipeline skeleton for driving risk and crash-related inference experiments.

## Purpose

- Prepare time-series windows from sensor and speed data.
- Train baseline models for risk/event estimation.
- Export TensorFlow Lite models for Android integration.

## Suggested Workflow

1. Add anonymized datasets under `ml/data/` following its README policy.
2. Implement preprocessing/windowing in `ml/src/`.
3. Prototype experiments in `ml/notebooks/`.
4. Train and evaluate candidate models.
5. Export `.tflite` artifacts to `ml/models/`.

## Folder Layout

- `data/`: local dataset inputs and metadata instructions.
- `notebooks/`: exploratory analysis and experiment notebooks.
- `src/`: reusable preprocessing, training, and export scripts.
- `models/`: placeholder and exported model artifacts.
- `requirements.txt`: Python dependencies.

## Privacy and Compliance

- Do not include personal identifiers.
- Do not commit raw sensitive location traces.
- Keep only the minimum data needed for model development.
