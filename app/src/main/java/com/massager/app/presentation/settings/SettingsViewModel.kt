package com.massager.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massager.app.data.local.SessionManager
import com.massager.app.domain.usecase.settings.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            user = SettingsUser(
                name = "Geoffrey",
                avatarUrl = "",
                cacheSize = "8.64MB",
                tempUnit = TemperatureUnit.Fahrenheit
            )
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun toggleTemperatureUnit() = viewModelScope.launch {
        _uiState.update { state ->
            val newUnit = state.user.tempUnit.toggle()
            state.copy(
                user = state.user.copy(tempUnit = newUnit),
                toastMessage = "Temperature unit changed to ${newUnit.display}"
            )
        }
    }

    fun clearCache() = viewModelScope.launch {
        _uiState.update { state ->
            state.copy(
                user = state.user.copy(cacheSize = "0MB"),
                toastMessage = "Cache cleared successfully"
            )
        }
    }

    fun consumeToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun logout() = viewModelScope.launch {
        logoutUseCase()
        sessionManager.clear()
    }
}

data class SettingsUiState(
    val user: SettingsUser,
    val toastMessage: String? = null
)

data class SettingsUser(
    val name: String,
    val avatarUrl: String,
    val cacheSize: String,
    val tempUnit: TemperatureUnit
)

enum class TemperatureUnit(val display: String) {
    Celsius("\u2103"),
    Fahrenheit("\u2109");

    fun toggle(): TemperatureUnit = if (this == Celsius) Fahrenheit else Celsius
}


