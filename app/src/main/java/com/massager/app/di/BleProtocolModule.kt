package com.massager.app.di

// 文件说明：注册蓝牙协议适配器与广播解码器到 Hilt 集合供扫描与通讯使用。
import com.massager.app.data.bluetooth.advertisement.AdvertisementDecoder
import com.massager.app.data.bluetooth.advertisement.HyAdvertisementDecoder
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

    @Provides
    @IntoSet
    fun provideHyAdvertisementDecoder(): AdvertisementDecoder = HyAdvertisementDecoder()
}
