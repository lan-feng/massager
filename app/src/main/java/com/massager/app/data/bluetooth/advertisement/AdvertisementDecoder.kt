package com.massager.app.data.bluetooth.advertisement

// 文件说明：蓝牙广播解码器接口，规范解析流程。
import android.bluetooth.le.ScanResult

interface AdvertisementDecoder {
    fun decode(result: ScanResult): ProtocolAdvertisement?
}
