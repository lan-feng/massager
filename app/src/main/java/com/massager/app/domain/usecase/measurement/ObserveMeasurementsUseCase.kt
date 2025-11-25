package com.massager.app.domain.usecase.measurement

// 文件说明：订阅本地测量记录变更的流式接口。
import com.massager.app.data.repository.MassagerRepository
import com.massager.app.domain.model.TemperatureRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveMeasurementsUseCase @Inject constructor(
    private val repository: MassagerRepository
) {
    operator fun invoke(): Flow<List<TemperatureRecord>> = repository.recentMeasurements()
}
