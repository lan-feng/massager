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
import com.massager.app.data.bluetooth.protocol.EmsV2ProtocolAdapter
import com.massager.app.data.bluetooth.protocol.EmsV2ProtocolAdapter.EmsV2Command
import com.massager.app.data.bluetooth.protocol.ProtocolMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import com.massager.app.presentation.navigation.Screen
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tag = logTag("DeviceControlVM")

    private var initialStatusRequested = false
    private var lastConnectionError: Pair<BleConnectionState.Status, String?>? = null

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

    fun selectZone(zone: BodyZone) {
        val current = _uiState.value
        if (current.isRunning) return
        _uiState.update { it.copy(zone = zone) }
    }

    fun selectMode(mode: Int) {
        val sanitized = mode.coerceIn(0, 7)
        val current = _uiState.value
        if (current.isRunning) return
        _uiState.update { it.copy(mode = sanitized) }
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
            val success = sendCommand(EmsV2Command.ToggleMute(target))
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
                if (!sendCommand(EmsV2Command.SetLevel(newLevel))) {
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
                when (message) {
                    is EmsV2ProtocolAdapter.EmsV2Message.Heartbeat -> handleHeartbeat(message)
                    is EmsV2ProtocolAdapter.EmsV2Message.ModeReport -> {
                        _uiState.update { it.copy(mode = message.mode.coerceIn(0, 7)) }
                    }
                    is EmsV2ProtocolAdapter.EmsV2Message.LevelReport -> {
                        _uiState.update { it.copy(level = message.level.coerceIn(MIN_LEVEL, MAX_LEVEL)) }
                    }
                    is EmsV2ProtocolAdapter.EmsV2Message.ZoneReport -> {
                        _uiState.update { it.copy(zone = BodyZone.fromIndex(message.zone)) }
                    }
                    is EmsV2ProtocolAdapter.EmsV2Message.TimerReport -> {
                        _uiState.update {
                            it.copy(
                                timerMinutes = message.minutes.coerceAtLeast(0),
                                remainingSeconds = message.minutes.coerceAtLeast(0) * 60
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun handleHeartbeat(message: EmsV2ProtocolAdapter.EmsV2Message.Heartbeat) {
        _uiState.update { state ->
            val battery = message.batteryPercent.coerceIn(0, 100)
            val zone = BodyZone.fromIndex(message.zone)
            val mode = message.mode.coerceIn(0, 7)
            val level = message.level.coerceIn(MIN_LEVEL, MAX_LEVEL)
            val minutes = message.timerMinutes.coerceAtLeast(0)
            val remainingSeconds = if (minutes == 0 && !message.isRunning) {
                0
            } else {
                minutes * 60
            }
            val becameIdle = state.isRunning && !message.isRunning
            val messageUpdate = when {
                battery <= 15 -> DeviceMessage.BatteryLow
                becameIdle -> DeviceMessage.SessionStopped
                else -> null
            }
            state.copy(
                isRunning = message.isRunning,
                zone = zone,
                mode = mode,
                level = level,
                timerMinutes = minutes.takeIf { !message.isRunning } ?: state.timerMinutes,
                remainingSeconds = if (message.isRunning) remainingSeconds else 0,
                batteryPercent = battery,
                isMuted = message.isMuted,
                message = messageUpdate ?: state.message
            )
        }
    }

    private fun startSession() {
        val current = _uiState.value
        if (!current.isConnected) {
            _uiState.update { it.copy(message = DeviceMessage.CommandFailed(R.string.device_status_disconnected)) }
            return
        }
        val timerMinutes = current.timerMinutes.takeIf { it > 0 } ?: DEFAULT_TIMER_MINUTES
        val zoneIndex = current.zone.index
        val level = max(current.level, 1)

        viewModelScope.launch {
            val commands = listOf(
                EmsV2Command.SetBodyZone(zoneIndex),
                EmsV2Command.SetMode(current.mode),
                EmsV2Command.SetLevel(level),
                EmsV2Command.SetTimer(timerMinutes),
                EmsV2Command.RunProgram(
                    zone = zoneIndex,
                    mode = current.mode,
                    level = level,
                    timerMinutes = timerMinutes
                )
            )
            val success = commands.all { sendCommand(it) }
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
            } else {
                _uiState.update { it.copy(message = DeviceMessage.CommandFailed()) }
            }
        }
    }

    private fun stopSession(userInitiated: Boolean) {
        val current = _uiState.value
        if (!current.isRunning && !userInitiated) return
        viewModelScope.launch {
            val success = sendCommand(EmsV2Command.StopProgram)
            _uiState.update {
                it.copy(
                    isRunning = false,
                    level = 0,
                    remainingSeconds = 0,
                    message = DeviceMessage.SessionStopped.takeIf { success } ?: DeviceMessage.CommandFailed()
                )
            }
        }
    }

    private fun requestStatus() {
        viewModelScope.launch {
            sendCommand(EmsV2Command.ReadStatus)
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

    private suspend fun sendCommand(command: EmsV2Command): Boolean {
        Log.d(tag, "sendCommand: preparing to send command=$command")
        val protocolReady = awaitProtocolReady()
        if (!protocolReady) {
            Log.w(tag, "sendCommand: protocol not ready for command=$command, attempting dispatch anyway")
        }
        val success = withContext(Dispatchers.IO) {
            bluetoothService.sendProtocolCommand(command)
        }
        if (!success) {
            Log.e(tag, "sendCommand: bluetoothService rejected command=$command")
            return false
        }
        delay(80)
        Log.v(tag, "sendCommand: command queued successfully command=$command")
        return true
    }

    private suspend fun awaitProtocolReady(
        attempts: Int = 50,
        intervalMillis: Long = 100
    ): Boolean {
        if (bluetoothService.isProtocolReady()) {
            Log.v(tag, "awaitProtocolReady: protocol already ready")
            return true
        }
        repeat(attempts) { index ->
            delay(intervalMillis)
            if (bluetoothService.isProtocolReady()) {
                Log.v(tag, "awaitProtocolReady: protocol ready after attempt=${index + 1}")
                return true
            }
        }
        val state = bluetoothService.connectionState.value
        Log.w(
            tag,
            "awaitProtocolReady: timed out after attempts=$attempts intervalMillis=$intervalMillis status=${state.status} " +
                "isProtocolReady=${state.isProtocolReady} device=${state.deviceAddress}"
        )
        return false
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
    val batteryPercent: Int = 0,
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
    data class CommandFailed(
        @StringRes val messageRes: Int = R.string.device_command_failed,
        val messageText: String? = null
    ) : DeviceMessage()
}
