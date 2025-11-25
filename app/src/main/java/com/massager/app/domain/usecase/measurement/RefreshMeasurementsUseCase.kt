package com.massager.app.domain.usecase.measurement

// 文件说明：触发测量数据刷新与同步的业务用例。
import com.massager.app.data.repository.MassagerRepository
import com.massager.app.domain.model.TemperatureRecord
import javax.inject.Inject

class RefreshMeasurementsUseCase @Inject constructor(
    private val repository: MassagerRepository
) {
    suspend operator fun invoke(deviceId: String): Result<List<TemperatureRecord>> {
        return repository.refreshMeasurements(deviceId)
    }
}
