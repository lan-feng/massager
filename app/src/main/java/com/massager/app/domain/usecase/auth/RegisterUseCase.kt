package com.massager.app.domain.usecase.auth

import com.massager.app.data.repository.AuthRepository
import com.massager.app.domain.model.AuthResult
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(name: String, email: String, password: String): AuthResult {
        return repository.register(name, email, password)
    }
}
