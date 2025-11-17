package com.massager.app.data.bluetooth.advertisement

import android.bluetooth.le.ScanResult
import android.util.Log
import com.massager.app.core.logging.logTag
import com.massager.app.data.bluetooth.protocol.HyAdvertisement
import javax.inject.Inject
import kotlin.math.min

class HyAdvertisementDecoder @Inject constructor() : AdvertisementDecoder {

    override fun decode(result: ScanResult): ProtocolAdvertisement? {
        val payload = extractHyPayload(result.scanRecord?.bytes)
        val fallbackName = try {
            result.device?.name
        } catch (securityException: SecurityException) {
            Log.w(TAG, "decode: unable to access device name", securityException)
            null
        }
        val parsed = HyAdvertisement.parse(payload)
            ?: HyAdvertisement.parse(fallbackName)
        if (parsed == null) {
            Log.v(TAG, "decode: unable to interpret advertisement for ${result.device?.address}")
            return null
        }
        return ProtocolAdvertisement(
            productId = parsed.productId,
            firmwareVersion = parsed.firmwareVersion,
            uniqueId = parsed.uniqueId
        )
    }

    private fun extractHyPayload(bytes: ByteArray?): ByteArray? {
        if (bytes == null) return null
        if (bytes.size < HyAdvertisement.MIN_PAYLOAD_LENGTH) return null
        for (index in 0 until bytes.size - 1) {
            if (bytes[index] == HY_HEADER_FIRST && bytes[index + 1] == HY_HEADER_SECOND) {
                val endExclusive = min(bytes.size, index + HY_PAYLOAD_LENGTH)
                return bytes.copyOfRange(index, endExclusive)
            }
        }
        return null
    }

    companion object {
        private val TAG = logTag("HyAdDecoder")
        private const val HY_PAYLOAD_LENGTH = HyAdvertisement.MIN_PAYLOAD_LENGTH
        private const val HY_HEADER_FIRST: Byte = 0x68
        private const val HY_HEADER_SECOND: Byte = 0x79
    }
}
