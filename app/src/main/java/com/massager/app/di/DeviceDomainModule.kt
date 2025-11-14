package com.massager.app.di

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
