package com.massager.app.domain.usecase.device

// 文件说明：订阅指定设备组合信息的变更。
import com.massager.app.data.repository.MassagerRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveDeviceComboInfoUseCase @Inject constructor(
    private val repository: MassagerRepository
) {
    operator fun invoke(deviceId: String): Flow<String?> =
        repository.observeDeviceComboInfo(deviceId)
}
