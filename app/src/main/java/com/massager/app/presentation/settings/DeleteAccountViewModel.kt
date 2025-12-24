package com.massager.app.presentation.settings

// 文件说明：处理删除账号的业务流程，包括调用后端接口并清理本地会话。
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massager.app.data.local.SessionManager
import com.massager.app.domain.usecase.profile.DeleteAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeleteAccountUiState())
    val uiState: StateFlow<DeleteAccountUiState> = _uiState.asStateFlow()

    fun deleteAccount() {
        if (_uiState.value.isLoading) return

        val userId = sessionManager.userId()?.toLongOrNull()
        if (userId == null) {
            _uiState.update { it.copy(errorMessage = "Unable to get user ID, please re-login and try again") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            deleteAccountUseCase(userId)
                .onSuccess {
                    sessionManager.clear()
                    _uiState.update { it.copy(isLoading = false, success = true) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Failed to delete account, please try again later"
                        )
                    }
                }
        }
    }

    fun consumeSuccess() {
        _uiState.update { it.copy(success = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class DeleteAccountUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val success: Boolean = false
)
