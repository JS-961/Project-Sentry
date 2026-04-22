package com.safedrive.ai.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safedrive.ai.data.local.CrashAlertEntity
import com.safedrive.ai.data.local.RiskEventEntity
import com.safedrive.ai.data.local.TripEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistoryScreen(
    trips: List<TripEntity>,
    recentEvents: List<RiskEventEntity>,
    recentAlerts: List<CrashAlertEntity>,
    onBack: () -> Unit,
) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault())
    }
    val totalEvents = trips.sumOf { it.totalEvents }
    val maxRisk = trips.maxOfOrNull { it.maxRiskScore } ?: 0
    val avgRisk = trips
        .filter { it.endedAtEpochMs != null }
        .map { it.avgRiskScore }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?: 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip History") },
                navigationIcon = {
                    OutlinedButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) {
                        Text("Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                HistorySummaryCard(
                    tripCount = trips.size,
                    totalEvents = totalEvents,
                    maxRisk = maxRisk,
                    avgRisk = avgRisk,
                )
            }

            item {
                SectionCard(title = "Recent Trips", subtitle = "Stored locally through Room.") {
                    if (trips.isEmpty()) {
                        EmptyState("No trips recorded yet. Start and stop driving mode once to populate this view.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            trips.take(8).forEach { trip ->
                                TripRow(trip, formatter)
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Recent Risk Events", subtitle = "Latest harsh driving events from the detector.") {
                    if (recentEvents.isEmpty()) {
                        EmptyState("No risk events recorded yet.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recentEvents.take(8).forEach { event ->
                                EventRow(event, formatter)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Crash Alerts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            if (recentAlerts.isEmpty()) {
                item {
                    EmptyState("No crash alert outcomes stored yet.")
                }
            } else {
                items(recentAlerts.take(8)) { alert ->
                    AlertRow(alert, formatter)
                }
            }
        }
    }
}

@Composable
private fun HistorySummaryCard(
    tripCount: Int,
    totalEvents: Int,
    maxRisk: Int,
    avgRisk: Double,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Trip Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryTile("Trips", tripCount.toString())
                SummaryTile("Events", totalEvents.toString())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryTile("Max risk", "$maxRisk")
                SummaryTile("Avg risk", avgRisk.toInt().toString())
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun RowScope.SummaryTile(label: String, value: String) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
    ) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TripRow(
    trip: TripEntity,
    formatter: DateTimeFormatter,
) {
    val ended = trip.endedAtEpochMs != null
    val duration = if (trip.endedAtEpochMs != null) {
        formatDuration(trip.endedAtEpochMs - trip.startedAtEpochMs)
    } else {
        "Active"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(formatEpoch(trip.startedAtEpochMs, formatter), fontWeight = FontWeight.Bold)
                Text(duration, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (ended) "Risk ${trip.maxRiskScore}" else "Live", color = if (ended) Color(0xFF38BDF8) else Color(0xFF22C55E), fontWeight = FontWeight.Bold)
                Text("${trip.totalEvents} events", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EventRow(
    event: RiskEventEntity,
    formatter: DateTimeFormatter,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(event.eventType.replace("_", " "), fontWeight = FontWeight.Bold)
                Text(formatEpoch(event.createdAtEpochMs, formatter), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${event.speedMps.times(3.6f).toInt()} km/h", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AlertRow(
    alert: CrashAlertEntity,
    formatter: DateTimeFormatter,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(alert.outcome, fontWeight = FontWeight.ExtraBold)
            Text(formatEpoch(alert.createdAtEpochMs, formatter), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("SMS: ${alert.smsStatus} | Call: ${alert.callStatus} | TTS: ${alert.ttsStatus}")
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    ) {
        Text(
            message,
            modifier = Modifier.padding(14.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatEpoch(
    epochMs: Long,
    formatter: DateTimeFormatter,
): String {
    return formatter.format(Instant.ofEpochMilli(epochMs))
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}
