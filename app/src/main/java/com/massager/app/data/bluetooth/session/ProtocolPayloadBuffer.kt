package com.massager.app.data.bluetooth.session

import android.util.Log
import com.massager.app.core.logging.logTag

class ProtocolPayloadBuffer(
    private val extractor: FrameExtractor
) {

    private val lock = Any()
    private var buffer = ByteArray(0)

    fun append(payload: ByteArray): List<ByteArray> {
        if (payload.isEmpty()) return emptyList()
        val frames = mutableListOf<ByteArray>()
        synchronized(lock) {
            buffer += payload
            while (true) {
                val extraction = extractor.extract(buffer) ?: break
                extraction.frame?.let(frames::add)
                buffer = if (extraction.consumed >= buffer.size) {
                    ByteArray(0)
                } else {
                    buffer.copyOfRange(extraction.consumed, buffer.size)
                }
            }
            if (buffer.size > BUFFER_CEILING) {
                Log.w(TAG, "append: clearing oversized buffer size=${buffer.size}")
                buffer = ByteArray(0)
            }
        }
        return frames
    }

    fun clear() {
        synchronized(lock) {
            buffer = ByteArray(0)
        }
    }

    companion object {
        private const val BUFFER_CEILING = 256
        private val TAG = logTag("ProtocolPayloadBuffer")
    }
}
