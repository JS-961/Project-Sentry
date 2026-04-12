import "package:shared_preferences/shared_preferences.dart";

import "../models/detection_config.dart";

class ConfigStore {
  static const String _accelKey = "sentry_accel_threshold_g";
  static const String _jerkKey = "sentry_jerk_threshold_gps";
  static const String _speedKey = "sentry_min_speed_mps";
  static const String _cooldownKey = "sentry_cooldown_seconds";

  Future<DetectionConfig> load() async {
    final prefs = await SharedPreferences.getInstance();
    return DetectionConfig(
      accelThresholdG: prefs.getDouble(_accelKey) ?? 3.0,
      jerkThresholdGPerS: prefs.getDouble(_jerkKey) ?? 15.0,
      minSpeedMps: prefs.getDouble(_speedKey) ?? 5.0,
      cooldownSeconds: prefs.getInt(_cooldownKey) ?? 10,
    );
  }

  Future<void> save(DetectionConfig config) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setDouble(_accelKey, config.accelThresholdG);
    await prefs.setDouble(_jerkKey, config.jerkThresholdGPerS);
    await prefs.setDouble(_speedKey, config.minSpeedMps);
    await prefs.setInt(_cooldownKey, config.cooldownSeconds);
  }

  Future<void> reset() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_accelKey);
    await prefs.remove(_jerkKey);
    await prefs.remove(_speedKey);
    await prefs.remove(_cooldownKey);
  }
}
