package com.massager.app.domain.usecase.auth

// 文件说明：执行用户登录逻辑的域层入口。
import com.massager.app.data.repository.AuthRepository
import com.massager.app.domain.model.AuthResult
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): AuthResult {
        return repository.login(email, password)
    }
}
