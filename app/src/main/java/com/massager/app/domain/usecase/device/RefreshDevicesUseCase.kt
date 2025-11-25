package com.massager.app.domain.usecase.device

// 文件说明：刷新设备列表数据，触发仓库同步。
import com.massager.app.data.repository.MassagerRepository
import com.massager.app.domain.model.DeviceMetadata
import javax.inject.Inject

class RefreshDevicesUseCase @Inject constructor(
    private val repository: MassagerRepository
) {
    suspend operator fun invoke(
        deviceTypes: List<Int> = emptyList()
    ): Result<List<DeviceMetadata>> {
        return if (deviceTypes.isEmpty()) {
            repository.refreshDevices()
        } else {
            repository.refreshDevices(deviceTypes)
        }
    }
}
