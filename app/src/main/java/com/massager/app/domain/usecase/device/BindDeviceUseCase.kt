package com.massager.app.domain.usecase.device

import com.massager.app.data.repository.MassagerRepository
import com.massager.app.domain.model.DeviceMetadata
import javax.inject.Inject

class BindDeviceUseCase @Inject constructor(
    private val repository: MassagerRepository
) {
    suspend operator fun invoke(
        serial: String,
        displayName: String,
        firmwareVersion: String? = null
    ): Result<DeviceMetadata> {
        return repository.bindDevice(
            deviceSerial = serial,
            displayName = displayName,
            firmwareVersion = firmwareVersion
        )
    }
}
