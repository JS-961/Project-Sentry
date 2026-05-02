from __future__ import annotations

import argparse
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import joblib
import numpy as np
import pandas as pd
from sklearn.base import clone
from sklearn.ensemble import ExtraTreesClassifier, GradientBoostingClassifier, RandomForestClassifier
from sklearn.feature_selection import f_classif
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix, f1_score
from sklearn.model_selection import GroupShuffleSplit, StratifiedGroupKFold, train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.tree import _tree
from sklearn.utils.class_weight import compute_sample_weight

from sentry_ml.features import FEATURE_NAMES, WindowConfig, build_dataset, merge_accel_gyro, normalize_axis_csv, normalize_elapsed_ms


ANDROID_MODEL_FILE = "advisory_risk_model.json"
ANDROID_DRIVER_MODEL_FILE = "advisory_driver_model.json"
ANDROID_ROAD_MODEL_FILE = "advisory_road_model.json"
MODEL_FORMAT = "project-sentry-advisory-risk-model-v3"


@dataclass(frozen=True)
class CandidateSpec:
    name: str
    model: Any
    uses_sample_weight: bool = False
    android_exportable: bool = True


def main() -> None:
    args = parse_args()
    data_dir = args.data_dir.resolve()
    output_dir = args.output_dir.resolve()
    output_dir.mkdir(parents=True, exist_ok=True)

    config = WindowConfig(
        window_seconds=args.window_seconds,
        stride_seconds=args.stride_seconds,
        sample_rate_hz=args.sample_rate_hz,
        min_window_samples=args.min_window_samples,
    )
    dataset = build_dataset(data_dir, config)
    labels = sorted(dataset.labels.unique())
    if len(labels) < 2:
        raise ValueError("Need at least two verified labels to train an advisory classifier.")

    save_dataset_audit(output_dir, dataset)
    save_debug_artifacts(output_dir, dataset, config, args.random_state)
    print_dataset_audit(dataset)

    strict_dir = output_dir / "strict_grouped_results"
    controlled_dir = output_dir / "controlled_random_results"
    strict = run_evaluation(
        mode_name="strict_grouped",
        output_dir=strict_dir,
        features=dataset.features,
        labels=dataset.labels,
        groups=dataset.groups,
        class_labels=labels,
        split_indices=strict_grouped_split(dataset.labels, dataset.groups, args.random_state, args.test_size),
        random_state=args.random_state,
    )
    controlled = run_evaluation(
        mode_name="controlled_random",
        output_dir=controlled_dir,
        features=dataset.features,
        labels=dataset.labels,
        groups=dataset.groups,
        class_labels=labels,
        split_indices=controlled_random_split(dataset.labels, args.random_state, args.test_size),
        random_state=args.random_state,
    )
    strict_cv = run_strict_grouped_cross_validation(
        mode_name="strict_grouped_cv",
        output_dir=strict_dir,
        features=dataset.features,
        labels=dataset.labels,
        groups=dataset.groups,
        class_labels=labels,
        random_state=args.random_state,
    )
    controlled_cv = run_controlled_random_repeated_validation(
        mode_name="controlled_random_repeated",
        output_dir=controlled_dir,
        features=dataset.features,
        labels=dataset.labels,
        class_labels=labels,
        random_state=args.random_state,
        test_size=args.test_size,
        repeats=args.random_repeats,
    )

    option_results = compare_task_options(dataset, args.random_state, args.test_size, output_dir, args.random_repeats)

    selected = select_for_android(strict_cv["summary_by_model"], strict["model_comparison"])
    final_spec = build_candidate_specs(args.random_state)[selected["model_name"]]
    final_model = final_spec.model
    fit_model(final_spec, final_model, dataset.features, dataset.labels)

    joblib_path = output_dir / f"{selected['model_name']}_final_all_verified_data.joblib"
    joblib.dump(final_model, joblib_path)

    android_model = export_android_model(
        model_name=selected["model_name"],
        model=final_model,
        labels=labels,
        strict_results=strict,
        controlled_results=controlled,
        strict_cv_results=strict_cv,
        controlled_cv_results=controlled_cv,
        option_results=option_results,
        dataset_summary=dataset_summary(dataset),
        config=config,
    )
    android_model_path = output_dir / ANDROID_MODEL_FILE
    android_model_path.write_text(json.dumps(android_model, indent=2), encoding="utf-8")

    if args.android_assets_dir:
        assets_dir = args.android_assets_dir.resolve()
        assets_dir.mkdir(parents=True, exist_ok=True)
        (assets_dir / ANDROID_MODEL_FILE).write_text(json.dumps(android_model, indent=2), encoding="utf-8")
    else:
        assets_dir = None

    binary_android_exports = export_task_option_android_models(
        output_dir=output_dir,
        assets_dir=assets_dir,
        dataset=dataset,
        option_results=option_results,
        config=config,
        random_state=args.random_state,
    )

    summary = {
        "selected_android_model": selected,
        "selection_basis": "best Android-exportable model by strict grouped cross-validation macro F1 mean, refit on all verified windows for deployment",
        "recommended_android_runtime": binary_android_exports["recommendation"],
        "binary_android_exports": binary_android_exports,
        "strict_grouped_results": strict["summary"],
        "strict_grouped_cv": strict_cv,
        "controlled_random_results": controlled["summary"],
        "controlled_random_repeated": controlled_cv,
        "task_option_results": option_results,
        "android_model_path": str(android_model_path),
        "joblib_path": str(joblib_path),
    }
    (output_dir / "training_summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")

    print("\nStrict grouped comparison:")
    print_comparison(strict["model_comparison"])
    print("\nStrict grouped cross-validation:")
    print_cv_comparison(strict_cv["summary_by_model"])
    print("\nControlled random comparison:")
    print_comparison(controlled["model_comparison"])
    print("\nControlled random repeated validation:")
    print_cv_comparison(controlled_cv["summary_by_model"])
    print(f"\nSelected Android model: {selected['model_name']} from strict grouped cross-validation")
    print(f"Android export: {android_model_path}")


def build_candidate_specs(random_state: int) -> dict[str, CandidateSpec]:
    specs = {
        "logistic_regression": CandidateSpec(
            name="logistic_regression",
            model=Pipeline(
                steps=[
                    ("scaler", StandardScaler()),
                    (
                        "model",
                        LogisticRegression(
                            max_iter=5000,
                            class_weight="balanced",
                            random_state=random_state,
                        ),
                    ),
                ]
            ),
        ),
        "random_forest": CandidateSpec(
            name="random_forest",
            model=RandomForestClassifier(
                n_estimators=220,
                max_depth=12,
                min_samples_leaf=2,
                class_weight="balanced_subsample",
                random_state=random_state,
                n_jobs=-1,
            ),
        ),
        "extra_trees": CandidateSpec(
            name="extra_trees",
            model=ExtraTreesClassifier(
                n_estimators=220,
                max_depth=12,
                min_samples_leaf=2,
                class_weight="balanced",
                random_state=random_state,
                n_jobs=-1,
            ),
        ),
        "gradient_boosting": CandidateSpec(
            name="gradient_boosting",
            model=GradientBoostingClassifier(random_state=random_state),
            uses_sample_weight=True,
            android_exportable=False,
        ),
    }
    return specs


def run_evaluation(
    mode_name: str,
    output_dir: Path,
    features: pd.DataFrame,
    labels: pd.Series,
    groups: pd.Series,
    class_labels: list[str],
    split_indices: tuple[np.ndarray, np.ndarray, dict[str, Any]],
    random_state: int,
) -> dict[str, Any]:
    output_dir.mkdir(parents=True, exist_ok=True)
    train_idx, test_idx, split_info = split_indices
    x_train = features.iloc[train_idx]
    x_test = features.iloc[test_idx]
    y_train = labels.iloc[train_idx]
    y_test = labels.iloc[test_idx]

    comparison: list[dict[str, Any]] = []
    for spec in build_candidate_specs(random_state).values():
        model = spec.model
        fit_model(spec, model, x_train, y_train)
        predictions = model.predict(x_test)
        probabilities = predict_probabilities(model, x_test)
        metrics = evaluate_model(
            mode_name=mode_name,
            spec=spec,
            labels=class_labels,
            y_test=y_test,
            predictions=predictions,
            probabilities=probabilities,
            split_info=split_info,
            output_dir=output_dir,
        )
        comparison.append(metrics)

    comparison = sorted(comparison, key=lambda item: (item["macro_f1"], item["accuracy"]), reverse=True)
    (output_dir / "model_comparison.json").write_text(json.dumps(comparison, indent=2), encoding="utf-8")
    summary = {
        "mode": mode_name,
        "best_model": comparison[0],
        "split": split_info,
    }
    (output_dir / "summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
    return {"summary": summary, "model_comparison": comparison}


def run_strict_grouped_cross_validation(
    mode_name: str,
    output_dir: Path,
    features: pd.DataFrame,
    labels: pd.Series,
    groups: pd.Series,
    class_labels: list[str],
    random_state: int,
) -> dict[str, Any]:
    group_labels = pd.DataFrame({"group": groups, "label": labels}).drop_duplicates()
    min_groups_per_label = int(group_labels.groupby("label")["group"].nunique().min())
    unique_group_count = int(groups.nunique())
    if min_groups_per_label >= 2:
        n_splits = min(5, min_groups_per_label)
        splitter = StratifiedGroupKFold(n_splits=n_splits, shuffle=True, random_state=random_state)
        splits = list(splitter.split(np.zeros(len(labels)), labels, groups))
        method = f"StratifiedGroupKFold(n_splits={n_splits})"
    elif unique_group_count >= 2:
        n_splits = min(5, unique_group_count)
        splitter = GroupShuffleSplit(n_splits=n_splits, test_size=0.2, random_state=random_state)
        splits = list(splitter.split(np.zeros(len(labels)), labels, groups))
        method = f"GroupShuffleSplit(n_splits={n_splits})"
    else:
        raise ValueError("Need at least two groups/sessions for strict grouped cross-validation.")

    return run_cross_validation(
        mode_name=mode_name,
        output_dir=output_dir,
        features=features,
        labels=labels,
        groups=groups,
        class_labels=class_labels,
        splits=splits,
        method=method,
        random_state=random_state,
    )


def run_controlled_random_repeated_validation(
    mode_name: str,
    output_dir: Path,
    features: pd.DataFrame,
    labels: pd.Series,
    class_labels: list[str],
    random_state: int,
    test_size: float,
    repeats: int,
) -> dict[str, Any]:
    indices = np.arange(len(labels))
    splits = [
        train_test_split(
            indices,
            test_size=test_size,
            random_state=random_state + repeat,
            stratify=labels,
        )
        for repeat in range(repeats)
    ]
    fake_groups = pd.Series(["controlled_random"] * len(labels), index=labels.index)
    return run_cross_validation(
        mode_name=mode_name,
        output_dir=output_dir,
        features=features,
        labels=labels,
        groups=fake_groups,
        class_labels=class_labels,
        splits=splits,
        method=f"Repeated stratified random row split(n_repeats={repeats})",
        random_state=random_state,
    )


def run_cross_validation(
    mode_name: str,
    output_dir: Path,
    features: pd.DataFrame,
    labels: pd.Series,
    groups: pd.Series,
    class_labels: list[str],
    splits: list[tuple[np.ndarray, np.ndarray]],
    method: str,
    random_state: int,
) -> dict[str, Any]:
    output_dir.mkdir(parents=True, exist_ok=True)
    rows: list[dict[str, Any]] = []
    for fold_index, (train_idx, test_idx) in enumerate(splits, start=1):
        x_train = features.iloc[train_idx]
        x_test = features.iloc[test_idx]
        y_train = labels.iloc[train_idx]
        y_test = labels.iloc[test_idx]
        for base_spec in build_candidate_specs(random_state + fold_index).values():
            spec = CandidateSpec(
                name=base_spec.name,
                model=clone(base_spec.model),
                uses_sample_weight=base_spec.uses_sample_weight,
                android_exportable=base_spec.android_exportable,
            )
            fit_model(spec, spec.model, x_train, y_train)
            predictions = spec.model.predict(x_test)
            probabilities = predict_probabilities(spec.model, x_test)
            row = {
                "mode": mode_name,
                "method": method,
                "fold": fold_index,
                "model_name": spec.name,
                "android_exportable": spec.android_exportable,
                "accuracy": float(accuracy_score(y_test, predictions)),
                "macro_f1": float(f1_score(y_test, predictions, labels=class_labels, average="macro", zero_division=0)),
                "train_rows": int(len(train_idx)),
                "test_rows": int(len(test_idx)),
                "train_groups": int(groups.iloc[train_idx].nunique()),
                "test_groups": int(groups.iloc[test_idx].nunique()),
                "train_label_counts": json.dumps(labels.iloc[train_idx].value_counts().to_dict()),
                "test_label_counts": json.dumps(labels.iloc[test_idx].value_counts().to_dict()),
            }
            if probabilities is not None and len(probabilities):
                row["mean_test_confidence"] = float(np.max(probabilities, axis=1).mean())
            rows.append(row)

    folds = pd.DataFrame(rows)
    folds.to_csv(output_dir / f"{mode_name}_fold_results.csv", index=False)
    summary_by_model = summarize_cv_results(folds)
    summary = {
        "mode": mode_name,
        "method": method,
        "fold_count": int(len(splits)),
        "labels": class_labels,
        "unique_groups": int(groups.nunique()),
        "summary_by_model": summary_by_model,
        "note": (
            "Strict grouped cross-validation holds out full sessions/groups."
            if "Group" in method or "StratifiedGroup" in method
            else "Controlled random repeated validation is for dataset/demo comparison only and can be optimistic."
        ),
    }
    (output_dir / f"{mode_name}_summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
    return {**summary, "fold_results": rows}


def summarize_cv_results(folds: pd.DataFrame) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for model_name, group in folds.groupby("model_name"):
        rows.append(
            {
                "model_name": model_name,
                "android_exportable": bool(group["android_exportable"].iloc[0]),
                "folds": int(len(group)),
                "accuracy_mean": float(group["accuracy"].mean()),
                "accuracy_std": float(group["accuracy"].std(ddof=0)),
                "macro_f1_mean": float(group["macro_f1"].mean()),
                "macro_f1_std": float(group["macro_f1"].std(ddof=0)),
                "mean_test_confidence": (
                    float(group["mean_test_confidence"].mean())
                    if "mean_test_confidence" in group.columns
                    else None
                ),
            }
        )
    return sorted(rows, key=lambda item: (item["macro_f1_mean"], item["accuracy_mean"]), reverse=True)


def fit_model(spec: CandidateSpec, model: Any, features: pd.DataFrame, labels: pd.Series) -> None:
    if spec.uses_sample_weight:
        weights = compute_sample_weight(class_weight="balanced", y=labels)
        model.fit(features, labels, sample_weight=weights)
    else:
        model.fit(features, labels)


def strict_grouped_split(
    labels: pd.Series,
    groups: pd.Series,
    random_state: int,
    test_size: float,
) -> tuple[np.ndarray, np.ndarray, dict[str, Any]]:
    group_labels = pd.DataFrame({"group": groups, "label": labels}).drop_duplicates()
    min_groups_per_label = int(group_labels.groupby("label")["group"].nunique().min())
    unique_group_count = int(groups.nunique())

    if min_groups_per_label >= 2:
        n_splits = min(5, min_groups_per_label)
        splitter = StratifiedGroupKFold(n_splits=n_splits, shuffle=True, random_state=random_state)
        train_idx, test_idx = next(splitter.split(np.zeros(len(labels)), labels, groups))
        method = f"StratifiedGroupKFold(n_splits={n_splits})"
    elif unique_group_count >= 2:
        splitter = GroupShuffleSplit(n_splits=1, test_size=test_size, random_state=random_state)
        train_idx, test_idx = next(splitter.split(np.zeros(len(labels)), labels, groups))
        method = "GroupShuffleSplit"
    else:
        raise ValueError("Need at least two groups/sessions for a defensible grouped evaluation split.")

    return train_idx, test_idx, split_summary(method, labels, groups, train_idx, test_idx)


def controlled_random_split(
    labels: pd.Series,
    random_state: int,
    test_size: float,
) -> tuple[np.ndarray, np.ndarray, dict[str, Any]]:
    indices = np.arange(len(labels))
    train_idx, test_idx = train_test_split(
        indices,
        test_size=test_size,
        random_state=random_state,
        stratify=labels,
    )
    fake_groups = pd.Series(["controlled_random"] * len(labels), index=labels.index)
    return train_idx, test_idx, split_summary("Stratified random row split", labels, fake_groups, train_idx, test_idx)


def split_summary(
    method: str,
    labels: pd.Series,
    groups: pd.Series,
    train_idx: np.ndarray,
    test_idx: np.ndarray,
) -> dict[str, Any]:
    return {
        "method": method,
        "note": (
            "Strict grouped split estimates generalization to held-out sessions."
            if "Group" in method or "StratifiedGroup" in method
            else "Controlled random split is for dataset/demo comparison only and can be optimistic."
        ),
        "unique_groups": int(groups.nunique()),
        "train_rows": int(len(train_idx)),
        "test_rows": int(len(test_idx)),
        "train_groups": int(groups.iloc[train_idx].nunique()),
        "test_groups": int(groups.iloc[test_idx].nunique()),
        "train_label_counts": labels.iloc[train_idx].value_counts().to_dict(),
        "test_label_counts": labels.iloc[test_idx].value_counts().to_dict(),
    }


def evaluate_model(
    mode_name: str,
    spec: CandidateSpec,
    labels: list[str],
    y_test: pd.Series,
    predictions: np.ndarray,
    probabilities: np.ndarray | None,
    split_info: dict[str, Any],
    output_dir: Path,
) -> dict[str, Any]:
    report_text = classification_report(y_test, predictions, labels=labels, zero_division=0)
    report_dict = classification_report(y_test, predictions, labels=labels, zero_division=0, output_dict=True)
    matrix = confusion_matrix(y_test, predictions, labels=labels)
    pd.DataFrame(matrix, index=labels, columns=labels).to_csv(output_dir / f"confusion_matrix_{spec.name}.csv")
    (output_dir / f"classification_report_{spec.name}.txt").write_text(report_text, encoding="utf-8")

    metrics = {
        "mode": mode_name,
        "model_name": spec.name,
        "android_exportable": spec.android_exportable,
        "accuracy": float(accuracy_score(y_test, predictions)),
        "macro_f1": float(f1_score(y_test, predictions, labels=labels, average="macro", zero_division=0)),
        "labels": labels,
        "confusion_matrix": matrix.tolist(),
        "classification_report": report_dict,
        "split": split_info,
    }
    if probabilities is not None and len(probabilities):
        metrics["mean_test_confidence"] = float(np.max(probabilities, axis=1).mean())
    return metrics


def compare_task_options(
    dataset: Any,
    random_state: int,
    test_size: float,
    output_dir: Path,
    random_repeats: int = 5,
) -> dict[str, Any]:
    option_dir = output_dir / "task_option_results"
    option_dir.mkdir(parents=True, exist_ok=True)
    results: dict[str, Any] = {
        "option_1_multiclass": {
            "labels": sorted(dataset.labels.unique()),
            "description": "single Android model: NORMAL / AGGRESSIVE / ROAD_ANOMALY",
        },
    }
    binary_tasks = {
        "driver_behavior_binary": {
            "features": dataset.features[dataset.labels.isin(["NORMAL", "AGGRESSIVE"])].reset_index(drop=True),
            "labels": dataset.labels[dataset.labels.isin(["NORMAL", "AGGRESSIVE"])].reset_index(drop=True),
            "groups": dataset.groups[dataset.labels.isin(["NORMAL", "AGGRESSIVE"])].reset_index(drop=True),
            "description": "driver behavior only: NORMAL vs AGGRESSIVE; road-anomaly rows excluded",
        },
        "road_condition_binary": {
            "features": dataset.features.reset_index(drop=True),
            "labels": dataset.labels.map(lambda label: "ROAD_ANOMALY" if label == "ROAD_ANOMALY" else "NORMAL_ROAD").reset_index(drop=True),
            "groups": dataset.groups.reset_index(drop=True),
            "description": "road condition only: NORMAL_ROAD means normal/aggressive driving without labeled bump/pothole",
        },
    }
    for task_name, task in binary_tasks.items():
        task_features = task["features"]
        task_labels_series = task["labels"]
        task_groups = task["groups"]
        if len(task_labels_series) < 10 or task_labels_series.nunique() < 2:
            continue
        class_labels = sorted(task_labels_series.unique())
        strict = run_evaluation(
            mode_name=f"{task_name}_strict_grouped",
            output_dir=option_dir / task_name / "strict_grouped_results",
            features=task_features,
            labels=task_labels_series,
            groups=task_groups,
            class_labels=class_labels,
            split_indices=strict_grouped_split(task_labels_series, task_groups, random_state, test_size),
            random_state=random_state,
        )
        controlled = run_evaluation(
            mode_name=f"{task_name}_controlled_random",
            output_dir=option_dir / task_name / "controlled_random_results",
            features=task_features,
            labels=task_labels_series,
            groups=task_groups,
            class_labels=class_labels,
            split_indices=controlled_random_split(task_labels_series, random_state, test_size),
            random_state=random_state,
        )
        strict_cv = run_strict_grouped_cross_validation(
            mode_name=f"{task_name}_strict_grouped_cv",
            output_dir=option_dir / task_name / "strict_grouped_results",
            features=task_features,
            labels=task_labels_series,
            groups=task_groups,
            class_labels=class_labels,
            random_state=random_state,
        )
        controlled_cv = run_controlled_random_repeated_validation(
            mode_name=f"{task_name}_controlled_random_repeated",
            output_dir=option_dir / task_name / "controlled_random_results",
            features=task_features,
            labels=task_labels_series,
            class_labels=class_labels,
            random_state=random_state,
            test_size=test_size,
            repeats=random_repeats,
        )
        results[task_name] = {
            "labels": class_labels,
            "description": task["description"],
            "strict_grouped_best": strict["summary"]["best_model"],
            "strict_grouped_cv": {key: value for key, value in strict_cv.items() if key != "fold_results"},
            "controlled_random_best": controlled["summary"]["best_model"],
            "controlled_random_repeated": {key: value for key, value in controlled_cv.items() if key != "fold_results"},
        }

    results["recommendation"] = (
        "Use the two-task advisory setup when possible: driver behavior as NORMAL vs AGGRESSIVE, and road condition "
        "as NORMAL_ROAD vs ROAD_ANOMALY. It is more defensible under strict grouped validation. Keep the single "
        "multiclass model as the simplest fallback. ROAD_ANOMALY remains a road-condition advisory, not driver behavior."
    )
    (option_dir / "summary.json").write_text(json.dumps(results, indent=2), encoding="utf-8")
    return results


def export_task_option_android_models(
    output_dir: Path,
    assets_dir: Path | None,
    dataset: Any,
    option_results: dict[str, Any],
    config: WindowConfig,
    random_state: int,
) -> dict[str, Any]:
    exports: dict[str, Any] = {
        "recommendation": (
            "Prefer the two-task advisory runtime for demos and reporting: "
            "driver behavior is NORMAL vs AGGRESSIVE, and road condition is NORMAL_ROAD vs ROAD_ANOMALY. "
            "The 3-class model remains exported as a fallback."
        )
    }
    tasks = {
        "driver_behavior_binary": {
            "file_name": ANDROID_DRIVER_MODEL_FILE,
            "features": dataset.features[dataset.labels.isin(["NORMAL", "AGGRESSIVE"])].reset_index(drop=True),
            "labels": dataset.labels[dataset.labels.isin(["NORMAL", "AGGRESSIVE"])].reset_index(drop=True),
        },
        "road_condition_binary": {
            "file_name": ANDROID_ROAD_MODEL_FILE,
            "features": dataset.features.reset_index(drop=True),
            "labels": dataset.labels.map(lambda label: "ROAD_ANOMALY" if label == "ROAD_ANOMALY" else "NORMAL_ROAD").reset_index(drop=True),
        },
    }
    for task_name, task in tasks.items():
        if task_name not in option_results:
            continue
        selected = select_for_android(
            option_results[task_name]["strict_grouped_cv"]["summary_by_model"],
            [],
        )
        spec = build_candidate_specs(random_state)[selected["model_name"]]
        model = spec.model
        labels = sorted(task["labels"].unique())
        fit_model(spec, model, task["features"], task["labels"])
        payload = export_android_payload(
            model_name=selected["model_name"],
            model=model,
            labels=labels,
            training={
                "selection_basis": "best Android-exportable strict grouped cross-validation macro F1 mean for this binary task",
                "task_name": task_name,
                "task_results": option_results[task_name],
                "window_seconds": config.window_seconds,
                "stride_seconds": config.stride_seconds,
                "sample_rate_hz": config.sample_rate_hz,
            },
        )
        path = output_dir / task["file_name"]
        path.write_text(json.dumps(payload, indent=2), encoding="utf-8")
        if assets_dir is not None:
            (assets_dir / task["file_name"]).write_text(json.dumps(payload, indent=2), encoding="utf-8")
        exports[task_name] = {
            "file_name": task["file_name"],
            "model_name": selected["model_name"],
            "labels": labels,
            "selection": selected,
            "path": str(path),
        }
    return exports


def export_android_payload(
    model_name: str,
    model: Any,
    labels: list[str],
    training: dict[str, Any],
) -> dict[str, Any]:
    base = {
        "format": MODEL_FORMAT,
        "version": 3,
        "model_name": model_name,
        "feature_names": FEATURE_NAMES,
        "classes": labels,
        "score_ranges": android_score_ranges(labels),
        "training": training,
    }
    if model_name == "logistic_regression":
        return {**base, "model_type": "logistic_regression", **export_logistic(model)}
    if model_name in {"random_forest", "extra_trees"}:
        return {**base, "model_type": model_name, **export_tree_ensemble(model)}
    raise ValueError(f"Model {model_name} is not exportable to Android JSON runtime.")


def android_score_ranges(labels: list[str]) -> dict[str, list[int]]:
    ranges = {
        "NORMAL": [0, 30],
        "NORMAL_ROAD": [0, 30],
        "AGGRESSIVE": [40, 70],
        "RISKY": [70, 100],
        "ROAD_ANOMALY": [60, 85],
    }
    return {label: ranges[label] for label in labels if label in ranges}


def predict_probabilities(model: Any, features: pd.DataFrame) -> np.ndarray | None:
    if hasattr(model, "predict_proba"):
        return model.predict_proba(features)
    return None


def select_for_android(cv_summary: list[dict[str, Any]], fallback_comparison: list[dict[str, Any]]) -> dict[str, Any]:
    exportable = [item for item in cv_summary if item["android_exportable"]]
    if exportable:
        selected = sorted(exportable, key=lambda item: (item["macro_f1_mean"], item["accuracy_mean"]), reverse=True)[0]
        return {
            "model_name": selected["model_name"],
            "android_exportable": selected["android_exportable"],
            "selection_mode": "strict_grouped_cross_validation",
            "accuracy_mean": selected["accuracy_mean"],
            "accuracy_std": selected["accuracy_std"],
            "macro_f1_mean": selected["macro_f1_mean"],
            "macro_f1_std": selected["macro_f1_std"],
        }

    exportable = [item for item in fallback_comparison if item["android_exportable"]]
    if not exportable:
        raise ValueError("No Android-exportable model was trained.")
    return sorted(exportable, key=lambda item: (item["macro_f1"], item["accuracy"]), reverse=True)[0]


def export_android_model(
    model_name: str,
    model: Any,
    labels: list[str],
    strict_results: dict[str, Any],
    controlled_results: dict[str, Any],
    strict_cv_results: dict[str, Any],
    controlled_cv_results: dict[str, Any],
    option_results: dict[str, Any],
    dataset_summary: dict[str, Any],
    config: WindowConfig,
) -> dict[str, Any]:
    base = {
        "format": MODEL_FORMAT,
        "version": 3,
        "model_name": model_name,
        "feature_names": FEATURE_NAMES,
        "classes": labels,
        "score_ranges": {
            "NORMAL": [0, 30],
            "AGGRESSIVE": [40, 70],
            "RISKY": [70, 100],
            "ROAD_ANOMALY": [60, 85],
        },
        "training": {
            "selection_basis": "best Android-exportable strict grouped cross-validation macro F1 mean; final exported model refit on all verified windows",
            "strict_grouped_results": strict_results["summary"],
            "strict_grouped_model_comparison": strict_results["model_comparison"],
            "strict_grouped_cross_validation": {
                key: value for key, value in strict_cv_results.items() if key != "fold_results"
            },
            "controlled_random_results": controlled_results["summary"],
            "controlled_random_model_comparison": controlled_results["model_comparison"],
            "controlled_random_repeated_validation": {
                key: value for key, value in controlled_cv_results.items() if key != "fold_results"
            },
            "task_option_results": option_results,
            "dataset_summary": dataset_summary,
            "window_seconds": config.window_seconds,
            "stride_seconds": config.stride_seconds,
            "sample_rate_hz": config.sample_rate_hz,
        },
    }
    if model_name == "logistic_regression":
        return {**base, "model_type": "logistic_regression", **export_logistic(model)}
    if model_name in {"random_forest", "extra_trees"}:
        return {**base, "model_type": model_name, **export_tree_ensemble(model)}
    raise ValueError(f"Model {model_name} is not exportable to Android JSON runtime.")


def export_logistic(pipeline: Pipeline) -> dict[str, Any]:
    scaler: StandardScaler = pipeline.named_steps["scaler"]
    model: LogisticRegression = pipeline.named_steps["model"]
    return {
        "scaler": {
            "mean": scaler.mean_.tolist(),
            "scale": scaler.scale_.tolist(),
        },
        "coefficients": model.coef_.tolist(),
        "intercepts": model.intercept_.tolist(),
    }


def export_tree_ensemble(model: RandomForestClassifier | ExtraTreesClassifier) -> dict[str, Any]:
    return {
        "trees": [export_tree(estimator.tree_) for estimator in model.estimators_],
    }


def export_tree(tree: _tree.Tree) -> dict[str, Any]:
    values = tree.value[:, 0, :].astype(float)
    row_sums = values.sum(axis=1)
    probabilities = np.divide(values, row_sums[:, None], out=np.zeros_like(values), where=row_sums[:, None] != 0)
    return {
        "children_left": tree.children_left.tolist(),
        "children_right": tree.children_right.tolist(),
        "feature": tree.feature.tolist(),
        "threshold": tree.threshold.tolist(),
        "probabilities": probabilities.tolist(),
    }


def save_debug_artifacts(output_dir: Path, dataset: Any, config: WindowConfig, random_state: int) -> None:
    diagnostics_dir = output_dir / "diagnostics"
    diagnostics_dir.mkdir(parents=True, exist_ok=True)
    (diagnostics_dir / "dataset_diagnostics.json").write_text(
        json.dumps(dataset_diagnostics(dataset), indent=2),
        encoding="utf-8",
    )
    label_mapping_check(dataset).to_csv(diagnostics_dir / "label_mapping_check.csv", index=False)
    preprocessing_diagnostics(dataset, config).to_csv(diagnostics_dir / "preprocessing_diagnostics.csv", index=False)
    save_feature_quality(diagnostics_dir, dataset)
    save_feature_importances(diagnostics_dir, dataset, random_state)


def dataset_diagnostics(dataset: Any) -> dict[str, Any]:
    windows_per_recording = dataset.windows.groupby(["label", "recording"]).size().reset_index(name="windows")
    return {
        "source_recordings_per_class": dataset.manifest.groupby("label")["path"].nunique().to_dict(),
        "sessions_or_groups_per_class": dataset.windows.groupby("label")["group"].nunique().to_dict(),
        "windows_per_class": dataset.labels.value_counts().to_dict(),
        "average_window_count_per_recording": {
            label: float(group["windows"].mean())
            for label, group in windows_per_recording.groupby("label")
        },
        "min_window_count_per_recording": {
            label: int(group["windows"].min())
            for label, group in windows_per_recording.groupby("label")
        },
        "max_window_count_per_recording": {
            label: int(group["windows"].max())
            for label, group in windows_per_recording.groupby("label")
        },
        "recording_count": int(dataset.manifest["path"].nunique()),
        "group_count": int(dataset.windows["group"].nunique()),
        "window_rows": int(len(dataset.windows)),
        "feature_count": len(FEATURE_NAMES),
        "skipped_recordings": dataset.skipped.to_dict("records"),
        "skipped_by_source_reason": (
            dataset.skipped.groupby(["source", "reason"]).size().reset_index(name="count").to_dict("records")
            if not dataset.skipped.empty
            else []
        ),
    }


def label_mapping_check(dataset: Any) -> pd.DataFrame:
    rows: list[dict[str, Any]] = []
    for row in dataset.manifest.to_dict("records"):
        expected = expected_label_from_path(Path(row["path"]))
        rows.append(
            {
                **row,
                "expected_from_folder": expected or "",
                "mapping_status": "OK" if expected == row["label"] else "CHECK",
            }
        )
    return pd.DataFrame(rows)


def expected_label_from_path(path: Path) -> str | None:
    parts = [part.lower() for part in path.parts]
    name = path.name.lower()
    if "road data" in parts and "driving behaviour" in parts:
        if "aggressive" in name:
            return "AGGRESSIVE"
        if "standard" in name or "slow" in name:
            return "NORMAL"
    if "road data" in parts and "road anomalies" in parts:
        if "bump" in name or "pothole" in name:
            return "ROAD_ANOMALY"
    return None


def preprocessing_diagnostics(dataset: Any, config: WindowConfig) -> pd.DataFrame:
    rows: list[dict[str, Any]] = []
    window_counts = dataset.windows.groupby("recording").size().to_dict()
    for row in dataset.manifest.to_dict("records"):
        accel_file = Path(row["accel_file"])
        gyro_file = Path(row["gyro_file"])
        accel = normalize_axis_csv(accel_file, "acc")
        gyro = normalize_axis_csv(gyro_file, "gyro")
        accel_timed = normalize_elapsed_ms(accel, config.sample_rate_hz) if not accel.empty else accel
        gyro_timed = normalize_elapsed_ms(gyro, config.sample_rate_hz) if not gyro.empty else gyro
        merged = merge_accel_gyro(accel_file, gyro_file)
        rows.append(
            {
                "recording": row["path"],
                "label": row["label"],
                "group": row["group"],
                "accel_file": str(accel_file),
                "gyro_file": str(gyro_file),
                "accel_columns": "|".join(read_columns(accel_file)),
                "gyro_columns": "|".join(read_columns(gyro_file)),
                "accel_timestamp_source": timestamp_source(read_columns(accel_file)),
                "gyro_timestamp_source": timestamp_source(read_columns(gyro_file)),
                "accel_rows": int(len(accel)),
                "gyro_rows": int(len(gyro)),
                "merged_rows": int(len(merged)),
                "feature_windows": int(window_counts.get(row["path"], 0)),
                "accel_estimated_hz": estimated_hz(accel_timed),
                "gyro_estimated_hz": estimated_hz(gyro_timed),
                "merged_estimated_hz": estimated_hz(merged),
                "duration_seconds": duration_seconds(merged),
                "window_seconds": config.window_seconds,
                "stride_seconds": config.stride_seconds,
                "min_window_samples": config.min_window_samples,
                "accel_duplicate_timestamps": duplicate_timestamps(accel_timed),
                "gyro_duplicate_timestamps": duplicate_timestamps(gyro_timed),
                "merged_duplicate_timestamps": duplicate_timestamps(merged),
                "required_columns_present": required_columns_present(merged),
            }
        )
    return pd.DataFrame(rows)


def read_columns(csv_file: Path) -> list[str]:
    try:
        return list(pd.read_csv(csv_file, nrows=0).columns)
    except Exception:
        return []


def timestamp_source(columns: list[str]) -> str:
    compacted = {"".join(ch for ch in column.lower() if ch.isalnum()) for column in columns}
    if "secondselapsed" in compacted or "elapsedseconds" in compacted:
        return "elapsed_seconds"
    if any("millisecond" in column or column == "ms" for column in compacted):
        return "elapsed_ms"
    if {"timestamp", "time", "unixtimestamp"} & compacted:
        return "timestamp"
    return "synthetic_sample_rate_fallback"


def estimated_hz(data: pd.DataFrame) -> float | None:
    if data.empty or "elapsed_ms" not in data.columns:
        return None
    elapsed = pd.to_numeric(data["elapsed_ms"], errors="coerce").dropna().sort_values()
    diffs = elapsed.diff().dropna()
    diffs = diffs[diffs > 0]
    if diffs.empty:
        return None
    median_dt_ms = float(diffs.median())
    if median_dt_ms <= 0:
        return None
    return 1000.0 / median_dt_ms


def duration_seconds(data: pd.DataFrame) -> float | None:
    if data.empty or "elapsed_ms" not in data.columns:
        return None
    elapsed = pd.to_numeric(data["elapsed_ms"], errors="coerce").dropna()
    if elapsed.empty:
        return None
    return float((elapsed.max() - elapsed.min()) / 1000.0)


def duplicate_timestamps(data: pd.DataFrame) -> int:
    if data.empty or "elapsed_ms" not in data.columns:
        return 0
    return int(pd.to_numeric(data["elapsed_ms"], errors="coerce").duplicated().sum())


def required_columns_present(data: pd.DataFrame) -> bool:
    return {"acc_x", "acc_y", "acc_z", "gyro_x", "gyro_y", "gyro_z", "elapsed_ms"}.issubset(data.columns)


def save_feature_quality(output_dir: Path, dataset: Any) -> None:
    features = dataset.features.astype(float)
    quality_rows: list[dict[str, Any]] = []
    for feature in FEATURE_NAMES:
        values = features[feature]
        finite = np.isfinite(values.to_numpy(dtype=float))
        quality_rows.append(
            {
                "feature": feature,
                "nan_count": int(values.isna().sum()),
                "non_finite_count": int((~finite).sum()),
                "unique_count": int(values.nunique(dropna=False)),
                "mean": float(values.mean()),
                "std": float(values.std(ddof=0)),
                "min": float(values.min()),
                "max": float(values.max()),
                "zero_fraction": float((values.abs() <= 1e-12).mean()),
                "is_constant": bool(values.nunique(dropna=False) <= 1 or values.std(ddof=0) <= 1e-12),
            }
        )
    quality = pd.DataFrame(quality_rows)
    quality.to_csv(output_dir / "feature_quality_summary.csv", index=False)

    by_label_rows: list[dict[str, Any]] = []
    combined = dataset.features.copy()
    combined["label"] = dataset.labels.values
    for label, group in combined.groupby("label"):
        for feature in FEATURE_NAMES:
            values = group[feature].astype(float)
            by_label_rows.append(
                {
                    "label": label,
                    "feature": feature,
                    "mean": float(values.mean()),
                    "std": float(values.std(ddof=0)),
                    "median": float(values.median()),
                    "p10": float(values.quantile(0.10)),
                    "p90": float(values.quantile(0.90)),
                }
            )
    pd.DataFrame(by_label_rows).to_csv(output_dir / "feature_summary_by_label.csv", index=False)

    f_values, p_values = f_classif(features, dataset.labels)
    separability = pd.DataFrame(
        {
            "feature": FEATURE_NAMES,
            "f_score": np.nan_to_num(f_values, nan=0.0, posinf=0.0, neginf=0.0),
            "p_value": np.nan_to_num(p_values, nan=1.0, posinf=1.0, neginf=1.0),
        }
    ).sort_values("f_score", ascending=False)
    separability.to_csv(output_dir / "feature_separability.csv", index=False)

    summary = {
        "duplicate_feature_rows": int(features.duplicated().sum()),
        "constant_features": quality.loc[quality["is_constant"], "feature"].tolist(),
        "nan_features": quality.loc[quality["nan_count"] > 0, "feature"].tolist(),
        "top_univariate_features": separability.head(12).to_dict("records"),
    }
    (output_dir / "feature_quality_summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")


def save_feature_importances(output_dir: Path, dataset: Any, random_state: int) -> None:
    for base_spec in build_candidate_specs(random_state).values():
        spec = CandidateSpec(
            name=base_spec.name,
            model=clone(base_spec.model),
            uses_sample_weight=base_spec.uses_sample_weight,
            android_exportable=base_spec.android_exportable,
        )
        fit_model(spec, spec.model, dataset.features, dataset.labels)
        importances = model_feature_importances(spec.model)
        if importances is None:
            continue
        pd.DataFrame({"feature": FEATURE_NAMES, "importance": importances}).sort_values(
            "importance",
            ascending=False,
        ).to_csv(output_dir / f"feature_importance_{spec.name}.csv", index=False)


def model_feature_importances(model: Any) -> list[float] | None:
    if isinstance(model, Pipeline):
        final_model = model.named_steps["model"]
        if hasattr(final_model, "coef_"):
            return np.mean(np.abs(final_model.coef_), axis=0).astype(float).tolist()
    if hasattr(model, "feature_importances_"):
        return np.asarray(model.feature_importances_, dtype=float).tolist()
    return None


def save_dataset_audit(output_dir: Path, dataset: Any) -> None:
    dataset.manifest.to_csv(output_dir / "label_manifest.csv", index=False)
    dataset.skipped.to_csv(output_dir / "skipped_recordings.csv", index=False)
    dataset.windows[["source", "recording", "group", "label", "label_source", *FEATURE_NAMES]].to_csv(
        output_dir / "feature_windows.csv",
        index=False,
    )
    (output_dir / "dataset_summary.json").write_text(
        json.dumps(dataset_summary(dataset), indent=2),
        encoding="utf-8",
    )


def print_dataset_audit(dataset: Any) -> None:
    print("\nVerified label mapping used for training:")
    if dataset.manifest.empty:
        print("(none)")
    else:
        display = dataset.manifest[["source", "label", "group", "label_source"]].sort_values(["source", "label", "group"])
        print(display.to_string(index=False))

    print("\nSource recordings per label:")
    print(dataset.manifest.groupby("label")["path"].nunique().to_string())

    print("\nGroups/sessions per label:")
    print(dataset.windows.groupby("label")["group"].nunique().to_string())

    print("\nWindow counts by label:")
    print(dataset.labels.value_counts().to_string())

    print("\nAverage window count per recording:")
    print(dataset.windows.groupby(["label", "recording"]).size().groupby("label").mean().round(1).to_string())

    print("\nSkipped recordings by reason:")
    if dataset.skipped.empty:
        print("(none)")
    else:
        print(dataset.skipped.groupby(["source", "reason"]).size().reset_index(name="count").to_string(index=False))


def dataset_summary(dataset: Any) -> dict[str, Any]:
    return {
        "window_rows": int(len(dataset.windows)),
        "feature_count": len(FEATURE_NAMES),
        "recording_count": int(dataset.manifest["group"].nunique()) if not dataset.manifest.empty else 0,
        "label_counts": dataset.labels.value_counts().to_dict(),
        "group_counts_by_label": dataset.windows.groupby("label")["group"].nunique().to_dict(),
        "source_counts": dataset.windows["source"].value_counts().to_dict(),
        "skipped_recordings": int(len(dataset.skipped)),
        "skipped_by_source_reason": (
            dataset.skipped.groupby(["source", "reason"]).size().reset_index(name="count").to_dict("records")
            if not dataset.skipped.empty
            else []
        ),
    }


def print_comparison(comparison: list[dict[str, Any]]) -> None:
    for item in comparison:
        exportable = "exportable" if item["android_exportable"] else "not Android-exported"
        print(f"- {item['model_name']} ({exportable}): accuracy={item['accuracy']:.3f}, macro_f1={item['macro_f1']:.3f}")


def print_cv_comparison(summary_by_model: list[dict[str, Any]]) -> None:
    for item in summary_by_model:
        exportable = "exportable" if item["android_exportable"] else "not Android-exported"
        print(
            f"- {item['model_name']} ({exportable}): "
            f"accuracy={item['accuracy_mean']:.3f}+/-{item['accuracy_std']:.3f}, "
            f"macro_f1={item['macro_f1_mean']:.3f}+/-{item['macro_f1_std']:.3f}"
        )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Train Project Sentry advisory driving-risk classifier.")
    parser.add_argument("--data-dir", type=Path, default=Path("ml/data/raw"))
    parser.add_argument("--output-dir", type=Path, default=Path("ml/models/advisory-risk"))
    parser.add_argument("--android-assets-dir", type=Path, default=None)
    parser.add_argument("--window-seconds", type=float, default=3.0)
    parser.add_argument("--stride-seconds", type=float, default=1.0)
    parser.add_argument("--sample-rate-hz", type=float, default=50.0)
    parser.add_argument("--min-window-samples", type=int, default=40)
    parser.add_argument("--test-size", type=float, default=0.2)
    parser.add_argument("--random-repeats", type=int, default=5)
    parser.add_argument("--random-state", type=int, default=42)
    return parser.parse_args()


if __name__ == "__main__":
    main()
