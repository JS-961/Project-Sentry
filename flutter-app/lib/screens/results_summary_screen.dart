import "dart:convert";
import "dart:typed_data";
import "dart:math";
import "dart:ui" as ui;

import "package:flutter/material.dart";
import "package:flutter/rendering.dart";
import "package:flutter/services.dart";
import "package:archive/archive.dart";

import "../models/crash_event.dart";
import "../services/asset_loader.dart";
import "../services/event_log.dart";
import "../services/file_exporter.dart";
import "../services/report_generator.dart";
import "../widgets/brand_logo.dart";
import "../widgets/reveal_on_build.dart";

class ResultsSummaryScreen extends StatefulWidget {
  const ResultsSummaryScreen({super.key, required this.eventLog});

  final EventLog eventLog;

  @override
  State<ResultsSummaryScreen> createState() => _ResultsSummaryScreenState();
}

class _ResultsSummaryScreenState extends State<ResultsSummaryScreen> {
  final GlobalKey _chartKey = GlobalKey();
  String? _diagrams;
  String? _template;
  String? _logoDataUri;
  bool _assetsLoaded = false;

  @override
  void initState() {
    super.initState();
    _loadAssets();
  }

  @override
  Widget build(BuildContext context) {
    final events = widget.eventLog.events;
    final summary = _summarize(events);
    final breakdown = _scenarioBreakdown(events);
    final reportMd = ReportGenerator().generateTestingReport(events);
    final reportHtml = ReportGenerator().generateTestingReportHtml(
      events,
      logoDataUri: _logoDataUri,
    );
    final reportActions = [
      _ActionItem(
        label: "Save Chart Image (PNG)",
        icon: Icons.photo_outlined,
        onTap: () => _saveChart(context),
      ),
      _ActionItem(
        label: "Copy Report (Markdown)",
        icon: Icons.copy_all_outlined,
        onTap: () => _copyReport(context, reportMd),
      ),
      _ActionItem(
        label: "Save Report (Markdown)",
        icon: Icons.description_outlined,
        onTap: () => _saveReportMd(context, reportMd),
      ),
      _ActionItem(
        label: "Save Report (HTML)",
        icon: Icons.language_outlined,
        onTap: () => _saveReportHtml(context, reportHtml),
      ),
      _ActionItem(
        label: "Save Scenario Outcomes (CSV)",
        icon: Icons.fact_check_outlined,
        onTap: () => _saveScenarioCsv(context, events),
      ),
      _ActionItem(
        label: "Save Final Report Pack",
        icon: Icons.inventory_2_outlined,
        onTap: () => _saveFinalPack(
          context,
          reportMd,
          reportHtml,
          widget.eventLog.exportCsv(),
          widget.eventLog.exportJson(),
        ),
        highlight: true,
      ),
    ];
    final logActions = [
      _ActionItem(
        label: "Save Log (CSV)",
        icon: Icons.table_chart_outlined,
        onTap: () => _saveCsv(context, widget.eventLog.exportCsv()),
      ),
      _ActionItem(
        label: "Save Log (JSON)",
        icon: Icons.data_object_outlined,
        onTap: () => _saveJson(context, widget.eventLog.exportJson()),
      ),
      _ActionItem(
        label: "Clear Event Log",
        icon: Icons.delete_outline,
        onTap:
            widget.eventLog.events.isEmpty ? null : () => _clearLog(context),
        destructive: true,
      ),
    ];

    return Scaffold(
      appBar: AppBar(
        title: const Text("Results Summary"),
        leading: const BrandLogo(),
        leadingWidth: 48,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          RevealOnBuild(
            delay: const Duration(milliseconds: 40),
            child: _buildSummaryCard(summary),
          ),
          const SizedBox(height: 12),
          RevealOnBuild(
            delay: const Duration(milliseconds: 120),
            child: _buildChart(summary, breakdown),
          ),
          const SizedBox(height: 12),
          RevealOnBuild(
            delay: const Duration(milliseconds: 200),
            child: _buildActionSection("Reports & Charts", reportActions),
          ),
          const SizedBox(height: 12),
          RevealOnBuild(
            delay: const Duration(milliseconds: 260),
            child: _buildActionSection("Raw Logs", logActions),
          ),
          const SizedBox(height: 16),
          _sectionTitle("Scenario Events"),
          const SizedBox(height: 8),
          RevealOnBuild(
            delay: const Duration(milliseconds: 320),
            child: _buildScenarioCard(summary.scenarioEvents),
          ),
          const SizedBox(height: 16),
          RevealOnBuild(
            delay: const Duration(milliseconds: 380),
            child: _buildPreviewCard(
              title: "Report Preview (Markdown)",
              subtitle: "Auto-generated summary used in the report pack.",
              content: reportMd,
            ),
          ),
          const SizedBox(height: 12),
          RevealOnBuild(
            delay: const Duration(milliseconds: 460),
            child: _buildPreviewCard(
              title: "Report Preview (HTML)",
              subtitle: "PDF-friendly layout with the Sentry logo.",
              content: reportHtml,
              monospace: true,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSummaryCard(_ResultsSummary summary) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              "Scenario Results",
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 8),
            Text("Total scenarios: ${summary.scenarioTotal}"),
            Text("Scenario PASS: ${summary.scenarioPass}"),
            Text("Scenario CHECK: ${summary.scenarioCheck}"),
            Text("Pass rate: ${summary.passRate}"),
            Text("Alert events: ${summary.alertEvents}"),
          ],
        ),
      ),
    );
  }

  Widget _buildChart(
    _ResultsSummary summary,
    List<_ScenarioBreakdown> breakdown,
  ) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              "Scenario Results Chart",
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 12),
            RepaintBoundary(
              key: _chartKey,
              child: ClipRRect(
                borderRadius: BorderRadius.circular(12),
                child: Container(
                  color: Colors.white,
                  padding: const EdgeInsets.all(10),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _ChartFrame(
                        child: SizedBox(
                          height: 180,
                          width: double.infinity,
                          child: _SimpleBarChart(
                            pass: summary.scenarioPass,
                            check: summary.scenarioCheck,
                            passColor: Colors.green,
                            checkColor: Colors.orange,
                          ),
                        ),
                      ),
                      const SizedBox(height: 10),
                      Wrap(
                        spacing: 16,
                        runSpacing: 6,
                        children: [
                          _legendSwatch(
                            Colors.green,
                            "PASS: ${summary.scenarioPass}",
                          ),
                          _legendSwatch(
                            Colors.orange,
                            "CHECK: ${summary.scenarioCheck}",
                          ),
                          Text("Pass rate: ${summary.passRate}"),
                        ],
                      ),
                      if (breakdown.isNotEmpty) ...[
                        const SizedBox(height: 12),
                        const Divider(height: 1),
                        const SizedBox(height: 8),
                        Text(
                          "Scenario Breakdown",
                          style: Theme.of(context)
                              .textTheme
                              .bodyMedium
                              ?.copyWith(fontWeight: FontWeight.w600),
                        ),
                        const SizedBox(height: 6),
                        for (final row in breakdown)
                          Padding(
                            padding: const EdgeInsets.symmetric(vertical: 4),
                            child: Row(
                              children: [
                                Expanded(
                                  child: Text(
                                    row.name,
                                    style: Theme.of(context)
                                        .textTheme
                                        .bodySmall
                                        ?.copyWith(color: Colors.black87),
                                  ),
                                ),
                                _countChip(
                                  "PASS",
                                  row.pass,
                                  Colors.green,
                                ),
                                const SizedBox(width: 6),
                                _countChip(
                                  "CHECK",
                                  row.check,
                                  Colors.orange,
                                ),
                              ],
                            ),
                          ),
                      ],
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _legendSwatch(Color color, String label) {
    return Row(
      children: [
        Container(width: 12, height: 12, color: color),
        const SizedBox(width: 6),
        Text(label),
      ],
    );
  }

  Widget _countChip(String label, int value, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.12),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: color.withOpacity(0.4)),
      ),
      child: Text(
        "$label $value",
        style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: color,
              fontWeight: FontWeight.w600,
            ),
      ),
    );
  }

  Widget _buildScenarioList(List<CrashEvent> scenarios) {
    if (scenarios.isEmpty) {
      return const Text("No scenario events recorded yet.");
    }
    return ListView.separated(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: scenarios.length,
      separatorBuilder: (_, __) => const Divider(height: 1),
      itemBuilder: (context, index) {
        final event = scenarios[index];
        final expected = event.expectedTrigger == true ? "Yes" : "No";
        final triggered = event.triggered == true ? "Yes" : "No";
        return ListTile(
          title: Text(event.outcome),
          subtitle: Text(
            "${event.timestamp.toLocal()} - ${event.scenarioId ?? "scenario"}\n"
            "Expected: $expected | Triggered: $triggered",
          ),
          contentPadding:
              const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
        );
      },
    );
  }

  Widget _buildScenarioCard(List<CrashEvent> scenarios) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: _buildScenarioList(scenarios),
      ),
    );
  }

  Widget _buildPreviewCard({
    required String title,
    required String subtitle,
    required String content,
    bool monospace = false,
  }) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: theme.textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              subtitle,
              style: theme.textTheme.bodySmall?.copyWith(color: Colors.black54),
            ),
            const SizedBox(height: 12),
            Container(
              decoration: BoxDecoration(
                color: Colors.black.withOpacity(0.03),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.black12),
              ),
              padding: const EdgeInsets.all(12),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxHeight: 220),
                child: SingleChildScrollView(
                  child: SelectableText(
                    content,
                    style: theme.textTheme.bodySmall?.copyWith(
                      fontFamily: monospace ? "monospace" : null,
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildActionSection(String title, List<_ActionItem> actions) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _sectionTitle(title),
            const SizedBox(height: 8),
            for (var i = 0; i < actions.length; i++) ...[
              _buildActionTile(actions[i]),
              if (i != actions.length - 1) const Divider(height: 1),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildActionTile(_ActionItem item) {
    final scheme = Theme.of(context).colorScheme;
    final canTap = item.onTap != null;
    Color? iconTint;
    Color? textTint;
    if (canTap && item.destructive) {
      iconTint = Colors.redAccent;
      textTint = Colors.redAccent;
    } else if (canTap && item.highlight) {
      iconTint = scheme.secondary;
    }
    return ListTile(
      dense: true,
      contentPadding: EdgeInsets.zero,
      leading: Icon(item.icon, color: iconTint),
      title: Text(
        item.label,
        style: TextStyle(
          fontWeight: item.highlight ? FontWeight.w600 : FontWeight.w500,
          color: textTint,
        ),
      ),
      onTap: item.onTap,
    );
  }

  Widget _sectionTitle(String label) {
    return Text(
      label,
      style: Theme.of(context)
          .textTheme
          .titleMedium
          ?.copyWith(fontWeight: FontWeight.w700),
    );
  }

  Future<void> _copyReport(BuildContext context, String report) async {
    await Clipboard.setData(ClipboardData(text: report));
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text("Report copied to clipboard")),
    );
  }

  Future<void> _saveReportMd(BuildContext context, String report) async {
    final fileName = _timestamped("sentry-report", "md");
    final result = await saveTextFile(fileName: fileName, content: report);
    if (!context.mounted) return;
    _showSaveResult(context, result, "Report saved");
  }

  Future<void> _saveReportHtml(BuildContext context, String report) async {
    final fileName = _timestamped("sentry-report", "html");
    final result = await saveTextFile(fileName: fileName, content: report);
    if (!context.mounted) return;
    _showSaveResult(context, result, "HTML report saved");
  }

  Future<void> _saveCsv(BuildContext context, String csv) async {
    final fileName = _timestamped("sentry-log", "csv");
    final result = await saveTextFile(fileName: fileName, content: csv);
    if (!context.mounted) return;
    _showSaveResult(context, result, "CSV saved");
  }

  Future<void> _saveJson(BuildContext context, String json) async {
    final fileName = _timestamped("sentry-log", "json");
    final result = await saveTextFile(fileName: fileName, content: json);
    if (!context.mounted) return;
    _showSaveResult(context, result, "JSON saved");
  }

  Future<void> _saveScenarioCsv(
    BuildContext context,
    List<CrashEvent> events,
  ) async {
    final fileName = _timestamped("sentry-scenarios", "csv");
    final result = await saveTextFile(
      fileName: fileName,
      content: _scenarioCsv(events),
    );
    if (!context.mounted) return;
    _showSaveResult(context, result, "Scenario CSV saved");
  }

  Future<void> _saveChart(BuildContext context) async {
    final bytes = await _captureChartPng();
    if (bytes == null) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Chart capture failed")),
      );
      return;
    }
    final fileName = _timestamped("sentry-chart", "png");
    final result = await saveBinaryFile(
      fileName: fileName,
      bytes: bytes,
      mimeType: "image/png",
    );
    if (!context.mounted) return;
    _showSaveResult(context, result, "Chart saved");
  }

  Future<void> _saveFinalPack(
    BuildContext context,
    String reportMd,
    String reportHtml,
    String csv,
    String json,
  ) async {
    final now = DateTime.now();
    final stamp = _stamp();
    final date = _dateStamp();
    final folder = "sentry-report-pack-$date";

    final archive = Archive();
    final included = <String>[];
    void addText(String name, String content) {
      final data = utf8.encode(content);
      archive.addFile(ArchiveFile("$folder/$name", data.length, data));
      included.add(name);
    }

    void addBytes(String name, List<int> bytes) {
      archive.addFile(ArchiveFile("$folder/$name", bytes.length, bytes));
      included.add(name);
    }

    addText(_withStamp("sentry-report", "md", stamp), reportMd);
    addText(_withStamp("sentry-report", "html", stamp), reportHtml);
    addText(_withStamp("sentry-log", "csv", stamp), csv);
    addText(_withStamp("sentry-log", "json", stamp), json);
    addText(
      _withStamp("sentry-scenarios", "csv", stamp),
      _scenarioCsv(widget.eventLog.events),
    );

    if (_assetsLoaded && _diagrams != null) {
      addText(_withStamp("sentry-diagrams", "md", stamp), _diagrams!);
    }
    if (_assetsLoaded && _template != null) {
      addText(_withStamp("sentry-report-template", "md", stamp), _template!);
    }

    final chartBytes = await _captureChartPng();
    if (chartBytes != null) {
      addBytes(_withStamp("sentry-chart", "png", stamp), chartBytes);
    }

    final summary = _summarize(widget.eventLog.events);
    final manifest = StringBuffer()
      ..writeln("Sentry Final Report Pack")
      ..writeln("Generated: ${now.toIso8601String()}")
      ..writeln("Folder: $folder")
      ..writeln("Items: ${included.length + 1}")
      ..writeln()
      ..writeln("Summary:")
      ..writeln(" - Total scenarios: ${summary.scenarioTotal}")
      ..writeln(" - Scenario PASS: ${summary.scenarioPass}")
      ..writeln(" - Scenario CHECK: ${summary.scenarioCheck}")
      ..writeln(" - Pass rate: ${summary.passRate}")
      ..writeln(" - Alert events: ${summary.alertEvents}")
      ..writeln()
      ..writeln("Included files:");
    for (final name in included) {
      manifest.writeln("- $name");
    }
    addText("manifest.txt", manifest.toString());

    final zipData = ZipEncoder().encode(archive);

    final zipName = "sentry-report-pack-$date.zip";
    final result = await saveBinaryFile(
      fileName: zipName,
      bytes: zipData,
      mimeType: "application/zip",
    );

    if (!context.mounted) return;
    _showSaveResult(context, result, "Final report pack saved");
  }

  Future<void> _clearLog(BuildContext context) async {
    await widget.eventLog.clear();
    if (!mounted) return;
    setState(() {});
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text("Event log cleared")),
    );
  }

  Future<void> _loadAssets() async {
    const loader = AssetLoader();
    final diagrams = await loader.loadOptional("assets/diagrams.md");
    final template = await loader.loadOptional("assets/report-template.md");
    final logoData = await _loadLogoDataUri();
    if (!mounted) return;
    setState(() {
      _diagrams = diagrams;
      _template = template;
      _logoDataUri = logoData;
      _assetsLoaded = true;
    });
  }

  Future<String?> _loadLogoDataUri() async {
    try {
      final bytes = await rootBundle.load("assets/logo.png");
      final encoded = base64Encode(bytes.buffer.asUint8List());
      return "data:image/png;base64,$encoded";
    } catch (_) {
      return null;
    }
  }

  Future<Uint8List?> _captureChartPng() async {
    try {
      final boundary =
          _chartKey.currentContext?.findRenderObject() as RenderRepaintBoundary?;
      if (boundary == null) return null;
      final image = await boundary.toImage(pixelRatio: 3.0);
      final byteData = await image.toByteData(format: ui.ImageByteFormat.png);
      return byteData?.buffer.asUint8List();
    } catch (_) {
      return null;
    }
  }

  void _showSaveResult(BuildContext context, String? result, String label) {
    final message = result == null ? "$label." : "$label: $result";
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  String _timestamped(String prefix, String ext) {
    final stamp = _stamp();
    return _withStamp(prefix, ext, stamp);
  }

  String _stamp() {
    return DateTime.now().toIso8601String().replaceAll(":", "-");
  }

  String _dateStamp() {
    return DateTime.now().toIso8601String().split("T").first;
  }

  String _withStamp(String prefix, String ext, String stamp) {
    return "$prefix-$stamp.$ext";
  }

  List<_ScenarioBreakdown> _scenarioBreakdown(List<CrashEvent> events) {
    final breakdown = <String, _ScenarioBreakdown>{};
    for (final event in events) {
      if (event.eventType != "scenario") continue;
      final scenarioId = event.scenarioId ?? "scenario";
      final expected = event.expectedTrigger;
      final triggered = event.triggered;
      final passed =
          expected != null && triggered != null && expected == triggered;
      breakdown.putIfAbsent(
        scenarioId,
        () => _ScenarioBreakdown(
          id: scenarioId,
          name: _prettyScenarioName(scenarioId),
        ),
      );
      if (passed) {
        breakdown[scenarioId]!.pass += 1;
      } else {
        breakdown[scenarioId]!.check += 1;
      }
    }
    return breakdown.values.toList()
      ..sort((a, b) => a.name.compareTo(b.name));
  }

  String _scenarioCsv(List<CrashEvent> events) {
    final buffer = StringBuffer();
    buffer.writeln(
      "timestamp,scenario_id,scenario_name,outcome,expected_trigger,triggered",
    );
    for (final event in events) {
      if (event.eventType != "scenario") continue;
      final scenarioId = event.scenarioId ?? "";
      final scenarioName = _prettyScenarioName(scenarioId);
      buffer.writeln(
        "${_csvEscape(event.timestamp.toIso8601String())},"
        "${_csvEscape(scenarioId)},"
        "${_csvEscape(scenarioName)},"
        "${_csvEscape(event.outcome)},"
        "${_csvEscape(event.expectedTrigger?.toString() ?? "")},"
        "${_csvEscape(event.triggered?.toString() ?? "")}",
      );
    }
    return buffer.toString();
  }

  String _csvEscape(String value) {
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      final escaped = value.replaceAll("\"", "\"\"");
      return "\"$escaped\"";
    }
    return value;
  }

  String _prettyScenarioName(String scenarioId) {
    if (scenarioId.isEmpty) return "Scenario";
    final parts = scenarioId.split("_");
    return parts
        .map((part) =>
            part.isEmpty ? part : part[0].toUpperCase() + part.substring(1))
        .join(" ");
  }

  _ResultsSummary _summarize(List<CrashEvent> events) {
    final scenarioEvents =
        events.where((event) => event.eventType == "scenario").toList();
    final alertEvents =
        events.where((event) => event.eventType == "alert").length;
    var pass = 0;
    var check = 0;
    for (final event in scenarioEvents) {
      if (event.expectedTrigger != null && event.triggered != null) {
        if (event.expectedTrigger == event.triggered) {
          pass += 1;
        } else {
          check += 1;
        }
      }
    }
    final total = scenarioEvents.length;
    final passRate =
        total == 0 ? "N/A" : "${(pass / total * 100).toStringAsFixed(1)}%";
    return _ResultsSummary(
      scenarioTotal: scenarioEvents.length,
      scenarioPass: pass,
      scenarioCheck: check,
      passRate: passRate,
      alertEvents: alertEvents,
      scenarioEvents: scenarioEvents,
    );
  }
}

class _ResultsSummary {
  const _ResultsSummary({
    required this.scenarioTotal,
    required this.scenarioPass,
    required this.scenarioCheck,
    required this.passRate,
    required this.alertEvents,
    required this.scenarioEvents,
  });

  final int scenarioTotal;
  final int scenarioPass;
  final int scenarioCheck;
  final String passRate;
  final int alertEvents;
  final List<CrashEvent> scenarioEvents;
}

class _ScenarioBreakdown {
  _ScenarioBreakdown({
    required this.id,
    required this.name,
  });

  final String id;
  final String name;
  int pass = 0;
  int check = 0;
}

class _ActionItem {
  const _ActionItem({
    required this.label,
    required this.icon,
    required this.onTap,
    this.highlight = false,
    this.destructive = false,
  });

  final String label;
  final IconData icon;
  final VoidCallback? onTap;
  final bool highlight;
  final bool destructive;
}

class _ChartFrame extends StatelessWidget {
  const _ChartFrame({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.black12),
      ),
      child: child,
    );
  }
}

class _SimpleBarChart extends StatelessWidget {
  const _SimpleBarChart({
    required this.pass,
    required this.check,
    required this.passColor,
    required this.checkColor,
  });

  final int pass;
  final int check;
  final Color passColor;
  final Color checkColor;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return LayoutBuilder(
      builder: (context, constraints) {
        final maxValue = max(pass, check).clamp(1, 1000000);

        double fractionFor(int value) {
          if (value == 0) return 0.06;
          final fraction = value / maxValue;
          return max(fraction, 0.08);
        }

        return Row(
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 8),
                child: _BarColumn(
                  label: "PASS",
                  value: pass,
                  fraction: fractionFor(pass),
                  color: passColor,
                  faded: pass == 0,
                  theme: theme,
                ),
              ),
            ),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 8),
                child: _BarColumn(
                  label: "CHECK",
                  value: check,
                  fraction: fractionFor(check),
                  color: checkColor,
                  faded: check == 0,
                  theme: theme,
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}

class _BarColumn extends StatelessWidget {
  const _BarColumn({
    required this.label,
    required this.value,
    required this.fraction,
    required this.color,
    required this.faded,
    required this.theme,
  });

  final String label;
  final int value;
  final double fraction;
  final Color color;
  final bool faded;
  final ThemeData theme;

  @override
  Widget build(BuildContext context) {
    final barColor = faded ? color.withOpacity(0.35) : color;
    return Column(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        Text(
          value.toString(),
          style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w700,
            color: Colors.black87,
          ),
        ),
        const SizedBox(height: 4),
        Expanded(
          child: Align(
            alignment: Alignment.bottomCenter,
            child: FractionallySizedBox(
              heightFactor: fraction,
              widthFactor: 1,
              child: Container(
                decoration: BoxDecoration(
                  color: barColor,
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
            ),
          ),
        ),
        const SizedBox(height: 6),
        Text(
          label,
          style: theme.textTheme.bodySmall?.copyWith(
            fontWeight: FontWeight.w600,
            color: Colors.black54,
            letterSpacing: 0.8,
          ),
        ),
      ],
    );
  }
}
