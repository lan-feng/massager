package com.massager.app.presentation.settings

// 文件说明：处理个人信息修改与头像选择的状态管理与提交逻辑。
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massager.app.R
import com.massager.app.core.avatar.DEFAULT_AVATAR_NAME
import com.massager.app.data.local.SessionManager
import com.massager.app.domain.usecase.profile.GetUserProfileUseCase
import com.massager.app.domain.usecase.profile.UpdateUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PersonalInformationViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalInfoUiState(isLoading = true))
    val uiState: StateFlow<PersonalInfoUiState> = _uiState.asStateFlow()

    init {
        if (sessionManager.isGuestMode()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isGuestMode = true,
                    name = sessionManager.guestName().orEmpty().ifBlank { defaultGuestName() },
                    avatarUrl = sessionManager.guestAvatarName().orEmpty().ifBlank { DEFAULT_AVATAR_NAME },
                    toastMessage = null
                )
            }
        } else {
            refresh()
        }
    }

    fun refresh() {
        if (sessionManager.isGuestMode()) {
            _uiState.update { it.copy(isLoading = false, isGuestMode = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getUserProfileUseCase()
                .onSuccess { profile ->
                    _uiState.update { state ->
                        val avatarName = profile.avatarUrl.orEmpty().ifBlank { DEFAULT_AVATAR_NAME }
                        sessionManager.saveAccountAvatarName(avatarName)
                        state.copy(
                            isLoading = false,
                            name = profile.name,
                            email = profile.email,
                            avatarUrl = avatarName
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            toastMessage = throwable.message ?: appContext.getString(R.string.profile_load_failed)
                        )
                    }
                }
        }
    }

    fun updateName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(toastMessage = appContext.getString(R.string.profile_name_empty)) }
            return
        }
        if (sessionManager.isGuestMode()) {
            sessionManager.saveGuestName(trimmed)
            _uiState.update {
                it.copy(
                    name = trimmed
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            updateUserProfileUseCase.updateName(trimmed)
                .onSuccess { profile ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            name = profile.name
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            toastMessage = throwable.message ?: appContext.getString(R.string.profile_name_update_failed)
                        )
                    }
                }
        }
    }

    fun updateAvatar(avatarName: String) {
        val finalName = avatarName.ifBlank { DEFAULT_AVATAR_NAME }
        if (sessionManager.isGuestMode()) {
            sessionManager.saveGuestAvatarName(finalName)
            _uiState.update {
                it.copy(
                    avatarUrl = finalName
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            updateUserProfileUseCase.updateAvatarUrl(finalName)
                .onSuccess { profile ->
                    sessionManager.saveAccountAvatarName(finalName)
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            avatarUrl = profile.avatarUrl.orEmpty().ifBlank { finalName }
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            toastMessage = throwable.message ?: appContext.getString(R.string.profile_avatar_update_failed)
                        )
                    }
                }
        }
    }

    fun consumeToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    private fun defaultGuestName(): String = appContext.getString(R.string.guest_placeholder_name)
}

data class PersonalInfoUiState(
    val name: String = "",
    val email: String = "",
    val avatarUrl: String = DEFAULT_AVATAR_NAME,
    val isLoading: Boolean = false,
    val toastMessage: String? = null,
    val isGuestMode: Boolean = false
)

