package com.massager.app.domain.device

import com.massager.app.data.bluetooth.protocol.EmsV2ProtocolAdapter
import com.massager.app.data.bluetooth.protocol.FrameDirection
import com.massager.app.data.bluetooth.protocol.ProtocolMessage
import javax.inject.Inject

class EmsDeviceTelemetryMapper @Inject constructor() : DeviceTelemetryMapper {

    override fun supports(message: ProtocolMessage): Boolean =
        message is EmsV2ProtocolAdapter.EmsV2Message

    override fun map(message: ProtocolMessage): DeviceTelemetry? {
        return when (message) {
            is EmsV2ProtocolAdapter.EmsV2Message.Heartbeat -> fromHeartbeat(message)
            is EmsV2ProtocolAdapter.EmsV2Message.ModeReport ->
                DeviceTelemetry(mode = message.mode, rawMessage = message)
            is EmsV2ProtocolAdapter.EmsV2Message.LevelReport ->
                DeviceTelemetry(
                    level = message.level,
                    message = levelChangedMessage(message),
                    rawMessage = message
                )
            is EmsV2ProtocolAdapter.EmsV2Message.ZoneReport ->
                DeviceTelemetry(zone = message.zone, rawMessage = message)
            is EmsV2ProtocolAdapter.EmsV2Message.TimerReport ->
                DeviceTelemetry(timerMinutes = message.minutes, remainingSeconds = message.minutes * 60)
            is EmsV2ProtocolAdapter.EmsV2Message.MuteReport ->
                DeviceTelemetry(isMuted = message.enabled, rawMessage = message)
            else -> null
        }
    }

    private fun levelChangedMessage(message: EmsV2ProtocolAdapter.EmsV2Message.LevelReport): DeviceTelemetryMessage? {
        if (message.direction != FrameDirection.DeviceToApp) return null
        return DeviceTelemetryMessage.RemoteLevelChanged(message.level)
    }

    private fun fromHeartbeat(message: EmsV2ProtocolAdapter.EmsV2Message.Heartbeat): DeviceTelemetry {
        val remaining = if (!message.isRunning && message.timerMinutes == 0) {
            0
        } else {
            message.timerMinutes * 60
        }
        val telemetryMessage = when {
            message.batteryPercent in 0..25 -> DeviceTelemetryMessage.BatteryLow
            !message.isRunning -> DeviceTelemetryMessage.SessionStopped
            else -> null
        }
        return DeviceTelemetry(
            isRunning = message.isRunning,
            batteryPercent = message.batteryPercent,
            mode = message.mode,
            level = message.level,
            zone = message.zone,
            timerMinutes = message.timerMinutes,
            remainingSeconds = remaining,
            isMuted = message.isMuted,
            message = telemetryMessage
        ).copy(rawMessage = message)
    }
}
