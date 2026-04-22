package com.safedrive.ai.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
        val contactCount = intent.getIntExtra(EXTRA_CONTACT_COUNT, 0)
        val callNumber = intent.getStringExtra(EXTRA_CALL_NUMBER).orEmpty()

        setContent {
            SafeDriveTheme {
                Surface {
                    CrashCountdownScreen(
                        totalSeconds = seconds,
                        contactCount = contactCount,
                        callNumber = callNumber,
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
        const val EXTRA_CONTACT_COUNT = "extra_contact_count"
        const val EXTRA_CALL_NUMBER = "extra_call_number"
    }
}

@Composable
private fun CrashCountdownScreen(
    totalSeconds: Int,
    contactCount: Int,
    callNumber: String,
    onImOk: () -> Unit,
    onCallNow: () -> Unit,
    onTimeout: () -> Unit,
) {
    var remainingSeconds by remember { mutableIntStateOf(totalSeconds) }
    var timeoutSent by remember { mutableStateOf(false) }
    val progress = (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF3B0707),
                        Color(0xFF160B12),
                        Color(0xFF070B14),
                    ),
                ),
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF2F2)),
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    "Potential Crash Detected",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF7F1D1D),
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Are you OK?",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF111827),
                    textAlign = TextAlign.Center,
                )

                Text(
                    remainingSeconds.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFDC2626),
                )
                CountdownProgress(progress)

                Text(
                    "If there is no response, Project Sentry will send SMS alerts, open the call flow, and play the TTS intro.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF374151),
                    textAlign = TextAlign.Center,
                )

                EscalationSummary(contactCount, callNumber)

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onImOk,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                ) {
                    Text("I'm OK - Cancel Alert")
                }
                Button(
                    onClick = onCallNow,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                ) {
                    Text("Call Now")
                }
            }
        }
    }
}

@Composable
private fun CountdownProgress(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFFECACA)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(12.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFDC2626)),
        )
    }
}

@Composable
private fun EscalationSummary(
    contactCount: Int,
    callNumber: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFFFFF),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Escalation target", fontWeight = FontWeight.ExtraBold, color = Color(0xFF111827))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("SMS contacts", color = Color(0xFF4B5563))
                Text(contactCount.toString(), fontWeight = FontWeight.Bold, color = Color(0xFF111827))
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Call number", color = Color(0xFF4B5563))
                Text(callNumber.ifBlank { "Not set" }, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
            }
        }
    }
}
