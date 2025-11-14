package com.massager.app.domain.device

import com.massager.app.data.bluetooth.protocol.ProtocolMessage

interface DeviceTelemetryMapper {
    fun supports(message: ProtocolMessage): Boolean
    fun map(message: ProtocolMessage): DeviceTelemetry?
}
