package com.massager.app.domain.usecase.profile

import com.massager.app.data.repository.UserRepository
import com.massager.app.domain.model.UserProfile
import javax.inject.Inject

class UpdateUserProfileUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend fun updateName(name: String): Result<UserProfile> =
        repository.updateName(name)

    suspend fun updateAvatarUrl(url: String): Result<UserProfile> =
        repository.updateAvatar(url)
}
