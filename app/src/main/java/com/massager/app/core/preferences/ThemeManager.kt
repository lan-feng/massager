package com.massager.app.core.preferences

// 文件说明：使用 StateFlow + DataStore 管理应用主题（亮/暗/跟随系统），避免调用 AppCompatDelegate 导致 Activity 重建。
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_preferences"
)

enum class AppTheme {
    System, Light, Dark
}

@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _appTheme = MutableStateFlow(AppTheme.System)
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    init {
        scope.launch {
            appContext.themeDataStore.data
                .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
                .map { prefs ->
                    prefs[KEY_THEME]?.let { name ->
                        runCatching { AppTheme.valueOf(name) }.getOrDefault(AppTheme.System)
                    } ?: AppTheme.System
                }
                .distinctUntilChanged()
                .collect { theme -> _appTheme.value = theme }
        }
    }

    suspend fun setTheme(theme: AppTheme) {
        _appTheme.value = theme
        appContext.themeDataStore.edit { prefs ->
            prefs[KEY_THEME] = theme.name
        }
    }

    companion object {
        private val KEY_THEME = stringPreferencesKey("pref_app_theme")
    }
}
