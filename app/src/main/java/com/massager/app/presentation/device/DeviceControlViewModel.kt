package com.massager.app.presentation.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_LEVEL = 20
private const val MIN_LEVEL = 0

@HiltViewModel
class DeviceControlViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceControlUiState())
    val uiState: StateFlow<DeviceControlUiState> = _uiState.asStateFlow()

    private var sessionJob: Job? = null

    fun selectZone(zone: BodyZone) {
        if (_uiState.value.isRunning) return
        _uiState.update { it.copy(zone = zone) }
    }

    fun selectMode(mode: Int) {
        if (_uiState.value.isRunning) return
        _uiState.update { it.copy(mode = mode.coerceIn(1, 5)) }
        viewModelScope.launch { sendCommand("SET_MODE", mode) }
    }

    fun selectTimer(minutes: Int) {
        if (_uiState.value.isRunning) return
        _uiState.update { it.copy(timerMinutes = minutes) }
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun increaseLevel() = adjustLevel(1)
    fun decreaseLevel() = adjustLevel(-1)

    private fun adjustLevel(delta: Int) {
        val current = _uiState.value
        if (!current.isRunning) return
        val newLevel = (current.level + delta).coerceIn(MIN_LEVEL, MAX_LEVEL)
        if (newLevel == current.level) return
        _uiState.update { it.copy(level = newLevel) }
        viewModelScope.launch { sendCommand("SET_LEVEL", newLevel) }
    }

    fun toggleSession() {
        if (_uiState.value.isRunning) {
            stopSession(userInitiated = true)
        } else {
            startSession()
        }
    }

    private fun startSession() {
        val current = _uiState.value
        val minutes = if (current.timerMinutes == 0) 15 else current.timerMinutes
        val batteryPercent = current.batteryPercent
        if (minutes <= 0) return
        _uiState.update {
            it.copy(
                isRunning = true,
                timerMinutes = minutes,
                remainingSeconds = minutes * 60,
                level = max(1, it.level)
            )
        }
        viewModelScope.launch { sendCommand("START_SESSION", minutes) }
        if (batteryPercent <= 15) {
            _uiState.update { it.copy(message = DeviceMessage.BatteryLow) }
        } else {
            _uiState.update {
                it.copy(
                    message = DeviceMessage.SessionStarted(
                        level = it.level,
                        mode = it.mode
                    )
                )
            }
        }
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0) {
                delay(1_000)
                _uiState.update { state ->
                    state.copy(remainingSeconds = max(0, state.remainingSeconds - 1))
                }
            }
            stopSession(userInitiated = false)
        }
    }

    private fun stopSession(userInitiated: Boolean) {
        sessionJob?.cancel()
        sessionJob = null
        _uiState.update {
            it.copy(
                isRunning = false,
                level = 0,
                remainingSeconds = 0
            )
        }
        viewModelScope.launch { sendCommand("STOP_SESSION") }
        _uiState.update { it.copy(message = DeviceMessage.SessionStopped) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private suspend fun sendCommand(command: String, value: Int? = null) {
        delay(120)
        // Placeholder for BLE implementation
    }
}

data class DeviceControlUiState(
    val zone: BodyZone = BodyZone.SHLDR,
    val mode: Int = 1,
    val level: Int = 0,
    val timerMinutes: Int = 0,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isMuted: Boolean = false,
    val batteryPercent: Int = 86,
    val message: DeviceMessage? = null
) {
    val availableTimerOptions: List<Int> = listOf(5, 10, 15, 20, 30, 45, 60)
}

enum class BodyZone {
    SHLDR, WAIST, LEGS, ARMS, JC
}

sealed class DeviceMessage {
    data class SessionStarted(val level: Int, val mode: Int) : DeviceMessage()
    object SessionStopped : DeviceMessage()
    object BatteryLow : DeviceMessage()
}
