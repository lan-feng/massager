package com.massager.app.presentation.device

// 文件说明：添加设备流程的状态与提交逻辑。
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massager.app.data.bluetooth.BleConnectionState
import com.massager.app.data.bluetooth.MassagerBluetoothService
import com.massager.app.data.bluetooth.scan.BleScanCoordinator
import com.massager.app.domain.usecase.device.BindDeviceUseCase
import com.massager.app.core.logging.logTag
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class NearbyDevice(
    val id: String,
    val name: String,
    val macAddress: String,
    val signalStrength: Int,
    val isConnected: Boolean,
    val productId: Int? = null,
    val firmwareVersion: String? = null,
    val uniqueId: String? = null
)

data class AddDeviceUiState(
    val isScanning: Boolean = false,
    val devices: List<NearbyDevice> = emptyList(),
    val showEmpty: Boolean = false,
    val connectingDeviceId: String? = null,
    val statusMessageRes: Int? = null,
    val errorMessage: String? = null
)

sealed interface AddDeviceEffect {
    data object NavigateHome : AddDeviceEffect
    data class ShowError(val message: String) : AddDeviceEffect
    data object RequestPermissions : AddDeviceEffect
}

@HiltViewModel
class AddDeviceViewModel @Inject constructor(
    private val bindDeviceUseCase: BindDeviceUseCase,
    private val bluetoothService: MassagerBluetoothService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDeviceUiState())
    val uiState: StateFlow<AddDeviceUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AddDeviceEffect>()
    val effects: SharedFlow<AddDeviceEffect> = _effects.asSharedFlow()

    private var emptyStateJob: Job? = null

    init {
        observeBluetoothState()
    }

    fun startScan() {
        Log.d(TAG, "startScan: resetting UI state and requesting BLE scan")
        val result = bluetoothService.restartScan()
        when (result) {
            is BleScanCoordinator.ScanStartResult.Started -> {
                _uiState.value = AddDeviceUiState(isScanning = true)
                scheduleEmptyStateCheck()
            }
            is BleScanCoordinator.ScanStartResult.Error -> {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        errorMessage = result.message,
                        showEmpty = false
                    )
                }
                // Trigger permission request if it's a permission-related failure.
                if (result.message.contains("permission", ignoreCase = true)) {
                    viewModelScope.launch { _effects.emit(AddDeviceEffect.RequestPermissions) }
                }
            }
        }
    }

    fun retryScan() {
        Log.d(TAG, "retryScan: user requested scan retry")
        startScan()
    }

    fun connectToDevice(device: NearbyDevice) {
        if (_uiState.value.connectingDeviceId != null) return
        viewModelScope.launch {
            Log.d(TAG, "connectToDevice: attempting connection to ${device.macAddress} (${device.name})")
            _uiState.update {
                it.copy(
                    connectingDeviceId = device.id,
                    statusMessageRes = com.massager.app.R.string.connecting,
                    errorMessage = null
                )
            }
            val connectStarted = bluetoothService.connect(device.macAddress)
            if (!connectStarted) {
                val error = "Unable to initiate connection"
                bluetoothService.restartScan()
                Log.w(TAG, "connectToDevice: failed to initiate connection for ${device.macAddress}")
                _uiState.update {
                    it.copy(
                        connectingDeviceId = null,
                        statusMessageRes = null,
                        errorMessage = error,
                        showEmpty = false
                    )
                }
                _effects.emit(AddDeviceEffect.ShowError(error))
                scheduleEmptyStateCheck()
                return@launch
            }
            val connectionOutcome = withTimeoutOrNull(CONNECTION_TIMEOUT_MS) {
                bluetoothService.connectionState.first { state ->
                    state.deviceAddress == device.macAddress && when (state.status) {
                        BleConnectionState.Status.Connected,
                        BleConnectionState.Status.Disconnected,
                        BleConnectionState.Status.Failed -> true
                        else -> false
                    }
                }
            }
            if (connectionOutcome == null || connectionOutcome.status != BleConnectionState.Status.Connected) {
                val reason = connectionOutcome?.errorMessage ?: "Connection failed"
                bluetoothService.restartScan()
                Log.w(
                    TAG,
                    "connectToDevice: connection unsuccessful for ${device.macAddress}, reason=$reason"
                )
                _uiState.update {
                    it.copy(
                        connectingDeviceId = null,
                        statusMessageRes = null,
                        errorMessage = reason,
                        showEmpty = false
                    )
                }
                _effects.emit(AddDeviceEffect.ShowError(reason))
                scheduleEmptyStateCheck()
                return@launch
            }
            val result = bindDeviceUseCase(
                serial = device.macAddress,
                displayName = device.name,
                productId = device.productId,
                firmwareVersion = device.firmwareVersion,
                uniqueId = device.uniqueId
            )
            result.onSuccess {
                Log.d(TAG, "connectToDevice: bind succeeded for ${device.macAddress}")
                _uiState.update { state ->
                    state.copy(
                        connectingDeviceId = null,
                        statusMessageRes = com.massager.app.R.string.connected_success
                    )
                }
                delay(POST_CONNECTION_SUCCESS_DELAY_MS)
                _effects.emit(AddDeviceEffect.NavigateHome)
            }.onFailure { throwable ->
                Log.e(TAG, "connectToDevice: bind failed for ${device.macAddress}", throwable)
                _uiState.update {
                    it.copy(
                        connectingDeviceId = null,
                        statusMessageRes = null,
                        errorMessage = throwable.message ?: "Unable to connect"
                    )
                }
                _effects.emit(
                    AddDeviceEffect.ShowError(
                        throwable.message ?: "Unable to connect"
                    )
                )
            }
        }
    }

    private fun scheduleEmptyStateCheck() {
        emptyStateJob?.cancel()
        emptyStateJob = viewModelScope.launch {
            delay(EMPTY_STATE_DELAY_MS)
            Log.d(TAG, "scheduleEmptyStateCheck: evaluating empty state after delay")
            _uiState.update { state ->
                if (state.devices.isEmpty()) {
                    state.copy(isScanning = false, showEmpty = true)
                } else {
                    state
                }
            }
        }
    }

    fun clearStatusMessage() {
        Log.v(TAG, "clearStatusMessage: dismissing status message")
        _uiState.update { it.copy(statusMessageRes = null) }
    }

    fun clearError() {
        Log.v(TAG, "clearError: dismissing error message and clearing Bluetooth errors")
        bluetoothService.clearError()
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun observeBluetoothState() {
        viewModelScope.launch {
            combine(
                bluetoothService.scanResults,
                bluetoothService.connectionState
            ) { devices, state ->
                val mappedDevices = devices.map { result ->
                    NearbyDevice(
                        id = result.address,
                        name = result.name,
                        macAddress = result.address,
                        signalStrength = result.rssi,
                        isConnected = result.isConnected,
                        productId = result.productId,
                        firmwareVersion = result.firmwareVersion,
                        uniqueId = result.uniqueId
                    )
                }
                Triple(mappedDevices, state.status, state.errorMessage)
            }.collect { (devices, status, errorMessage) ->
                Log.d(
                    TAG,
                    "observeBluetoothState: devices=${devices.size}, status=$status, error=$errorMessage"
                )
                _uiState.update { current ->
                    val updated = current.copy(
                        devices = devices,
                        isScanning = status == BleConnectionState.Status.Scanning,
                        errorMessage = errorMessage ?: current.errorMessage,
                        showEmpty = if (devices.isNotEmpty()) false else current.showEmpty
                    )
                    updated
                }
            }
        }
    }

    companion object {
        private const val EMPTY_STATE_DELAY_MS = 10_000L
        private const val CONNECTION_TIMEOUT_MS = 15_000L
        private const val POST_CONNECTION_SUCCESS_DELAY_MS = 1_000L
        private val TAG = logTag("AddDeviceViewModel")
    }

    override fun onCleared() {
        Log.d(TAG, "onCleared: cancelling timers and stopping scan")
        emptyStateJob?.cancel()
        bluetoothService.stopScan()
        super.onCleared()
    }
}
