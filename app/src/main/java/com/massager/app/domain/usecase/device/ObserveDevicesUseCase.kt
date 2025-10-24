package com.massager.app.domain.usecase.device

import com.massager.app.data.repository.MassagerRepository
import com.massager.app.domain.model.DeviceMetadata
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDevicesUseCase @Inject constructor(
    private val repository: MassagerRepository
) {
    operator fun invoke(): Flow<List<DeviceMetadata>> = repository.deviceMetadata()
}
