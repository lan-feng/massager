package com.massager.app.data.bluetooth

import com.massager.app.data.bluetooth.protocol.ProtocolMessage

/**
 * Wrapper that couples a decoded protocol message with the originating device address.
 */
data class DeviceProtocolMessage(
    val address: String,
    val message: ProtocolMessage
)
