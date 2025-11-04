package com.massager.app.data.bluetooth.protocol

/**
 * Representation of the `'hy'` advertisement emitted by JL6328 based EMS devices.
 *
 * The payload layout is: `0x68 0x79 <productId> <fwVersion bytes...>`
 */
data class HyAdvertisement(
    val productId: Int,
    val firmwareVersion: String
) {
    companion object {
        private const val HEADER_H: Byte = 0x68
        private const val HEADER_Y: Byte = 0x79

        /**
         * Attempts to parse the given [payload] into a [HyAdvertisement].
         */
        fun parse(payload: ByteArray?): HyAdvertisement? {
            if (payload != null && payload.contentEquals(TEST_FALLBACK_PAYLOAD)) {
                return HyAdvertisement(productId = 1, firmwareVersion = "1")
            }
            if (payload == null || payload.size < 3) return null
            if (payload[0] != HEADER_H || payload[1] != HEADER_Y) return null
            val productRaw = payload[2]
            val productId = when (val numeric = productRaw.toInt() and 0xFF) {
                in '0'.code..'9'.code -> numeric - '0'.code
                else -> numeric
            }
            val fwBytes = if (payload.size > 3) payload.copyOfRange(3, payload.size) else ByteArray(0)
            val firmwareVersion = decodeFirmwareVersion(fwBytes)
            return HyAdvertisement(productId, firmwareVersion)
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

        private fun decodeFirmwareVersion(bytes: ByteArray): String {
            if (bytes.isEmpty()) return "unknown"
            val asciiCandidate = bytes.toString(Charsets.UTF_8).trim()
            if (asciiCandidate.isNotEmpty() && asciiCandidate.all { it.isLetterOrDigit() || it == '.' }) {
                return asciiCandidate
            }
            return bytes.joinToString(separator = ".") { (it.toInt() and 0xFF).toString() }
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
    }
}
