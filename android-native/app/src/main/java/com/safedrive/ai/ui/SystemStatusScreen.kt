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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safedrive.ai.data.AppSettings
import com.safedrive.ai.data.DrivingUiState
import com.safedrive.ai.data.PermissionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemStatusScreen(
    state: DrivingUiState,
    settings: AppSettings,
    permissionStatus: PermissionStatus,
    tripCount: Int,
    eventCount: Int,
    alertCount: Int,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Status") },
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
                StatusSummaryCard(state, permissionStatus)
            }
            item {
                ReadinessCard(settings, permissionStatus, onOpenSettings)
            }
            item {
                DataHealthCard(tripCount, eventCount, alertCount)
            }
            item {
                DemoScriptCard()
            }
        }
    }
}

@Composable
private fun StatusSummaryCard(
    state: DrivingUiState,
    permissionStatus: PermissionStatus,
) {
    StatusCard(title = "Runtime Health", subtitle = "Quick QA view before a presentation.") {
        StatusRow("Driving mode", if (state.isDriving) "Active" else "Idle", state.isDriving)
        StatusRow("Crash countdown", if (state.crashCountdownActive) "Active" else "Inactive", !state.crashCountdownActive)
        StatusRow("Location permission", if (permissionStatus.location) "Granted" else "Missing", permissionStatus.location)
        StatusRow("Crash permissions", if (permissionStatus.crashFlowReady) "Ready" else "Limited", permissionStatus.crashFlowReady)
        StatusRow("Latest event", state.latestEventLabel, true)
        StatusRow("Last alert", state.lastAlertOutcome ?: "None yet", true)
    }
}

@Composable
private fun ReadinessCard(
    settings: AppSettings,
    permissionStatus: PermissionStatus,
    onOpenSettings: () -> Unit,
) {
    val contactsReady = settings.emergencyContacts.isNotEmpty()
    val callReady = settings.demoCallNumber.isNotBlank()
    StatusCard(title = "Preflight Readiness", subtitle = "These are the items that usually break live demos.") {
        StatusRow("Emergency contacts", if (contactsReady) "${settings.emergencyContacts.size} saved" else "Missing", contactsReady)
        StatusRow("Call number", if (callReady) settings.demoCallNumber else "Missing", callReady)
        StatusRow("Notifications", if (permissionStatus.notifications) "Granted" else "Missing", permissionStatus.notifications)
        StatusRow("SMS", if (permissionStatus.sms) "Granted" else "Missing", permissionStatus.sms)
        StatusRow("Phone call", if (permissionStatus.phone) "Granted" else "Missing", permissionStatus.phone)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Fix setup in Settings")
        }
    }
}

@Composable
private fun DataHealthCard(
    tripCount: Int,
    eventCount: Int,
    alertCount: Int,
) {
    StatusCard(title = "Local Data", subtitle = "Room-backed proof that the native app records activity.") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DataTile("Trips", tripCount.toString())
            DataTile("Events", eventCount.toString())
            DataTile("Alerts", alertCount.toString())
        }
    }
}

@Composable
private fun DemoScriptCard() {
    StatusCard(title = "Capstone Demo Script", subtitle = "Fast path if graders ask what to watch.") {
        ScriptStep("1", "Open Status and show permissions/config readiness.")
        ScriptStep("2", "Start Drive from Home and point out GPS/sensor status.")
        ScriptStep("3", "Use Simulate Crash to show countdown and escalation path.")
        ScriptStep("4", "Open History to prove trips, events, and alerts are persisted.")
    }
}

@Composable
private fun StatusCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            content()
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
private fun RowScope.DataTile(label: String, value: String) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
