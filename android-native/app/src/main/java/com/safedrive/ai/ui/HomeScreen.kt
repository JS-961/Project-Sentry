package com.safedrive.ai.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safedrive.ai.data.DrivingUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: DrivingUiState,
    onStartDriving: () -> Unit,
    onStopDriving: () -> Unit,
    onSimulateCrash: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val counterItems = listOf(
        "Harsh braking" to state.counters.harshBraking,
        "Harsh acceleration" to state.counters.harshAcceleration,
        "Sharp cornering" to state.counters.sharpCornering,
        "Speeding" to state.counters.speeding,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Project Sentry Native") },
                actions = {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("Settings")
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Driving Mode", style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (state.isDriving) "Active" else "Inactive",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onStartDriving) {
                                Text("Start")
                            }
                            OutlinedButton(onClick = onStopDriving) {
                                Text("Stop")
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Live Risk Score", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${state.riskScore}/100",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Speed: ${state.latestSpeedKmh.toInt()} km/h",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "Latest event: ${state.latestEventLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Event Counters", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            items(counterItems) { (label, value) ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                        Text(value.toString(), fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Crash Testing",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onSimulateCrash,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Simulate Crash")
                        }
                    }
                }
            }
        }
    }
}
