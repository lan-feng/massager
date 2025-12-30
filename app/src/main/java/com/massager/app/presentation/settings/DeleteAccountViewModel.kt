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
            _uiState.update { it.copy(errorMessageRes = com.massager.app.R.string.delete_account_missing_user_id) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessageRes = null, errorMessageText = null) }
            deleteAccountUseCase(userId)
                .onSuccess {
                    sessionManager.clear()
                    _uiState.update { it.copy(isLoading = false, success = true) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessageText = throwable.message,
                            errorMessageRes = throwable.message?.let { null }
                                ?: com.massager.app.R.string.delete_account_failed
                        )
                    }
                }
        }
    }

    fun consumeSuccess() {
        _uiState.update { it.copy(success = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessageRes = null, errorMessageText = null) }
    }
}

data class DeleteAccountUiState(
    val isLoading: Boolean = false,
    val errorMessageRes: Int? = null,
    val errorMessageText: String? = null,
    val success: Boolean = false
)
