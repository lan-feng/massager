package com.massager.app.domain.usecase.profile

// 文件说明：负责头像文件上传的业务用例。
import com.massager.app.data.repository.UserRepository
import javax.inject.Inject

class UploadAvatarUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(bytes: ByteArray, fileName: String): Result<String> {
        return repository.uploadAvatar(bytes, fileName).map { it.url }
    }
}
