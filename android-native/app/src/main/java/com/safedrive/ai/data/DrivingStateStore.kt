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
                current.copy(isDriving = true)
            } else {
                DrivingUiState(isDriving = false)
            }
        }
    }

    fun setCrashCountdownActive(active: Boolean) {
        _state.update { it.copy(crashCountdownActive = active) }
    }

    fun setSpeedKmh(speedKmh: Float) {
        _state.update { it.copy(latestSpeedKmh = speedKmh) }
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
}
