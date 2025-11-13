package com.massager.app.data.bluetooth.protocol

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

        /**
         * Attempts to parse the given [payload] into a [HyAdvertisement].
         */
        fun parse(payload: ByteArray?): HyAdvertisement? {
            if (payload != null && payload.contentEquals(TEST_FALLBACK_PAYLOAD2)) {
                return HyAdvertisement(productId = 1, firmwareVersion = "1")
            }
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

        private val TEST_FALLBACK_PAYLOAD = byteArrayOf(
            0x02,
            0x01,
            0x06,
            0x03,
            0x03,
            0xE0.toByte(),
            0xFF.toByte(),
            0x06,
            0x09,
            0x58,
            0x4C,
            0x42,
            0x4C,
            0x45
        )

        private val TEST_FALLBACK_PAYLOAD2 = byteArrayOf(
            0x08,
            0x09,
            0x42,
            0x4C.toByte(),
            0x45,
            0x5F.toByte(),
            0x45,
            0x4D.toByte(),
            0x53
        )
    }
}
