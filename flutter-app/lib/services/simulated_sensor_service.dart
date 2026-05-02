import "dart:async";
import "dart:math";

import "../models/sensor_sample.dart";
import "sensor_source.dart";

class SimulatedSensorService implements SensorSource {
  SimulatedSensorService({this.sampleRateHz = 10});

  final int sampleRateHz;
  final StreamController<SensorSample> _controller =
      StreamController<SensorSample>.broadcast();
  final Random _random = Random();
  Timer? _timer;
  int _crashTicks = 0;
  double _speedMps = 15.0;

  @override
  Stream<SensorSample> get stream => _controller.stream;

  @override
  Future<void> start() async {
    if (_timer != null) return;
    final periodMs = (1000 / sampleRateHz).round();
    _timer = Timer.periodic(Duration(milliseconds: periodMs), _emitSample);
  }

  @override
  Future<void> stop() async {
    _timer?.cancel();
    _timer = null;
  }

  @override
  void dispose() {
    _timer?.cancel();
    _timer = null;
    _controller.close();
  }

  void injectCrash() {
    _crashTicks = 6;
  }

  void setSpeedMps(double speedMps) {
    _speedMps = speedMps;
  }

  void _emitSample(Timer timer) {
    double ax = _random.nextDouble() * 0.1 - 0.05;
    double ay = _random.nextDouble() * 0.1 - 0.05;
    double az = 1.0 + (_random.nextDouble() * 0.1 - 0.05);

    if (_crashTicks > 0) {
      final spike = 3.5 + _random.nextDouble() * 1.5;
      ax = spike * (_random.nextBool() ? 1 : -1);
      ay = spike * (_random.nextBool() ? 1 : -1);
      az = 1.0 + spike;
      _crashTicks -= 1;
    }

    _controller.add(
      SensorSample(
        timestamp: DateTime.now(),
        ax: ax,
        ay: ay,
        az: az,
        speedMps: _speedMps,
      ),
    );
  }
}
