package com.massager.app.data.bluetooth.protocol

// 文件说明：HY 设备广播数据模型与解析逻辑。
import java.util.Locale

/**
 * Representation of the `'hy'` advertisement emitted by JL6328 based EMS devices.
 *
 * The payload layout is: `0x68 0x79 <productId> <fwVersion> <uniqueId[4]>`
 */
data class HyAdvertisement(
    val productId: Int,
    val firmwareVersion: String,
    val uniqueId: String? = null
) {
    companion object {
        private const val HEADER_FIRST: Byte = 0x68
        private const val HEADER_SECOND: Byte = 0x79
        private const val UNIQUE_ID_START = 4
        private const val UNIQUE_ID_LENGTH = 4
        const val MIN_PAYLOAD_LENGTH = UNIQUE_ID_START + UNIQUE_ID_LENGTH

        /**
         * Attempts to parse the given [payload] into a [HyAdvertisement].
         */
        fun parse(payload: ByteArray?): HyAdvertisement? {
            if (payload == null || payload.size < 4) return null
            if (payload[0] != HEADER_FIRST || payload[1] != HEADER_SECOND) return null
            val productRaw = payload[2]
            val productId = when (val numeric = productRaw.toInt() and 0xFF) {
                in '0'.code..'9'.code -> numeric - '0'.code
                else -> numeric
            }
            val firmwareVersion = decodeFirmwareVersion(payload.getOrNull(3))
            val uniqueId = decodeUniqueId(payload)
            return HyAdvertisement(productId, firmwareVersion, uniqueId)
        }

        fun parse(advertisedName: String?): HyAdvertisement? {
            if (advertisedName.isNullOrBlank()) return null
            val prefixed = advertisedName.trim()
            if (!prefixed.startsWith("hy", ignoreCase = true) || prefixed.length < 3) return null
            val productChar = prefixed[2]
            val productId = productChar.digitToIntOrNull() ?: return null
            val version = if (prefixed.length > 3) prefixed.substring(3) else "unknown"
            return HyAdvertisement(productId, version)
        }

        private fun decodeFirmwareVersion(byte: Byte?): String {
            if (byte == null) return "unknown"
            val value = byte.toInt() and 0xFF
            val candidate = value.toChar()
            return if (candidate.isLetterOrDigit() || candidate == '.' || candidate == '-') {
                candidate.toString()
            } else {
                value.toString()
            }
        }

        private fun decodeUniqueId(payload: ByteArray): String? {
            if (payload.size < UNIQUE_ID_START + UNIQUE_ID_LENGTH) return null
            val builder = StringBuilder(UNIQUE_ID_LENGTH * 2)
            for (index in UNIQUE_ID_START until UNIQUE_ID_START + UNIQUE_ID_LENGTH) {
                builder.append(
                    String.format(
                        Locale.US,
                        "%02X",
                        payload[index].toInt() and 0xFF
                    )
                )
            }
            return builder.toString()
        }
    }
}
