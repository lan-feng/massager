package com.massager.app.domain.usecase.device

// 文件说明：执行设备绑定流程的域层用例。
import com.massager.app.data.repository.MassagerRepository
import com.massager.app.domain.model.DeviceMetadata
import javax.inject.Inject

class BindDeviceUseCase @Inject constructor(
    private val repository: MassagerRepository
) {
    suspend operator fun invoke(
        serial: String,
        displayName: String,
        productId: Int? = null,
        firmwareVersion: String? = null,
        uniqueId: String? = null
    ): Result<DeviceMetadata> {
        return repository.bindDevice(
            deviceSerial = serial,
            displayName = displayName,
            productId = productId,
            firmwareVersion = firmwareVersion,
            uniqueId = uniqueId
        )
    }
}
