package com.massager.app.presentation.settings

// 文件说明：负责账户安全页的 UI 状态、验证码发送与重置密码触发。
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massager.app.data.local.SessionManager
import com.massager.app.domain.usecase.profile.GetUserProfileUseCase
import com.massager.app.domain.usecase.profile.UnbindProviderUseCase
import com.massager.app.domain.usecase.settings.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AccountSecurityViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
    private val sessionManager: SessionManager,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val unbindProviderUseCase: UnbindProviderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AccountSecurityUiState(
            thirdPartyAccounts = listOf(
                ThirdPartyAccountBinding(platform = ThirdPartyPlatform.Google, isBound = false)
            )
        )
    )
    val uiState: StateFlow<AccountSecurityUiState> = _uiState.asStateFlow()

    init {
        if (sessionManager.isGuestMode()) {
            _uiState.update { it.copy(isGuestMode = true) }
        } else {
            loadAccountEmail()
        }
    }

    fun toggleLogoutDialog(show: Boolean) {
        _uiState.update { it.copy(showLogoutDialog = show) }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _uiState.update {
                it.copy(
                    showLogoutDialog = false,
                    logoutCompleted = true
                )
            }
        }
    }

    fun consumeLogoutNavigation() {
        _uiState.update { it.copy(logoutCompleted = false) }
    }

    fun consumeUnbindResult() {
        _uiState.update { it.copy(unbindSucceeded = false, unbindError = null) }
    }

    fun consumeBindResult() {
        _uiState.update { it.copy(bindSucceeded = false, bindError = null) }
    }

    fun onExternalBindStart() {
        _uiState.update { it.copy(isBinding = true, bindError = null, bindSucceeded = false) }
    }

    fun onExternalBindSuccess() {
        loadAccountEmail()
        _uiState.update { it.copy(bindSucceeded = true, isBinding = false) }
    }

    fun onExternalBindFailed(message: String?) {
        _uiState.update { it.copy(isBinding = false, bindError = message ?: "Bind failed") }
    }

    fun unbind(platform: ThirdPartyPlatform) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUnbinding = true, unbindSucceeded = false, unbindError = null) }
            unbindProviderUseCase(platform.apiName)
                .onSuccess {
                    loadAccountEmail()
                    _uiState.update { it.copy(isUnbinding = false, unbindSucceeded = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isUnbinding = false,
                            unbindError = error.message ?: "Unbind failed"
                        )
                    }
                }
        }
    }

    private fun loadAccountEmail() {
        viewModelScope.launch {
            getUserProfileUseCase()
                .onSuccess { profile ->
                    _uiState.update {
                        val googleInfo = profile.thirdPartyProfiles[ThirdPartyPlatform.Google.apiName]
                        it.copy(
                            userEmail = profile.email,
                            hasPassword = profile.hasPassword,
                            thirdPartyAccounts = listOf(
                                ThirdPartyAccountBinding(
                                    platform = ThirdPartyPlatform.Google,
                                    isBound = !profile.firebaseUid.isNullOrBlank(),
                                    displayName = googleInfo?.name ?: googleInfo?.email,
                                    email = googleInfo?.email
                                )
                            ),
                            facebookBound = !profile.facebookUid.isNullOrBlank()
                        )
                    }
                }
                .onFailure {
                    // Keep previous email if available; no explicit error handling required here.
                }
        }
    }
}

data class AccountSecurityUiState(
    val userEmail: String = "",
    val thirdPartyAccounts: List<ThirdPartyAccountBinding> = emptyList(),
    val showLogoutDialog: Boolean = false,
    val logoutCompleted: Boolean = false,
    val isGuestMode: Boolean = false,
    val hasPassword: Boolean = false,
    val facebookBound: Boolean = false,
    val isUnbinding: Boolean = false,
    val unbindSucceeded: Boolean = false,
    val unbindError: String? = null,
    val bindSucceeded: Boolean = false,
    val isBinding: Boolean = false,
    val bindError: String? = null
)

data class ThirdPartyAccountBinding(
    val platform: ThirdPartyPlatform,
    val isBound: Boolean,
    val displayName: String? = null,
    val email: String? = null
)

enum class ThirdPartyPlatform(
    val displayNameRes: Int,
    val badgeLabel: String,
    val apiName: String
) {
    Google(
        displayNameRes = com.massager.app.R.string.third_party_google,
        badgeLabel = "G",
        apiName = "google"
    )
}
