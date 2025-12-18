package com.massager.app.data.remote

// 文件说明：统一为所有网络请求注入 Accept-Language 头，语言来源于应用内语言设置。
import com.massager.app.core.preferences.LanguageManager
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class AcceptLanguageInterceptor @Inject constructor(
    private val languageManager: LanguageManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val languageTag = resolveLanguageTag()

        val request = original.newBuilder()
            .removeHeader(HEADER_ACCEPT_LANGUAGE)
            .addHeader(HEADER_ACCEPT_LANGUAGE, languageTag)
            .build()

        return chain.proceed(request)
    }

    private fun resolveLanguageTag(): String {
        val locale = languageManager.appLocale.value.locales.takeIf { it.size() > 0 }?.get(0)
            ?: Locale.getDefault()
        return locale.toLanguageTag()
    }

    private companion object {
        private const val HEADER_ACCEPT_LANGUAGE = "Accept-Language"
    }
}
