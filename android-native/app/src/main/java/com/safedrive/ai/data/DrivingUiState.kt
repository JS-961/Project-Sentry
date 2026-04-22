package com.safedrive.ai.data

data class RiskCounters(
    val harshBraking: Int = 0,
    val harshAcceleration: Int = 0,
    val sharpCornering: Int = 0,
    val speeding: Int = 0,
) {
    val total: Int
        get() = harshBraking + harshAcceleration + sharpCornering + speeding
}

data class DrivingUiState(
    val isDriving: Boolean = false,
    val riskScore: Int = 0,
    val latestSpeedKmh: Float = 0f,
    val latestEventLabel: String = "No events yet",
    val counters: RiskCounters = RiskCounters(),
    val crashCountdownActive: Boolean = false,
    val sessionStartedAtEpochMs: Long? = null,
    val lastLocationEpochMs: Long? = null,
    val lastSensorEpochMs: Long? = null,
    val lastAlertOutcome: String? = null,
)
