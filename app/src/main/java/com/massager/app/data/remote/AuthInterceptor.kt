package com.massager.app.data.remote

// 文件说明：为网络请求附加认证信息与应用标识的 OkHttp 拦截器。
import com.massager.app.BuildConfig
import com.massager.app.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        val appId = sessionManager.appId() ?: BuildConfig.APP_ID
        requestBuilder.addHeader("x-app-id", appId)

        if (shouldAttachAuthHeader(originalRequest.url.encodedPath, originalRequest.header("Authorization"))) {
            sessionManager.authToken()?.takeIf { it.isNotBlank() }?.let { token ->
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
        }

        return chain.proceed(requestBuilder.build())
    }

    private fun shouldAttachAuthHeader(path: String, existingAuthorization: String?): Boolean {
        if (!existingAuthorization.isNullOrBlank()) {
            return false
        }

        return AUTH_EXCLUDED_PATH_SUFFIXES.none { suffix -> path.endsWith(suffix) }
    }

    private companion object {
        private val AUTH_EXCLUDED_PATH_SUFFIXES = setOf(
            "/auth/v1/login",
            "/auth/v1/register"
        )
    }
}
