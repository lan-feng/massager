package com.massager.app.domain.usecase.device

import com.massager.app.data.repository.MassagerRepository
import javax.inject.Inject

class UpdateDeviceComboInfoUseCase @Inject constructor(
    private val repository: MassagerRepository
) {
    suspend operator fun invoke(deviceId: String, comboInfo: String): Result<Unit> =
        repository.updateDeviceComboInfo(deviceId, comboInfo)
}
