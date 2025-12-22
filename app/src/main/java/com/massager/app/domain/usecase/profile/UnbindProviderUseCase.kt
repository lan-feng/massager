package com.massager.app.domain.usecase.profile

// 文件说明：解绑第三方账号的用例。
import com.massager.app.data.repository.UserRepository
import javax.inject.Inject

class UnbindProviderUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(provider: String): Result<Unit> = repository.unbindProvider(provider)
}
