package com.massager.app.presentation.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massager.app.R
import com.massager.app.data.local.SessionManager
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
import kotlinx.coroutines.launch

data class HomeUiState(
    val devices: List<DeviceMetadata> = emptyList(),
    val measurements: List<TemperatureRecord> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val selectedDeviceIds: Set<String> = emptySet(),
    val isRenameDialogVisible: Boolean = false,
    val renameInput: String = "",
    val renameInputError: Int? = null,
    val isRemoveDialogVisible: Boolean = false,
    val isActionInProgress: Boolean = false
) {
    val isManagementActive: Boolean
        get() = selectedDeviceIds.isNotEmpty()
}

sealed interface HomeEffect {
    data class ShowMessage(@StringRes val messageRes: Int) : HomeEffect
    data class ShowMessageText(val message: String) : HomeEffect
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeDevicesUseCase: ObserveDevicesUseCase,
    observeMeasurementsUseCase: ObserveMeasurementsUseCase,
    private val refreshDevicesUseCase: RefreshDevicesUseCase,
    private val refreshMeasurementsUseCase: RefreshMeasurementsUseCase,
    private val renameDeviceUseCase: RenameDeviceUseCase,
    private val removeDeviceUseCase: RemoveDeviceUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<HomeEffect>()
    val effects: SharedFlow<HomeEffect> = _effects.asSharedFlow()

    private var guestEntryPromptShown = false
    private var guestSyncPromptShown = false

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
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            if (sessionManager.isGuestMode() && !guestSyncPromptShown) {
                emitMessageRes(R.string.guest_mode_cloud_restricted)
                guestSyncPromptShown = true
            }
            val deviceResult = refreshDevicesUseCase()
            val devices = deviceResult.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage = error.message ?: "Unable to load devices"
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
                            errorMessage = error.message ?: "Unable to load measurements"
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

    fun toggleDeviceSelection(deviceId: String) {
        _uiState.update { state ->
            val updatedSelection = state.selectedDeviceIds.toMutableSet().also { set ->
                if (!set.add(deviceId)) {
                    set.remove(deviceId)
                }
            }.toSet()
            state.copy(
                selectedDeviceIds = updatedSelection,
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
                emitMessageRes(R.string.device_renamed)
                _uiState.update { current ->
                    current.copy(
                        isActionInProgress = false,
                        isRenameDialogVisible = false,
                        renameInput = "",
                        renameInputError = null
                    )
                }
            }.onFailure { throwable ->
                emitMessageText(throwable.message ?: "Unable to rename device")
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
                emitMessageRes(R.string.device_removed)
                _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        isRemoveDialogVisible = false,
                        selectedDeviceIds = emptySet()
                    )
                }
            } else {
                emitMessageText(failure?.message ?: "Unable to remove device")
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

    private fun emitMessageText(message: String) {
        viewModelScope.launch {
            _effects.emit(HomeEffect.ShowMessageText(message))
        }
    }
}
