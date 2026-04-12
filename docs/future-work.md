# Future Work

## Near-Term Cleanup

1. Add a native history and export screen backed by Room queries.
2. Add startup smoke checks or CI for both `flutter-app/` and `android-native/`.
3. Continue aligning remaining low-risk branding and polish details.

## Low-Risk Integration Paths

1. Share trace formats between Flutter validation captures and native Android
   analysis tools.
2. Share report inputs and output conventions.
3. Add launcher or deep-link coordination so demos can jump between the two
   apps.

## Higher-Risk Work Deliberately Deferred

1. Rewriting the native Android service into Flutter.
2. Large MethodChannel integrations without a stable contract.
3. Collapsing both apps into one runtime before the MVP responsibilities are
   stable.

## ML Roadmap

1. Build reusable preprocessing code in `ml/src/`.
2. Evaluate anonymized driving and road-safety datasets.
3. Export candidate TensorFlow Lite models.
4. Integrate only after the repository can demonstrate real offline inference.
