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

## Suggested Structure

```text
ml/data/
|-- raw/        # source files (gitignored when sensitive)
|-- interim/    # cleaned intermediate files
`-- processed/  # model-ready windows/features
```

## Notes

Use synthetic or public-safe sample data for repository examples.
