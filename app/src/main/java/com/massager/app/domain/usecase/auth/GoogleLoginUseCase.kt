package com.massager.app.domain.usecase.auth

// 文件说明：封装 Google 登录调用。
import com.massager.app.data.repository.AuthRepository
import com.massager.app.domain.model.AuthResult
import javax.inject.Inject

class GoogleLoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): AuthResult {
        return repository.loginWithGoogle(idToken)
    }
}
