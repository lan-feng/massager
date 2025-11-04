package com.massager.app.data.bluetooth.protocol

/**
 * CRC16-CCITT (poly 0x1021) calculation helper.
 *
 * The firmware expects a seed of `0x0000` and the reflected variant is **not** used,
 * matching the reference implementation shared in the EMS v2 specification.
 */
object Crc16Ccitt {
    private const val Polynomial = 0x1021
    private const val InitialValue = 0x0000

    /**
     * Computes the CRC16 checksum for the supplied [data].
     */
    fun compute(data: ByteArray): Int {
        var crc = InitialValue
        data.forEach { value ->
            crc = crc xor ((value.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) {
                    (crc shl 1) xor Polynomial
                } else {
                    crc shl 1
                }
                crc = crc and 0xFFFF
            }
        }
        return crc and 0xFFFF
    }
}
