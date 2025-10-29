package com.massager.app.domain.usecase.profile

import com.massager.app.data.repository.UserRepository
import javax.inject.Inject

class UploadAvatarUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(bytes: ByteArray, fileName: String): Result<String> {
        return repository.uploadAvatar(bytes, fileName).map { it.url }
    }
}
