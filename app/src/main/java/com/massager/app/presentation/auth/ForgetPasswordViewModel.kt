package com.massager.app.presentation.auth

// 文件说明：忘记密码流程的状态管理与验证码发送逻辑。
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massager.app.R
import com.massager.app.domain.usecase.auth.ResetPasswordUseCase
import com.massager.app.domain.usecase.settings.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgetPasswordUiState(
    val isSendingCode: Boolean = false,
    val isResetting: Boolean = false,
    val countdownSeconds: Int = 0,
    val toastMessageRes: Int? = null,
    val snackbarMessageRes: Int? = null,
    val errorMessageRes: Int? = null,
    val errorMessageText: String? = null
)

@HiltViewModel
class ForgetPasswordViewModel @Inject constructor(
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgetPasswordUiState())
    val uiState: StateFlow<ForgetPasswordUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    fun sendCode(email: String) {
        if (_uiState.value.isSendingCode) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingCode = true, errorMessageRes = null, errorMessageText = null) }
            resetPasswordUseCase.sendCode(email)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSendingCode = false,
                            toastMessageRes = R.string.code_sent
                        )
                    }
                    startCountdown()
                }
                .onFailure { throwable ->
                    val message = throwable.message
                    _uiState.update {
                        it.copy(
                            isSendingCode = false,
                            errorMessageRes = if (message.isNullOrBlank()) R.string.verification_code_failed else null,
                            errorMessageText = message
                        )
                    }
                }
        }
    }

    fun resetPassword(email: String, verificationCode: String, newPassword: String) {
        if (_uiState.value.isResetting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isResetting = true, errorMessageRes = null, errorMessageText = null) }
            resetPasswordUseCase.reset(email, verificationCode, newPassword)
                .onSuccess {
                    logoutUseCase()
                    _uiState.update {
                        it.copy(
                            isResetting = false,
                            snackbarMessageRes = R.string.password_reset_success
                        )
                    }
                }
                .onFailure { throwable ->
                    val message = throwable.message
                    _uiState.update {
                        it.copy(
                            isResetting = false,
                            errorMessageRes = if (message.isNullOrBlank()) R.string.password_reset_failed else null,
                            errorMessageText = message
                        )
                    }
                }
        }
    }

    fun consumeToast() {
        _uiState.update { it.copy(toastMessageRes = null) }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessageRes = null, errorMessageText = null) }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessageRes = null) }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = 60
            while (remaining > 0) {
                _uiState.update { it.copy(countdownSeconds = remaining) }
                delay(1_000)
                remaining--
            }
            _uiState.update { it.copy(countdownSeconds = 0) }
        }
    }

    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }
}
