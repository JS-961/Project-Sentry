import "dart:async";

import "package:flutter/foundation.dart";

import "../models/sensor_sample.dart";
import "sensor_source.dart";

class RecordedSensorService implements SensorSource {
  RecordedSensorService({
    required List<SensorSample> samples,
    this.onComplete,
  }) : _samples = List<SensorSample>.unmodifiable(samples);

  final List<SensorSample> _samples;
  final VoidCallback? onComplete;

  final StreamController<SensorSample> _controller =
      StreamController<SensorSample>.broadcast();

  Timer? _timer;
  int _index = 0;
  bool _active = false;

  @override
  Stream<SensorSample> get stream => _controller.stream;

  @override
  Future<void> start() async {
    if (_active) {
      return;
    }
    _active = true;
    _index = 0;
    if (_samples.isEmpty) {
      _active = false;
      onComplete?.call();
      return;
    }
    _emitNext();
  }

  @override
  Future<void> stop() async {
    _active = false;
    _timer?.cancel();
    _timer = null;
  }

  @override
  void dispose() {
    unawaited(stop());
    _controller.close();
  }

  void _emitNext() {
    if (!_active) {
      return;
    }

    if (_index >= _samples.length) {
      _active = false;
      onComplete?.call();
      return;
    }

    final sample = _samples[_index];
    _controller.add(sample);
    _index += 1;

    if (_index >= _samples.length) {
      _active = false;
      onComplete?.call();
      return;
    }

    final next = _samples[_index];
    final delay = next.timestamp.difference(sample.timestamp);
    _timer = Timer(
      delay > Duration.zero ? delay : const Duration(milliseconds: 16),
      _emitNext,
    );
  }
}
