package com.safedrive.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.safedrive.ai.data.DrivingStateStore
import com.safedrive.ai.data.PermissionStatus
import com.safedrive.ai.service.DrivingModeService
import com.safedrive.ai.ui.HomeScreen
import com.safedrive.ai.ui.SettingsScreen
import com.safedrive.ai.ui.theme.SafeDriveTheme

class MainActivity : ComponentActivity() {
    private var pendingForegroundAction: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as SafeDriveApplication
        val tripDao = app.database.tripDao()
        val riskEventDao = app.database.riskEventDao()
        val crashAlertDao = app.database.crashAlertDao()

        setContent {
            SafeDriveTheme {
                val navController = rememberNavController()
                var permissionExplanation by remember { mutableStateOf<PermissionExplanationRequest?>(null) }
                val drivingState by DrivingStateStore.state.collectAsStateWithLifecycle()
                val settings by app.settingsRepository.settings.collectAsStateWithLifecycle()
                val recentTrips by tripDao.observeRecentTrips(20).collectAsStateWithLifecycle(emptyList())
                val recentEvents by riskEventDao.observeRecentEvents(20).collectAsStateWithLifecycle(emptyList())
                val recentAlerts by crashAlertDao.observeRecentAlerts(20).collectAsStateWithLifecycle(emptyList())
                val tripCount by tripDao.observeTripCount().collectAsStateWithLifecycle(0)
                val eventCount by riskEventDao.observeEventCount().collectAsStateWithLifecycle(0)
                val alertCount by crashAlertDao.observeAlertCount().collectAsStateWithLifecycle(0)
                val permissionStatus = currentPermissionStatus()

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                ) {
                    val action = pendingForegroundAction ?: return@rememberLauncherForActivityResult
                    pendingForegroundAction = null
                    if (hasLocationPermission()) {
                        publishOptionalPermissionWarningIfNeeded()
                        startForegroundServiceAction(action)
                    } else {
                        DrivingStateStore.setLatestEvent("Location permission is required to start monitoring")
                    }
                }

                permissionExplanation?.let { request ->
                    PermissionExplanationDialog(
                        request = request,
                        onProceed = {
                            permissionExplanation = null
                            permissionLauncher.launch(request.permissions.toTypedArray())
                        },
                        onDismiss = {
                            pendingForegroundAction = null
                            permissionExplanation = null
                            DrivingStateStore.setLatestEvent("Permission request postponed")
                        },
                    )
                }

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            state = drivingState,
                            settings = settings,
                            permissionStatus = permissionStatus,
                            recentTrips = recentTrips,
                            recentEvents = recentEvents,
                            recentAlerts = recentAlerts,
                            tripCount = tripCount,
                            eventCount = eventCount,
                            alertCount = alertCount,
                            onStartDriving = {
                                requestPermissionsThenStart(
                                    action = DrivingModeService.ACTION_START_DRIVING,
                                    showExplanation = { permissionExplanation = it },
                                )
                            },
                            onStopDriving = {
                                startServiceAction(DrivingModeService.ACTION_STOP_DRIVING)
                            },
                            onSimulateCrash = {
                                requestPermissionsThenStart(
                                    action = DrivingModeService.ACTION_SIMULATE_CRASH,
                                    showExplanation = { permissionExplanation = it },
                                )
                            },
                            onOpenSettings = {
                                navController.navigate("settings")
                            },
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            settings = settings,
                            permissionStatus = permissionStatus,
                            onBack = { navController.popBackStack() },
                            onSave = { updated -> app.settingsRepository.save(updated) },
                            onTestCrashFlow = {
                                requestPermissionsThenStart(
                                    action = DrivingModeService.ACTION_SIMULATE_CRASH,
                                    showExplanation = { permissionExplanation = it },
                                )
                            },
                        )
                    }

                }
            }
        }
    }

    private fun startServiceAction(action: String) {
        val intent = Intent(this, DrivingModeService::class.java).setAction(action)
        startService(intent)
    }

    private fun startForegroundServiceAction(action: String) {
        try {
            val intent = Intent(this, DrivingModeService::class.java).setAction(action)
            ContextCompat.startForegroundService(this, intent)
        } catch (_: SecurityException) {
            pendingForegroundAction = null
            DrivingStateStore.setLatestEvent("Unable to start monitoring until location permission is granted")
        } catch (_: IllegalStateException) {
            pendingForegroundAction = null
            DrivingStateStore.setLatestEvent("Unable to start driving mode right now")
        }
    }

    private fun requestPermissionsThenStart(
        action: String,
        showExplanation: (PermissionExplanationRequest) -> Unit,
    ) {
        val missing = missingStartPermissions()
        if (missing.isEmpty()) {
            pendingForegroundAction = null
            publishOptionalPermissionWarningIfNeeded()
            startForegroundServiceAction(action)
        } else {
            pendingForegroundAction = action
            showExplanation(
                PermissionExplanationRequest(
                    permissions = missing,
                    items = permissionExplanationItems(missing),
                ),
            )
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasLocationPermission(): Boolean {
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun currentPermissionStatus(): PermissionStatus {
        return PermissionStatus(
            location = hasLocationPermission(),
            notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                hasPermission(Manifest.permission.POST_NOTIFICATIONS),
            sms = hasPermission(Manifest.permission.SEND_SMS),
            phone = hasPermission(Manifest.permission.CALL_PHONE),
        )
    }

    private fun missingStartPermissions(): List<String> {
        return buildList {
            if (!hasLocationPermission()) {
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (!hasPermission(Manifest.permission.SEND_SMS)) {
                add(Manifest.permission.SEND_SMS)
            }
            if (!hasPermission(Manifest.permission.CALL_PHONE)) {
                add(Manifest.permission.CALL_PHONE)
            }
        }
    }

    private fun permissionExplanationItems(permissions: List<String>): List<PermissionExplanationItem> {
        return buildList {
            if (
                permissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION) ||
                permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION)
            ) {
                add(
                    PermissionExplanationItem(
                        title = "Location",
                        body = "Used during driving mode to track speed, route context, and the location attached to an emergency alert.",
                    ),
                )
            }
            if (permissions.contains(Manifest.permission.SEND_SMS)) {
                add(
                    PermissionExplanationItem(
                        title = "SMS",
                        body = "Used only for crash escalation so Sentry can send your saved contacts the alert message and location link.",
                    ),
                )
            }
            if (permissions.contains(Manifest.permission.CALL_PHONE)) {
                add(
                    PermissionExplanationItem(
                        title = "Phone call",
                        body = "Used only after the crash countdown to open the emergency call flow for your saved number.",
                    ),
                )
            }
            if (permissions.contains(Manifest.permission.POST_NOTIFICATIONS)) {
                add(
                    PermissionExplanationItem(
                        title = "Notifications",
                        body = "Used to keep driving mode visible and show crash countdown alerts when Sentry is not in the foreground.",
                    ),
                )
            }
        }
    }

    private fun publishOptionalPermissionWarningIfNeeded() {
        val missing = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
            ) {
                add("notifications")
            }
            if (!hasPermission(Manifest.permission.SEND_SMS)) {
                add("SMS")
            }
            if (!hasPermission(Manifest.permission.CALL_PHONE)) {
                add("phone")
            }
        }
        if (missing.isNotEmpty()) {
            DrivingStateStore.setLatestEvent("Limited crash flow: missing ${missing.joinToString()}")
        }
    }
}

@Composable
private fun PermissionExplanationDialog(
    request: PermissionExplanationRequest,
    onProceed: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Before Permissions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Sentry monitors phone motion and GPS speed while driving mode is active. Android will ask for the permissions below after this step.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                request.items.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(item.title, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text(item.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "You can continue now or come back later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = onProceed) {
                Text("Proceed to Permissions")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        },
    )
}

private data class PermissionExplanationRequest(
    val permissions: List<String>,
    val items: List<PermissionExplanationItem>,
)

private data class PermissionExplanationItem(
    val title: String,
    val body: String,
)
