package com.massager.app.domain.usecase.profile

import com.massager.app.data.repository.UserRepository
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(userId: Long): Result<Unit> =
        repository.deleteAccount(userId)
}
