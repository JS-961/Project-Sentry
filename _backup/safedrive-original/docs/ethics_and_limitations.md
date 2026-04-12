# Ethics and Limitations

## Intended Use

SafeDrive AI is an educational/demo safety assistant prototype, not a certified emergency response system.

## Critical Limitations

- False positives can trigger unnecessary alerts.
- False negatives can miss real incidents.
- Smartphone mounting position and sensor quality materially affect performance.
- Location accuracy and speed estimates can degrade in tunnels, urban canyons, or poor GPS conditions.

## Emergency Calling Constraints (Android)

- Android and carriers may enforce restrictions around call placement behavior.
- Emergency calling may require explicit user confirmation.
- Demo should use a non-emergency configurable phone number only.

## Privacy Principles

- Local-first storage only for MVP.
- Collect only data necessary for risk/crash logic and trip summaries.
- Avoid storing raw sensor streams long-term unless required for validation.
- Never commit real phone numbers, real personal locations, or other identifying data.

## Testing Ethics

- Never test with actual crashes or dangerous driving.
- Use simulation and controlled, lawful, low-risk scenarios.
- Obtain participant consent if collecting driving-related datasets.
- Anonymize any data used for ML training and remove personal identifiers.

## Responsible Demo Messaging

- Clearly state this is a prototype.
- Explain potential inaccuracies before demos.
- Keep human-in-the-loop controls visible (cancel countdown, "I'm OK").
