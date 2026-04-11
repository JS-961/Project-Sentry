import "../models/scenario.dart";

class ScenarioLibrary {
  static List<Scenario> all() {
    return [
      Scenario(
        id: "normal_drive",
        name: "Normal Drive",
        description: "Smooth motion with small bumps. Should NOT trigger.",
        frames: _normalDrive(),
        expectedTrigger: false,
      ),
      Scenario(
        id: "hard_brake",
        name: "Hard Brake",
        description: "Sudden deceleration but below crash thresholds. Should NOT trigger.",
        frames: _hardBrake(),
        expectedTrigger: false,
      ),
      Scenario(
        id: "stop_and_go",
        name: "Stop & Go Traffic",
        description:
            "Repeated stops and gentle accelerations in traffic. Should NOT trigger.",
        frames: _stopAndGo(),
        expectedTrigger: false,
      ),
      Scenario(
        id: "phone_drop",
        name: "Phone Drop (Low Speed)",
        description: "High acceleration spike while speed is low. Should NOT trigger.",
        frames: _phoneDrop(),
        expectedTrigger: false,
      ),
      Scenario(
        id: "collision",
        name: "Collision Spike",
        description: "High acceleration + jerk at speed. Should trigger.",
        frames: _collision(),
        expectedTrigger: true,
      ),
      Scenario(
        id: "pothole_hit",
        name: "Pothole Hit",
        description: "Sharp vertical bump at speed but below crash threshold. Should NOT trigger.",
        frames: _potholeHit(),
        expectedTrigger: false,
      ),
      Scenario(
        id: "sharp_turn",
        name: "Sharp Turn / Evasive",
        description: "Aggressive steering with lateral g-force. Should NOT trigger.",
        frames: _sharpTurn(),
        expectedTrigger: false,
      ),
      Scenario(
        id: "side_swipe",
        name: "Side Swipe Impact",
        description: "High lateral spike while moving. Should trigger.",
        frames: _sideSwipe(),
        expectedTrigger: true,
      ),
      Scenario(
        id: "rear_end",
        name: "Rear-End Collision",
        description: "Sudden deceleration at highway speed. Should trigger.",
        frames: _rearEnd(),
        expectedTrigger: true,
      ),
    ];
  }

  static List<ScenarioFrame> _normalDrive() {
    final frames = <ScenarioFrame>[];
    for (var i = 0; i < 120; i += 1) {
      final wobble = (i % 10) * 0.01;
      frames.add(
        ScenarioFrame(
          ax: wobble,
          ay: -wobble,
          az: 1.0 + (i % 3) * 0.01,
          speedMps: 13.0,
        ),
      );
    }
    return frames;
  }

  static List<ScenarioFrame> _hardBrake() {
    final frames = <ScenarioFrame>[];
    for (var i = 0; i < 40; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 0.03,
          ay: -0.02,
          az: 1.0,
          speedMps: 15.0,
        ),
      );
    }
    for (var i = 0; i < 8; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 1.8,
          ay: -0.4,
          az: 1.6,
          speedMps: 10.0,
        ),
      );
    }
    for (var i = 0; i < 30; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 0.05,
          ay: 0.01,
          az: 1.0,
          speedMps: 8.0,
        ),
      );
    }
    return frames;
  }

  static List<ScenarioFrame> _stopAndGo() {
    final frames = <ScenarioFrame>[];
    for (var i = 0; i < 20; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 0.02,
          ay: -0.01,
          az: 1.0,
          speedMps: 0.0,
        ),
      );
    }
    for (var i = 0; i < 10; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 1.2,
          ay: 0.2,
          az: 1.1,
          speedMps: 4.0,
        ),
      );
    }
    for (var i = 0; i < 18; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 0.04,
          ay: 0.01,
          az: 1.0,
          speedMps: 10.0,
        ),
      );
    }
    for (var i = 0; i < 8; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: -1.4,
          ay: -0.1,
          az: 1.0,
          speedMps: 6.0,
        ),
      );
    }
    for (var i = 0; i < 14; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 0.02,
          ay: 0.0,
          az: 1.0,
          speedMps: 1.0,
        ),
      );
    }
    return frames;
  }

  static List<ScenarioFrame> _phoneDrop() {
    final frames = <ScenarioFrame>[];
    for (var i = 0; i < 20; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 0.01,
          ay: 0.02,
          az: 1.0,
          speedMps: 0.0,
        ),
      );
    }
    for (var i = 0; i < 4; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 4.2,
          ay: -3.7,
          az: 5.0,
          speedMps: 0.0,
        ),
      );
    }
    for (var i = 0; i < 20; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 0.0,
          ay: 0.0,
          az: 1.0,
          speedMps: 0.0,
        ),
      );
    }
    return frames;
  }

  static List<ScenarioFrame> _collision() {
    final frames = <ScenarioFrame>[];
    for (var i = 0; i < 30; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 0.03,
          ay: -0.02,
          az: 1.0,
          speedMps: 16.0,
        ),
      );
    }
    for (var i = 0; i < 6; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 4.8,
          ay: -4.1,
          az: 5.2,
          speedMps: 16.0,
        ),
      );
    }
    for (var i = 0; i < 20; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 0.1,
          ay: 0.05,
          az: 1.0,
          speedMps: 4.0,
        ),
      );
    }
    return frames;
  }

  static List<ScenarioFrame> _potholeHit() {
    final frames = <ScenarioFrame>[];
    for (var i = 0; i < 28; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 0.04,
          ay: -0.03,
          az: 1.02,
          speedMps: 13.0,
        ),
      );
    }
    for (var i = 0; i < 3; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 0.2,
          ay: 0.1,
          az: 2.6,
          speedMps: 13.0,
        ),
      );
    }
    for (var i = 0; i < 20; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 0.03,
          ay: 0.02,
          az: 1.0,
          speedMps: 12.0,
        ),
      );
    }
    return frames;
  }

  static List<ScenarioFrame> _sharpTurn() {
    final frames = <ScenarioFrame>[];
    for (var i = 0; i < 24; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 0.02,
          ay: -0.02,
          az: 1.0,
          speedMps: 18.0,
        ),
      );
    }
    for (var i = 0; i < 10; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 1.4,
          ay: 1.2,
          az: 1.0,
          speedMps: 16.0,
        ),
      );
    }
    for (var i = 0; i < 20; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 0.05,
          ay: 0.01,
          az: 1.0,
          speedMps: 15.0,
        ),
      );
    }
    return frames;
  }

  static List<ScenarioFrame> _sideSwipe() {
    final frames = <ScenarioFrame>[];
    for (var i = 0; i < 24; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 0.05,
          ay: -0.04,
          az: 1.0,
          speedMps: 14.0,
        ),
      );
    }
    for (var i = 0; i < 4; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 3.8,
          ay: 3.4,
          az: 1.0,
          speedMps: 14.0,
        ),
      );
    }
    for (var i = 0; i < 16; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 0.1,
          ay: 0.05,
          az: 1.0,
          speedMps: 6.0,
        ),
      );
    }
    return frames;
  }

  static List<ScenarioFrame> _rearEnd() {
    final frames = <ScenarioFrame>[];
    for (var i = 0; i < 30; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 0.03,
          ay: 0.01,
          az: 1.0,
          speedMps: 22.0,
        ),
      );
    }
    for (var i = 0; i < 5; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 5.1,
          ay: -0.2,
          az: 1.3,
          speedMps: 21.0,
        ),
      );
    }
    for (var i = 0; i < 20; i += 1) {
      frames.add(
        ScenarioFrame(
          ax: 0.08,
          ay: 0.02,
          az: 1.0,
          speedMps: 7.0,
        ),
      );
    }
    return frames;
  }
}
