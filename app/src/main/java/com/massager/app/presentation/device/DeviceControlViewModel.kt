package com.massager.app.presentation.device

// 文件说明：设备控制界面的状态管理与指令发送逻辑。
import android.content.Context
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
import com.massager.app.data.bluetooth.protocol.ProtocolMessage
import com.massager.app.domain.device.DeviceSession
import com.massager.app.domain.device.DeviceSessionRegistry
import com.massager.app.domain.device.DeviceTelemetry
import com.massager.app.domain.device.DeviceTelemetryMapper
import com.massager.app.domain.device.DeviceTelemetryMessage
import com.massager.app.data.bluetooth.protocol.EmsV2ProtocolAdapter.EmsV2Command
import com.massager.app.domain.model.ComboDeviceInfo
import com.massager.app.domain.model.ComboInfoPayload
import com.massager.app.domain.model.ComboInfoSerializer
import com.massager.app.domain.usecase.device.ObserveDeviceComboInfoUseCase
import com.massager.app.domain.usecase.device.UpdateDeviceComboInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.massager.app.presentation.navigation.Screen
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private const val MAX_LEVEL = 19
private const val MIN_LEVEL = 0
private const val DEFAULT_TIMER_MINUTES = 30
private const val CONNECTING_OVERLAY_DURATION_MS = 3_000L
private const val PROTOCOL_SIGNAL_TIMEOUT_MS = 1_200L
private const val LEVEL_COMMAND_DEBOUNCE_MS = 200L
private const val LEVEL_HEARTBEAT_SUPPRESSION_WINDOW_MS = 5_000L

@HiltViewModel
class DeviceControlViewModel @Inject constructor(
    private val bluetoothService: MassagerBluetoothService,
    savedStateHandle: SavedStateHandle,
    private val sessionRegistry: DeviceSessionRegistry,
    private val telemetryMappers: Set<@JvmSuppressWildcards DeviceTelemetryMapper>,
    private val observeComboInfoUseCase: ObserveDeviceComboInfoUseCase,
    private val updateDeviceComboInfoUseCase: UpdateDeviceComboInfoUseCase,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val tag = logTag("DeviceControlVM")

    private val mainDeviceId =
        savedStateHandle.get<String>(Screen.DeviceControl.ARG_DEVICE_ID)?.takeIf { it.isNotBlank() }
    private var initialStatusRequested = false
    private var lastConnectionError: Pair<BleConnectionState.Status, String?>? = null
    private var awaitingStopAck = false
    private var comboDevices: List<ComboDeviceInfo> = emptyList()
    private var comboInfoRaw: String? = null
    private var pendingSelectionSerial: String? = null
    private var selectedDeviceSerial: String? = null
    private var hasProtocolSignal = false
    private var protocolSignalTimeoutJob: Job? = null
    private var countdownJob: Job? = null
    private var lastTickAtMillis: Long = 0L
    private val deviceUiCache = mutableMapOf<String, DeviceSnapshot>()
    private val heartbeatRequested = mutableSetOf<String>()
    private val lastHeartbeatSentAt = mutableMapOf<String, Long>()
    private val userTimerOverrides = mutableMapOf<String, Int>()
    private val lastTelemetryRemainingSeconds = mutableMapOf<String, Int>()
    private var levelDispatchJob: Job? = null
    private var pendingLevelToSend: Int? = null
    private var lastLocalLevelSetAtMillis: Long = 0L

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
        selectedDeviceSerial = targetAddress
        bluetoothService.setPreferredAddress(selectedDeviceSerial)
        _uiState.update { it.copy(selectedDeviceSerial = selectedDeviceSerial) }
        buildDeviceCards()
        observeConnection()
        observeConnectionStates()
        observeProtocolMessages()
        observeComboDevices()
        observeCountdown()
        attemptInitialConnection()
    }

    private fun currentAddress(): String? =
        _uiState.value.selectedDeviceSerial ?: targetAddress

    private fun currentSession(): DeviceSession? =
        sessionRegistry.sessionFor(bluetoothService.activeProtocolKey(selectedDeviceSerial))

    private suspend fun withSession(block: suspend (DeviceSession) -> Boolean): Boolean =
        withContext(Dispatchers.IO) {
            val session = currentSession() ?: return@withContext false
            block(session)
        }

    private fun processConnectionState(state: BleConnectionState) {
        Log.d(
            tag,
            "processConnectionState: status=${state.status} protocolReady=${state.isProtocolReady} " +
                "device=${state.deviceAddress} name=${state.deviceName} hasSignal=$hasProtocolSignal"
        )
        val activeStatus = state.deviceAddress?.let { address ->
            bluetoothService.connectionStates.value[address]
        }
        val isConnectedAlready = activeStatus?.status == BleConnectionState.Status.Connected &&
            activeStatus.isProtocolReady
        if (isConnectedAlready && (state.status == BleConnectionState.Status.Scanning || state.status == BleConnectionState.Status.Idle)) {
            // Ignore scan/idle state updates that reuse the active device address; keep the UI in the real connection state.
            return
        }
        if (state.status != BleConnectionState.Status.Connected) {
            hasProtocolSignal = false
            protocolSignalTimeoutJob?.cancel()
            protocolSignalTimeoutJob = null
        }
        val eventAddress = state.deviceAddress
        val isProtocolReady = state.isProtocolReady
        val isConnected =
            state.status == BleConnectionState.Status.Connected && isProtocolReady
        val isConnecting = state.status == BleConnectionState.Status.Connecting ||
            (state.status == BleConnectionState.Status.Connected && !isProtocolReady)

        if (eventAddress != null) {
            _uiState.update { current ->
                val resolvedName = resolveDisplayName(
                    connectionName = state.deviceName,
                    currentName = current.deviceStatuses[eventAddress]?.deviceName
                        ?: current.deviceName,
                    preferred = preferredNameFor(eventAddress),
                    address = eventAddress
                )
                val existing = current.deviceStatuses[eventAddress] ?: DeviceStatus(address = eventAddress)
                val updatedStatuses = current.deviceStatuses + (eventAddress to existing.copy(
                    deviceName = resolvedName.ifBlank { existing.deviceName },
                    connectionStatus = state.status,
                    isProtocolReady = isProtocolReady
                ))

                val shouldBindToUi = current.selectedDeviceSerial == null ||
                    current.selectedDeviceSerial.equals(eventAddress, ignoreCase = true)

                current.copy(
                    deviceName = if (shouldBindToUi) resolvedName else current.deviceName,
                    deviceAddress = if (shouldBindToUi) eventAddress else current.deviceAddress,
                    selectedDeviceSerial = if (current.selectedDeviceSerial == null && shouldBindToUi) {
                        eventAddress
                    } else {
                        current.selectedDeviceSerial
                    },
                    isConnected = if (shouldBindToUi) isConnected else current.isConnected,
                    isConnecting = if (shouldBindToUi) isConnecting else current.isConnecting,
                    isProtocolReady = if (shouldBindToUi) isProtocolReady else current.isProtocolReady,
                    deviceStatuses = updatedStatuses
                )
            }
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
        if (isConnected && isProtocolReady) {
            state.deviceAddress?.let { addr -> maybeRequestHeartbeat(addr) }
        } else {
            initialStatusRequested = false
        }
        if (state.status == BleConnectionState.Status.Connected && isProtocolReady && !hasProtocolSignal && protocolSignalTimeoutJob == null) {
            scheduleProtocolSignalTimeout()
        }
    }

    private fun scheduleProtocolSignalTimeout() {
        protocolSignalTimeoutJob = viewModelScope.launch {
            val timeoutMs = PROTOCOL_SIGNAL_TIMEOUT_MS
            Log.d(tag, "scheduleProtocolSignalTimeout: waiting ${timeoutMs}ms for protocol traffic")
            delay(timeoutMs)
            if (!hasProtocolSignal) {
                Log.w(tag, "scheduleProtocolSignalTimeout: no protocol traffic received, keeping connected but will request heartbeat")
                protocolSignalTimeoutJob = null
                _uiState.value.deviceAddress?.let { addr -> maybeRequestHeartbeat(addr, force = true) }
            } else {
                protocolSignalTimeoutJob = null
            }
        }
    }

    private fun maybeRequestHeartbeat(address: String, force: Boolean = false) {
        val now = System.currentTimeMillis()
        val last = lastHeartbeatSentAt[address] ?: 0L
        if (!force && heartbeatRequested.contains(address)) return
        if (!force && now - last < PROTOCOL_SIGNAL_TIMEOUT_MS) return
        heartbeatRequested.add(address)
        lastHeartbeatSentAt[address] = now
        viewModelScope.launch {
            bluetoothService.sendProtocolCommand(EmsV2Command.RequestHeartbeat, address = address)
        }
    }

    private fun observeCountdown() {
        viewModelScope.launch {
            uiState.collect { state ->
                if (state.isRunning && state.remainingSeconds > 0) {
                    startCountdownIfNeeded()
                } else {
                    stopCountdownTimer()
                }
            }
        }
    }

    private fun startCountdownIfNeeded() {
        if (countdownJob?.isActive == true) return
        refreshCountdownAnchor()
        countdownJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                tickCountdown()
            }
        }
    }

    private fun stopCountdownTimer() {
        countdownJob?.cancel()
        countdownJob = null
        lastTickAtMillis = 0L
    }

    private suspend fun tickCountdown() {
        val now = System.currentTimeMillis()
        val elapsedSeconds =
            ((now - lastTickAtMillis).coerceAtLeast(0L) / 1000L).toInt().coerceAtLeast(1)
        lastTickAtMillis = now
        var targetAddress: String? = null
        var shouldForceHeartbeat = false
        _uiState.update { state ->
            if (!state.isRunning) return@update state
            val nextRemaining = (state.remainingSeconds - elapsedSeconds).coerceAtLeast(0)
            if (nextRemaining < 5 && state.isProtocolReady) {
                targetAddress = state.selectedDeviceSerial ?: state.deviceAddress
                shouldForceHeartbeat = targetAddress != null
            }
            if (nextRemaining != state.remainingSeconds) {
                state.copy(remainingSeconds = nextRemaining)
            } else {
                state
            }
        }
        if (shouldForceHeartbeat) {
            targetAddress?.let { maybeRequestHeartbeat(it, force = true) }
        }
    }

    private fun refreshCountdownAnchor() {
        lastTickAtMillis = System.currentTimeMillis()
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
        if (current.isProtocolReady) {
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
        if (current.isProtocolReady) {
            viewModelScope.launch {
                if (!withSession { session -> session.selectMode(sanitized) }) {
                    _uiState.update { it.copy(message = DeviceMessage.CommandFailed()) }
                }
            }
        }
    }

    fun selectTimer(minutes: Int) {
        val sanitized = minutes.coerceAtLeast(1)
        viewModelScope.launch {
            val success = withSession { session -> session.selectTimer(sanitized) }
            if (success) {
                currentAddress()?.let { address -> userTimerOverrides[address] = sanitized }
                _uiState.update {
                    it.copy(
                        timerMinutes = sanitized,
                        remainingSeconds = sanitized * 60
                    )
                }
                refreshCountdownAnchor()
            } else {
                _uiState.update { it.copy(message = DeviceMessage.CommandFailed()) }
            }
        }
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
    fun previewLevel(level: Int) = updateLevel(level, dispatchCommand = false)
    fun commitLevel(level: Int) = updateLevel(level, dispatchCommand = true)

    private fun adjustLevel(delta: Int) {
        val current = _uiState.value
        val newLevel = (current.level + delta).coerceIn(MIN_LEVEL, MAX_LEVEL)
        updateLevel(newLevel, dispatchCommand = true)
    }

    private fun updateLevel(level: Int, dispatchCommand: Boolean) {
        val current = _uiState.value
        val sanitized = level.coerceIn(MIN_LEVEL, MAX_LEVEL)
        val shouldUpdateState = sanitized != current.level
        if (shouldUpdateState) {
            _uiState.update {
                val shouldSeedTimer = sanitized > 0 && !it.isRunning && it.remainingSeconds <= 0
                it.copy(
                    level = sanitized,
                    isRunning = sanitized > 0,
                    remainingSeconds = if (shouldSeedTimer) max(1, it.timerMinutes) * 60 else it.remainingSeconds
                )
            }
        }
        Log.d(
            tag,
            "updateLevel: level=$sanitized dispatchCommand=$dispatchCommand isRunning=${current.isRunning}"
        )
        if (dispatchCommand && current.isProtocolReady) {
            enqueueLevelCommand(sanitized)
        }
    }

    private fun enqueueLevelCommand(level: Int) {
        lastLocalLevelSetAtMillis = System.currentTimeMillis()
        pendingLevelToSend = level
        levelDispatchJob?.cancel()
        val job = viewModelScope.launch {
            delay(LEVEL_COMMAND_DEBOUNCE_MS)
            val targetLevel = pendingLevelToSend ?: level
            pendingLevelToSend = null
            if (!_uiState.value.isProtocolReady) {
                Log.w(tag, "enqueueLevelCommand: protocol not ready, skip level dispatch")
                lastLocalLevelSetAtMillis = 0L
                return@launch
            }
            Log.d(tag, "enqueueLevelCommand: sending debounced level=$targetLevel")
            val success = withSession { session -> session.selectLevel(targetLevel) }
            if (success) {
                lastLocalLevelSetAtMillis = System.currentTimeMillis()
            } else {
                lastLocalLevelSetAtMillis = 0L
                _uiState.update { it.copy(message = DeviceMessage.CommandFailed()) }
            }
        }
        levelDispatchJob = job
        job.invokeOnCompletion { throwable ->
            if (throwable != null && throwable !is kotlinx.coroutines.CancellationException) {
                Log.w(tag, "enqueueLevelCommand: job completed with error", throwable)
            }
            if (levelDispatchJob === job) {
                levelDispatchJob = null
            }
        }
    }

    fun toggleSession() {
        Log.d(tag, "toggleSession: stop requested by user")
        stopSession(userInitiated = true)
    }

    fun reconnect(address: String? = null) {
        val target = address ?: _uiState.value.selectedDeviceSerial ?: targetAddress ?: return
        viewModelScope.launch {
            bluetoothService.setPreferredAddress(target)
            _uiState.update { state ->
                val status = state.deviceStatuses[target] ?: DeviceStatus(address = target)
                state.copy(
                    deviceStatuses = state.deviceStatuses + (target to status.copy(
                        connectionStatus = BleConnectionState.Status.Connecting,
                        isProtocolReady = false
                    ))
                )
            }
            Log.i(tag, "reconnect: attempting to reconnect to $target")
            bluetoothService.clearError()
            val success = bluetoothService.connect(target)
            if (!success) {
                Log.w(tag, "reconnect: connect returned false for $target")
                _uiState.update { state ->
                    val status = state.deviceStatuses[target] ?: DeviceStatus(address = target)
                    state.copy(
                        deviceStatuses = state.deviceStatuses + (target to status.copy(
                            connectionStatus = BleConnectionState.Status.Failed,
                            isProtocolReady = false
                        )),
                        message = DeviceMessage.CommandFailed()
                    )
                }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun consumeTransientMessage() {
        _uiState.update { it.copy(transientMessage = null) }
    }

    fun refreshAfterReturning() {
        val current = _uiState.value
        if (!current.isConnected && !current.isConnecting) {
            reconnect()
        }
    }

    fun disconnectActiveDevice() {
        bluetoothService.disconnect()
    }

    private fun observeConnection() {
        viewModelScope.launch {
            bluetoothService.connectionState.collectLatest { state ->
                Log.d(tag, "observeConnection: state=$state hasSignal=$hasProtocolSignal")
                processConnectionState(state)
            }
        }
    }

    private fun observeConnectionStates() {
        viewModelScope.launch {
            bluetoothService.connectionStates.collectLatest { states ->
                _uiState.update { state ->
                    var next = state
                    states.forEach { (address, connection) ->
                        val currentStatus = state.deviceStatuses[address] ?: DeviceStatus(address = address)
                        val updated = currentStatus.copy(
                            deviceName = currentStatus.deviceName ?: connection.deviceName,
                            connectionStatus = connection.status,
                            isProtocolReady = connection.isProtocolReady
                        )
                        next = next.copy(
                            deviceStatuses = next.deviceStatuses + (address to updated)
                        )
                    }
                    next
                }
            }
        }
    }

    private fun observeProtocolMessages() {
        viewModelScope.launch {
            bluetoothService.deviceProtocolMessages.collectLatest { payload ->
                Log.d(tag, "observeProtocolMessages: received protocol message=${payload.message} mac=${payload.address}")
                if (!hasProtocolSignal) {
                    hasProtocolSignal = true
                    protocolSignalTimeoutJob?.cancel()
                    protocolSignalTimeoutJob = null
                    // Re-evaluate connection state once we have seen protocol traffic.
                    processConnectionState(bluetoothService.connectionState.value)
                }
                val telemetry = mapTelemetry(payload.message)?.copy(address = payload.address)
                    ?: return@collectLatest
                applyTelemetry(payload.address, telemetry)
            }
        }
    }

    private fun observeComboDevices() {
        val deviceId = mainDeviceId ?: return
        viewModelScope.launch {
            observeComboInfoUseCase(deviceId).collectLatest { raw ->
                comboInfoRaw = raw
                comboDevices = ComboInfoSerializer.parse(raw).devices
                buildDeviceCards()
                attemptPendingSelection()
                ensureConnections()
            }
        }
    }

    private fun buildDeviceCards() {
        val cards = buildList {
            val mainName = preferredName?.takeIf { it.isNotBlank() }
                ?: _uiState.value.deviceName.takeIf { it.isNotBlank() }
                ?: targetAddress.orEmpty()
            add(
                DeviceCardState(
                    deviceSerial = targetAddress,
                    displayName = mainName,
                    isSelected = isSerialSelected(targetAddress),
                    isMainDevice = true
                )
            )
            comboDevices.forEach { device ->
                val displayName = device.nameAlias?.takeIf { it.isNotBlank() } ?: device.deviceSerial
                add(
                    DeviceCardState(
                        deviceSerial = device.deviceSerial,
                        displayName = displayName,
                        isSelected = isSerialSelected(device.deviceSerial),
                        isMainDevice = false
                    )
                )
            }
        }
        _uiState.update { it.copy(deviceCards = cards) }
        ensureConnections()
    }

    private fun ensureConnections() {
        val addresses = buildSet {
            targetAddress?.let { add(it) }
            addAll(comboDevices.mapNotNull { it.deviceSerial })
        }
        addresses.forEach { addr ->
            val state = bluetoothService.connectionStates.value[addr]
            val alreadyActive = state?.status == BleConnectionState.Status.Connected ||
                state?.status == BleConnectionState.Status.Connecting
            if (!alreadyActive) {
                bluetoothService.connect(addr)
            }
        }
    }

    private fun attemptPendingSelection() {
        val pending = pendingSelectionSerial ?: return
        val available = buildList {
            targetAddress?.let { add(it) }
            addAll(comboDevices.map(ComboDeviceInfo::deviceSerial))
        }
        if (available.any { it.equals(pending, ignoreCase = true) }) {
        updateSelectedDevice(pending, shouldReconnect = true)
        pendingSelectionSerial = null
    }
    }

    fun selectComboDevice(serial: String?) {
        val sanitized = serial ?: targetAddress
        if (sanitized.isNullOrBlank()) return
        pendingSelectionSerial = null
        // 切换卡片不再断开其他设备，仅切换选中设备并重连到目标
        updateSelectedDevice(sanitized, shouldReconnect = true)
    }

    fun handleComboResult(serial: String?) {
        pendingSelectionSerial = serial
        attemptPendingSelection()
    }

    fun removeAttachedDevice(serial: String) {
        if (serial.isBlank()) return
        val updated = comboDevices.filterNot { device ->
            device.deviceSerial.equals(serial, ignoreCase = true)
        }
        if (updated.size == comboDevices.size) {
            postTransientMessage(appContext.getString(R.string.device_combo_manage_error_not_found))
            return
        }
        modifyComboDevices(
            updatedDevices = updated
        ) {
            bluetoothService.disconnect(serial)
            if (serial.equals(selectedDeviceSerial, ignoreCase = true)) {
                val fallback = targetAddress ?: updated.firstOrNull()?.deviceSerial
                if (fallback != null) {
                    updateSelectedDevice(fallback, shouldReconnect = true)
                } else {
                    bluetoothService.disconnect()
                    selectedDeviceSerial = null
                    buildDeviceCards()
                }
            }
        }
    }

    fun renameAttachedDevice(serial: String, newName: String) {
        if (serial.isBlank()) return
        val updated = comboDevices.map { device ->
            if (device.deviceSerial.equals(serial, ignoreCase = true)) {
                device.copy(nameAlias = newName)
            } else {
                device
            }
        }
        if (updated == comboDevices) {
            postTransientMessage(appContext.getString(R.string.device_combo_manage_error_not_found))
            return
        }
        modifyComboDevices(
            updatedDevices = updated
        )
    }

    private fun updateSelectedDevice(serial: String, shouldReconnect: Boolean) {
        if (isSerialSelected(serial)) return
        cacheCurrentSelectedState()
        selectedDeviceSerial = serial
        val resolvedName = resolveDisplayName(
            connectionName = _uiState.value.deviceName,
            currentName = _uiState.value.deviceName,
            preferred = preferredNameFor(serial),
            address = serial
        )
        _uiState.update {
            it.copy(
                selectedDeviceSerial = serial,
                deviceAddress = serial,
                deviceName = resolvedName
            )
        }
        bluetoothService.setPreferredAddress(serial)
        refreshConnectionState(serial)
        applyCachedState(serial)
        buildDeviceCards()
        if (shouldReconnect) {
            val existing = bluetoothService.connectionStates.value[serial]
            val alreadyActive = existing?.status == BleConnectionState.Status.Connected ||
                existing?.status == BleConnectionState.Status.Connecting
            if (!alreadyActive) {
                bluetoothService.connect(serial)
            }
        }
    }

    private fun isSerialSelected(serial: String?): Boolean =
        serial != null && selectedDeviceSerial?.equals(serial, ignoreCase = true) == true

    private fun preferredNameFor(address: String?): String? {
        if (address.isNullOrBlank()) return preferredName
        return if (address.equals(targetAddress, ignoreCase = true)) {
            preferredName
        } else {
            comboDevices.firstOrNull { it.deviceSerial.equals(address, ignoreCase = true) }?.nameAlias
        }
    }

    private fun cacheCurrentSelectedState() {
        val address = selectedDeviceSerial ?: return
        cacheStateFor(address, _uiState.value)
    }

    private fun cacheStateFor(address: String, state: DeviceControlUiState) {
        deviceUiCache[address] = DeviceSnapshot(
            mode = state.mode,
            level = state.level,
            zone = state.zone,
            timerMinutes = state.timerMinutes,
            remainingSeconds = state.remainingSeconds,
            isRunning = state.isRunning,
            isMuted = state.isMuted,
            batteryPercent = state.batteryPercent,
            userTimerMinutes = userTimerOverrides[address],
            telemetryRemainingSeconds = lastTelemetryRemainingSeconds[address]
        )
    }

    private fun applyCachedState(address: String) {
        val cached = deviceUiCache[address]
        val status = _uiState.value.deviceStatuses[address]
        val defaultBattery = status?.batteryPercent ?: -1
        if (cached != null) {
            cached.userTimerMinutes?.let { userTimerOverrides[address] = it }
            cached.telemetryRemainingSeconds?.let { lastTelemetryRemainingSeconds[address] = it }
            val (resolvedTimer, resolvedRemaining) = resolveTimerDisplay(
                address = address,
                incomingTimerMinutes = cached.timerMinutes,
                incomingRemainingSeconds = cached.remainingSeconds,
                isRunning = cached.isRunning,
                localRemainingSeconds = cached.remainingSeconds
            )
            _uiState.update {
                it.copy(
                    mode = cached.mode,
                    level = cached.level,
                    zone = cached.zone,
                    timerMinutes = resolvedTimer,
                    remainingSeconds = resolvedRemaining,
                    isRunning = cached.isRunning,
                    isMuted = cached.isMuted,
                    batteryPercent = cached.batteryPercent
                )
            }
        } else {
            val (resolvedTimer, resolvedRemaining) = resolveTimerDisplay(
                address = address,
                incomingTimerMinutes = DEFAULT_TIMER_MINUTES,
                incomingRemainingSeconds = lastTelemetryRemainingSeconds[address],
                isRunning = false
            )
            _uiState.update {
                it.copy(
                    mode = 0,
                    level = 0,
                    zone = BodyZone.SHOULDER,
                    timerMinutes = resolvedTimer,
                    remainingSeconds = resolvedRemaining,
                    isRunning = false,
                    isMuted = false,
                    batteryPercent = defaultBattery
                )
            }
        }
    }

    private fun refreshConnectionState(address: String?) {
        if (address.isNullOrBlank()) {
            _uiState.update { it.copy(isConnected = false, isProtocolReady = false, isConnecting = false) }
            return
        }
        val connection = bluetoothService.connectionStates.value[address]
        val isConnected = connection?.status == BleConnectionState.Status.Connected && connection.isProtocolReady
        val isConnecting = connection?.status == BleConnectionState.Status.Connecting ||
            (connection?.status == BleConnectionState.Status.Connected && connection?.isProtocolReady == false)
        _uiState.update {
            it.copy(
                isConnected = isConnected,
                isConnecting = isConnecting,
                isProtocolReady = connection?.isProtocolReady ?: false
            )
        }
    }

    private fun modifyComboDevices(
        updatedDevices: List<ComboDeviceInfo>,
        afterSuccess: () -> Unit = {}
    ) {
        val deviceId = mainDeviceId ?: run {
            postTransientMessage(appContext.getString(R.string.device_combo_manage_error_not_found))
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isComboUpdating = true) }
            val payload = ComboInfoPayload(updatedDevices)
            val json = payload.toJson()
            val result = updateDeviceComboInfoUseCase(deviceId, json)
            result.onSuccess {
                comboInfoRaw = json
                comboDevices = updatedDevices
                buildDeviceCards()
                afterSuccess()
            }.onFailure { throwable ->
                postTransientMessage(
                    throwable.message
                        ?: appContext.getString(R.string.device_combo_manage_error_not_found)
                )
            }
            _uiState.update { it.copy(isComboUpdating = false) }
        }
    }

    private fun postTransientMessage(message: String?) {
        if (message.isNullOrBlank()) return
        _uiState.update { it.copy(transientMessage = message) }
    }

    private fun handleRemoteLevelCommand(level: Int, targetAddress: String?) {
        val snapshot = _uiState.value
        val isRemoteStart = level > 0 && snapshot.level == 0
        val isRemoteStop = level == 0
        if (!isRemoteStart && !isRemoteStop) return

        val resolvedAddress = targetAddress ?: snapshot.selectedDeviceSerial ?: snapshot.deviceAddress
        resolvedAddress?.let { addr -> maybeRequestHeartbeat(addr, force = true) }

        if (isRemoteStart) {
            _uiState.update { state ->
                val shouldSeedTimer = state.remainingSeconds <= 0
                val seededRemaining = if (shouldSeedTimer) max(1, state.timerMinutes) * 60 else state.remainingSeconds
                state.copy(
                    isRunning = true,
                    remainingSeconds = seededRemaining
                )
            }
        } else {
            stopCountdownTimer()
            _uiState.update { state ->
                state.copy(
                    isRunning = false,
                    level = MIN_LEVEL
                )
            }
        }
    }

    private fun applyTelemetry(address: String?, telemetry: DeviceTelemetry) {
        Log.d(tag, "applyTelemetry: address=$address telemetry=$telemetry")
        val targetAddress = telemetry.address ?: address ?: return
        val applyToSelected = targetAddress.equals(_uiState.value.selectedDeviceSerial, ignoreCase = true)
        val remoteLevelChange = telemetry.message as? DeviceTelemetryMessage.RemoteLevelChanged
        if (applyToSelected && remoteLevelChange != null) {
            handleRemoteLevelCommand(remoteLevelChange.level, targetAddress)
        }
        telemetry.remainingSeconds?.let { remaining ->
            lastTelemetryRemainingSeconds[targetAddress] = remaining.coerceAtLeast(0)
        }
        if (applyToSelected && awaitingStopAck && telemetry.isRunning == true) {
            Log.v(tag, "applyTelemetry: awaiting stop ack, ignoring running telemetry")
            return
        }
        if (applyToSelected && awaitingStopAck && telemetry.isRunning == false) {
            awaitingStopAck = false
        }
        _uiState.update { state ->
            var next = state
            val resolvedAddress = targetAddress ?: state.selectedDeviceSerial ?: state.deviceAddress
            if (!resolvedAddress.isNullOrBlank()) {
                val currentStatus = state.deviceStatuses[resolvedAddress] ?: DeviceStatus(address = resolvedAddress)
                val updatedStatus = currentStatus.copy(
                    deviceName = currentStatus.deviceName ?: state.deviceName,
                    isRunning = telemetry.isRunning ?: currentStatus.isRunning,
                    batteryPercent = telemetry.batteryPercent ?: currentStatus.batteryPercent,
                    lastTelemetryAtMillis = System.currentTimeMillis()
                )
                next = next.copy(
                    deviceStatuses = next.deviceStatuses + (resolvedAddress to updatedStatus)
                )
            }

            if (!applyToSelected) return@update next

            telemetry.isRunning?.let { next = next.copy(isRunning = it) }

            val shouldUpdateControls = next.isRunning || telemetry.isRunning == true
            if (telemetry.isRunning == false && (telemetry.remainingSeconds ?: 0) <= 0) {
                // Heartbeat shows session ended; force level to 0.
                next = next.copy(level = MIN_LEVEL)
            }
            if (shouldUpdateControls) {
                telemetry.zone?.let { next = next.copy(zone = BodyZone.fromIndex(it)) }
                telemetry.mode?.let { next = next.copy(mode = it.coerceIn(0, 7)) }
                telemetry.level?.let { level ->
                    if (shouldApplyTelemetryLevel(telemetry)) {
                        next = next.copy(level = level.coerceIn(MIN_LEVEL, MAX_LEVEL))
                    }
                }
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

            if (applyToSelected) {
                val addressKey = targetAddress
                val currentRemaining = next.remainingSeconds
                val remoteRemaining = telemetry.remainingSeconds
                val shouldResyncToHeartbeat = next.isRunning &&
                    remoteRemaining != null &&
                    abs(remoteRemaining - currentRemaining) > 3
                val (resolvedTimer, resolvedRemaining) = resolveTimerDisplay(
                    address = addressKey,
                    incomingTimerMinutes = telemetry.timerMinutes,
                    incomingRemainingSeconds = telemetry.remainingSeconds,
                    isRunning = next.isRunning,
                    localRemainingSeconds = currentRemaining
                )
                if (shouldResyncToHeartbeat) {
                    refreshCountdownAnchor()
                }
                next = next.copy(timerMinutes = resolvedTimer, remainingSeconds = resolvedRemaining)
            }
            next
        }
        // 缓存选中设备的最新 UI 状态，便于切换时恢复
        val addressKey = targetAddress ?: _uiState.value.selectedDeviceSerial
        if (applyToSelected && addressKey != null) {
            cacheStateFor(addressKey, _uiState.value)
        }
    }

    private fun shouldApplyTelemetryLevel(telemetry: DeviceTelemetry): Boolean {
        val isRemoteLevelChange = telemetry.message is DeviceTelemetryMessage.RemoteLevelChanged
        if (isRemoteLevelChange) return true
        val isHeartbeatLevel = telemetry.rawMessage is EmsV2ProtocolAdapter.EmsV2Message.Heartbeat
        if (!isHeartbeatLevel) return true
        val lastSetAt = lastLocalLevelSetAtMillis
        if (lastSetAt <= 0) return true
        val now = System.currentTimeMillis()
        val withinWindow = now - lastSetAt < LEVEL_HEARTBEAT_SUPPRESSION_WINDOW_MS
        if (withinWindow) {
            Log.v(tag, "shouldApplyTelemetryLevel: suppress heartbeat level within window")
        }
        return !withinWindow
    }

    private fun resolveTimerDisplay(
        address: String?,
        incomingTimerMinutes: Int?,
        incomingRemainingSeconds: Int?,
        isRunning: Boolean,
        localRemainingSeconds: Int? = null
    ): Pair<Int, Int> {
        val userTimer = address?.let { userTimerOverrides[it] }
        val telemetryRemaining = address?.let { lastTelemetryRemainingSeconds[it] }
        return if (isRunning) {
            val incomingSafe = incomingRemainingSeconds?.coerceAtLeast(0)
            val cachedIncoming = telemetryRemaining?.coerceAtLeast(0)
            val localSafe = localRemainingSeconds?.coerceAtLeast(0)
            val remoteCandidate = incomingSafe ?: cachedIncoming.takeIf { localSafe == null }
            val baseRemaining = when {
                remoteCandidate != null && localSafe != null && abs(remoteCandidate - localSafe) > 3 -> remoteCandidate
                localSafe != null -> localSafe
                remoteCandidate != null -> remoteCandidate
                else -> ((incomingTimerMinutes ?: DEFAULT_TIMER_MINUTES) * 60).coerceAtLeast(0)
            }
            val minutes = max(1, (baseRemaining + 59) / 60)
            minutes to baseRemaining
        } else {
            val preferredRemaining = when {
                incomingRemainingSeconds != null -> incomingRemainingSeconds.coerceAtLeast(0)
                localRemainingSeconds != null -> localRemainingSeconds.coerceAtLeast(0)
                telemetryRemaining != null -> telemetryRemaining.coerceAtLeast(0)
                else -> null
            }
            val minutes = when {
                userTimer != null -> userTimer
                preferredRemaining != null -> (preferredRemaining + 59) / 60
                incomingTimerMinutes != null && incomingTimerMinutes > 0 -> incomingTimerMinutes
                else -> DEFAULT_TIMER_MINUTES
            }
            val resolvedRemaining = preferredRemaining
                ?: localRemainingSeconds?.coerceAtLeast(0)
                ?: telemetryRemaining?.coerceAtLeast(0)
                ?: 0
            minutes to resolvedRemaining
        }
    }

    private fun stopSession(userInitiated: Boolean) {
        val current = _uiState.value
        if (!current.isRunning && !userInitiated) return
        viewModelScope.launch {
            val success = withSession { session -> session.selectLevel(MIN_LEVEL) }
            _uiState.update {
                it.copy(
                    isRunning = false,
                    // 点击停止将强度设为 0，且保留当前剩余时间。
                    level = MIN_LEVEL,
                    remainingSeconds = current.remainingSeconds,
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
            withSession { session ->
                session.requestHeartbeat()
            }
        }
    }

    private fun resolveDisplayName(
        connectionName: String?,
        currentName: String,
        preferred: String?,
        address: String?
    ): String {
        val candidates = listOf(
            preferred,
            currentName,
            connectionName,
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
        val address = selectedDeviceSerial ?: targetAddress ?: return
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
    val showConnectingOverlay: Boolean = false,
    val selectedDeviceSerial: String? = null,
    val deviceCards: List<DeviceCardState> = emptyList(),
    val deviceStatuses: Map<String, DeviceStatus> = emptyMap(),
    val isComboUpdating: Boolean = false,
    val transientMessage: String? = null
) {
    val availableTimerOptions: List<Int> = listOf(5, 10, 15, 20, 30, 45, 60)
}

data class DeviceCardState(
    val deviceSerial: String?,
    val displayName: String,
    val isSelected: Boolean,
    val isMainDevice: Boolean
)

data class DeviceStatus(
    val address: String,
    val deviceName: String? = null,
    val connectionStatus: BleConnectionState.Status = BleConnectionState.Status.Idle,
    val isProtocolReady: Boolean = false,
    val isRunning: Boolean = false,
    val batteryPercent: Int = -1,
    val lastTelemetryAtMillis: Long = 0L
) {
    val isConnected: Boolean
        get() = connectionStatus == BleConnectionState.Status.Connected && isProtocolReady
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

private data class DeviceSnapshot(
    val mode: Int,
    val level: Int,
    val zone: BodyZone,
    val timerMinutes: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean,
    val isMuted: Boolean,
    val batteryPercent: Int,
    val userTimerMinutes: Int?,
    val telemetryRemainingSeconds: Int?
)
