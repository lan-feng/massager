package com.massager.app.presentation.settings

// 文件说明：管理设置页状态与动作，如登出、跳转和用户信息加载。
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massager.app.R
import com.massager.app.core.avatar.DEFAULT_AVATAR_NAME
import com.massager.app.core.preferences.AppLanguage
import com.massager.app.core.preferences.AppTheme
import com.massager.app.core.preferences.LanguageManager
import com.massager.app.core.preferences.ThemeManager
import com.massager.app.data.local.AppCacheManager
import com.massager.app.data.local.SessionManager
import com.massager.app.domain.model.UserProfile
import com.massager.app.domain.usecase.profile.GetUserProfileUseCase
import com.massager.app.domain.usecase.profile.UpdateUserProfileUseCase
import com.massager.app.domain.usecase.settings.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val logoutUseCase: LogoutUseCase,
    private val sessionManager: SessionManager,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val cacheManager: AppCacheManager,
    private val themeManager: ThemeManager,
    private val languageManager: LanguageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        if (isGuestMode()) {
            _uiState.update {
                it.copy(isGuestMode = true)
            }
            refreshCacheSize()
        } else {
            refreshProfile()
            refreshCacheSize()
        }
        observeDisplayPreferences()
    }

    private fun observeDisplayPreferences() {
        viewModelScope.launch {
            themeManager.appTheme.collect { theme ->
                _uiState.update { it.copy(appTheme = theme) }
            }
        }
        viewModelScope.launch {
            languageManager.appLanguage.collect { language ->
                _uiState.update { it.copy(appLanguage = language) }
            }
        }
    }

    fun refreshProfile() {
        if (isGuestMode()) {
            _uiState.update { it.copy(isLoading = false, isGuestMode = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getUserProfileUseCase()
                .onSuccess { profile ->
                    _uiState.update { state ->
                        val avatarName = normalizeAvatarName(
                            profile.avatarUrl?.takeIf { it.isNotBlank() }
                                ?: sessionManager.accountAvatarName()
                                ?: DEFAULT_AVATAR_NAME
                        )
                        sessionManager.saveAccountAvatarName(avatarName)
                        state.copy(
                            isLoading = false,
                            user = profile.toSettingsUser(state.user).copy(avatarUrl = avatarName)
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
        refreshCacheSize()
    }

    fun toggleTemperatureUnit() = viewModelScope.launch {
        _uiState.update { state ->
            val newUnit = state.user.tempUnit.toggle()
            state.copy(
                user = state.user.copy(tempUnit = newUnit)
            )
        }
    }

    fun clearCache() = viewModelScope.launch {
        runCatching { cacheManager.clearCache() }
            .onSuccess { result ->
                _uiState.update { state ->
                    state.copy(
                        user = state.user.copy(cacheSize = result.remainingDisplay)
                    )
                }
            }
            .onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        toastMessage = throwable.message ?: appContext.getString(R.string.settings_cache_clear_failed)
                    )
                }
            }
    }

    fun consumeToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun setTheme(theme: AppTheme) {
        _uiState.update { it.copy(appTheme = theme) }
        viewModelScope.launch {
            themeManager.setTheme(theme)
        }
    }

    fun setLanguage(language: AppLanguage) {
        _uiState.update { it.copy(appLanguage = language) }
        viewModelScope.launch {
            languageManager.setLanguage(language)
        }
    }

    fun updateUserName(newName: String) {
        if (isGuestMode()) {
            showGuestRestriction()
            return
        }
        val trimmed = newName.trim()
        if (trimmed.length < 2) {
            _uiState.update { it.copy(toastMessage = appContext.getString(R.string.profile_name_too_short)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            updateUserProfileUseCase.updateName(trimmed)
                .onSuccess { profile ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            user = profile.toSettingsUser(state.user)
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
        if (isGuestMode()) {
            showGuestRestriction()
            return
        }
        val finalName = normalizeAvatarName(avatarName)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            updateUserProfileUseCase.updateAvatarUrl(finalName)
                .onSuccess { profile ->
                    sessionManager.saveAccountAvatarName(finalName)
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            user = profile.toSettingsUser(state.user).copy(avatarUrl = finalName)
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

    fun logout() = viewModelScope.launch {
        val result = logoutUseCase()
        result.onFailure { throwable ->
            _uiState.update {
                it.copy(
                    toastMessage = throwable.message ?: appContext.getString(R.string.logout_failed)
                )
            }
        }
    }

    fun showGuestRestriction(message: String = guestRestrictionMessage) {
        _uiState.update { it.copy(toastMessage = message) }
    }

    private fun isGuestMode(): Boolean = sessionManager.isGuestMode()

    private fun refreshCacheSize() {
        viewModelScope.launch {
            runCatching { cacheManager.cacheSnapshot() }
                .onSuccess { snapshot ->
                    _uiState.update { state ->
                        state.copy(
                            user = state.user.copy(cacheSize = snapshot.display)
                        )
                    }
                }
        }
    }

    private fun UserProfile.toSettingsUser(previous: SettingsUser): SettingsUser =
        previous.copy(
            id = id,
            name = name,
            email = email,
            avatarUrl = avatarUrl,
            cacheSize = cacheSize ?: previous.cacheSize
        )

    private val guestRestrictionMessage: String
        get() = appContext.getString(R.string.guest_mode_cloud_restricted)
}

data class SettingsUiState(
    val user: SettingsUser = SettingsUser(),
    val toastMessage: String? = null,
    val isLoading: Boolean = false,
    val isGuestMode: Boolean = false,
    val appTheme: AppTheme = AppTheme.System,
    val appLanguage: AppLanguage = AppLanguage.System
)

data class SettingsUser(
    val id: Long = -1L,
    val name: String = "",
    val email: String = "",
    val avatarUrl: String? = DEFAULT_AVATAR_NAME,
    val cacheSize: String = "--",
    val tempUnit: TemperatureUnit = TemperatureUnit.Fahrenheit
)

enum class TemperatureUnit(val display: String) {
    Celsius("\u2103"),
    Fahrenheit("\u2109");

    fun toggle(): TemperatureUnit = if (this == Celsius) Fahrenheit else Celsius
}



