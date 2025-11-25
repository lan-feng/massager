package com.massager.app.data.bluetooth.session

// 文件说明：通用蓝牙数据帧抽取接口。
interface FrameExtractor {
    fun extract(buffer: ByteArray): FrameExtraction?
}

data class FrameExtraction(
    val frame: ByteArray?,
    val consumed: Int
)
