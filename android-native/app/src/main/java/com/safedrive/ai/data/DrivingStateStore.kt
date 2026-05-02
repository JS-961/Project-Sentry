package com.safedrive.ai.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object DrivingStateStore {
    private val _state = MutableStateFlow(DrivingUiState())
    val state: StateFlow<DrivingUiState> = _state.asStateFlow()

    fun setDrivingActive(active: Boolean) {
        _state.update { current ->
            if (active) {
                current.copy(
                    isDriving = true,
                    sessionStartedAtEpochMs = current.sessionStartedAtEpochMs
                        ?: System.currentTimeMillis(),
                )
            } else {
                DrivingUiState(
                    isDriving = false,
                    lastAlertOutcome = current.lastAlertOutcome,
                )
            }
        }
    }

    fun setCrashCountdownActive(active: Boolean) {
        _state.update { it.copy(crashCountdownActive = active) }
    }

    fun setSpeedKmh(speedKmh: Float) {
        _state.update {
            it.copy(
                latestSpeedKmh = speedKmh,
                lastLocationEpochMs = System.currentTimeMillis(),
            )
        }
    }

    fun setLatestEvent(label: String) {
        _state.update { it.copy(latestEventLabel = label) }
    }

    fun updateRisk(
        riskScore: Int,
        counters: RiskCounters,
    ) {
        _state.update {
            it.copy(
                riskScore = riskScore.coerceIn(0, 100),
                counters = counters,
            )
        }
    }

    fun updateMlRisk(
        riskScore: Int,
        label: String,
        confidence: Float,
        source: String,
    ) {
        _state.update {
            it.copy(
                mlRiskScore = riskScore.coerceIn(0, 100),
                mlRiskLabel = label,
                mlRiskConfidence = confidence.coerceIn(0f, 1f),
                mlModelSource = source,
            )
        }
    }

    fun setSensorHeartbeat(timestampMs: Long = System.currentTimeMillis()) {
        _state.update { it.copy(lastSensorEpochMs = timestampMs) }
    }

    fun setLastAlertOutcome(label: String) {
        _state.update { it.copy(lastAlertOutcome = label) }
    }
}
