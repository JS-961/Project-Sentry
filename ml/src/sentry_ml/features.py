from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

import numpy as np
import pandas as pd


GRAVITY_MPS2 = 9.80665
FEATURE_NAMES = [
    "accel_mean",
    "accel_std",
    "accel_min",
    "accel_max",
    "accel_rms",
    "accel_p50",
    "accel_p75",
    "accel_p90",
    "accel_p95",
    "accel_energy",
    "accel_peak_count",
    "accel_sma",
    "jerk_mean",
    "jerk_std",
    "jerk_min",
    "jerk_max",
    "jerk_rms",
    "jerk_p90",
    "jerk_p95",
    "jerk_energy",
    "jerk_peak_count",
    "gyro_mean",
    "gyro_std",
    "gyro_min",
    "gyro_max",
    "gyro_rms",
    "gyro_p90",
    "gyro_p95",
    "gyro_energy",
    "gyro_peak_count",
    "gyro_sma",
    "motion_mean",
    "motion_std",
    "motion_max",
    "motion_rms",
    "motion_energy",
]
DEPLOYABLE_LABELS = ["NORMAL", "AGGRESSIVE", "ROAD_ANOMALY"]
OPTIONAL_LABELS = ["RISKY"]
ALL_LABELS = [*DEPLOYABLE_LABELS, *OPTIONAL_LABELS]


@dataclass(frozen=True)
class WindowConfig:
    window_seconds: float = 3.0
    stride_seconds: float = 1.0
    sample_rate_hz: float = 50.0
    min_window_samples: int = 40


@dataclass(frozen=True)
class Recording:
    path: Path
    label: str
    source: str
    label_source: str
    group: str
    accel_file: Path
    gyro_file: Path


@dataclass(frozen=True)
class SkippedRecording:
    path: Path
    source: str
    reason: str


@dataclass(frozen=True)
class DatasetBuildResult:
    features: pd.DataFrame
    labels: pd.Series
    groups: pd.Series
    windows: pd.DataFrame
    manifest: pd.DataFrame
    skipped: pd.DataFrame


def build_dataset(data_dir: Path, config: WindowConfig) -> DatasetBuildResult:
    """Read verified labeled recordings and return model-ready feature windows."""
    frames: list[pd.DataFrame] = []
    recordings, skipped = discover_recordings(data_dir)

    for recording in recordings:
        merged = merge_accel_gyro(recording.accel_file, recording.gyro_file)
        if merged.empty:
            skipped.append(
                SkippedRecording(
                    path=recording.path,
                    source=recording.source,
                    reason="accelerometer/gyroscope files could not be merged",
                )
            )
            continue
        merged["label"] = recording.label
        windows = extract_windows(merged, config)
        if windows.empty:
            skipped.append(
                SkippedRecording(
                    path=recording.path,
                    source=recording.source,
                    reason="no usable feature windows",
                )
            )
            continue
        windows["group"] = recording.group
        windows["source"] = recording.source
        windows["recording"] = str(recording.path)
        windows["label_source"] = recording.label_source
        frames.append(windows)

    if not frames:
        raise ValueError(
            "No defensible labeled sensor windows found. Check ml/data/raw/ and "
            "avoid guessing labels for undocumented folders."
        )

    windows = pd.concat(frames, ignore_index=True)
    return DatasetBuildResult(
        features=windows[FEATURE_NAMES],
        labels=windows["label"],
        groups=windows["group"],
        windows=windows,
        manifest=manifest_frame(recordings),
        skipped=skipped_frame(skipped),
    )


def discover_recordings(data_dir: Path) -> tuple[list[Recording], list[SkippedRecording]]:
    recordings: list[Recording] = []
    skipped: list[SkippedRecording] = []
    for folder in sorted({path.parent for path in data_dir.rglob("*.csv")}):
        accel = first_named(folder, "accelerometer")
        gyro = first_named(folder, "gyroscope")
        if accel is None or gyro is None:
            continue

        label, source, label_source, reason = infer_verified_label(folder)
        if label is None:
            skipped.append(
                SkippedRecording(
                    path=folder,
                    source=source,
                    reason=reason or "no verified label mapping",
                )
            )
            continue

        recordings.append(
            Recording(
                path=folder,
                label=label,
                source=source,
                label_source=label_source,
                group=recording_group(folder),
                accel_file=accel,
                gyro_file=gyro,
            )
        )
    return recordings, skipped


def infer_verified_label(folder: Path) -> tuple[str | None, str, str, str | None]:
    parts = [part.lower() for part in folder.parts]
    name = folder.name.lower()

    if "road data" in parts and "driving behaviour" in parts:
        if "aggressive" in name:
            return "AGGRESSIVE", "STRIDE", "folder name: Driving Behaviour/*Aggressive", None
        if "standard" in name or "slow" in name:
            return "NORMAL", "STRIDE", "folder name: Driving Behaviour/*Standard or *Slow", None
        return None, "STRIDE", "", "unrecognized STRIDE driving-behaviour folder"

    if "road data" in parts and "road anomalies" in parts:
        if "bump" in name or "pothole" in name:
            return "ROAD_ANOMALY", "STRIDE", "folder name: Road Anomalies/*Bump or *Pothole", None
        return None, "STRIDE", "", "unrecognized STRIDE road-anomaly folder"

    if "mendeley" in parts:
        return (
            None,
            "MENDELEY",
            "",
            "skipped: downloaded files do not provide a verified folder-to-label mapping; Data in Brief describes Day-*S/*R as trip timing folders, not class labels",
        )

    label = infer_explicit_label_from_csv(folder)
    if label is not None:
        return label, "CUSTOM", "explicit label column in CSV", None

    return None, "UNKNOWN", "", "no explicit label source"


def infer_explicit_label_from_csv(folder: Path) -> str | None:
    for csv_file in folder.glob("*.csv"):
        try:
            data = pd.read_csv(csv_file, nrows=25)
        except Exception:
            continue
        for column in data.columns:
            if compact(column) in {"label", "class", "behavior", "behaviour"}:
                labels = data[column].dropna().map(normalize_label).dropna()
                if not labels.empty and labels.nunique() == 1:
                    return str(labels.iloc[0])
    return None


def extract_windows(recording: pd.DataFrame, config: WindowConfig) -> pd.DataFrame:
    recording = recording.dropna(subset=["acc_x", "acc_y", "acc_z", "gyro_x", "gyro_y", "gyro_z", "label"]).copy()
    if recording.empty:
        return pd.DataFrame(columns=[*FEATURE_NAMES, "label"])

    recording = normalize_elapsed_ms(recording, config.sample_rate_hz)
    recording = normalize_gyro_units(recording)
    recording = recording.sort_values("elapsed_ms").reset_index(drop=True)

    window_ms = int(config.window_seconds * 1000)
    stride_ms = int(config.stride_seconds * 1000)
    start_ms = int(recording["elapsed_ms"].min())
    end_ms = int(recording["elapsed_ms"].max())

    rows: list[dict[str, float | str]] = []
    current = start_ms
    while current + window_ms <= end_ms + 1:
        window = recording[(recording["elapsed_ms"] >= current) & (recording["elapsed_ms"] < current + window_ms)]
        if len(window) >= config.min_window_samples:
            label = window["label"].mode(dropna=True)
            if not label.empty:
                features = feature_row(window)
                features["label"] = str(label.iloc[0])
                rows.append(features)
        current += stride_ms

    return pd.DataFrame(rows)


def feature_row(window: pd.DataFrame) -> dict[str, float]:
    acc_mag = np.sqrt(window["acc_x"] ** 2 + window["acc_y"] ** 2 + window["acc_z"] ** 2)
    linear_accel = np.abs(acc_mag if acc_mag.median() < 6.0 else acc_mag - GRAVITY_MPS2)
    elapsed_seconds = window["elapsed_ms"].to_numpy(dtype=float) / 1000.0
    linear_values = np.asarray(linear_accel, dtype=float)
    jerk = np.abs(np.diff(linear_values)) / np.maximum(np.diff(elapsed_seconds), 1e-3)
    gyro_mag = np.sqrt(window["gyro_x"] ** 2 + window["gyro_y"] ** 2 + window["gyro_z"] ** 2)
    motion = linear_accel + gyro_mag

    return {
        **signal_features("accel", linear_accel, peak_threshold=2.5),
        **signal_features("jerk", pd.Series(jerk), peak_threshold=15.0, include_sma=False, percentiles=(0.90, 0.95)),
        **signal_features("gyro", gyro_mag, peak_threshold=0.75, percentiles=(0.90, 0.95)),
        **compact_signal_features("motion", motion),
    }


def signal_features(
    prefix: str,
    values: pd.Series,
    peak_threshold: float,
    include_sma: bool = True,
    percentiles: tuple[float, ...] = (0.50, 0.75, 0.90, 0.95),
) -> dict[str, float]:
    arr = np.asarray(values, dtype=float)
    result = {
        f"{prefix}_mean": float(np.mean(arr)),
        f"{prefix}_std": float(np.std(arr)),
        f"{prefix}_min": float(np.min(arr)),
        f"{prefix}_max": float(np.max(arr)),
        f"{prefix}_rms": float(np.sqrt(np.mean(np.square(arr)))),
        f"{prefix}_energy": float(np.mean(np.square(arr))),
        f"{prefix}_peak_count": float(np.sum(arr >= peak_threshold)),
    }
    for percentile in percentiles:
        result[f"{prefix}_p{int(percentile * 100)}"] = float(np.quantile(arr, percentile))
    if include_sma:
        result[f"{prefix}_sma"] = float(np.mean(np.abs(arr)))
    return result


def compact_signal_features(prefix: str, values: pd.Series) -> dict[str, float]:
    arr = np.asarray(values, dtype=float)
    return {
        f"{prefix}_mean": float(np.mean(arr)),
        f"{prefix}_std": float(np.std(arr)),
        f"{prefix}_max": float(np.max(arr)),
        f"{prefix}_rms": float(np.sqrt(np.mean(np.square(arr)))),
        f"{prefix}_energy": float(np.mean(np.square(arr))),
    }


def merge_accel_gyro(accel_file: Path, gyro_file: Path) -> pd.DataFrame:
    accel = normalize_axis_csv(accel_file, "acc")
    gyro = normalize_axis_csv(gyro_file, "gyro")
    if accel.empty or gyro.empty:
        return pd.DataFrame()

    accel = normalize_elapsed_ms(accel, sample_rate_hz=50.0)
    gyro = normalize_elapsed_ms(gyro, sample_rate_hz=50.0)
    merged = pd.merge_asof(
        accel.sort_values("elapsed_ms"),
        gyro.sort_values("elapsed_ms"),
        on="elapsed_ms",
        direction="nearest",
        tolerance=40,
    )
    return merged.dropna(subset=["acc_x", "acc_y", "acc_z", "gyro_x", "gyro_y", "gyro_z"])


def normalize_axis_csv(csv_file: Path, prefix: str) -> pd.DataFrame:
    raw = read_csv(csv_file)
    if raw.empty:
        return raw

    renamed: dict[str, str] = {}
    for column in raw.columns:
        key = compact(column)
        if key == "x":
            renamed[column] = f"{prefix}_x"
        elif key == "y":
            renamed[column] = f"{prefix}_y"
        elif key == "z":
            renamed[column] = f"{prefix}_z"
        elif key in {"secondselapsed", "elapsedseconds"}:
            renamed[column] = "elapsed_seconds"
        elif "millisecond" in key or key == "ms":
            renamed[column] = "elapsed_ms"
        elif key in {"timestamp", "time", "unixtimestamp"}:
            renamed[column] = "timestamp"

    data = raw.rename(columns=renamed)
    required = {f"{prefix}_x", f"{prefix}_y", f"{prefix}_z"}
    if not required.issubset(data.columns):
        return pd.DataFrame()
    return data


def normalize_elapsed_ms(data: pd.DataFrame, sample_rate_hz: float) -> pd.DataFrame:
    data = data.copy()
    if "elapsed_ms" in data.columns:
        data["elapsed_ms"] = pd.to_numeric(data["elapsed_ms"], errors="coerce")
        if data["elapsed_ms"].notna().any():
            first = data["elapsed_ms"].dropna().iloc[0]
            if first > 10_000:
                data["elapsed_ms"] = data["elapsed_ms"] - first
            return data

    if "elapsed_seconds" in data.columns:
        seconds = pd.to_numeric(data["elapsed_seconds"], errors="coerce")
        if seconds.notna().any():
            data["elapsed_ms"] = (seconds - seconds.dropna().iloc[0]) * 1000.0
            return data

    if "timestamp" in data.columns:
        numeric = pd.to_numeric(data["timestamp"], errors="coerce")
        if numeric.notna().any():
            first = numeric.dropna().iloc[0]
            if numeric.max() > 10_000_000_000_000:
                multiplier = 0.000001
            elif numeric.max() > 10_000_000_000:
                multiplier = 1.0
            else:
                multiplier = 1000.0
            data["elapsed_ms"] = (numeric - first) * multiplier
            return data

    data["elapsed_ms"] = np.arange(len(data)) * (1000.0 / sample_rate_hz)
    return data


def normalize_gyro_units(data: pd.DataFrame) -> pd.DataFrame:
    data = data.copy()
    gyro_cols = ["gyro_x", "gyro_y", "gyro_z"]
    gyro_abs_p95 = data[gyro_cols].abs().quantile(0.95).max()
    if gyro_abs_p95 > 10.0:
        data[gyro_cols] = np.deg2rad(data[gyro_cols])
    return data


def first_named(folder: Path, token: str) -> Path | None:
    matches = sorted(path for path in folder.glob("*.csv") if token in compact(path.stem))
    return matches[0] if matches else None


def recording_group(folder: Path) -> str:
    parts = list(folder.parts)
    if "Road Data" in parts:
        index = parts.index("Road Data")
        return "/".join(parts[index + 1 :])
    if "mendeley" in [part.lower() for part in parts]:
        return folder.name
    return str(folder)


def normalize_label(value: object) -> str | None:
    text = str(value).strip().lower()
    if text in {"0", "normal", "safe", "standard", "slow", "calm"}:
        return "NORMAL"
    if text in {"1", "aggressive", "agressive", "moderate"}:
        return "AGGRESSIVE"
    if text in {"2", "risky", "risk", "rash", "dangerous"}:
        return "RISKY"
    if text in {"3", "road_anomaly", "road anomaly", "bump", "pothole"}:
        return "ROAD_ANOMALY"
    return None


def manifest_frame(recordings: Iterable[Recording]) -> pd.DataFrame:
    rows = [
        {
            "source": recording.source,
            "path": str(recording.path),
            "label": recording.label,
            "group": recording.group,
            "label_source": recording.label_source,
            "accel_file": str(recording.accel_file),
            "gyro_file": str(recording.gyro_file),
        }
        for recording in recordings
    ]
    return pd.DataFrame(rows, columns=["source", "path", "label", "group", "label_source", "accel_file", "gyro_file"])


def skipped_frame(skipped: Iterable[SkippedRecording]) -> pd.DataFrame:
    rows = [
        {
            "source": item.source,
            "path": str(item.path),
            "reason": item.reason,
        }
        for item in skipped
    ]
    return pd.DataFrame(rows, columns=["source", "path", "reason"])


def compact(value: object) -> str:
    return "".join(ch for ch in str(value).lower() if ch.isalnum())


def read_csv(csv_file: Path) -> pd.DataFrame:
    try:
        return pd.read_csv(csv_file)
    except UnicodeDecodeError:
        return pd.read_csv(csv_file, encoding="latin1")
    except Exception:
        return pd.DataFrame()
