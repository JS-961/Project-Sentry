import "dart:async";

import "package:flutter/foundation.dart";

import "../models/detection_config.dart";
import "../models/sensor_sample.dart";
import "detection_engine.dart";
import "sensor_source.dart";
import "simulated_sensor_service.dart";

class TripController extends ChangeNotifier {
  TripController({
    required SensorSource sensorSource,
    DetectionEngine? detector,
  })  : _sensorSource = sensorSource,
        _detector = detector ?? DetectionEngine();

  final SensorSource _sensorSource;
  DetectionEngine _detector;

  bool _active = false;
  bool _alertVisible = false;
  SensorSample? _latestSample;
  DetectionDecision? _lastDecision;

  StreamSubscription<SensorSample>? _subscription;
  final StreamController<DetectionDecision> _decisions =
      StreamController<DetectionDecision>.broadcast();

  bool get isActive => _active;
  bool get alertVisible => _alertVisible;
  SensorSample? get latestSample => _latestSample;
  DetectionDecision? get lastDecision => _lastDecision;
  Stream<DetectionDecision> get decisions => _decisions.stream;

  Future<void> start() async {
    if (_active) return;
    _active = true;
    notifyListeners();
    await _sensorSource.start();
    _subscription?.cancel();
    _subscription = _sensorSource.stream.listen(_handleSample);
  }

  Future<void> stop() async {
    if (!_active) return;
    await _subscription?.cancel();
    _subscription = null;
    await _sensorSource.stop();
    _active = false;
    notifyListeners();
  }

  void setAlertVisible(bool value) {
    _alertVisible = value;
  }

  void injectCrash() {
    if (_sensorSource is SimulatedSensorService) {
      (_sensorSource as SimulatedSensorService).injectCrash();
    }
  }

  void updateConfig(DetectionConfig config) {
    _detector = DetectionEngine(config: config);
    _lastDecision = null;
  }

  @override
  void dispose() {
    _subscription?.cancel();
    _sensorSource.dispose();
    _decisions.close();
    super.dispose();
  }

  void _handleSample(SensorSample sample) {
    _latestSample = sample;
    final decision = _detector.addSample(sample);
    if (decision != null) {
      _lastDecision = decision;
      _decisions.add(decision);
    }
    notifyListeners();
  }
}
