import "package:flutter_test/flutter_test.dart";
import "package:shared_preferences/shared_preferences.dart";

import "package:sentry/models/detection_config.dart";
import "package:sentry/services/config_store.dart";

void main() {
  test("ConfigStore loads defaults when empty", () async {
    SharedPreferences.setMockInitialValues({});
    final store = ConfigStore();
    final config = await store.load();
    expect(config.accelThresholdG, 3.0);
    expect(config.jerkThresholdGPerS, 15.0);
    expect(config.minSpeedMps, 5.0);
    expect(config.cooldownSeconds, 10);
  });

  test("ConfigStore saves and loads values", () async {
    SharedPreferences.setMockInitialValues({});
    final store = ConfigStore();
    const updated = DetectionConfig(
      accelThresholdG: 4.2,
      jerkThresholdGPerS: 20.5,
      minSpeedMps: 8.0,
      cooldownSeconds: 12,
    );
    await store.save(updated);

    final loaded = await store.load();
    expect(loaded.accelThresholdG, 4.2);
    expect(loaded.jerkThresholdGPerS, 20.5);
    expect(loaded.minSpeedMps, 8.0);
    expect(loaded.cooldownSeconds, 12);
  });
}
