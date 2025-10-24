package com.massager.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massager.app.domain.model.DeviceMetadata
import com.massager.app.domain.model.TemperatureRecord
import com.massager.app.domain.usecase.device.ObserveDevicesUseCase
import com.massager.app.domain.usecase.device.RefreshDevicesUseCase
import com.massager.app.domain.usecase.measurement.ObserveMeasurementsUseCase
import com.massager.app.domain.usecase.measurement.RefreshMeasurementsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val devices: List<DeviceMetadata> = emptyList(),
    val measurements: List<TemperatureRecord> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val showDeviceAddedToast: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeDevicesUseCase: ObserveDevicesUseCase,
    observeMeasurementsUseCase: ObserveMeasurementsUseCase,
    private val refreshDevicesUseCase: RefreshDevicesUseCase,
    private val refreshMeasurementsUseCase: RefreshMeasurementsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeDevicesUseCase().collect { devices ->
                _uiState.update { it.copy(devices = devices) }
            }
        }
        viewModelScope.launch {
            observeMeasurementsUseCase().collect { measurements ->
                _uiState.update { it.copy(measurements = measurements) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            val deviceResult = refreshDevicesUseCase()
            val devices = deviceResult.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage = error.message ?: "无法获取设备列表"
                    )
                }
                return@launch
            }

            val deviceId = devices.firstOrNull()?.id
                ?: _uiState.value.devices.firstOrNull()?.id

            if (deviceId != null) {
                refreshMeasurementsUseCase(deviceId).getOrElse { error ->
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessage = error.message ?: "无法获取测量数据"
                        )
                    }
                    return@launch
                }
            }

            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onAddDevice() {
        _uiState.update { it.copy(showDeviceAddedToast = true) }
    }

    fun consumeAddDeviceToast() {
        _uiState.update { it.copy(showDeviceAddedToast = false) }
    }
}
