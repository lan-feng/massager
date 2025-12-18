package com.massager.app

// 文件说明：Hilt 注入的应用入口，用于初始化 Firebase 与 Crashlytics 等全局配置。
import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.massager.app.core.preferences.LanguageManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MassagerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 在进程启动时预先应用持久化语言，避免外部 Activity 返回后被系统语言覆盖
        LanguageManager.preloadPersistedLocale(this)

        FirebaseApp.initializeApp(this)
        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(BuildConfig.CRASHLYTICS_ENABLED)
            setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            setCustomKey("version_code", BuildConfig.VERSION_CODE)
            setCustomKey("version_name", BuildConfig.VERSION_NAME)
        }
    }
}
