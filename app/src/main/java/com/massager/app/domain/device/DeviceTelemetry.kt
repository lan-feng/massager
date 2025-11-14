package com.massager.app.domain.device

import com.massager.app.data.bluetooth.protocol.ProtocolMessage

data class DeviceTelemetry(
    val isRunning: Boolean? = null,
    val batteryPercent: Int? = null,
    val mode: Int? = null,
    val level: Int? = null,
    val zone: Int? = null,
    val timerMinutes: Int? = null,
    val remainingSeconds: Int? = null,
    val isMuted: Boolean? = null,
    val message: DeviceTelemetryMessage? = null,
    val rawMessage: ProtocolMessage? = null
)

sealed interface DeviceTelemetryMessage {
    data object BatteryLow : DeviceTelemetryMessage
    data object SessionStopped : DeviceTelemetryMessage
    data class RemoteLevelChanged(val level: Int) : DeviceTelemetryMessage
}
