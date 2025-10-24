package com.massager.app.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun authToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveUserId(id: String) {
        prefs.edit().putString(KEY_USER_ID, id).apply()
    }

    fun userId(): String? = prefs.getString(KEY_USER_ID, null)

    fun saveAppId(appId: String) {
        prefs.edit().putString(KEY_APP_ID, appId).apply()
    }

    fun appId(): String? = prefs.getString(KEY_APP_ID, null)

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_TOKEN = "key_token"
        const val KEY_USER_ID = "key_user_id"
        const val KEY_APP_ID = "key_app_id"
    }
}
