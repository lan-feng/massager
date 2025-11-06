package com.massager.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MassagerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(BuildConfig.CRASHLYTICS_ENABLED)
            setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            setCustomKey("version_code", BuildConfig.VERSION_CODE)
            setCustomKey("version_name", BuildConfig.VERSION_NAME)
        }
    }
}
