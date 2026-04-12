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
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.safedrive.ai.data.DrivingStateStore
import com.safedrive.ai.service.DrivingModeService
import com.safedrive.ai.ui.HomeScreen
import com.safedrive.ai.ui.SettingsScreen
import com.safedrive.ai.ui.theme.SafeDriveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as SafeDriveApplication

        setContent {
            SafeDriveTheme {
                val navController = rememberNavController()
                val drivingState by DrivingStateStore.state.collectAsStateWithLifecycle()
                val settings by app.settingsRepository.settings.collectAsStateWithLifecycle()

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                ) {
                    // No-op: buttons can be tapped again after grant.
                }

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            state = drivingState,
                            onStartDriving = {
                                requestMissingPermissions(permissionLauncher)
                                startForegroundServiceAction(DrivingModeService.ACTION_START_DRIVING)
                            },
                            onStopDriving = {
                                startServiceAction(DrivingModeService.ACTION_STOP_DRIVING)
                            },
                            onSimulateCrash = {
                                requestMissingPermissions(permissionLauncher)
                                startForegroundServiceAction(DrivingModeService.ACTION_SIMULATE_CRASH)
                            },
                            onOpenSettings = {
                                navController.navigate("settings")
                            },
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            settings = settings,
                            onBack = { navController.popBackStack() },
                            onSave = { updated -> app.settingsRepository.save(updated) },
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
        val intent = Intent(this, DrivingModeService::class.java).setAction(action)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun requestMissingPermissions(launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
        val required = requiredPermissions().filterNot(::hasPermission)
        if (required.isNotEmpty()) {
            launcher.launch(required.toTypedArray())
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requiredPermissions(): List<String> {
        val base = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            base += Manifest.permission.POST_NOTIFICATIONS
        }
        base += Manifest.permission.CALL_PHONE
        return base
    }
}
