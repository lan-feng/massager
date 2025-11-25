package com.massager.app.domain.usecase.device

// 文件说明：监听设备列表变化的流式用例。
import com.massager.app.data.repository.MassagerRepository
import com.massager.app.domain.model.DeviceMetadata
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDevicesUseCase @Inject constructor(
    private val repository: MassagerRepository
) {
    operator fun invoke(): Flow<List<DeviceMetadata>> = repository.deviceMetadata()
}
