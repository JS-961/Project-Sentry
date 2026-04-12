import "package:flutter/material.dart";

import "../models/detection_config.dart";
import "../services/config_store.dart";
import "../services/event_log.dart";
import "home_screen.dart";
import "results_summary_screen.dart";
import "scenario_lab_screen.dart";

class AppShell extends StatefulWidget {
  const AppShell({super.key});

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  final EventLog _eventLog = EventLog();
  final ConfigStore _configStore = ConfigStore();

  DetectionConfig _config = const DetectionConfig();
  bool _logLoaded = false;
  int _index = 0;

  @override
  void initState() {
    super.initState();
    _loadState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _index,
        children: [
          HomeScreen(
            eventLog: _eventLog,
            config: _config,
            logLoaded: _logLoaded,
            onConfigChanged: _updateConfig,
          ),
          ScenarioLabScreen(
            eventLog: _eventLog,
            config: _config,
          ),
          ResultsSummaryScreen(eventLog: _eventLog),
        ],
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _index,
        onTap: (value) => setState(() => _index = value),
        items: const [
          BottomNavigationBarItem(
            icon: Icon(Icons.home_outlined),
            label: "Home",
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.science_outlined),
            label: "Lab",
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.analytics_outlined),
            label: "Results",
          ),
        ],
      ),
    );
  }

  Future<void> _loadState() async {
    await _eventLog.load();
    final loaded = await _configStore.load();
    if (!mounted) return;
    setState(() {
      _logLoaded = true;
      _config = loaded;
    });
  }

  Future<void> _updateConfig(DetectionConfig config) async {
    await _configStore.save(config);
    if (!mounted) return;
    setState(() {
      _config = config;
    });
  }
}
