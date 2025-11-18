package com.massager.app.data.local

import android.content.Context
import android.util.Base64
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

    fun enableGuestMode() {
        prefs.edit()
            .putBoolean(KEY_GUEST_MODE, true)
            .apply()
    }

    fun disableGuestMode() {
        prefs.edit()
            .putBoolean(KEY_GUEST_MODE, false)
            .apply()
    }

    fun isGuestMode(): Boolean = prefs.getBoolean(KEY_GUEST_MODE, false)

    fun saveGuestName(name: String) {
        prefs.edit().putString(KEY_GUEST_NAME, name).apply()
    }

    fun guestName(): String? = prefs.getString(KEY_GUEST_NAME, null)

    fun saveGuestAvatar(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        prefs.edit().putString(KEY_GUEST_AVATAR, encoded).apply()
    }

    fun guestAvatar(): ByteArray? =
        prefs.getString(KEY_GUEST_AVATAR, null)?.let { encoded ->
            runCatching { Base64.decode(encoded, Base64.NO_WRAP) }.getOrNull()
        }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_TOKEN = "key_token"
        const val KEY_USER_ID = "key_user_id"
        const val KEY_APP_ID = "key_app_id"
        const val KEY_GUEST_MODE = "key_guest_mode"
        const val KEY_GUEST_NAME = "key_guest_name"
        const val KEY_GUEST_AVATAR = "key_guest_avatar"
    }
}
