package com.massager.app.data.bluetooth.session

import kotlin.math.min

class EmsFrameExtractor : FrameExtractor {

    override fun extract(buffer: ByteArray): FrameExtraction? {
        if (buffer.isEmpty()) return null
        val headerIndex = buffer.indexOfFirstHeader()
        if (headerIndex < 0) {
            return FrameExtraction(frame = null, consumed = buffer.size)
        }
        if (headerIndex > 0) {
            return FrameExtraction(frame = null, consumed = headerIndex)
        }
        if (buffer.size < MIN_FRAME_SIZE) return null

        val terminatorIndex = buffer.indexOfTerminator(startIndex = HEADER_LENGTH + LENGTH_FIELD)
        if (terminatorIndex < 0) return null
        val advertisedLength = toUInt16Be(buffer[HEADER_LENGTH], buffer[HEADER_LENGTH + 1])
        val expectedEnd = HEADER_LENGTH + advertisedLength
        if (expectedEnd > buffer.size) return null
        val endExclusive = min(buffer.size, terminatorIndex + TERMINATOR_LENGTH)
        if (endExclusive > buffer.size) return null
        val frame = buffer.copyOfRange(0, endExclusive)
        return FrameExtraction(frame = frame, consumed = endExclusive)
    }

    private fun ByteArray.indexOfFirstHeader(): Int {
        for (index in indices) {
            if (this[index] == EMS_V2_HEADER_FIRST && index + 1 < size && this[index + 1] == EMS_V2_HEADER_SECOND) {
                return index
            }
        }
        return -1
    }

    private fun ByteArray.indexOfTerminator(startIndex: Int): Int {
        val begin = startIndex.coerceIn(0, size)
        for (index in begin until size - 1) {
            if (this[index] == EMS_V2_TERMINATOR_CR && this[index + 1] == EMS_V2_TERMINATOR_LF) {
                return index
            }
        }
        return -1
    }

    private fun toUInt16Be(high: Byte, low: Byte): Int =
        ((high.toInt() and 0xFF) shl 8) or (low.toInt() and 0xFF)

    companion object {
        private const val EMS_V2_HEADER_FIRST: Byte = 0x68
        private const val EMS_V2_HEADER_SECOND: Byte = 0x79
        private const val EMS_V2_TERMINATOR_CR: Byte = 0x0D
        private const val EMS_V2_TERMINATOR_LF: Byte = 0x0A
        private const val HEADER_LENGTH = 2
        private const val LENGTH_FIELD = 2
        private const val MIN_FRAME_SIZE = 8
        private const val TERMINATOR_LENGTH = 2
    }
}
