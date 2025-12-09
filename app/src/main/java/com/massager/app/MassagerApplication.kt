package com.massager.app

// 文件说明：Hilt 注入的应用入口，用于初始化 Firebase 与 Crashlytics 等全局配置。
import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale

@HiltAndroidApp
class MassagerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        applySavedTheme()
        applySavedLocale()

        FirebaseApp.initializeApp(this)
        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(BuildConfig.CRASHLYTICS_ENABLED)
            setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            setCustomKey("version_code", BuildConfig.VERSION_CODE)
            setCustomKey("version_name", BuildConfig.VERSION_NAME)
        }
    }

    private fun applySavedTheme() {
        val prefs = getSharedPreferences("user_settings", Context.MODE_PRIVATE)
        when (prefs.getString("pref_app_theme", "System")) {
            "Light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun applySavedLocale() {
        val prefs = getSharedPreferences("user_settings", Context.MODE_PRIVATE)
        val saved = prefs.getString("pref_app_language", "System")
        val locales = when (saved) {
            "Chinese" -> LocaleListCompat.create(Locale.SIMPLIFIED_CHINESE)
            "English" -> LocaleListCompat.create(Locale.ENGLISH)
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
