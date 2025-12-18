package com.massager.app.core.preferences

// 文件说明：统一管理应用语言偏好，StateFlow + DataStore 持久化，并提供 Compose CompositionLocal 注入本地化 Context。
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import android.text.TextUtils
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.languageDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "language_preferences"
)

enum class AppLanguage {
    System, Chinese, English
}

@Immutable
data class AppLocaleState(
    val language: AppLanguage,
    val locales: LocaleList
) {
    val layoutDirection: LayoutDirection
        get() {
            val primaryLocale = if (locales.size() > 0) locales[0] else Locale.getDefault()
            return if (TextUtils.getLayoutDirectionFromLocale(primaryLocale) == android.view.View.LAYOUT_DIRECTION_RTL) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }
        }

    fun wrap(baseContext: Context): Context {
        val configuration = Configuration(baseContext.resources.configuration)
        configuration.setLocales(locales)
        return baseContext.createConfigurationContext(configuration)
    }
}

val LocalAppLocale = staticCompositionLocalOf {
    AppLocaleState(
        language = AppLanguage.System,
        locales = LocaleList.getDefault()
    )
}

@Composable
fun ProvideAppLocale(
    appLocaleState: AppLocaleState,
    content: @Composable () -> Unit
) {
    val activity = LocalContext.current as? Activity
    if (activity == null) {
        CompositionLocalProvider(
            LocalAppLocale provides appLocaleState,
            LocalConfiguration provides LocalConfiguration.current,
            LocalLayoutDirection provides appLocaleState.layoutDirection
        ) { content() }
        return
    }

    val configuration = remember(appLocaleState) {
        Configuration(activity.resources.configuration).apply {
            setLocales(appLocaleState.locales)
        }
    }

    SideEffect {
        val locales = appLocaleState.locales
        if (locales.size() > 0) {
            LocaleList.setDefault(locales)
            Locale.setDefault(locales[0])
        }
        val resources = activity.resources
        val currentLocales = resources.configuration.locales
        if (currentLocales != appLocaleState.locales) {
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }
    }

    CompositionLocalProvider(
        LocalAppLocale provides appLocaleState,
        LocalConfiguration provides configuration,
        LocalLayoutDirection provides appLocaleState.layoutDirection
    ) { content() }
}

@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _appLocale = MutableStateFlow(createLocaleState(AppLanguage.System))
    val appLocale: StateFlow<AppLocaleState> = _appLocale.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.System)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    init {
        scope.launch {
            appContext.languageDataStore.data
                .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
                .map { prefs ->
                    prefs[KEY_LANGUAGE]?.let { name ->
                        runCatching { AppLanguage.valueOf(name) }.getOrDefault(AppLanguage.System)
                    } ?: AppLanguage.System
                }
                .distinctUntilChanged()
                .collect { language ->
                    _appLanguage.value = language
                    _appLocale.value = createLocaleState(language)
                }
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        _appLanguage.value = language
        _appLocale.value = createLocaleState(language)
        appContext.languageDataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language.name
        }
    }

    @VisibleForTesting
    internal fun createLocaleState(language: AppLanguage): AppLocaleState {
        val locales = when (language) {
            AppLanguage.System -> LocaleList.getDefault()
            AppLanguage.Chinese -> LocaleList(Locale.SIMPLIFIED_CHINESE)
            AppLanguage.English -> LocaleList(Locale.ENGLISH)
        }
        if (language != AppLanguage.System && locales.size() > 0) {
            LocaleList.setDefault(locales)
            Locale.setDefault(locales[0])
        }
        return AppLocaleState(language = language, locales = locales)
    }

    companion object {
        private val KEY_LANGUAGE = stringPreferencesKey("pref_app_language")

        /**
         * 提前应用持久化语言，避免第三方登录返回后被系统语言覆盖。
         * 可在 Application.onCreate 中调用。
         */
        fun preloadPersistedLocale(context: Context) {
            runCatching {
                val persistedLanguage = runBlocking {
                    context.languageDataStore.data.first()[KEY_LANGUAGE]
                }?.let { saved ->
                    runCatching { AppLanguage.valueOf(saved) }.getOrDefault(AppLanguage.System)
                } ?: AppLanguage.System
                val locales = when (persistedLanguage) {
                    AppLanguage.System -> LocaleList.getDefault()
                    AppLanguage.Chinese -> LocaleList(Locale.SIMPLIFIED_CHINESE)
                    AppLanguage.English -> LocaleList(Locale.ENGLISH)
                }
                if (locales.size() > 0) {
                    LocaleList.setDefault(locales)
                    Locale.setDefault(locales[0])
                    val resources = context.resources
                    val config = Configuration(resources.configuration).apply { setLocales(locales) }
                    @Suppress("DEPRECATION")
                    resources.updateConfiguration(config, resources.displayMetrics)
                }
            }
        }

        /**
         * 为 Activity 提供带持久化语言的 baseContext，确保跳转外部页面返回后仍然维持 App 语言。
         */
        fun wrapWithPersistedLocale(base: Context): Context {
            val persistedLanguage = runBlocking {
                base.languageDataStore.data.first()[KEY_LANGUAGE]
            }?.let { saved ->
                runCatching { AppLanguage.valueOf(saved) }.getOrDefault(AppLanguage.System)
            } ?: AppLanguage.System
            val locales = when (persistedLanguage) {
                AppLanguage.System -> LocaleList.getDefault()
                AppLanguage.Chinese -> LocaleList(Locale.SIMPLIFIED_CHINESE)
                AppLanguage.English -> LocaleList(Locale.ENGLISH)
            }
            return if (locales.size() > 0) {
                val config = Configuration(base.resources.configuration).apply { setLocales(locales) }
                base.createConfigurationContext(config)
            } else {
                base
            }
        }
    }
}
