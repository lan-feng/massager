package com.massager.app.domain.usecase.device

import com.massager.app.core.DeviceTypeConfig
import com.massager.app.data.repository.MassagerRepository
import com.massager.app.domain.model.DeviceMetadata
import javax.inject.Inject

class RefreshDevicesUseCase @Inject constructor(
    private val repository: MassagerRepository
) {
    suspend operator fun invoke(
        deviceTypes: List<Int> = DeviceTypeConfig.RESERVED_DEVICE_TYPES
    ): Result<List<DeviceMetadata>> {
        return repository.refreshDevices(deviceTypes)
    }
}
