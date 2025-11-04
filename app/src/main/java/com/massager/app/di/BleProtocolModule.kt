package com.massager.app.di

import com.massager.app.data.bluetooth.protocol.BleProtocolAdapter
import com.massager.app.data.bluetooth.protocol.EmsV2ProtocolAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object BleProtocolModule {

    @Provides
    @IntoSet
    fun provideEmsV2ProtocolAdapter(): BleProtocolAdapter = EmsV2ProtocolAdapter()
}
