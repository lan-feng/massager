package com.massager.app.data.repository

// Handles login/registration, verification codes, logout, and session persistence for authentication flows.
import com.massager.app.BuildConfig
import androidx.room.withTransaction
import com.massager.app.core.avatar.DEFAULT_AVATAR_NAME
import com.massager.app.core.avatar.sanitizeAvatarForGoogle
import com.massager.app.data.local.MassagerDatabase
import com.massager.app.data.local.SessionManager
import com.massager.app.data.remote.AuthApiService
import com.massager.app.data.remote.dto.AuthRequest
import com.massager.app.data.remote.dto.FirebaseLoginRequest
import com.massager.app.data.remote.dto.RegisterRequest
import com.massager.app.data.remote.dto.ResetPasswordRequest
import com.massager.app.domain.model.AuthResult
import com.massager.app.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApiService,
    private val sessionManager: SessionManager,
    private val database: MassagerDatabase,
    private val firebaseAuth: FirebaseAuth
) {

    suspend fun login(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        runCatching {
            val envelope = api.login(AuthRequest(email, password))
            val response = envelope.data ?: error(envelope.message ?: "Empty login response")
            if (envelope.success.not()) {
                error(envelope.message ?: "Login failed")
            }
            sessionManager.saveAuthToken(response.token)
            sessionManager.saveUserId(response.user.id.toString())
            sessionManager.saveAppId(response.user.appId ?: BuildConfig.APP_ID)
            AuthResult.LoginSuccess(
                user = User(
                    id = response.user.id.toString(),
                    displayName = response.user.name,
                    email = response.user.email,
                    avatarUrl = response.user.avatarUrl,
                    appId = response.user.appId
                )
            )
        }.getOrElse {
            AuthResult.Error(message = it.message ?: "Login failed")
        }
    }

    suspend fun loginWithGoogle(idToken: String): AuthResult = withContext(Dispatchers.IO) {
        runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = firebaseAuth.currentUser ?: error("Unable to retrieve Firebase user")
            // Pass the verified Firebase ID token to backend instead of the raw Google Sign-In token
            val firebaseIdToken = firebaseUser.getIdToken(true).await().token
                ?: error("Unable to retrieve Firebase ID token")

            val exchangeRequest = FirebaseLoginRequest(
                idToken = firebaseIdToken,
                appId = sessionManager.appId() ?: BuildConfig.APP_ID
            )
            val envelope = api.loginWithFirebase(exchangeRequest)
            val response = envelope.data ?: error(envelope.message ?: "Firebase login returned empty data")
            if (envelope.success.not()) {
                error(envelope.message ?: "Firebase login failed")
            }

            sessionManager.saveAuthToken(response.token)
            sessionManager.saveUserId(response.user.id.toString())
            sessionManager.saveAppId(response.user.appId ?: BuildConfig.APP_ID)

            val avatarName = sanitizeAvatarForGoogle(
                avatarUrl = response.user.avatarUrl ?: firebaseUser.photoUrl?.toString(),
                hasGoogleAccount = true
            ) ?: DEFAULT_AVATAR_NAME

            AuthResult.LoginSuccess(
                user = User(
                    id = response.user.id.toString(),
                    displayName = response.user.name.ifBlank {
                        firebaseUser.displayName ?: firebaseUser.email ?: "Google user"
                    },
                    email = response.user.email.ifBlank { firebaseUser.email.orEmpty() },
                    avatarUrl = avatarName,
                    appId = response.user.appId
                )
            )
        }.getOrElse { throwable ->
            AuthResult.Error(message = throwable.message ?: "Google login failed")
        }
    }

    suspend fun register(name: String, email: String, password: String, verificationCode: String): AuthResult =
        withContext(Dispatchers.IO) {
            runCatching {
                val envelope = api.register(
                    RegisterRequest(
                        email = email,
                        password = password,
                        name = name,
                        verificationCode = verificationCode,
                        appId = BuildConfig.APP_ID
                    )
                )
                if (envelope.success.not() || envelope.data == null) {
                    error(envelope.message ?: "Registration failed")
                }
                val registeredUser = envelope.data
                sessionManager.saveUserId(registeredUser.id.toString())
                sessionManager.saveAppId(registeredUser.appId ?: BuildConfig.APP_ID)
                AuthResult.RegisterSuccess(
                    user = User(
                        id = registeredUser.id.toString(),
                        displayName = registeredUser.name,
                        email = registeredUser.email,
                        avatarUrl = registeredUser.avatarUrl,
                        appId = registeredUser.appId
                    )
                )
            }.getOrElse {
                AuthResult.Error(message = it.message ?: "Registration failed")
            }
        }

    suspend fun sendRegisterVerificationCode(email: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val envelope = api.sendRegisterCode(email)
                if (envelope.success.not()) {
                    error(envelope.message ?: "Failed to send verification code")
                }
            }
        }

    suspend fun sendPasswordResetCode(email: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val envelope = api.sendRegisterCode(email)
                if (envelope.success.not()) {
                    error(envelope.message ?: "Failed to send verification code")
                }
            }
        }

    suspend fun resetPassword(email: String, verificationCode: String, newPassword: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val envelope = api.resetPassword(
                    ResetPasswordRequest(
                        email = email,
                        newPassword = newPassword,
                        verificationCode = verificationCode
                    )
                )
                if (envelope.success.not()) {
                    error(envelope.message ?: "Password reset failed")
                }
            }
        }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        val isGuest = sessionManager.isGuestMode()
        val ownerId = if (isGuest) {
            SessionManager.GUEST_USER_ID
        } else {
            sessionManager.accountOwnerId()
        }
        val apiResult = if (isGuest) {
            Result.success(Unit)
        } else {
            runCatching {
                val response = api.logout()
                if (response.success.not()) {
                    throw IllegalStateException(response.message ?: "Logout failed")
                }
            }
        }
        val clearResult = runCatching { clearLocalData(ownerId) }
        sessionManager.clear()
        if (apiResult.isFailure) apiResult else clearResult
    }

    private suspend fun clearLocalData(ownerId: String?) {
        if (ownerId.isNullOrBlank()) {
            database.clearAllTables()
            return
        }
        database.withTransaction {
            val ownedDevices = database.deviceDao().listDevicesForOwner(ownerId)
            ownedDevices.forEach { device ->
                database.measurementDao().deleteForDevice(device.id)
            }
            database.deviceDao().clearForOwner(ownerId)
            database.userDao().clear()
        }
    }
}

