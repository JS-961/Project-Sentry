# Dataset Guidelines

Place driving datasets here for local ML experimentation.

## Rules

- Use anonymized data only.
- Remove direct identifiers (names, phone numbers, exact home/work addresses).
- Prefer derived or coarse-grained location features over raw personal traces when possible.
- Do not commit private or licensed datasets without permission.

## Expected Inputs (Starter)

- Timestamped accelerometer and gyroscope streams.
- Speed estimates and optional heading.
- Event labels (harsh brake/accel/cornering/speeding; optional crash-like labels).

For Android-native advisory training, use only labels that are explicit in the
dataset folder names, metadata, or CSV columns. Do not infer abbreviations such
as `R`, `S`, or `E` unless source documentation proves the meaning.

Supported labels:

- `NORMAL`
- `AGGRESSIVE`
- `RISKY`
- `ROAD_ANOMALY`

The training code can infer labels from parent folder names such as
`normal/`, `aggressive/`, `risky/`, `bump/`, and `pothole/`, or from a CSV
column named `label`, `class`, `behavior`, or `behaviour`.

## Suggested Structure

```text
ml/data/
|-- raw/        # source files (gitignored when sensitive)
|-- interim/    # cleaned intermediate files
`-- processed/  # model-ready windows/features
```

## Notes

Use synthetic or public-safe sample data for repository examples.
