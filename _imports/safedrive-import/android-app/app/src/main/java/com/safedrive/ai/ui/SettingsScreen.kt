package com.safedrive.ai.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.safedrive.ai.data.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onSave: (AppSettings) -> Unit,
) {
    var contactsText by remember { mutableStateOf("") }
    var callNumber by remember { mutableStateOf("") }
    var ttsTemplate by remember { mutableStateOf("") }

    LaunchedEffect(settings) {
        contactsText = settings.emergencyContacts.joinToString(separator = "\n")
        callNumber = settings.demoCallNumber
        ttsTemplate = settings.ttsTemplate
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Emergency SMS Contacts",
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedTextField(
                    value = contactsText,
                    onValueChange = { contactsText = it },
                    placeholder = { Text("+10000000000") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("One number per line.") },
                    minLines = 3,
                )
            }

            item {
                Text("Demo Call Number", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = callNumber,
                    onValueChange = { callNumber = it },
                    placeholder = { Text("+10000000000") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Text("TTS Message Template", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = ttsTemplate,
                    onValueChange = { ttsTemplate = it },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Keep this short for a <=10s intro.") },
                    minLines = 2,
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val contacts = contactsText
                                .split("\n", ",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                            onSave(
                                AppSettings(
                                    emergencyContacts = contacts,
                                    demoCallNumber = callNumber,
                                    ttsTemplate = ttsTemplate,
                                ),
                            )
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Save")
                    }
                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Text("Back")
                    }
                }
            }
        }
    }
}
