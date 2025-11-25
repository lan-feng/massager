package com.massager.app.di

// 文件说明：绑定设备域层实现到接口集合，注入 EMS 会话与遥测映射器。
import com.massager.app.domain.device.DeviceSession
import com.massager.app.domain.device.DeviceTelemetryMapper
import com.massager.app.domain.device.EmsDeviceSession
import com.massager.app.domain.device.EmsDeviceTelemetryMapper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class DeviceDomainModule {

    @Binds
    @IntoSet
    abstract fun bindEmsDeviceSession(session: EmsDeviceSession): DeviceSession

    @Binds
    @IntoSet
    abstract fun bindEmsTelemetryMapper(mapper: EmsDeviceTelemetryMapper): DeviceTelemetryMapper
}
