class CrashEvent {
  CrashEvent({
    required this.timestamp,
    required this.source,
    required this.outcome,
    required this.notes,
    this.eventType = "alert",
    this.reason,
    this.severity,
    this.latitude,
    this.longitude,
    this.scenarioId,
    this.expectedTrigger,
    this.triggered,
  });

  final DateTime timestamp;
  final String source;
  final String outcome;
  final String notes;
  final String eventType;
  final String? reason;
  final double? severity;
  final double? latitude;
  final double? longitude;
  final String? scenarioId;
  final bool? expectedTrigger;
  final bool? triggered;

  Map<String, dynamic> toJson() {
    return {
      "timestamp": timestamp.toIso8601String(),
      "source": source,
      "outcome": outcome,
      "notes": notes,
      "eventType": eventType,
      "reason": reason,
      "severity": severity,
      "latitude": latitude,
      "longitude": longitude,
      "scenarioId": scenarioId,
      "expectedTrigger": expectedTrigger,
      "triggered": triggered,
    };
  }

  static CrashEvent fromJson(Map<String, dynamic> json) {
    return CrashEvent(
      timestamp: DateTime.parse(json["timestamp"] as String),
      source: json["source"] as String? ?? "Unknown",
      outcome: json["outcome"] as String? ?? "Unknown",
      notes: json["notes"] as String? ?? "",
      eventType: json["eventType"] as String? ?? "alert",
      reason: json["reason"] as String?,
      severity: (json["severity"] as num?)?.toDouble(),
      latitude: (json["latitude"] as num?)?.toDouble(),
      longitude: (json["longitude"] as num?)?.toDouble(),
      scenarioId: json["scenarioId"] as String?,
      expectedTrigger: json["expectedTrigger"] as bool?,
      triggered: json["triggered"] as bool?,
    );
  }
}
