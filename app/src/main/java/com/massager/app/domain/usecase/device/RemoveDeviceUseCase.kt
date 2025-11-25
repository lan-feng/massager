package com.massager.app.domain.usecase.device

// 文件说明：处理解绑或删除设备的业务流程。
import com.massager.app.data.repository.MassagerRepository
import javax.inject.Inject

class RemoveDeviceUseCase @Inject constructor(
    private val repository: MassagerRepository
) {
    suspend operator fun invoke(deviceId: String): Result<Unit> =
        repository.removeDevice(deviceId)
}
