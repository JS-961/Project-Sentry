# ML Workspace

This folder is the shared machine-learning scaffold for Project Sentry.

Current role:

- collect anonymized training and evaluation data
- prototype preprocessing and model experiments
- export lightweight Android-readable model artifacts for advisory integration

Current status:

- advisory driving-risk pipeline scaffolded for Android-native
- trains/evaluates tabular classifiers from expanded sensor-window features
- exports a lightweight JSON model that Android can run without TensorFlow Lite
- keeps crash detection rule-based; ML is advisory only

Suggested workflow:

1. Place anonymized datasets under `ml/data/`.
2. Use `ml/src/sentry_ml/` for preprocessing, feature extraction, training, and export.
3. Use `ml/notebooks/` for exploratory work only.
4. Save trained artifacts under `ml/models/`.
5. Copy `advisory_risk_model.json` into `android-native/app/src/main/assets/` before claiming trained ML in demos.

## Advisory Risk Model

Datasets inspected:

- Mendeley: Driver Behavior Detection Using Smartphone, DOI `10.17632/9vr83n7z5j.2`.
  Its paper describes the overall task as normal/aggressive/risky driver behavior
  classification, but the downloaded files do not provide a verified
  folder-to-label mapping. The `Day-*S/*R` folders are described as start/return
  trip timing folders, so they are skipped instead of guessed.
- STRIDE: Sensor Technologies for Road Insights and Driving Evaluation, DOI
  `10.6084/m9.figshare.25460755.v4`. Its downloaded folder names provide clear
  labels for driving behavior and road anomalies.

Verified training mapping:

- `STRIDE/Road Data/Driving Behaviour/*Aggressive` -> `AGGRESSIVE`
- `STRIDE/Road Data/Driving Behaviour/*Standard` -> `NORMAL`
- `STRIDE/Road Data/Driving Behaviour/*Slow` -> `NORMAL`
- `STRIDE/Road Data/Road Anomalies/*Bump` -> `ROAD_ANOMALY`
- `STRIDE/Road Data/Road Anomalies/*Pothole` -> `ROAD_ANOMALY`

`ROAD_ANOMALY` means road condition event, not bad driving. It is useful for
explaining bump/pothole acceleration spikes that can look crash-like.

Train from the repository root after downloading/extracting data into
`ml/data/raw/`:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r ml\requirements.txt
$env:PYTHONPATH = "ml\src"
python -m sentry_ml.train_advisory_model `
  --data-dir ml\data\raw `
  --output-dir ml\models\advisory-risk `
  --android-assets-dir android-native\app\src\main\assets
```

The trainer prints the label manifest before fitting, compares logistic
regression, random forest, extra trees, and gradient boosting, evaluates with
strict grouped cross-validation plus controlled random repeated validation, and
saves:

- `ml/models/advisory-risk/label_manifest.csv`
- `ml/models/advisory-risk/skipped_recordings.csv`
- `ml/models/advisory-risk/diagnostics/dataset_diagnostics.json`
- `ml/models/advisory-risk/diagnostics/preprocessing_diagnostics.csv`
- `ml/models/advisory-risk/diagnostics/label_mapping_check.csv`
- `ml/models/advisory-risk/diagnostics/feature_quality_summary.json`
- `ml/models/advisory-risk/diagnostics/feature_importance_*.csv`
- `ml/models/advisory-risk/strict_grouped_results/model_comparison.json`
- `ml/models/advisory-risk/strict_grouped_results/strict_grouped_cv_summary.json`
- `ml/models/advisory-risk/controlled_random_results/model_comparison.json`
- `ml/models/advisory-risk/controlled_random_results/controlled_random_repeated_summary.json`
- `ml/models/advisory-risk/**/classification_report_*.txt`
- `ml/models/advisory-risk/**/confusion_matrix_*.csv`
- `ml/models/advisory-risk/task_option_results/summary.json`
- `ml/models/advisory-risk/advisory_risk_model.json`
- `ml/models/advisory-risk/advisory_driver_model.json`
- `ml/models/advisory-risk/advisory_road_model.json`

Current exported Android models:

- recommended runtime: two-task advisory model
- driver behavior model: logistic regression, labels `AGGRESSIVE`, `NORMAL`
- road condition model: random forest, labels `NORMAL_ROAD`, `ROAD_ANOMALY`
- fallback model: 3-class logistic regression, labels `AGGRESSIVE`, `NORMAL`, `ROAD_ANOMALY`
- feature count: `36`
- strict grouped CV for 3-class fallback: accuracy `0.326 +/- 0.093`,
  macro F1-score `0.275 +/- 0.063`
- strict grouped CV for driver behavior: accuracy `0.698 +/- 0.036`,
  macro F1-score `0.604 +/- 0.127`
- strict grouped CV for road condition: accuracy `0.557 +/- 0.142`,
  macro F1-score `0.500 +/- 0.113`
- controlled random repeated 3-class result: accuracy `0.840 +/- 0.016`,
  macro F1-score `0.836 +/- 0.016` with random forest
- controlled random repeated driver behavior result: accuracy
  `0.915 +/- 0.016`, macro F1-score `0.909 +/- 0.017` with random forest
- controlled random repeated road condition result: accuracy
  `0.893 +/- 0.004`, macro F1-score `0.872 +/- 0.005` with random forest

Strict grouped metrics are the real-world estimate because they hold out full
sessions/groups instead of randomly mixing windows from the same recording. The
3-class strict score is low mainly because the dataset has many windows but only
two aggressive source recordings/groups, so held-session generalization is much
harder than random row splitting makes it look. Controlled random metrics are
for dataset/demo comparison only and can be optimistic. Do not present
controlled random accuracy as field performance.

Feature families:

- acceleration magnitude: mean/std/min/max/RMS/percentiles/energy/peak count/SMA
- jerk magnitude: mean/std/min/max/RMS/percentiles/energy/peak count
- gyroscope magnitude: mean/std/min/max/RMS/percentiles/energy/peak count/SMA
- combined motion: mean/std/max/RMS/energy

Model option check:

- One multiclass model keeps Android integration simplest, but its strict grouped
  CV is weak.
- Two binary classifiers are more defensible under strict grouped CV:
  driver behavior (`NORMAL` vs `AGGRESSIVE`) and road condition
  (`NORMAL_ROAD` vs `ROAD_ANOMALY`).
- Android now prefers the two-task advisory runtime when
  `advisory_driver_model.json` and `advisory_road_model.json` are present, and
  falls back to the single 3-class model if either binary model is missing.
- `ROAD_ANOMALY` remains a road-condition advisory class, not driver behavior.
- Feature diagnostics found no NaN features, constant features, or duplicate
  feature rows after jerk extraction was fixed to avoid inserting an artificial
  zero into every window.

Android smoothing:

- majority vote over the last few predictions
- moving average for `mlRiskScore`
- confidence threshold before changing labels
- confidence displayed in the UI
- demo assist hook is present but disabled by default and is not counted as
  trained accuracy

The exported Android model is advisory only. Rule-based crash detection in
`DrivingModeService` remains the source of truth for crash countdowns.

Privacy rules:

- never commit personal identifiers
- never commit raw sensitive location traces
- keep only the minimum data needed for validation and model development
