package com.massager.app.presentation.home

// 文件说明：汇总首页仪表盘的设备、测量数据加载与刷新逻辑。
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massager.app.R
import com.massager.app.data.local.SessionManager
import com.massager.app.data.bluetooth.MassagerBluetoothService
import com.massager.app.data.bluetooth.scan.BleScanCoordinator
import com.massager.app.domain.model.DeviceMetadata
import com.massager.app.domain.model.TemperatureRecord
import com.massager.app.domain.usecase.device.ObserveDevicesUseCase
import com.massager.app.domain.usecase.device.RefreshDevicesUseCase
import com.massager.app.domain.usecase.device.RemoveDeviceUseCase
import com.massager.app.domain.usecase.device.RenameDeviceUseCase
import com.massager.app.domain.usecase.measurement.ObserveMeasurementsUseCase
import com.massager.app.domain.usecase.measurement.RefreshMeasurementsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

private const val AUTO_REFRESH_COOLDOWN_MS = 15_000L
private const val MANUAL_REFRESH_DEBOUNCE_MS = 1_200L

data class HomeUiState(
    val devices: List<DeviceMetadata> = emptyList(),
    val measurements: List<TemperatureRecord> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorMessageRes: Int? = null,
    val errorMessageText: String? = null,
    val selectedDeviceIds: Set<String> = emptySet(),
    val suppressOverlay: Boolean = false,
    val isRenameDialogVisible: Boolean = false,
    val renameInput: String = "",
    val renameInputError: Int? = null,
    val isRemoveDialogVisible: Boolean = false,
    val isActionInProgress: Boolean = false,
    val isScanningOnline: Boolean = false,
    val lastCheckedAt: Long? = null,
    val onlineStatus: Map<String, Boolean> = emptyMap()
) {
    val isManagementActive: Boolean
        get() = selectedDeviceIds.isNotEmpty()
}

sealed interface HomeEffect {
    data class ShowMessage(@StringRes val messageRes: Int) : HomeEffect
    data class ShowMessageText(val message: String) : HomeEffect
}

enum class RefreshTrigger {
    Initial,
    User,
    AutoResume,
    DeviceAdded
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeDevicesUseCase: ObserveDevicesUseCase,
    observeMeasurementsUseCase: ObserveMeasurementsUseCase,
    private val refreshDevicesUseCase: RefreshDevicesUseCase,
    private val refreshMeasurementsUseCase: RefreshMeasurementsUseCase,
    private val renameDeviceUseCase: RenameDeviceUseCase,
    private val removeDeviceUseCase: RemoveDeviceUseCase,
    private val sessionManager: SessionManager,
    private val bluetoothService: MassagerBluetoothService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<HomeEffect>()
    val effects: SharedFlow<HomeEffect> = _effects.asSharedFlow()

    private var guestEntryPromptShown = false
    private var guestSyncPromptShown = false
    private var lastOnlineStatusCache: Map<String, Boolean> = emptyMap()
    private var lastRefreshTimestamp: Long = 0L
    private var lastManualRefreshTimestamp: Long = 0L
    private var manualRefreshJob: Job? = null
    private var initialRefreshDone = false

    init {
        viewModelScope.launch {
            observeDevicesUseCase().collect { devices ->
                _uiState.update { state ->
                    val availableIds = devices.map { it.id }.toSet()
                    val filteredSelection = state.selectedDeviceIds.filter { availableIds.contains(it) }.toSet()
                    val renameInput = if (state.isRenameDialogVisible) {
                        filteredSelection.firstOrNull()?.let { id ->
                            devices.firstOrNull { it.id == id }?.name ?: ""
                        } ?: ""
                    } else {
                        state.renameInput
                    }
                    state.copy(
                        devices = devices,
                        selectedDeviceIds = filteredSelection,
                        isRenameDialogVisible = state.isRenameDialogVisible && filteredSelection.isNotEmpty(),
                        isRemoveDialogVisible = state.isRemoveDialogVisible && filteredSelection.isNotEmpty(),
                        renameInput = if (state.isRenameDialogVisible) renameInput else state.renameInput,
                        renameInputError = if (filteredSelection.isEmpty()) null else state.renameInputError
                    )
                }
            }
        }
        if (sessionManager.isGuestMode()) {
            guestEntryPromptShown = true
            emitMessageRes(R.string.guest_mode_entry_notice)
        }
        viewModelScope.launch {
            observeMeasurementsUseCase().collect { measurements ->
                _uiState.update { it.copy(measurements = measurements) }
            }
        }
        refreshAll(RefreshTrigger.Initial)
    }

    fun refreshAll(trigger: RefreshTrigger = RefreshTrigger.AutoResume) {
        val now = System.currentTimeMillis()
        when (trigger) {
            RefreshTrigger.Initial -> {
                if (initialRefreshDone) return
                initialRefreshDone = true
                performRefresh(RefreshTrigger.Initial)
            }
            RefreshTrigger.User -> {
                val elapsedSinceManual = if (lastManualRefreshTimestamp == 0L) {
                    Long.MAX_VALUE
                } else {
                    now - lastManualRefreshTimestamp
                }
                if (elapsedSinceManual < MANUAL_REFRESH_DEBOUNCE_MS) {
                    manualRefreshJob?.cancel()
                    manualRefreshJob = viewModelScope.launch {
                        delay(MANUAL_REFRESH_DEBOUNCE_MS - elapsedSinceManual)
                        performRefresh(RefreshTrigger.User)
                    }
                } else {
                    performRefresh(RefreshTrigger.User)
                }
            }

            RefreshTrigger.AutoResume -> {
                if (now - lastRefreshTimestamp < AUTO_REFRESH_COOLDOWN_MS) return
                performRefresh(RefreshTrigger.AutoResume)
            }

            RefreshTrigger.DeviceAdded -> {
                performRefresh(trigger)
            }
        }
    }

    private fun performRefresh(trigger: RefreshTrigger) {
        if (_uiState.value.isRefreshing) return
        val now = System.currentTimeMillis()
        lastRefreshTimestamp = now
        if (trigger == RefreshTrigger.User) {
            lastManualRefreshTimestamp = now
        }
        refresh(
            refreshOnlineStatus = true,
            isUserInitiated = trigger == RefreshTrigger.User
        )
    }

    fun refresh(refreshOnlineStatus: Boolean = false, isUserInitiated: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessageRes = null, errorMessageText = null) }
            if (sessionManager.isGuestMode() && !guestSyncPromptShown) {
                emitMessageRes(R.string.guest_mode_cloud_restricted)
                guestSyncPromptShown = true
            }
            val deviceResult = refreshDevicesUseCase()
            val devices = deviceResult.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessageText = error.message,
                        errorMessageRes = error.message?.let { null } ?: R.string.home_error_load_devices
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(devices = devices) }

            val deviceId = devices.firstOrNull()?.id
                ?: _uiState.value.devices.firstOrNull()?.id

            if (deviceId != null) {
                refreshMeasurementsUseCase(deviceId).getOrElse { error ->
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessageText = error.message,
                            errorMessageRes = error.message?.let { null } ?: R.string.home_error_load_measurements
                        )
                    }
                    return@launch
                }
            }

            _uiState.update { it.copy(isRefreshing = false) }
            if (refreshOnlineStatus) {
                checkOnlineStatus(isUserInitiated)
            }
        }
    }

    fun checkOnlineStatus(isUserInitiated: Boolean = false) {
        if (_uiState.value.isScanningOnline) return
        val devicesSnapshot = _uiState.value.devices
        if (devicesSnapshot.isEmpty()) return
        viewModelScope.launch {
            val targetMap = devicesSnapshot.mapNotNull { device ->
                device.macAddress?.lowercase()?.takeIf { it.isNotBlank() }?.let { addr -> addr to device.id }
            }.toMap()
            if (targetMap.isEmpty()) {
                _uiState.update {
                    it.copy(
                        onlineStatus = emptyMap(),
                        isScanningOnline = false,
                        lastCheckedAt = System.currentTimeMillis()
                    )
                }
                return@launch
            }
            _uiState.update {
                it.copy(
                    isScanningOnline = true,
                    lastCheckedAt = null,
                    // 清空当前 UI 状态，等待扫描结果逐步填充
                    onlineStatus = emptyMap()
                )
            }
            val scanStartResult = bluetoothService.restartScan()
            if (scanStartResult is BleScanCoordinator.ScanStartResult.Error) {
                _uiState.update {
                    it.copy(
                        isScanningOnline = false,
                        lastCheckedAt = System.currentTimeMillis(),
                        onlineStatus = lastOnlineStatusCache
                    )
                }
                return@launch
            }
            val found = mutableSetOf<String>()
            var sawAnyAdvertisements = false
            val scanJob = launch {
                bluetoothService.scanResults.collect { results ->
                    if (results.isNotEmpty()) sawAnyAdvertisements = true
                    results.forEach { scan ->
                        val address = scan.address?.lowercase() ?: return@forEach
                        targetMap[address]?.let(found::add)
                    }
                    // 仅更新已匹配设备，未匹配的保持空白
                    val interimStatus: Map<String, Boolean> = found.associateWith { true }
                    _uiState.update { it.copy(onlineStatus = interimStatus) }
                    if (found.size >= targetMap.size) this.cancel()
                }
            }
            withTimeoutOrNull(5_000L) { scanJob.join() }
            if (scanJob.isActive) scanJob.cancel()
            bluetoothService.stopScan()
            val statusMap = when {
                sawAnyAdvertisements -> devicesSnapshot.associate { device ->
                    device.id to found.contains(device.id) // 扫描结束后未匹配到的统一标记为离线
                }
                lastOnlineStatusCache.isNotEmpty() -> devicesSnapshot.associate { device ->
                    device.id to (lastOnlineStatusCache[device.id] ?: false)
                }
                else -> devicesSnapshot.associate { device ->
                    device.id to false
                }
            }
            lastOnlineStatusCache = statusMap
            _uiState.update {
                it.copy(
                    isScanningOnline = false,
                    lastCheckedAt = System.currentTimeMillis(),
                    onlineStatus = statusMap
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessageRes = null, errorMessageText = null) }
    }

    private fun emitMessageText(message: String) = enqueueMessage(message)

    fun selectSingleDevice(deviceId: String) {
        if (deviceId.isBlank()) return
        _uiState.update { state ->
            state.copy(
                selectedDeviceIds = setOf(deviceId),
                suppressOverlay = true,
                renameInputError = null,
                isRenameDialogVisible = false,
                isRemoveDialogVisible = false
            )
        }
    }

    fun toggleDeviceSelection(deviceId: String) {
        _uiState.update { state ->
            val updatedSelection = state.selectedDeviceIds.toMutableSet().also { set ->
                if (!set.add(deviceId)) {
                    set.remove(deviceId)
                }
            }.toSet()
            state.copy(
                selectedDeviceIds = updatedSelection,
                suppressOverlay = false,
                renameInputError = null,
                isRenameDialogVisible = state.isRenameDialogVisible && updatedSelection.isNotEmpty(),
                isRemoveDialogVisible = state.isRemoveDialogVisible && updatedSelection.isNotEmpty()
            )
        }
    }

    fun cancelManagement() {
        _uiState.update {
            it.copy(
                selectedDeviceIds = emptySet(),
                suppressOverlay = false,
                isRenameDialogVisible = false,
                isRemoveDialogVisible = false,
                renameInput = "",
                renameInputError = null
            )
        }
    }

    fun showRenameDialog() {
        val state = _uiState.value
        when {
            state.selectedDeviceIds.isEmpty() -> emitMessageRes(R.string.home_management_select_device)
            state.selectedDeviceIds.size > 1 -> emitMessageRes(R.string.home_management_rename_single_hint)
            else -> {
                val deviceId = state.selectedDeviceIds.first()
                val device = state.devices.firstOrNull { it.id == deviceId }
                if (device == null) {
                    emitMessageRes(R.string.home_management_select_device)
                    return
                }
                _uiState.update {
                    it.copy(
                        isRenameDialogVisible = true,
                        renameInput = device.name,
                        renameInputError = null
                    )
                }
            }
        }
    }

    fun dismissRenameDialog() {
        _uiState.update {
            it.copy(
                isRenameDialogVisible = false,
                renameInput = "",
                renameInputError = null
            )
        }
    }

    fun onRenameInputChanged(value: String) {
        _uiState.update {
            it.copy(
                renameInput = value,
                renameInputError = null
            )
        }
    }

    fun confirmRename() {
        val state = _uiState.value
        if (!state.isRenameDialogVisible || state.isActionInProgress) return
        val deviceId = state.selectedDeviceIds.firstOrNull()
        if (deviceId == null) {
            emitMessageRes(R.string.home_management_select_device)
            return
        }
        val trimmedName = state.renameInput.trim()
        val validationError = validateRenameInput(trimmedName)
        if (validationError != null) {
            _uiState.update { it.copy(renameInputError = validationError) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            val result = renameDeviceUseCase(deviceId, trimmedName)
            result.onSuccess {
                _uiState.update { current ->
                    current.copy(
                        isActionInProgress = false,
                        isRenameDialogVisible = false,
                        renameInput = "",
                        renameInputError = null,
                        selectedDeviceIds = emptySet(),
                        suppressOverlay = false
                    )
                }
            }.onFailure { throwable ->
                val message = throwable.message
                if (message.isNullOrBlank()) {
                    emitMessageRes(R.string.home_error_rename_device)
                } else {
                    emitMessageText(message)
                }
                _uiState.update { current ->
                    current.copy(isActionInProgress = false)
                }
            }
        }
    }

    fun showRemoveDialog() {
        if (_uiState.value.selectedDeviceIds.isEmpty()) {
            emitMessageRes(R.string.home_management_select_device)
        } else {
            _uiState.update { it.copy(isRemoveDialogVisible = true) }
        }
    }

    fun dismissRemoveDialog() {
        _uiState.update { it.copy(isRemoveDialogVisible = false) }
    }

    fun confirmRemove() {
        val state = _uiState.value
        if (!state.isRemoveDialogVisible || state.isActionInProgress) return
        val ids = state.selectedDeviceIds
        if (ids.isEmpty()) {
            emitMessageRes(R.string.home_management_select_device)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            var failure: Throwable? = null
            for (id in ids) {
                val result = removeDeviceUseCase(id)
                if (result.isFailure) {
                    failure = result.exceptionOrNull()
                    break
                }
            }
            if (failure == null) {
                _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        isRemoveDialogVisible = false,
                        selectedDeviceIds = emptySet(),
                        suppressOverlay = false
                    )
                }
            } else {
                val message = failure?.message
                if (message.isNullOrBlank()) {
                    emitMessageRes(R.string.home_error_remove_device)
                } else {
                    emitMessageText(message)
                }
                _uiState.update { it.copy(isActionInProgress = false) }
            }
        }
    }

    private fun validateRenameInput(value: String): Int? {
        if (value.length !in 2..30) return R.string.rename_error_length
        val regex = Regex("^[A-Za-z0-9\\s]+\$")
        return if (!regex.matches(value)) R.string.rename_error_invalid else null
    }

    private fun emitMessageRes(@StringRes resId: Int) {
        viewModelScope.launch {
            _effects.emit(HomeEffect.ShowMessage(resId))
        }
    }

    private fun emitMessageTextInternal(message: String) {
        viewModelScope.launch {
            _effects.emit(HomeEffect.ShowMessageText(message))
        }
    }

    private fun enqueueMessage(message: String) {
        emitMessageTextInternal(message)
    }
}
