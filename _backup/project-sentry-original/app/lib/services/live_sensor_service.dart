import "dart:async";

import "package:geolocator/geolocator.dart";
import "package:sensors_plus/sensors_plus.dart";

import "../models/sensor_sample.dart";
import "sensor_source.dart";

class LiveSensorService implements SensorSource {
  LiveSensorService({
    this.samplingPeriod = SensorInterval.gameInterval,
    this.enableSpeedTracking = true,
  });

  static const double _gravityMps2 = 9.80665;

  final Duration samplingPeriod;
  final bool enableSpeedTracking;

  final StreamController<SensorSample> _controller =
      StreamController<SensorSample>.broadcast();

  StreamSubscription<AccelerometerEvent>? _accelerometerSubscription;
  StreamSubscription<Position>? _positionSubscription;
  double _speedMps = 0.0;

  @override
  Stream<SensorSample> get stream => _controller.stream;

  @override
  Future<void> start() async {
    if (_accelerometerSubscription != null) {
      return;
    }

    if (enableSpeedTracking) {
      unawaited(_startSpeedStream());
    }

    _accelerometerSubscription = accelerometerEventStream(
      samplingPeriod: samplingPeriod,
    ).listen(
      (event) {
        _controller.add(
          SensorSample(
            timestamp: event.timestamp,
            ax: event.x / _gravityMps2,
            ay: event.y / _gravityMps2,
            az: event.z / _gravityMps2,
            speedMps: _speedMps,
          ),
        );
      },
      onError: _controller.addError,
    );
  }

  @override
  Future<void> stop() async {
    await _accelerometerSubscription?.cancel();
    _accelerometerSubscription = null;
    await _positionSubscription?.cancel();
    _positionSubscription = null;
    _speedMps = 0.0;
  }

  @override
  void dispose() {
    unawaited(stop());
    _controller.close();
  }

  Future<void> _startSpeedStream() async {
    if (_positionSubscription != null) {
      return;
    }

    try {
      final serviceEnabled = await Geolocator.isLocationServiceEnabled();
      if (!serviceEnabled) {
        return;
      }

      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.denied ||
          permission == LocationPermission.deniedForever) {
        return;
      }

      _positionSubscription = Geolocator.getPositionStream(
        locationSettings: const LocationSettings(
          accuracy: LocationAccuracy.best,
          distanceFilter: 0,
        ),
      ).listen((position) {
        final speed = position.speed;
        if (speed.isFinite && speed >= 0) {
          _speedMps = speed;
        }
      });
    } catch (_) {
      // Sensor capture is still useful even if GPS speed is unavailable.
    }
  }
}
