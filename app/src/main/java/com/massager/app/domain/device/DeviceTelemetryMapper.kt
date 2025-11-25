package com.massager.app.domain.device

// 文件说明：设备遥测映射接口，供具体协议实现扩展。
import com.massager.app.data.bluetooth.protocol.ProtocolMessage

interface DeviceTelemetryMapper {
    fun supports(message: ProtocolMessage): Boolean
    fun map(message: ProtocolMessage): DeviceTelemetry?
}
