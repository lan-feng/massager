package com.massager.app.presentation.settings

// 文件说明：管理设置页状态与动作，如登出、跳转和用户信息加载。
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.massager.app.R
import com.massager.app.data.local.AppCacheManager
import com.massager.app.data.local.SessionManager
import com.massager.app.domain.model.UserProfile
import com.massager.app.domain.usecase.profile.GetUserProfileUseCase
import com.massager.app.domain.usecase.profile.UpdateUserProfileUseCase
import com.massager.app.domain.usecase.profile.UploadAvatarUseCase
import com.massager.app.domain.usecase.settings.LogoutUseCase
import com.massager.app.presentation.navigation.Screen
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
    private val uploadAvatarUseCase: UploadAvatarUseCase,
    private val cacheManager: AppCacheManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private val prefs = appContext.getSharedPreferences("user_settings", Context.MODE_PRIVATE)

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
        loadDisplayPreferences()
    }

    private fun loadDisplayPreferences() {
        val theme = prefs.getString(KEY_THEME, AppTheme.System.name)?.let {
            runCatching { AppTheme.valueOf(it) }.getOrNull()
        } ?: AppTheme.System
        val language = prefs.getString(KEY_LANGUAGE, AppLanguage.System.name)?.let {
            runCatching { AppLanguage.valueOf(it) }.getOrNull()
        } ?: AppLanguage.System
        applyTheme(theme, save = false)
        applyLanguage(language, save = false)
        _uiState.update { it.copy(appTheme = theme, appLanguage = language) }
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
                            toastMessage = throwable.message ?: "Failed to load profile"
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
                user = state.user.copy(tempUnit = newUnit),
                toastMessage = "Temperature unit changed to ${newUnit.display}"
            )
        }
    }

    fun clearCache() = viewModelScope.launch {
        runCatching { cacheManager.clearCache() }
            .onSuccess { result ->
                val message = if (result.freedBytes > 0) {
                    "Cache cleared successfully (${result.freedDisplay} freed)"
                } else {
                    "Cache already clean"
                }
                _uiState.update { state ->
                    state.copy(
                        user = state.user.copy(cacheSize = result.remainingDisplay),
                        toastMessage = message
                    )
                }
            }
            .onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        toastMessage = throwable.message ?: "Unable to clear cache"
                    )
                }
            }
    }

    fun consumeToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun setTheme(theme: AppTheme) {
        _uiState.update { it.copy(appTheme = theme) }
        applyTheme(theme, save = true)
        prefs.edit().putString(KEY_PENDING_ROUTE, Screen.Settings.route).apply()
    }

    fun setLanguage(language: AppLanguage) {
        _uiState.update { it.copy(appLanguage = language) }
        applyLanguage(language, save = true)
        prefs.edit().putString(KEY_PENDING_ROUTE, Screen.Settings.route).apply()
    }

    private fun applyTheme(theme: AppTheme, save: Boolean) {
        val mode = when (theme) {
            AppTheme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            AppTheme.Light -> AppCompatDelegate.MODE_NIGHT_NO
            AppTheme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(mode)
        if (save) prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    private fun applyLanguage(language: AppLanguage, save: Boolean) {
        val locales = when (language) {
            AppLanguage.System -> LocaleListCompat.getEmptyLocaleList()
            AppLanguage.Chinese -> LocaleListCompat.create(java.util.Locale.SIMPLIFIED_CHINESE)
            AppLanguage.English -> LocaleListCompat.create(java.util.Locale.ENGLISH)
        }
        val current = AppCompatDelegate.getApplicationLocales()
        if (current != locales) {
            AppCompatDelegate.setApplicationLocales(locales)
        }
        if (save) prefs.edit().putString(KEY_LANGUAGE, language.name).apply()
    }

    fun updateUserName(newName: String) {
        if (isGuestMode()) {
            showGuestRestriction()
            return
        }
        val trimmed = newName.trim()
        if (trimmed.length < 2) {
            _uiState.update { it.copy(toastMessage = "Name must be at least 2 characters") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            updateUserProfileUseCase.updateName(trimmed)
                .onSuccess { profile ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            user = profile.toSettingsUser(state.user),
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
        if (isGuestMode()) {
            showGuestRestriction()
            return
        }
        if (bytes.isEmpty()) return
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
                            user = profile.toSettingsUser(state.user).copy(avatarBytes = bytes),
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
    val avatarUrl: String? = null,
    val cacheSize: String = "--",
    val tempUnit: TemperatureUnit = TemperatureUnit.Fahrenheit,
    val avatarBytes: ByteArray? = null
)

enum class TemperatureUnit(val display: String) {
    Celsius("\u2103"),
    Fahrenheit("\u2109");

    fun toggle(): TemperatureUnit = if (this == Celsius) Fahrenheit else Celsius
}

enum class AppTheme {
    System, Light, Dark
}

enum class AppLanguage {
    System, Chinese, English
}

private const val KEY_THEME = "pref_app_theme"
private const val KEY_LANGUAGE = "pref_app_language"
const val KEY_PENDING_ROUTE = "pref_pending_route"



