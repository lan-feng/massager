package com.massager.app.domain.usecase.auth

// 文件说明：处理注册业务，调用仓库并返回认证结果。
import com.massager.app.data.repository.AuthRepository
import com.massager.app.domain.model.AuthResult
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        name: String,
        email: String,
        password: String,
        verificationCode: String
    ): AuthResult {
        return repository.register(name, email, password, verificationCode)
    }

    suspend fun sendVerificationCode(email: String): Result<Unit> {
        return repository.sendRegisterVerificationCode(email)
    }
}
