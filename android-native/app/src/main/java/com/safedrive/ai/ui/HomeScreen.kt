package com.safedrive.ai.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safedrive.ai.R
import com.safedrive.ai.data.AppSettings
import com.safedrive.ai.data.DrivingUiState
import com.safedrive.ai.data.PermissionStatus
import com.safedrive.ai.data.local.CrashAlertEntity
import com.safedrive.ai.data.local.RiskEventEntity
import com.safedrive.ai.data.local.TripEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: DrivingUiState,
    settings: AppSettings,
    permissionStatus: PermissionStatus,
    recentTrips: List<TripEntity>,
    recentEvents: List<RiskEventEntity>,
    recentAlerts: List<CrashAlertEntity>,
    tripCount: Int,
    eventCount: Int,
    alertCount: Int,
    onStartDriving: () -> Unit,
    onStopDriving: () -> Unit,
    onSimulateCrash: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(SentryTab.Drive) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.isDriving, state.crashCountdownActive) {
        while (state.isDriving || state.crashCountdownActive) {
            now = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.sentry_logo_white),
                            contentDescription = "Sentry logo",
                            modifier = Modifier.size(34.dp),
                        )
                        Column {
                            Text("Sentry", fontWeight = FontWeight.ExtraBold)
                            Text(
                                selectedTab.subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    OutlinedButton(onClick = onOpenSettings, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Settings")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp)),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                tonalElevation = 8.dp,
            ) {
                SentryTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                painter = painterResource(tab.iconRes),
                                contentDescription = tab.label,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        label = {
                            Text(tab.label, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        when (selectedTab) {
            SentryTab.Drive -> DriveTab(
                padding = padding,
                state = state,
                settings = settings,
                permissionStatus = permissionStatus,
                recentEvents = recentEvents,
                now = now,
                onStartDriving = onStartDriving,
                onStopDriving = onStopDriving,
                onSimulateCrash = onSimulateCrash,
                onOpenSettings = onOpenSettings,
            )

            SentryTab.History -> HistoryTab(
                padding = padding,
                trips = recentTrips,
                recentEvents = recentEvents,
                recentAlerts = recentAlerts,
            )

            SentryTab.Status -> StatusTab(
                padding = padding,
                state = state,
                settings = settings,
                permissionStatus = permissionStatus,
                tripCount = tripCount,
                eventCount = eventCount,
                alertCount = alertCount,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

@Composable
private fun DriveTab(
    padding: PaddingValues,
    state: DrivingUiState,
    settings: AppSettings,
    permissionStatus: PermissionStatus,
    recentEvents: List<RiskEventEntity>,
    now: Long,
    onStartDriving: () -> Unit,
    onStopDriving: () -> Unit,
    onSimulateCrash: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val contactsReady = settings.emergencyContacts.isNotEmpty()
    val callReady = settings.demoCallNumber.isNotBlank()
    val status = driveStatusLabel(state, permissionStatus, contactsReady, callReady)
    val severity = RiskSeverity.from(state.riskScore)
    val tripDuration = state.sessionStartedAtEpochMs?.let { formatDuration(now - it) } ?: "00:00"
    val gpsStatus = healthLabel(
        isActive = state.isDriving,
        lastEpochMs = state.lastLocationEpochMs,
        now = now,
        readyLabel = "GPS ready",
        activeLabel = "GPS locked",
        waitingLabel = "Waiting for GPS",
    )
    val sensorStatus = healthLabel(
        isActive = state.isDriving,
        lastEpochMs = state.lastSensorEpochMs,
        now = now,
        readyLabel = "Sensors ready",
        activeLabel = "Sensors streaming",
        waitingLabel = "Waiting for sensors",
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            DriveStatusCard(
                status = status,
                statusColor = status.color,
                tripDuration = tripDuration,
                gpsStatus = gpsStatus,
                sensorStatus = sensorStatus,
                state = state,
                onStartDriving = onStartDriving,
                onStopDriving = onStopDriving,
                onOpenSettings = onOpenSettings,
            )
        }

        item {
            RiskCard(
                riskScore = state.riskScore,
                severity = severity,
                mlRiskScore = state.mlRiskScore,
                mlRiskLabel = state.mlRiskLabel,
                mlRiskConfidence = state.mlRiskConfidence,
                mlModelSource = state.mlModelSource,
                speedKmh = state.latestSpeedKmh,
                latestEvent = state.latestEventLabel,
                recentEvents = recentEvents,
            )
        }

        item {
            EventCounterCard(state)
        }

        item {
            LastAlertCard(state.lastAlertOutcome)
        }

        item {
            CrashDemoCard(
                contactsReady = contactsReady,
                callReady = callReady,
                permissionStatus = permissionStatus,
                onSimulateCrash = onSimulateCrash,
            )
        }
    }
}

@Composable
private fun HistoryTab(
    padding: PaddingValues,
    trips: List<TripEntity>,
    recentEvents: List<RiskEventEntity>,
    recentAlerts: List<CrashAlertEntity>,
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
            SentryCard(title = "Recent Trips", subtitle = "Stored locally through Room.") {
                if (trips.isEmpty()) {
                    EmptyState("No trips recorded yet. Start and stop driving mode once to populate this view.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        trips.take(8).forEach { trip -> TripRow(trip, formatter) }
                    }
                }
            }
        }

        item {
            SentryCard(title = "Recent Risk Events", subtitle = "Latest harsh driving events from the detector.") {
                if (recentEvents.isEmpty()) {
                    EmptyState("No risk events recorded yet.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentEvents.take(8).forEach { event -> EventRow(event, formatter) }
                    }
                }
            }
        }

        item {
            Text("Crash Alerts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }

        if (recentAlerts.isEmpty()) {
            item { EmptyState("No crash alert outcomes stored yet.") }
        } else {
            items(recentAlerts.take(8)) { alert -> AlertRow(alert, formatter) }
        }
    }
}

@Composable
private fun StatusTab(
    padding: PaddingValues,
    state: DrivingUiState,
    settings: AppSettings,
    permissionStatus: PermissionStatus,
    tripCount: Int,
    eventCount: Int,
    alertCount: Int,
    onOpenSettings: () -> Unit,
) {
    val contactsReady = settings.emergencyContacts.isNotEmpty()
    val callReady = settings.demoCallNumber.isNotBlank()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SentryCard(title = "Preflight Checklist", subtitle = "Everything to confirm before Sentry starts.") {
                ChecklistRow("Location permission", permissionStatus.location, "Required for foreground monitoring")
                ChecklistRow("Notifications", permissionStatus.notifications, "Required for crash alerts outside the app")
                ChecklistRow("SMS permission + contact", permissionStatus.sms && contactsReady, "Needed for emergency message")
                ChecklistRow("Phone permission + number", permissionStatus.phone && callReady, "Needed for call escalation")
                Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Fix setup in Settings")
                }
            }
        }

        item {
            SentryCard(title = "Runtime Health", subtitle = "Quick QA view before a presentation.") {
                StatusRow("Driving mode", if (state.isDriving) "Active" else "Idle", state.isDriving)
                StatusRow("Crash countdown", if (state.crashCountdownActive) "Active" else "Inactive", !state.crashCountdownActive)
                StatusRow("Latest event", state.latestEventLabel, true)
                StatusRow("ML Advisory", mlModelLabel(state.mlModelSource), state.mlModelSource.startsWith("trained-json"))
                StatusRow("ML result", "${displayMlLabel(state.mlRiskLabel)} ${state.mlRiskScore}/100", true)
                StatusRow("ML confidence", "${(state.mlRiskConfidence * 100).toInt()}%", state.mlRiskConfidence >= 0.45f)
                StatusRow("Last alert", state.lastAlertOutcome ?: "None yet", true)
            }
        }

        item {
            SentryCard(title = "Local Data", subtitle = "Room-backed proof that the native app records activity.") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DataTile("Trips", tripCount.toString())
                    DataTile("Events", eventCount.toString())
                    DataTile("Alerts", alertCount.toString())
                }
            }
        }

        item {
            SentryCard(title = "Test Checklist", subtitle = "Fast path before a live walkthrough.") {
                ScriptStep("1", "Open Status and show permissions/config readiness.")
                ScriptStep("2", "Start Drive and point out GPS/sensor status.")
                ScriptStep("3", "Use Simulate Crash to show countdown and escalation path.")
                ScriptStep("4", "Open History to prove trips, events, and alerts are persisted.")
            }
        }
    }
}

@Composable
private fun DriveStatusCard(
    status: DriveStatus,
    statusColor: Color,
    tripDuration: String,
    gpsStatus: String,
    sensorStatus: String,
    state: DrivingUiState,
    onStartDriving: () -> Unit,
    onStopDriving: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(28.dp),
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF0B1220),
                            Color(0xFF102A35),
                            Color(0xFF13251D),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(28.dp),
                )
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Drive Status",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.70f),
                        )
                        Text(
                            status.label,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            status.caption,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.72f),
                        )
                    }
                    StatusPill(status.label, statusColor)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile("Trip", tripDuration)
                    MetricTile("Speed", "${state.latestSpeedKmh.toInt()} km/h")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile("Location", gpsStatus)
                    MetricTile("Sensors", sensorStatus)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onStartDriving,
                        enabled = !state.isDriving,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (state.isDriving) "Monitoring" else "Start Drive")
                    }
                    OutlinedButton(
                        onClick = onStopDriving,
                        enabled = state.isDriving,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    ) {
                        Text("Stop")
                    }
                }

                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) {
                    Text("Review setup and contacts")
                }
            }
        }
    }
}

@Composable
private fun RiskCard(
    riskScore: Int,
    severity: RiskSeverity,
    mlRiskScore: Int,
    mlRiskLabel: String,
    mlRiskConfidence: Float,
    mlModelSource: String,
    speedKmh: Float,
    latestEvent: String,
    recentEvents: List<RiskEventEntity>,
) {
    val mlSeverity = RiskSeverity.from(mlRiskScore)
    SentryCard(title = "Live Risk", subtitle = "Rule score drives crash flow; ML advisory stays separate.") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "$riskScore/100",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = severity.color,
                )
                Text(
                    severity.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${speedKmh.toInt()} km/h", style = MaterialTheme.typography.titleLarge)
                Text("Current speed", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        RiskProgressBar(riskScore, severity.color)
        Spacer(modifier = Modifier.height(14.dp))

        Text("ML advisory", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "$mlRiskScore/100",
                    style = MaterialTheme.typography.titleLarge,
                    color = mlSeverity.color,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(displayMlLabel(mlRiskLabel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${(mlRiskConfidence * 100).toInt()}%", fontWeight = FontWeight.Bold)
                Text(mlModelLabel(mlModelSource), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        RiskProgressBar(mlRiskScore, mlSeverity.color)
        Spacer(modifier = Modifier.height(14.dp))

        Text("Latest event", style = MaterialTheme.typography.labelLarge)
        Text(latestEvent, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(12.dp))
        Text("Recent event chips", style = MaterialTheme.typography.labelLarge)
        if (recentEvents.isEmpty()) {
            Text("No risky events logged in this install yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recentEvents.take(3).forEach { event ->
                    EventChip(
                        event.eventType.replace("_", " "),
                        "${event.value.toInt()} | ${event.speedMps.times(3.6f).toInt()} km/h",
                    )
                }
            }
        }
    }
}

@Composable
private fun EventCounterCard(state: DrivingUiState) {
    val counters = listOf(
        "Harsh braking" to state.counters.harshBraking,
        "Harsh acceleration" to state.counters.harshAcceleration,
        "Sharp cornering" to state.counters.sharpCornering,
        "Speeding" to state.counters.speeding,
    )

    SentryCard(title = "Event Counters", subtitle = "Live trip totals reset when driving mode stops.") {
        counters.forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label)
                Text(value.toString(), fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun LastAlertCard(lastAlertOutcome: String?) {
    SentryCard(title = "Last Alert Result", subtitle = "Visible outcome helps the safety flow stay clear.") {
        Text(
            lastAlertOutcome ?: "No crash alerts resolved yet.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CrashDemoCard(
    contactsReady: Boolean,
    callReady: Boolean,
    permissionStatus: PermissionStatus,
    onSimulateCrash: () -> Unit,
) {
    val ready = contactsReady && callReady && permissionStatus.crashFlowReady
    SentryCard(title = "Crash Flow Test", subtitle = "Manual trigger for testing; real detector remains sensor-driven.") {
        ChecklistRow("Emergency flow ready", ready, if (ready) "SMS, call, TTS, and alert notification are configured" else "Open Settings before the final test")
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onSimulateCrash,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
        ) {
            Text("Simulate Crash Countdown")
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
    SentryCard(title = "Trip Summary", subtitle = "Quick snapshot of recorded native sessions.") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DataTile("Trips", tripCount.toString())
            DataTile("Events", totalEvents.toString())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DataTile("Max risk", maxRisk.toString())
            DataTile("Avg risk", avgRisk.toInt().toString())
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
                Text(
                    if (ended) "Risk ${trip.maxRiskScore}" else "Live",
                    color = if (ended) Color(0xFF38BDF8) else Color(0xFF22C55E),
                    fontWeight = FontWeight.Bold,
                )
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
private fun StatusRow(
    label: String,
    value: String,
    healthy: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontWeight = FontWeight.Bold,
            color = if (healthy) Color(0xFF22C55E) else Color(0xFFF97316),
        )
    }
}

@Composable
private fun ScriptStep(index: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Text(index, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.Bold)
        }
        Text(text, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SentryCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun ChecklistRow(label: String, ready: Boolean, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (ready) Color(0xFF22C55E) else Color(0xFFEF4444)),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (ready) "Y" else "!", color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RowScope.MetricTile(label: String, value: String) {
    Surface(
        modifier = Modifier.weight(1f),
        color = Color.White.copy(alpha = 0.09f),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.62f))
            Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RowScope.DataTile(label: String, value: String) {
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
private fun StatusPill(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.18f), shape = RoundedCornerShape(999.dp)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun RiskProgressBar(score: Int, color: Color) {
    val progress = (score.coerceIn(0, 100) / 100f).coerceAtLeast(0.02f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .clip(RoundedCornerShape(999.dp))
                .background(color),
        )
    }
}

@Composable
private fun EventChip(label: String, meta: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontWeight = FontWeight.Bold)
            Text(meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private enum class SentryTab(
    val label: String,
    val subtitle: String,
    @DrawableRes val iconRes: Int,
) {
    Drive("Drive", "Live monitoring", R.drawable.ic_sentry_drive),
    History("History", "Trips and alerts", R.drawable.ic_sentry_history),
    Status("Status", "Setup checks", R.drawable.ic_sentry_status),
}

private data class DriveStatus(
    val label: String,
    val caption: String,
    val color: Color,
)

private fun driveStatusLabel(
    state: DrivingUiState,
    permissionStatus: PermissionStatus,
    contactsReady: Boolean,
    callReady: Boolean,
): DriveStatus {
    return when {
        state.crashCountdownActive -> DriveStatus("Crash suspected", "Countdown is active. Awaiting driver response.", Color(0xFFF97316))
        state.isDriving -> DriveStatus("Monitoring", "Sensors and GPS are watching the current trip.", Color(0xFF22C55E))
        !permissionStatus.location -> DriveStatus("Setup needed", "Location permission is required before monitoring.", Color(0xFFEF4444))
        !contactsReady || !callReady || !permissionStatus.crashFlowReady -> DriveStatus("Almost ready", "Finish contacts and crash permissions before the final test.", Color(0xFFFBBF24))
        else -> DriveStatus("Ready", "Preflight checks look good. Start driving mode when ready.", Color(0xFF38BDF8))
    }
}

private data class RiskSeverity(
    val label: String,
    val color: Color,
) {
    companion object {
        fun from(score: Int): RiskSeverity {
            return when {
                score >= 75 -> RiskSeverity("Critical", Color(0xFFEF4444))
                score >= 50 -> RiskSeverity("High", Color(0xFFF97316))
                score >= 25 -> RiskSeverity("Guarded", Color(0xFFFBBF24))
                else -> RiskSeverity("Low", Color(0xFF22C55E))
            }
        }
    }
}

private fun healthLabel(
    isActive: Boolean,
    lastEpochMs: Long?,
    now: Long,
    readyLabel: String,
    activeLabel: String,
    waitingLabel: String,
): String {
    if (!isActive) return readyLabel
    val fresh = lastEpochMs != null && now - lastEpochMs <= 8_000L
    return if (fresh) activeLabel else waitingLabel
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

private fun mlModelLabel(source: String): String {
    return when {
        source.contains("demo-assist") -> "Assist"
        source == "trained-json" -> "Trained Model"
        else -> "Baseline"
    }
}

private fun displayMlLabel(label: String): String {
    return label.replace("_", " ")
}
