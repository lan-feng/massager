package com.massager.app.presentation.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massager.app.R
import com.massager.app.domain.usecase.device.BindDeviceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ManualAddUiState(
    val name: String = "",
    val mac: String = "",
    val nameErrorRes: Int? = null,
    val macErrorRes: Int? = null,
    val isSubmitting: Boolean = false,
    val statusMessageRes: Int? = null
)

sealed interface ManualAddEffect {
    data object NavigateHome : ManualAddEffect
    data class ShowError(val message: String) : ManualAddEffect
}

@HiltViewModel
class ManualAddDeviceViewModel @Inject constructor(
    private val bindDeviceUseCase: BindDeviceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManualAddUiState())
    val uiState: StateFlow<ManualAddUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ManualAddEffect>()
    val effects: SharedFlow<ManualAddEffect> = _effects.asSharedFlow()

    fun onNameChanged(value: String) {
        _uiState.update { it.copy(name = value, nameErrorRes = null) }
    }

    fun onMacChanged(value: String) {
        _uiState.update { it.copy(mac = value.uppercase(), macErrorRes = null) }
    }

    fun submit() {
        val current = _uiState.value
        val name = current.name.trim()
        val mac = current.mac.trim()
        var hasError = false
        if (name.isBlank()) {
            hasError = true
            _uiState.update { it.copy(nameErrorRes = R.string.manual_add_name_error) }
        }
        if (!MAC_REGEX.matches(mac)) {
            hasError = true
            _uiState.update { it.copy(macErrorRes = R.string.manual_add_mac_error) }
        }
        if (hasError) return
        if (_uiState.value.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, statusMessageRes = R.string.connecting) }
            val result = bindDeviceUseCase(serial = mac, displayName = name)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        statusMessageRes = R.string.connected_success
                    )
                }
                _effects.emit(ManualAddEffect.NavigateHome)
            }.onFailure { throwable ->
                _uiState.update { it.copy(isSubmitting = false, statusMessageRes = null) }
                _effects.emit(
                    ManualAddEffect.ShowError(
                        throwable.message ?: "Unable to add device"
                    )
                )
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessageRes = null) }
    }

    companion object {
        private val MAC_REGEX =
            Regex("^[0-9A-F]{2}(:[0-9A-F]{2}){5}\$")
    }
}
