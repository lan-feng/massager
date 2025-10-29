package com.massager.app.presentation.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massager.app.data.bluetooth.BleConnectionState
import com.massager.app.data.bluetooth.MassagerBluetoothService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceListItem(
    val name: String,
    val address: String,
    val signalStrength: Int,
    val isConnected: Boolean
)

data class DeviceScanUiState(
    val devices: List<DeviceListItem> = emptyList(),
    val connectionState: BleConnectionState = BleConnectionState(),
    val isScanning: Boolean = false
)

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val bluetoothService: MassagerBluetoothService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceScanUiState())
    val uiState: StateFlow<DeviceScanUiState> = _uiState.asStateFlow()

    init {
        bluetoothService.startScan()
        viewModelScope.launch {
            combine(
                bluetoothService.scanResults,
                bluetoothService.connectionState
            ) { devices, state ->
                DeviceScanUiState(
                    devices = devices.map {
                        DeviceListItem(
                            name = it.name,
                            address = it.address,
                            signalStrength = it.rssi,
                            isConnected = it.isConnected
                        )
                    },
                    connectionState = state,
                    isScanning = state.status == BleConnectionState.Status.Scanning
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

    fun onDeviceSelected(address: String) {
        val current = _uiState.value.connectionState
        if (current.deviceAddress == address &&
            current.status == BleConnectionState.Status.Connected
        ) {
            bluetoothService.disconnect()
        } else {
            bluetoothService.connect(address)
        }
    }

    fun clearErrorMessage() {
        bluetoothService.clearError()
        _uiState.update { it.copy(connectionState = it.connectionState.copy(errorMessage = null)) }
    }

    override fun onCleared() {
        bluetoothService.stopScan()
        super.onCleared()
    }
}
