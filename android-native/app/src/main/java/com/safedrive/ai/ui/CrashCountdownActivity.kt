package com.safedrive.ai.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safedrive.ai.service.DrivingModeService
import com.safedrive.ai.ui.theme.SafeDriveTheme
import kotlinx.coroutines.delay

class CrashCountdownActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        val seconds = intent.getIntExtra(EXTRA_SECONDS, 12)

        setContent {
            SafeDriveTheme {
                Surface {
                    CrashCountdownScreen(
                        totalSeconds = seconds,
                        onCancel = {
                            sendAction(DrivingModeService.ACTION_CRASH_CANCELLED)
                            finish()
                        },
                        onImOk = {
                            sendAction(DrivingModeService.ACTION_CRASH_CANCELLED)
                            finish()
                        },
                        onCallNow = {
                            sendAction(DrivingModeService.ACTION_CRASH_CALL_NOW)
                            finish()
                        },
                        onTimeout = {
                            sendAction(DrivingModeService.ACTION_CRASH_TIMEOUT)
                            finish()
                        },
                    )
                }
            }
        }
    }

    private fun sendAction(action: String) {
        val intent = Intent(this, DrivingModeService::class.java).setAction(action)
        startService(intent)
    }

    companion object {
        const val EXTRA_SECONDS = "extra_seconds"
    }
}

@Composable
private fun CrashCountdownScreen(
    totalSeconds: Int,
    onCancel: () -> Unit,
    onImOk: () -> Unit,
    onCallNow: () -> Unit,
    onTimeout: () -> Unit,
) {
    var remainingSeconds by remember { mutableIntStateOf(totalSeconds) }
    var timeoutSent by remember { mutableStateOf(false) }

    LaunchedEffect(totalSeconds) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        }
        if (!timeoutSent) {
            timeoutSent = true
            onTimeout()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Potential Crash Detected", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Are you OK?",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            remainingSeconds.toString(),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "If no response, Project Sentry will send SMS, start call flow, and play the TTS intro.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(28.dp))
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onImOk, modifier = Modifier.fillMaxWidth()) {
            Text("I'm OK")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onCallNow, modifier = Modifier.fillMaxWidth()) {
            Text("Call Now")
        }
    }
}
