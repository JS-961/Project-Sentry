import "dart:math";

class SensorSample {
  SensorSample({
    required this.timestamp,
    required this.ax,
    required this.ay,
    required this.az,
    required this.speedMps,
  });

  final DateTime timestamp;
  final double ax;
  final double ay;
  final double az;
  final double speedMps;

  double get magnitude => sqrt((ax * ax) + (ay * ay) + (az * az));
}
