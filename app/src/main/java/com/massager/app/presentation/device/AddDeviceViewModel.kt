package com.massager.app.presentation.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massager.app.domain.usecase.device.BindDeviceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NearbyDevice(
    val id: String,
    val name: String,
    val macAddress: String,
    val signalStrength: Int
)

data class AddDeviceUiState(
    val isScanning: Boolean = true,
    val devices: List<NearbyDevice> = emptyList(),
    val showEmpty: Boolean = false,
    val connectingDeviceId: String? = null,
    val statusMessageRes: Int? = null,
    val errorMessage: String? = null
)

sealed interface AddDeviceEffect {
    data object NavigateHome : AddDeviceEffect
    data class ShowError(val message: String) : AddDeviceEffect
}

@HiltViewModel
class AddDeviceViewModel @Inject constructor(
    private val bindDeviceUseCase: BindDeviceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDeviceUiState())
    val uiState: StateFlow<AddDeviceUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AddDeviceEffect>()
    val effects: SharedFlow<AddDeviceEffect> = _effects.asSharedFlow()

    private var scanJob: Job? = null
    private var emptyStateJob: Job? = null

    init {
        startScan()
    }

    fun startScan() {
        scanJob?.cancel()
        emptyStateJob?.cancel()
        _uiState.value = AddDeviceUiState()
        scanJob = viewModelScope.launch {
            delay(SCAN_RESULT_DELAY_MS)
            val devices = generateDemoDevices()
            _uiState.update {
                it.copy(
                    devices = devices,
                    isScanning = false,
                    showEmpty = devices.isEmpty()
                )
            }
        }
        emptyStateJob = viewModelScope.launch {
            delay(EMPTY_STATE_DELAY_MS)
            _uiState.update { state ->
                if (state.devices.isEmpty()) {
                    state.copy(isScanning = false, showEmpty = true)
                } else {
                    state
                }
            }
        }
    }

    fun retryScan() {
        startScan()
    }

    fun connectToDevice(device: NearbyDevice) {
        if (_uiState.value.connectingDeviceId != null) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    connectingDeviceId = device.id,
                    statusMessageRes = com.massager.app.R.string.connecting,
                    errorMessage = null
                )
            }
            delay(CONNECTION_SIMULATION_DELAY_MS)
            val result = bindDeviceUseCase(
                serial = device.macAddress,
                displayName = device.name
            )
            result.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        connectingDeviceId = null,
                        statusMessageRes = com.massager.app.R.string.connected_success
                    )
                }
                delay(POST_CONNECTION_SUCCESS_DELAY_MS)
                _effects.emit(AddDeviceEffect.NavigateHome)
            }.onFailure { throwable ->
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

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessageRes = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun generateDemoDevices(): List<NearbyDevice> {
        val baseDevices = listOf(
            "SmartPulse TENS Unit",
            "ComfyTemp Relaxer",
            "Massager Mini Pro"
        )
        return baseDevices.shuffled()
            .take(Random.nextInt(1, baseDevices.size + 1))
            .mapIndexed { index, name ->
                NearbyDevice(
                    id = "${System.currentTimeMillis()}-$index",
                    name = name,
                    macAddress = randomMacAddress(index),
                    signalStrength = Random.nextInt(-80, -40)
                )
            }
    }

    private fun randomMacAddress(seed: Int): String {
        val random = Random(System.currentTimeMillis() + seed)
        return (0 until 6).joinToString(":") {
            "%02X".format(random.nextInt(0, 255))
        }
    }

    companion object {
        private const val SCAN_RESULT_DELAY_MS = 3_500L
        private const val EMPTY_STATE_DELAY_MS = 10_000L
        private const val CONNECTION_SIMULATION_DELAY_MS = 1_000L
        private const val POST_CONNECTION_SUCCESS_DELAY_MS = 1_000L
    }
}
