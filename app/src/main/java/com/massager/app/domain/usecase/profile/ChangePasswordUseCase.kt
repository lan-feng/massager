package com.massager.app.domain.usecase.profile

// 文件说明：处理密码修改请求的业务入口。
import com.massager.app.data.repository.UserRepository
import javax.inject.Inject

class ChangePasswordUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(oldPassword: String, newPassword: String): Result<Unit> =
        repository.changePassword(oldPassword, newPassword)
}
