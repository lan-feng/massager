package com.massager.app.presentation.device

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massager.app.R
import com.massager.app.core.logging.logTag
import com.massager.app.data.bluetooth.BleConnectionState
import com.massager.app.data.bluetooth.MassagerBluetoothService
import com.massager.app.data.bluetooth.protocol.ProtocolMessage
import com.massager.app.domain.device.DeviceSession
import com.massager.app.domain.device.DeviceSessionRegistry
import com.massager.app.domain.device.DeviceTelemetry
import com.massager.app.domain.device.DeviceTelemetryMapper
import com.massager.app.domain.device.DeviceTelemetryMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import com.massager.app.presentation.navigation.Screen
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_LEVEL = 19
private const val MIN_LEVEL = 0
private const val DEFAULT_TIMER_MINUTES = 15
private const val CONNECTING_OVERLAY_DURATION_MS = 3_000L

@HiltViewModel
class DeviceControlViewModel @Inject constructor(
    private val bluetoothService: MassagerBluetoothService,
    savedStateHandle: SavedStateHandle,
    private val sessionRegistry: DeviceSessionRegistry,
    private val telemetryMappers: Set<@JvmSuppressWildcards DeviceTelemetryMapper>
) : ViewModel() {

    private val tag = logTag("DeviceControlVM")

    private var initialStatusRequested = false
    private var lastConnectionError: Pair<BleConnectionState.Status, String?>? = null
    private var awaitingStopAck = false

    private val preferredName: String? =
        savedStateHandle.get<String>(Screen.DeviceControl.ARG_DEVICE_NAME)?.takeIf { it.isNotBlank() }
    private val targetAddress: String? =
        savedStateHandle.get<String>(Screen.DeviceControl.ARG_DEVICE_MAC)?.takeIf { it.isNotBlank() }
    private val _uiState = MutableStateFlow(DeviceControlUiState())
    val uiState: StateFlow<DeviceControlUiState> = _uiState.asStateFlow()

    init {
        preferredName?.let { name ->
            _uiState.update { state ->
                if (state.deviceName.isNotBlank()) state else state.copy(deviceName = name)
            }
        }
        targetAddress?.let { address ->
            _uiState.update { it.copy(deviceAddress = address) }
        }
        observeConnection()
        observeProtocolMessages()
        attemptInitialConnection()
    }

    private fun currentSession(): DeviceSession? =
        sessionRegistry.sessionFor(bluetoothService.activeProtocolKey())

    private suspend fun withSession(block: suspend (DeviceSession) -> Boolean): Boolean =
        withContext(Dispatchers.IO) {
            val session = currentSession() ?: return@withContext false
            block(session)
        }

    private fun mapTelemetry(message: ProtocolMessage): DeviceTelemetry? {
        telemetryMappers.forEach { mapper ->
            if (mapper.supports(message)) {
                return mapper.map(message)
            }
        }
        return null
    }

    fun selectZone(zone: BodyZone) {
        val current = _uiState.value
        if (current.zone == zone) return
        _uiState.update { it.copy(zone = zone) }
        if (current.isRunning) {
            viewModelScope.launch {
                if (!withSession { session -> session.selectZone(zone.index) }) {
                    _uiState.update { it.copy(message = DeviceMessage.CommandFailed()) }
                }
            }
        }
    }

    fun selectMode(mode: Int) {
        val sanitized = mode.coerceIn(0, 7)
        val current = _uiState.value
        if (current.mode == sanitized) return
        _uiState.update { it.copy(mode = sanitized) }
        if (current.isRunning) {
            viewModelScope.launch {
                if (!withSession { session -> session.selectMode(sanitized) }) {
                    _uiState.update { it.copy(message = DeviceMessage.CommandFailed()) }
                }
            }
        }
    }

    fun selectTimer(minutes: Int) {
        val current = _uiState.value
        if (current.isRunning) return
        _uiState.update { it.copy(timerMinutes = minutes.coerceAtLeast(1)) }
    }

    fun toggleMute() {
        val target = !_uiState.value.isMuted
        viewModelScope.launch {
            Log.d(tag, "toggleMute: issuing command for muted=$target")
            val success = withSession { session -> session.toggleMute(target) }
            if (success) {
                _uiState.update { it.copy(isMuted = target, message = DeviceMessage.MuteChanged(target)) }
            } else {
                _uiState.update { it.copy(message = DeviceMessage.CommandFailed()) }
            }
        }
    }

    fun increaseLevel() = adjustLevel(1)
    fun decreaseLevel() = adjustLevel(-1)

    private fun adjustLevel(delta: Int) {
        val current = _uiState.value
        val newLevel = (current.level + delta).coerceIn(MIN_LEVEL, MAX_LEVEL)
        if (newLevel == current.level) return
        _uiState.update { it.copy(level = newLevel) }
        Log.d(tag, "adjustLevel: requested delta=$delta resultingLevel=$newLevel isRunning=${current.isRunning}")
        if (current.isRunning) {
            viewModelScope.launch {
                Log.d(tag, "adjustLevel: sending live level update level=$newLevel")
                if (!withSession { session -> session.selectLevel(newLevel) }) {
                    _uiState.update { it.copy(message = DeviceMessage.CommandFailed()) }
                }
            }
        }
    }

    fun toggleSession() {
        val current = _uiState.value
        if (current.isRunning) {
            Log.d(tag, "toggleSession: stop requested by user")
            stopSession(userInitiated = true)
        } else {
            Log.d(tag, "toggleSession: start requested with zone=${current.zone} mode=${current.mode} level=${current.level} timer=${current.timerMinutes}")
            startSession()
        }
    }

    fun reconnect() {
        val address = _uiState.value.deviceAddress ?: targetAddress ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(showConnectingOverlay = true, isConnecting = true) }
            val overlayJob = launch {
                delay(CONNECTING_OVERLAY_DURATION_MS)
                _uiState.update { current ->
                    current.copy(
                        showConnectingOverlay = false,
                        isConnecting = false
                    )
                }
            }
            Log.i(tag, "reconnect: attempting to reconnect to $address")
            bluetoothService.clearError()
            val success = bluetoothService.connect(address)
            if (!success) {
                Log.w(tag, "reconnect: connect returned false for $address")
                overlayJob.cancel()
                _uiState.update {
                    it.copy(
                        message = DeviceMessage.CommandFailed(),
                        showConnectingOverlay = false,
                        isConnecting = false
                    )
                }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun observeConnection() {
        viewModelScope.launch {
            bluetoothService.connectionState.collectLatest { state ->
                val isConnected = state.status == BleConnectionState.Status.Connected
                val isConnecting = state.status == BleConnectionState.Status.Connecting
                _uiState.update {
                    val resolvedName = resolveDisplayName(
                        connectionName = state.deviceName,
                        currentName = it.deviceName,
                        preferred = preferredName,
                        address = state.deviceAddress ?: it.deviceAddress ?: targetAddress
                    )
                    it.copy(
                        deviceName = resolvedName,
                        deviceAddress = state.deviceAddress ?: it.deviceAddress ?: targetAddress,
                        isConnected = isConnected,
                        isConnecting = isConnecting,
                        isProtocolReady = state.isProtocolReady
                    )
                }
                if (!isConnecting) {
                    _uiState.update { current -> current.copy(showConnectingOverlay = false) }
                }
                if (state.errorMessage.isNullOrBlank()) {
                    lastConnectionError = null
                } else {
                    val token = state.status to state.errorMessage
                    if (lastConnectionError != token) {
                        lastConnectionError = token
                        _uiState.update { current ->
                            current.copy(
                                message = DeviceMessage.CommandFailed(
                                    messageRes = R.string.device_command_failed,
                                    messageText = state.errorMessage
                                )
                            )
                        }
                    }
                }
                if (isConnected && state.isProtocolReady) {
                    if (!initialStatusRequested) {
                        initialStatusRequested = true
                        requestStatus()
                    }
                } else {
                    initialStatusRequested = false
                }
            }
        }
    }

    private fun observeProtocolMessages() {
        viewModelScope.launch {
            bluetoothService.protocolMessages.collectLatest { message ->
                val telemetry = mapTelemetry(message) ?: return@collectLatest
                applyTelemetry(telemetry)
            }
        }
    }

    private fun applyTelemetry(telemetry: DeviceTelemetry) {
        if (awaitingStopAck && telemetry.isRunning == true) {
            Log.v(tag, "applyTelemetry: awaiting stop ack, ignoring running telemetry")
            return
        }
        if (awaitingStopAck && telemetry.isRunning == false) {
            awaitingStopAck = false
        }
        _uiState.update { state ->
            var next = state
            telemetry.isRunning?.let { next = next.copy(isRunning = it) }

            val shouldUpdateControls = next.isRunning || telemetry.isRunning == true
            if (shouldUpdateControls) {
                telemetry.zone?.let { next = next.copy(zone = BodyZone.fromIndex(it)) }
                telemetry.mode?.let { next = next.copy(mode = it.coerceIn(0, 7)) }
                telemetry.level?.let { level ->
                    next = next.copy(level = level.coerceIn(MIN_LEVEL, MAX_LEVEL))
                }
                telemetry.timerMinutes?.let { minutes ->
                    next = next.copy(timerMinutes = minutes.coerceAtLeast(0))
                }
                telemetry.remainingSeconds?.let { seconds ->
                    next = next.copy(remainingSeconds = seconds.coerceAtLeast(0))
                }
            } else if (telemetry.isRunning == false) {
                next = next.copy(remainingSeconds = 0)
            }

            val batteryPercent = telemetry.batteryPercent
                ?.takeIf { it >= 0 }
                ?.coerceIn(0, 100)
            var pendingMessage: DeviceMessage? = null
            if (batteryPercent != null) {
                val shouldNotifyBattery =
                    batteryPercent in 0..25 && (state.batteryPercent > 25 || state.batteryPercent < 0)
                next = next.copy(batteryPercent = batteryPercent)
                if (shouldNotifyBattery) {
                    pendingMessage = DeviceMessage.BatteryLow
                }
            }

            telemetry.isMuted?.let { muted ->
                next = next.copy(isMuted = muted)
            }

            val mappedMessage = when (val teleMessage = telemetry.message) {
                DeviceTelemetryMessage.BatteryLow -> null // handled above
                DeviceTelemetryMessage.SessionStopped -> DeviceMessage.SessionStopped
                is DeviceTelemetryMessage.RemoteLevelChanged -> {
                    if (shouldUpdateControls) {
                        DeviceMessage.RemoteLevelChanged(teleMessage.level)
                    } else {
                        null
                    }
                }
                null -> null
            }

            val messageToShow = pendingMessage ?: mappedMessage
            if (messageToShow != null) {
                val canApply =
                    state.message == null || messageToShow !is DeviceMessage.RemoteLevelChanged
                if (canApply) {
                    next = next.copy(message = messageToShow)
                }
            }
            next
        }
    }

    private fun startSession() {
        val current = _uiState.value
        if (!current.isConnected) {
            _uiState.update { it.copy(message = DeviceMessage.CommandFailed(R.string.device_status_disconnected)) }
            return
        }
        if (!current.isProtocolReady) {
            Log.w(tag, "startSession: protocol not ready, skipping start")
            _uiState.update { it.copy(message = DeviceMessage.CommandFailed()) }
            return
        }
        val timerMinutes = current.timerMinutes.takeIf { it > 0 } ?: DEFAULT_TIMER_MINUTES
        val zoneIndex = current.zone.index
        val level = max(current.level, 1)

        viewModelScope.launch {
            val success = withSession { session ->
                session.selectZone(zoneIndex) &&
                    session.selectMode(current.mode) &&
                    session.selectLevel(level) &&
                    session.selectTimer(timerMinutes) &&
                    session.runProgram(
                        zone = zoneIndex,
                        mode = current.mode,
                        level = level,
                        timerMinutes = timerMinutes
                    )
            }
            if (success) {
                _uiState.update {
                    it.copy(
                        isRunning = true,
                        level = level,
                        timerMinutes = timerMinutes,
                        remainingSeconds = timerMinutes * 60,
                        message = DeviceMessage.SessionStarted(level = level, mode = current.mode)
                    )
                }
                awaitingStopAck = false
            } else {
                _uiState.update { it.copy(message = DeviceMessage.CommandFailed()) }
            }
        }
    }

    private fun stopSession(userInitiated: Boolean) {
        val current = _uiState.value
        if (!current.isRunning && !userInitiated) return
        viewModelScope.launch {
            val success = withSession { session -> session.stopProgram() }
            _uiState.update {
                it.copy(
                    isRunning = false,
                    level = 0,
                    remainingSeconds = 0,
                    message = if (success) DeviceMessage.SessionStopped else DeviceMessage.CommandFailed()
                )
            }
            if (success) {
                awaitingStopAck = true
            }
        }
    }

    private fun requestStatus() {
        viewModelScope.launch {
            withSession { session -> session.requestStatus() }
        }
    }

    private fun resolveDisplayName(
        connectionName: String?,
        currentName: String,
        preferred: String?,
        address: String?
    ): String {
        val candidates = listOf(
            connectionName,
            currentName,
            preferred,
            bluetoothService.cachedDeviceName(address),
            address
        )
        return candidates.firstNotNullOfOrNull { name ->
            name?.takeIf { it.isNotBlank() && !it.isMacAddress() }
        } ?: preferred?.takeIf { it.isNotBlank() } ?: currentName.ifBlank {
            address?.takeUnless { it.isBlank() } ?: ""
        }
    }

    private fun String.isMacAddress(): Boolean {
        return matches(Regex("^[0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2}){5}\$"))
    }

    private fun attemptInitialConnection() {
        val address = targetAddress ?: return
        val connection = bluetoothService.connectionState.value
        val alreadyActive = connection.deviceAddress == address &&
            when (connection.status) {
                BleConnectionState.Status.Connected,
                BleConnectionState.Status.Connecting -> true
                else -> false
            }
        if (alreadyActive) return

        viewModelScope.launch {
            bluetoothService.clearError()
            val started = bluetoothService.connect(address)
            if (!started) {
                Log.w(tag, "attemptInitialConnection: connect failed for initial address=$address")
                _uiState.update { it.copy(message = DeviceMessage.CommandFailed()) }
            }
        }
    }

}

data class DeviceControlUiState(
    val deviceName: String = "",
    val deviceAddress: String? = null,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isProtocolReady: Boolean = false,
    val zone: BodyZone = BodyZone.SHOULDER,
    val mode: Int = 0,
    val level: Int = 0,
    val timerMinutes: Int = DEFAULT_TIMER_MINUTES,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isMuted: Boolean = false,
    val batteryPercent: Int = -1,
    val message: DeviceMessage? = null,
    val showConnectingOverlay: Boolean = false
) {
    val availableTimerOptions: List<Int> = listOf(5, 10, 15, 20, 30, 45, 60)
}

enum class BodyZone(val index: Int, @StringRes val labelRes: Int) {
    SHOULDER(0, R.string.device_zone_shldr),
    WAIST(1, R.string.device_zone_waist),
    LEGS(2, R.string.device_zone_legs),
    ARMS(3, R.string.device_zone_arms),
    JOINT(4, R.string.device_zone_jc),
    BODY(5, R.string.device_zone_body);

    companion object {
        fun fromIndex(index: Int): BodyZone =
            values().firstOrNull { it.index == index } ?: SHOULDER
    }
}

sealed class DeviceMessage {
    data class SessionStarted(val level: Int, val mode: Int) : DeviceMessage()
    object SessionStopped : DeviceMessage()
    object BatteryLow : DeviceMessage()
    data class MuteChanged(val enabled: Boolean) : DeviceMessage()
    data class RemoteLevelChanged(val level: Int) : DeviceMessage()
    data class CommandFailed(
        @StringRes val messageRes: Int = R.string.device_command_failed,
        val messageText: String? = null
    ) : DeviceMessage()
}
