package com.massager.app.data.repository

import com.massager.app.BuildConfig
import com.massager.app.data.local.SessionManager
import com.massager.app.data.remote.AuthApiService
import com.massager.app.data.remote.dto.AuthRequest
import com.massager.app.data.remote.dto.RegisterRequest
import com.massager.app.domain.model.AuthResult
import com.massager.app.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApiService,
    private val sessionManager: SessionManager
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

    suspend fun register(displayName: String, email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            runCatching {
                val envelope = api.register(
                    RegisterRequest(
                        email = email,
                        password = password,
                        displayName = displayName
                    )
                )
                if (envelope.success.not() || envelope.data == null) {
                    error(envelope.message ?: "Registration failed")
                }
                val response = envelope.data
                sessionManager.saveAuthToken(response.token)
                sessionManager.saveUserId(response.user.id.toString())
                sessionManager.saveAppId(response.user.appId ?: BuildConfig.APP_ID)
                AuthResult.RegisterSuccess(
                    user = User(
                        id = response.user.id.toString(),
                        displayName = response.user.name,
                        email = response.user.email,
                        avatarUrl = response.user.avatarUrl,
                        appId = response.user.appId
                    )
                )
            }.getOrElse {
                AuthResult.Error(message = it.message ?: "Registration failed")
            }
        }

    fun logout() {
        sessionManager.clear()
    }
}
