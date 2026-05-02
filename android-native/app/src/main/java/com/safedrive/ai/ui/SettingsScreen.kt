package com.safedrive.ai.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safedrive.ai.data.AppSettings
import com.safedrive.ai.data.PermissionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    permissionStatus: PermissionStatus,
    onBack: () -> Unit,
    onSave: (AppSettings) -> Unit,
    onTestCrashFlow: () -> Unit,
) {
    var contactsText by remember { mutableStateOf("") }
    var callNumber by remember { mutableStateOf("") }
    var ttsTemplate by remember { mutableStateOf("") }

    LaunchedEffect(settings) {
        contactsText = settings.emergencyContacts.joinToString(separator = "\n")
        callNumber = settings.demoCallNumber
        ttsTemplate = settings.ttsTemplate
    }

    val contacts = parseNumbers(contactsText)
    val invalidContacts = contacts.filterNot(::looksLikePhoneNumber)
    val callNumberTrimmed = callNumber.trim()
    val callValid = callNumberTrimmed.isNotBlank() && looksLikePhoneNumber(callNumberTrimmed)
    val canSave = contacts.isNotEmpty() && invalidContacts.isEmpty() && callValid && ttsTemplate.trim().isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                SettingsCard(
                    title = "Crash Flow Readiness",
                    subtitle = "Permissions and contacts required before escalation.",
                ) {
                    PermissionRow("Location", permissionStatus.location)
                    PermissionRow("Notifications", permissionStatus.notifications)
                    PermissionRow("SMS", permissionStatus.sms)
                    PermissionRow("Phone call", permissionStatus.phone)
                }
            }

            item {
                SettingsCard(
                    title = "Emergency Contacts",
                    subtitle = "One phone number per line. Use test-safe numbers.",
                ) {
                    OutlinedTextField(
                        value = contactsText,
                        onValueChange = { contactsText = it },
                        placeholder = { Text("+96170123456") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text(
                                when {
                                    contacts.isEmpty() -> "At least one SMS contact is required for escalation."
                                    invalidContacts.isNotEmpty() -> "Invalid: ${invalidContacts.joinToString()}"
                                    else -> "${contacts.size} contact(s) ready."
                                },
                            )
                        },
                        isError = contacts.isEmpty() || invalidContacts.isNotEmpty(),
                        minLines = 3,
                    )
                }
            }

            item {
                SettingsCard(
                    title = "Call Escalation",
                    subtitle = "The app opens this number when the countdown expires or Call Now is pressed.",
                ) {
                    OutlinedTextField(
                        value = callNumber,
                        onValueChange = { callNumber = it },
                        placeholder = { Text("+96170123456") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text(if (callValid) "Call number ready." else "Enter a valid phone number.")
                        },
                        isError = !callValid,
                    )
                }
            }

            item {
                SettingsCard(
                    title = "TTS Message",
                    subtitle = "Short voice intro played during escalation.",
                ) {
                    OutlinedTextField(
                        value = ttsTemplate,
                        onValueChange = { ttsTemplate = it },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("${ttsTemplate.trim().length}/180 characters recommended max.") },
                        isError = ttsTemplate.trim().isBlank(),
                        minLines = 2,
                    )
                }
            }

            item {
                SettingsCard(
                    title = "Flow Tools",
                    subtitle = "Use this after saving to verify countdown, notification, SMS, call, and TTS together.",
                ) {
                    Button(
                        onClick = {
                            onSave(
                                AppSettings(
                                    emergencyContacts = contacts,
                                    demoCallNumber = callNumberTrimmed,
                                    ttsTemplate = ttsTemplate,
                                ),
                            )
                        },
                        enabled = canSave,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Save Configuration")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            onSave(
                                AppSettings(
                                    emergencyContacts = contacts,
                                    demoCallNumber = callNumberTrimmed,
                                    ttsTemplate = ttsTemplate,
                                ),
                            )
                            onTestCrashFlow()
                        },
                        enabled = canSave,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Test Full Crash Flow")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Text("Back to app")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
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
private fun PermissionRow(
    label: String,
    ready: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = if (ready) Color(0xFF22C55E).copy(alpha = 0.16f) else Color(0xFFEF4444).copy(alpha = 0.16f),
        ) {
            Text(
                if (ready) "Granted" else "Missing",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                color = if (ready) Color(0xFF22C55E) else Color(0xFFEF4444),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun parseNumbers(raw: String): List<String> {
    return raw
        .split("\n", ",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

private fun looksLikePhoneNumber(number: String): Boolean {
    return Regex("^\\+?[0-9]{7,15}$").matches(number.trim())
}
