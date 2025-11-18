package com.massager.app.domain.usecase.auth

import com.massager.app.BuildConfig
import com.massager.app.data.local.SessionManager
import javax.inject.Inject

class EnableGuestModeUseCase @Inject constructor(
    private val sessionManager: SessionManager
){
    operator fun invoke() {
        sessionManager.clear()
        sessionManager.saveUserId("guest")
        sessionManager.saveAppId(BuildConfig.APP_ID)
        sessionManager.enableGuestMode()
    }
}
