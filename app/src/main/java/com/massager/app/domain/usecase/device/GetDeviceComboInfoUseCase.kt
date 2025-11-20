package com.massager.app.domain.usecase.device

import com.massager.app.data.repository.MassagerRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class GetDeviceComboInfoUseCase @Inject constructor(
    private val repository: MassagerRepository
) {
    suspend operator fun invoke(deviceId: String): String? =
        repository.observeDeviceComboInfo(deviceId).first()
}
