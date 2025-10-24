package com.massager.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massager.app.domain.model.AuthResult
import com.massager.app.domain.usecase.auth.LoginUseCase
import com.massager.app.domain.usecase.auth.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val registrationSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = loginUseCase(email, password)) {
                is AuthResult.LoginSuccess -> _uiState.value = AuthUiState(isAuthenticated = true)
                is AuthResult.Error -> _uiState.value = AuthUiState(errorMessage = result.message)
                else -> _uiState.value = AuthUiState()
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = registerUseCase(name, email, password)) {
                is AuthResult.RegisterSuccess -> _uiState.value = AuthUiState(registrationSuccess = true)
                is AuthResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                else -> _uiState.value = AuthUiState()
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearRegistrationFlag() {
        _uiState.value = _uiState.value.copy(registrationSuccess = false)
    }
}
