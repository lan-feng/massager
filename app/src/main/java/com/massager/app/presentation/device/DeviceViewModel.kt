package com.massager.app.presentation.device

// 文件说明：设备列表页的状态管理与刷新逻辑。
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massager.app.R
import com.massager.app.core.DeviceCatalog
import com.massager.app.data.bluetooth.BleConnectionState
import com.massager.app.data.bluetooth.MassagerBluetoothService
import com.massager.app.domain.model.ComboDeviceInfo
import com.massager.app.domain.model.ComboInfoSerializer
import com.massager.app.domain.usecase.device.BindDeviceUseCase
import com.massager.app.domain.usecase.device.GetDeviceComboInfoUseCase
import com.massager.app.domain.usecase.device.UpdateDeviceComboInfoUseCase
import com.massager.app.presentation.navigation.DeviceScanSource
import com.massager.app.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceListItem(
    val name: String,
    val address: String,
    val signalStrength: Int,
    val isConnected: Boolean,
    val productId: Int? = null,
    val firmwareVersion: String? = null,
    val uniqueId: String? = null
)

data class DeviceScanUiState(
    val devices: List<DeviceListItem> = emptyList(),
    val connectionState: BleConnectionState = BleConnectionState(),
    val isScanning: Boolean = false,
    val scanSource: DeviceScanSource = DeviceScanSource.HOME,
    val processingAddress: String? = null
)

sealed interface DeviceScanEffect {
    data object NavigateHome : DeviceScanEffect
    data class ReturnToControl(val deviceSerial: String) : DeviceScanEffect
    data class ShowMessage(
        @StringRes val messageRes: Int? = null,
        val message: String? = null
    ) : DeviceScanEffect
}

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val bluetoothService: MassagerBluetoothService,
    private val bindDeviceUseCase: BindDeviceUseCase,
    private val updateDeviceComboInfoUseCase: UpdateDeviceComboInfoUseCase,
    private val getDeviceComboInfoUseCase: GetDeviceComboInfoUseCase,
    private val deviceCatalog: DeviceCatalog,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val scanSource = savedStateHandle.get<String>(Screen.DeviceScan.ARG_SOURCE)
        ?.let { raw -> runCatching { DeviceScanSource.valueOf(raw) }.getOrNull() }
        ?: DeviceScanSource.HOME
    private val ownerDeviceId =
        savedStateHandle.get<String>(Screen.DeviceScan.ARG_OWNER_DEVICE_ID)?.takeIf { it.isNotBlank() }
    private val excludedSerials =
        savedStateHandle.get<String>(Screen.DeviceScan.ARG_EXCLUDED)
            ?.takeIf { it.isNotBlank() }
            ?.split("|")
            ?.mapNotNull { serial -> serial.takeIf { it.isNotBlank() }?.lowercase() }
            ?.toSet()
            ?: emptySet()

    private val _uiState = MutableStateFlow(DeviceScanUiState(scanSource = scanSource))
    val uiState: StateFlow<DeviceScanUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<DeviceScanEffect>()
    val effects: SharedFlow<DeviceScanEffect> = _effects.asSharedFlow()

    init {
        bluetoothService.startScan()
        observeBluetooth()
    }

    private fun observeBluetooth() {
        viewModelScope.launch {
            combine(
                bluetoothService.scanResults,
                bluetoothService.connectionState
            ) { devices, state ->
                val filtered = devices.filterNot { device ->
                    excludedSerials.contains(device.address.lowercase())
                }
                DeviceScanUiState(
                    devices = filtered.map { device ->
                        DeviceListItem(
                            name = device.name,
                            address = device.address,
                            signalStrength = device.rssi,
                            isConnected = device.isConnected,
                            productId = device.productId,
                            firmwareVersion = device.firmwareVersion,
                            uniqueId = device.uniqueId
                        )
                    },
                    connectionState = state,
                    isScanning = state.status == BleConnectionState.Status.Scanning,
                    scanSource = scanSource,
                    processingAddress = _uiState.value.processingAddress
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun toggleScan() {
        if (_uiState.value.isScanning) {
            bluetoothService.stopScan()
        } else {
            bluetoothService.startScan()
        }
    }

    fun refreshScan() {
        bluetoothService.restartScan()
    }

    fun onDeviceSelected(device: DeviceListItem) {
        if (_uiState.value.processingAddress != null) return
        when (scanSource) {
            DeviceScanSource.HOME -> bindDevice(device)
            DeviceScanSource.CONTROL -> addDeviceToCombo(device)
        }
    }

    fun clearErrorMessage() {
        bluetoothService.clearError()
        _uiState.update { it.copy(connectionState = it.connectionState.copy(errorMessage = null)) }
    }

    private fun bindDevice(device: DeviceListItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(processingAddress = device.address) }
            val result = bindDeviceUseCase(
                serial = device.address,
                displayName = device.name,
                productId = device.productId,
                firmwareVersion = device.firmwareVersion,
                uniqueId = device.uniqueId
            )
            result.onSuccess {
                _effects.emit(DeviceScanEffect.NavigateHome)
            }.onFailure { throwable ->
                _effects.emit(
                    DeviceScanEffect.ShowMessage(
                        message = throwable.message ?: "Unable to add device"
                    )
                )
            }
            _uiState.update { it.copy(processingAddress = null) }
        }
    }

    private fun addDeviceToCombo(device: DeviceListItem) {
        val ownerId = ownerDeviceId
        if (ownerId.isNullOrBlank()) {
            viewModelScope.launch {
                _effects.emit(
                    DeviceScanEffect.ShowMessage(
                        messageRes = R.string.device_scan_action_error_owner
                    )
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(processingAddress = device.address) }
            val current = getDeviceComboInfoUseCase(ownerId)
            val entry = ComboDeviceInfo(
                deviceSerial = device.address,
                deviceType = deviceCatalog.resolveType(device.productId, device.name),
                firmwareVersion = device.firmwareVersion,
                uniqueId = device.uniqueId,
                nameAlias = device.name
            )
            val payload = ComboInfoSerializer.append(current, entry).toJson()
            val result = updateDeviceComboInfoUseCase(ownerId, payload)
            result.onSuccess {
                _effects.emit(
                    DeviceScanEffect.ShowMessage(
                        messageRes = R.string.device_scan_combo_success
                    )
                )
                _effects.emit(DeviceScanEffect.ReturnToControl(device.address))
            }.onFailure { throwable ->
                _effects.emit(
                    DeviceScanEffect.ShowMessage(
                        message = throwable.message ?: "Unable to add device"
                    )
                )
            }
            _uiState.update { it.copy(processingAddress = null) }
        }
    }

    override fun onCleared() {
        bluetoothService.stopScan()
        super.onCleared()
    }
}
