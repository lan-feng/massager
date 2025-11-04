package com.massager.app.domain.usecase.device

import com.massager.app.data.repository.MassagerRepository
import javax.inject.Inject

class RemoveDeviceUseCase @Inject constructor(
    private val repository: MassagerRepository
) {
    suspend operator fun invoke(deviceId: String): Result<Unit> =
        repository.removeDevice(deviceId)
}
