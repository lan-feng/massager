package com.massager.app.domain.usecase.device

// 文件说明：封装设备组合信息更新的域层用例。
import com.massager.app.data.repository.MassagerRepository
import javax.inject.Inject

class UpdateDeviceComboInfoUseCase @Inject constructor(
    private val repository: MassagerRepository
) {
    suspend operator fun invoke(deviceId: String, comboInfo: String): Result<Unit> =
        repository.updateDeviceComboInfo(deviceId, comboInfo)
}
