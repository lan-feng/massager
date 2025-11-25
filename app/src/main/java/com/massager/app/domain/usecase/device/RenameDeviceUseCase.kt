package com.massager.app.domain.usecase.device

// 文件说明：提供设备重命名的业务入口。
import com.massager.app.data.repository.MassagerRepository
import com.massager.app.domain.model.DeviceMetadata
import javax.inject.Inject

class RenameDeviceUseCase @Inject constructor(
    private val repository: MassagerRepository
) {
    suspend operator fun invoke(deviceId: String, newName: String): Result<DeviceMetadata> =
        repository.renameDevice(deviceId, newName)
}
