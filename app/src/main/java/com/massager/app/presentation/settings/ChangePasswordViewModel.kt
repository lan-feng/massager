package com.massager.app.presentation.settings

// 文件说明：驱动修改密码流程，校验输入并调用域层用例。
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massager.app.domain.usecase.profile.ChangePasswordUseCase
import com.massager.app.domain.usecase.settings.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val PASSWORD_REGEX = Regex("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,12}$")

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun onOldPasswordChanged(value: String) {
        _uiState.update { it.copy(oldPassword = value, oldPasswordError = false, snackbarMessage = null) }
    }

    fun onNewPasswordChanged(value: String) {
        _uiState.update { it.copy(newPassword = value, newPasswordError = false, snackbarMessage = null) }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = false, snackbarMessage = null) }
    }

    fun submit() {
        val current = _uiState.value
        val oldError = current.oldPassword.isBlank()
        val newError = !PASSWORD_REGEX.matches(current.newPassword)
        val confirmError = current.newPassword != current.confirmPassword

        if (oldError || newError || confirmError) {
            _uiState.update {
                it.copy(
                    oldPasswordError = oldError,
                    newPasswordError = newError,
                    confirmPasswordError = confirmError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, snackbarMessage = null) }
            changePasswordUseCase(current.oldPassword, current.newPassword)
                .onSuccess {
                    logoutUseCase()
                    _uiState.update { it.copy(isSubmitting = false, success = true) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            snackbarMessage = throwable.message ?: "error_old_password_incorrect",
                            oldPasswordError = true
                        )
                    }
                }
        }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun consumeSuccess() {
        _uiState.update { it.copy(success = false) }
    }
}

data class ChangePasswordUiState(
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val oldPasswordError: Boolean = false,
    val newPasswordError: Boolean = false,
    val confirmPasswordError: Boolean = false,
    val isSubmitting: Boolean = false,
    val snackbarMessage: String? = null,
    val success: Boolean = false
) {
    val isFormValid: Boolean
        get() = oldPassword.isNotBlank() && PASSWORD_REGEX.matches(newPassword) && newPassword == confirmPassword
}
