package com.massager.app.domain.usecase.device

import com.massager.app.data.repository.MassagerRepository
import com.massager.app.domain.model.DeviceMetadata
import javax.inject.Inject

class RenameDeviceUseCase @Inject constructor(
    private val repository: MassagerRepository
) {
    suspend operator fun invoke(deviceId: String, newName: String): Result<DeviceMetadata> =
        repository.renameDevice(deviceId, newName)
}
