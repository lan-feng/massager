package com.massager.app.domain.usecase.settings

// 文件说明：包装登出业务，调用认证仓库清理会话并返回结果。
import com.massager.app.data.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.logout()
}
