package com.massager.app.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massager.app.R
import com.massager.app.data.local.SessionManager
import com.massager.app.domain.model.UserProfile
import com.massager.app.domain.usecase.profile.GetUserProfileUseCase
import com.massager.app.domain.usecase.profile.UpdateUserProfileUseCase
import com.massager.app.domain.usecase.profile.UploadAvatarUseCase
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
    private val uploadAvatarUseCase: UploadAvatarUseCase,
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
                    avatarBytes = sessionManager.guestAvatar(),
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
                        state.copy(
                            isLoading = false,
                            name = profile.name,
                            email = profile.email,
                            avatarUrl = profile.avatarUrl.orEmpty()
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            toastMessage = throwable.message ?: "Failed to load profile"
                        )
                    }
                }
        }
    }

    fun updateName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(toastMessage = "Name cannot be empty") }
            return
        }
        if (sessionManager.isGuestMode()) {
            sessionManager.saveGuestName(trimmed)
            _uiState.update {
                it.copy(
                    name = trimmed,
                    toastMessage = appContext.getString(R.string.profile_name_updated_local)
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
                            name = profile.name,
                            toastMessage = "Name updated"
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            toastMessage = throwable.message ?: "Unable to update name"
                        )
                    }
                }
        }
    }

    fun updateAvatar(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        if (sessionManager.isGuestMode()) {
            sessionManager.saveGuestAvatar(bytes)
            _uiState.update {
                it.copy(
                    avatarBytes = bytes,
                    toastMessage = appContext.getString(R.string.profile_avatar_updated_local)
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val fileName = "avatar_${System.currentTimeMillis()}.jpg"
            val uploaded = uploadAvatarUseCase(bytes, fileName)
            if (uploaded.isFailure) {
                val message = uploaded.exceptionOrNull()?.message ?: "Unable to upload avatar"
                _uiState.update { it.copy(isLoading = false, toastMessage = message) }
                return@launch
            }
            val url = uploaded.getOrNull() ?: run {
                _uiState.update { it.copy(isLoading = false, toastMessage = "Upload failed") }
                return@launch
            }
            updateUserProfileUseCase.updateAvatarUrl(url)
                .onSuccess { profile ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            avatarUrl = profile.avatarUrl.orEmpty(),
                            avatarBytes = bytes,
                            toastMessage = "Avatar updated"
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            toastMessage = throwable.message ?: "Unable to update avatar"
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
    val avatarUrl: String = "",
    val avatarBytes: ByteArray? = null,
    val isLoading: Boolean = false,
    val toastMessage: String? = null,
    val isGuestMode: Boolean = false
)

