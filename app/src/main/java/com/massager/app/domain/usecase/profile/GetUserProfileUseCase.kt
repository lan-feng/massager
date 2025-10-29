package com.massager.app.domain.usecase.profile

import com.massager.app.data.repository.UserRepository
import com.massager.app.domain.model.UserProfile
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(): Result<UserProfile> = repository.fetchProfile()
}
