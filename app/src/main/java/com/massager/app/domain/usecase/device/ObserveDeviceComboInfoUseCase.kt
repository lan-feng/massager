package com.massager.app.domain.usecase.device

import com.massager.app.data.repository.MassagerRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveDeviceComboInfoUseCase @Inject constructor(
    private val repository: MassagerRepository
) {
    operator fun invoke(deviceId: String): Flow<String?> =
        repository.observeDeviceComboInfo(deviceId)
}
