import "../models/sensor_sample.dart";

abstract class SensorSource {
  Stream<SensorSample> get stream;
  Future<void> start();
  Future<void> stop();
  void dispose();
}
