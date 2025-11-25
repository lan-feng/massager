package com.massager.app.domain.usecase.profile

// 文件说明：更新用户资料并返回结果的域层用例。
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
