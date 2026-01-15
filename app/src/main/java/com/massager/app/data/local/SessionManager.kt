package com.massager.app.data.local

// 文件说明：管理本地会话信息、令牌与账户模式切换。
import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)
    private val _sessionEvents = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 1)
    val sessionEvents: SharedFlow<SessionEvent> = _sessionEvents.asSharedFlow()

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

    fun saveGuestAvatarName(name: String) {
        if (name.isBlank()) return
        prefs.edit().putString(KEY_GUEST_AVATAR_NAME, name).apply()
    }

    fun guestAvatarName(): String? = prefs.getString(KEY_GUEST_AVATAR_NAME, null)

    fun saveAccountAvatarName(name: String) {
        if (name.isBlank()) return
        prefs.edit().putString(KEY_ACCOUNT_AVATAR_NAME, name).apply()
    }

    fun accountAvatarName(): String? = prefs.getString(KEY_ACCOUNT_AVATAR_NAME, null)

    fun saveGuestAvatar(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        prefs.edit().putString(KEY_GUEST_AVATAR, encoded).apply()
    }

    fun saveAccountAvatar(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        prefs.edit().putString(KEY_ACCOUNT_AVATAR, encoded).apply()
    }

    fun guestAvatar(): ByteArray? =
        prefs.getString(KEY_GUEST_AVATAR, null)?.let { encoded ->
            runCatching { Base64.decode(encoded, Base64.NO_WRAP) }.getOrNull()
        }

    fun accountAvatar(): ByteArray? =
        prefs.getString(KEY_ACCOUNT_AVATAR, null)?.let { encoded ->
            runCatching { Base64.decode(encoded, Base64.NO_WRAP) }.getOrNull()
        }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun handleAuthExpired() {
        clear()
        _sessionEvents.tryEmit(SessionEvent.AuthExpired)
    }

    fun activeOwnerId(): String =
        userId()?.takeIf { it.isNotBlank() }
            ?: if (isGuestMode()) GUEST_USER_ID else GUEST_USER_ID

    fun accountOwnerId(): String? = userId()?.takeIf { it.isNotBlank() }

    companion object {
        const val KEY_TOKEN = "key_token"
        const val KEY_USER_ID = "key_user_id"
        const val KEY_APP_ID = "key_app_id"
        const val KEY_GUEST_MODE = "key_guest_mode"
        const val KEY_GUEST_NAME = "key_guest_name"
        const val KEY_GUEST_AVATAR_NAME = "key_guest_avatar_name"
        const val KEY_GUEST_AVATAR = "key_guest_avatar"
        const val KEY_ACCOUNT_AVATAR_NAME = "key_account_avatar_name"
        const val KEY_ACCOUNT_AVATAR = "key_account_avatar"

        const val GUEST_USER_ID = "guest"
    }
}

sealed class SessionEvent {
    data object AuthExpired : SessionEvent()
}
