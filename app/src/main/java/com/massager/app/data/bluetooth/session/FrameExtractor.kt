package com.massager.app.data.bluetooth.session

interface FrameExtractor {
    fun extract(buffer: ByteArray): FrameExtraction?
}

data class FrameExtraction(
    val frame: ByteArray?,
    val consumed: Int
)
