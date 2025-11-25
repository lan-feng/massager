package com.massager.app.domain.usecase.auth

// 文件说明：开启游客模式并准备本地会话的用例。
import com.massager.app.BuildConfig
import com.massager.app.data.local.SessionManager
import javax.inject.Inject

class EnableGuestModeUseCase @Inject constructor(
    private val sessionManager: SessionManager
){
    operator fun invoke() {
        sessionManager.clear()
        sessionManager.saveUserId(SessionManager.GUEST_USER_ID)
        sessionManager.saveAppId(BuildConfig.APP_ID)
        sessionManager.enableGuestMode()
    }
}
