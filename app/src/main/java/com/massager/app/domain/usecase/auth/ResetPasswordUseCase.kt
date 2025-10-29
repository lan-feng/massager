package com.massager.app.domain.usecase.auth

import com.massager.app.data.repository.AuthRepository
import javax.inject.Inject

class ResetPasswordUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend fun sendCode(email: String): Result<Unit> {
        return repository.sendPasswordResetCode(email)
    }

    suspend fun reset(email: String, verificationCode: String, newPassword: String): Result<Unit> {
        return repository.resetPassword(email, verificationCode, newPassword)
    }
}
