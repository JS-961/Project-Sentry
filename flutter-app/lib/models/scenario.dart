class ScenarioFrame {
  const ScenarioFrame({
    required this.ax,
    required this.ay,
    required this.az,
    required this.speedMps,
  });

  final double ax;
  final double ay;
  final double az;
  final double speedMps;
}

class Scenario {
  const Scenario({
    required this.id,
    required this.name,
    required this.description,
    required this.frames,
    required this.expectedTrigger,
    this.sampleRateHz = 10,
  });

  final String id;
  final String name;
  final String description;
  final List<ScenarioFrame> frames;
  final bool expectedTrigger;
  final int sampleRateHz;
}
