import "dart:async";

import "package:flutter/foundation.dart";

import "../models/scenario.dart";
import "../models/sensor_sample.dart";
import "sensor_source.dart";

class ScenarioSensorService implements SensorSource {
  ScenarioSensorService({
    required List<ScenarioFrame> frames,
    this.sampleRateHz = 10,
    this.onComplete,
  }) : _frames = frames;

  final List<ScenarioFrame> _frames;
  final int sampleRateHz;
  final VoidCallback? onComplete;

  final StreamController<SensorSample> _controller =
      StreamController<SensorSample>.broadcast();
  Timer? _timer;
  int _index = 0;

  @override
  Stream<SensorSample> get stream => _controller.stream;

  @override
  Future<void> start() async {
    if (_timer != null) return;
    _index = 0;
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

  void _emitSample(Timer timer) {
    if (_index >= _frames.length) {
      stop();
      onComplete?.call();
      return;
    }

    final frame = _frames[_index];
    _index += 1;

    _controller.add(
      SensorSample(
        timestamp: DateTime.now(),
        ax: frame.ax,
        ay: frame.ay,
        az: frame.az,
        speedMps: frame.speedMps,
      ),
    );
  }
}
