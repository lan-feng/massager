package com.massager.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massager.app.data.local.SessionManager
import com.massager.app.domain.usecase.profile.GetUserProfileUseCase
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
    private val getUserProfileUseCase: GetUserProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AccountSecurityUiState(
            thirdPartyAccounts = ThirdPartyPlatform.values().map { platform ->
                ThirdPartyAccountBinding(platform = platform, isBound = false)
            }
        )
    )
    val uiState: StateFlow<AccountSecurityUiState> = _uiState.asStateFlow()

    init {
        loadAccountEmail()
    }

    fun toggleLogoutDialog(show: Boolean) {
        _uiState.update { it.copy(showLogoutDialog = show) }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            sessionManager.clear()
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

    private fun loadAccountEmail() {
        viewModelScope.launch {
            getUserProfileUseCase()
                .onSuccess { profile ->
                    _uiState.update { it.copy(userEmail = profile.email) }
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
    val logoutCompleted: Boolean = false
)

data class ThirdPartyAccountBinding(
    val platform: ThirdPartyPlatform,
    val isBound: Boolean
)

enum class ThirdPartyPlatform(
    val displayNameRes: Int,
    val badgeLabel: String
) {
    Facebook(
        displayNameRes = com.massager.app.R.string.third_party_facebook,
        badgeLabel = "f"
    ),
    Google(
        displayNameRes = com.massager.app.R.string.third_party_google,
        badgeLabel = "G"
    ),
    Apple(
        displayNameRes = com.massager.app.R.string.third_party_apple,
        badgeLabel = "A"
    )
}
