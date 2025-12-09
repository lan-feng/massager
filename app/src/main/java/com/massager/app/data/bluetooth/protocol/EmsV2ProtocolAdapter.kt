package com.massager.app.data.bluetooth.protocol

// 文件说明：EMS V2 协议适配器，负责编码解码指令帧。
import java.util.UUID

private const val EMS_V2_HEADER_FIRST: Byte = 0x68
private const val EMS_V2_HEADER_SECOND: Byte = 0x79
private const val EMS_V2_TERMINATOR_CR: Byte = 0x0D
private const val EMS_V2_TERMINATOR_LF: Byte = 0x0A

/**
 * EMS v2 BLE protocol handler.
 *
 * The frame structure is described in `doc/prompt_014.md`. We honour the documented layout.
 */
class EmsV2ProtocolAdapter : BleProtocolAdapter {

    override val protocolKey: String
        get() = PROTOCOL_KEY

    override val productId: Int = 1

    override val serviceUuidCandidates: List<UUID> = listOf(
        UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB"),
        UUID.fromString("0000FCC0-0000-1000-8000-00805F9B34FB")
    )

    override val writeCharacteristicUuidCandidates: List<UUID> = listOf(
        UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB"),
        UUID.fromString("0000FCC1-0000-1000-8000-00805F9B34FB")
    )

    override val notifyCharacteristicUuidCandidates: List<UUID> = listOf(
        UUID.fromString("0000FFE2-0000-1000-8000-00805F9B34FB"),
        UUID.fromString("0000FCC2-0000-1000-8000-00805F9B34FB")
    )

    override fun decode(payload: ByteArray): List<ProtocolMessage> {
        if (payload.size < MIN_FRAME_SIZE) return emptyList()
        if (payload[0] != EMS_V2_HEADER_FIRST || payload[1] != EMS_V2_HEADER_SECOND) return emptyList()
        if (payload[payload.lastIndex - 1] != EMS_V2_TERMINATOR_CR ||
            payload[payload.lastIndex] != EMS_V2_TERMINATOR_LF
        ) return emptyList()

        val advertisedLength = toUInt16Be(payload[2], payload[3])
        val allowableLengths = setOf(payload.size, payload.size - TERMINATOR_LENGTH)

        if (advertisedLength !in allowableLengths) return emptyList()

        val crcIndex = payload.size - TERMINATOR_LENGTH - CRC_LENGTH
        val bodyEnd = crcIndex - 1

        /**
         * TODO:EMS报文做了CRC
         */
        val crcCalculated = Crc16Ccitt.compute(payload.copyOfRange(0, crcIndex))
        val crcReceivedLe = toUInt16Le(payload[crcIndex], payload[crcIndex + 1])
        val crcReceivedBe = toUInt16Be(payload[crcIndex], payload[crcIndex + 1])
        if (crcCalculated != crcReceivedLe && crcCalculated != crcReceivedBe) return emptyList()

        val direction = when (payload[4].toInt()) {
            1 -> FrameDirection.AppToDevice
            2 -> FrameDirection.DeviceToApp
            else -> FrameDirection.DeviceToApp
        }
        val commandId = payload[5].toInt() and 0xFF
        val body = if (bodyEnd >= 5) payload.copyOfRange(6, bodyEnd + 1) else ByteArray(0)

        val message: EmsV2Message = when (commandId) {
            CommandId.STATUS.value -> {
                EmsV2Message.Heartbeat(
                    direction = direction,
                    commandId = commandId,
                    isRunning = body.getOrNull(0)?.toInt() == 1,
                    batteryPercent = toBatteryPercent(body.getOrNull(1)),
                    mode = body.getOrNull(2)?.toInt() ?: 0,
                    level = body.getOrNull(3)?.toInt() ?: 0,
                    zone = body.getOrNull(4)?.toInt() ?: 0,
                    timerSeconds = when {
                        body.size >= 8 -> toUInt16Be(body[5], body[6])
                        else -> (body.getOrNull(5)?.toInt() ?: 0) * 60
                    },
                    isMuted = when {
                        body.size >= 8 -> body.getOrNull(7)?.let { it.toInt() == 0 }
                        else -> body.getOrNull(6)?.let { it.toInt() == 0 }
                    },
                    chargeStatus = when {
                        body.size >= 9 -> body.getOrNull(8)?.toInt()
                        else -> body.getOrNull(7)?.toInt()
                    }
                )
            }
            CommandId.MODE.value -> {
                EmsV2Message.ModeReport(
                    direction = direction,
                    commandId = commandId,
                    mode = body.firstOrNull()?.toInt() ?: 0
                )
            }
            CommandId.INTENSITY.value -> {
                EmsV2Message.LevelReport(
                    direction = direction,
                    commandId = commandId,
                    level = body.firstOrNull()?.toInt() ?: 0
                )
            }
            CommandId.BODY_ZONE.value -> {
                EmsV2Message.ZoneReport(
                    direction = direction,
                    commandId = commandId,
                    zone = body.firstOrNull()?.toInt() ?: 0
                )
            }
            CommandId.TIMER.value -> {
                EmsV2Message.TimerReport(
                    direction = direction,
                    commandId = commandId,
                    seconds = when {
                        body.size >= 2 -> toUInt16Be(body[0], body[1])
                        else -> body.firstOrNull()?.toInt() ?: 0
                    }
                )
            }
            CommandId.BUZZER.value -> {
                EmsV2Message.MuteReport(
                    direction = direction,
                    commandId = commandId,
                    enabled = body.firstOrNull()?.toInt() == 0
                )
            }
            else -> EmsV2Message.Generic(
                direction = direction,
                commandId = commandId,
                rawPayload = body
            )
        }
        return listOf(message)
    }

    override fun encode(command: ProtocolCommand): ByteArray =
        when (command) {
            is EmsV2Command.ReadStatus -> buildFrame(
                direction = FrameDirection.AppToDevice,
                commandId = CommandId.STATUS.value,
                body = byteArrayOf(0x00)
            )

            is EmsV2Command.SetMode -> buildFrame(
                direction = FrameDirection.AppToDevice,
                commandId = CommandId.MODE.value,
                body = byteArrayOf(command.mode.toByte())
            )

            is EmsV2Command.SetBodyZone -> buildFrame(
                direction = FrameDirection.AppToDevice,
                commandId = CommandId.BODY_ZONE.value,
                body = byteArrayOf(command.zone.toByte())
            )

            is EmsV2Command.SetLevel -> buildFrame(
                direction = FrameDirection.AppToDevice,
                commandId = CommandId.INTENSITY.value,
                body = byteArrayOf(command.level.toByte())
            )

            is EmsV2Command.SetTimer -> {
                val seconds = command.seconds.coerceIn(0, 65_535)
                val timerBytes = fromUInt16Be(seconds)
                buildFrame(
                    direction = FrameDirection.AppToDevice,
                    commandId = CommandId.TIMER.value,
                    body = byteArrayOf(timerBytes.first, timerBytes.second)
                )
            }

            EmsV2Command.RequestHeartbeat -> buildFrame(
                direction = FrameDirection.AppToDevice,
                commandId = 0x0A,
                body = byteArrayOf()
            )

            is EmsV2Command.SetRunState -> {
                val seconds = command.durationSeconds.coerceIn(0, 65_535)
                val durationBytes = fromUInt16Be(seconds)
                val body = byteArrayOf(
                    if (command.running) 0x01 else 0x00,
                    durationBytes.first,
                    durationBytes.second
                )
                buildFrame(
                    direction = FrameDirection.AppToDevice,
                    commandId = CommandId.RUN_STATE.value,
                    body = body
                )
            }

            is EmsV2Command.RunProgram -> {
                val body = byteArrayOf(
                    0x01, // run flag
                    command.zone.coerceIn(0, 5).toByte(),
                    command.mode.coerceIn(0, 7).toByte(),
                    command.level.coerceIn(0, 19).toByte(),
                    command.timerMinutes.coerceIn(0, 255).toByte()
                )
                buildFrame(
                    direction = FrameDirection.AppToDevice,
                    commandId = CommandId.STATUS.value,
                    body = body
                )
            }

            EmsV2Command.StopProgram -> {
                val body = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00)
                buildFrame(
                    direction = FrameDirection.AppToDevice,
                    commandId = CommandId.STATUS.value,
                    body = body
                )
            }

            is EmsV2Command.ToggleMute -> buildFrame(
                direction = FrameDirection.AppToDevice,
                commandId = CommandId.BUZZER.value,
                body = byteArrayOf(if (command.enabled) 0 else 1)
            )

            is EmsV2Command.Raw -> buildFrame(
                direction = command.direction,
                commandId = command.commandId,
                body = command.payload
            )
            else -> throw IllegalArgumentException("Unsupported EMS v2 command: $command")
        }

    private fun buildFrame(
        direction: FrameDirection,
        commandId: Int,
        body: ByteArray
    ): ByteArray {
        val packetLength = HEADER_LENGTH + LENGTH_FIELD + DIRECTION_LENGTH + COMMAND_LENGTH +
            body.size + CRC_LENGTH + TERMINATOR_LENGTH
        val frame = ByteArray(packetLength)
        var cursor = 0
        frame[cursor++] = EMS_V2_HEADER_FIRST
        frame[cursor++] = EMS_V2_HEADER_SECOND

        val lengthBytes = fromUInt16Be(packetLength)
        frame[cursor++] = lengthBytes.first
        frame[cursor++] = lengthBytes.second

        frame[cursor++] = when (direction) {
            FrameDirection.AppToDevice -> 0x01
            FrameDirection.DeviceToApp -> 0x02
        }
        frame[cursor++] = (commandId and 0xFF).toByte()
        body.copyInto(frame, destinationOffset = cursor)
        cursor += body.size

        val crcRange = frame.copyOfRange(0, cursor)
        val crc = Crc16Ccitt.compute(crcRange)
        val crcBytes = fromUInt16Be(crc)
        frame[cursor++] = crcBytes.first
        frame[cursor++] = crcBytes.second

        frame[cursor++] = EMS_V2_TERMINATOR_CR
        frame[cursor] = EMS_V2_TERMINATOR_LF
        return frame
    }

    private fun toBatteryPercent(raw: Byte?): Int {
        val level = raw?.toInt() ?: return -1
        if (level !in 0..4) return -1
        return (level * 25).coerceAtMost(100)
    }

    private fun toUInt16Be(high: Byte, low: Byte): Int =
        ((high.toInt() and 0xFF) shl 8) or (low.toInt() and 0xFF)

    private fun fromUInt16Be(value: Int): Pair<Byte, Byte> {
        val hi = ((value shr 8) and 0xFF).toByte()
        val lo = (value and 0xFF).toByte()
        return hi to lo
    }

    /**
     * TODO:EMS的CRC 发送和接收大小端不一致，暂时兼容
     */
    private fun toUInt16Le(low: Byte, high: Byte): Int =
        ((high.toInt() and 0xFF) shl 8) or (low.toInt() and 0xFF)

    companion object {
        const val PROTOCOL_KEY = "ems_v2"
        private const val HEADER_LENGTH = 2
        private const val LENGTH_FIELD = 2
        private const val DIRECTION_LENGTH = 1
        private const val COMMAND_LENGTH = 1
        private const val CRC_LENGTH = 2
        private const val TERMINATOR_LENGTH = 2
        private const val MIN_FRAME_SIZE = HEADER_LENGTH + LENGTH_FIELD + DIRECTION_LENGTH +
            COMMAND_LENGTH + CRC_LENGTH + TERMINATOR_LENGTH
    }

    /**
     * Known EMS v2 command identifiers.
     */
    enum class CommandId(val value: Int) {
        STATUS(0),
        MODE(1),
        INTENSITY(2),
        BODY_ZONE(3),
        HOT_SITE(4),
        POWER(5),
        BATTERY(6),
        BUZZER(7),
        TIMER(8),
        RUN_STATE(11)
    }

    /**
     * Typed messages emitted by the adapter.
     */
    sealed class EmsV2Message(
        override val direction: FrameDirection,
        override val commandId: Int
    ) : ProtocolMessage {
        data class Heartbeat(
            override val direction: FrameDirection,
            override val commandId: Int,
            val isRunning: Boolean,
            val batteryPercent: Int,
            val mode: Int,
            val level: Int,
            val zone: Int,
            val timerSeconds: Int,
            val isMuted: Boolean?,
            val chargeStatus: Int?
        ) : EmsV2Message(direction, commandId)

        data class ModeReport(
            override val direction: FrameDirection,
            override val commandId: Int,
            val mode: Int
        ) : EmsV2Message(direction, commandId)

        data class LevelReport(
            override val direction: FrameDirection,
            override val commandId: Int,
            val level: Int
        ) : EmsV2Message(direction, commandId)

        data class ZoneReport(
            override val direction: FrameDirection,
            override val commandId: Int,
            val zone: Int
        ) : EmsV2Message(direction, commandId)

        data class TimerReport(
            override val direction: FrameDirection,
            override val commandId: Int,
            val seconds: Int
        ) : EmsV2Message(direction, commandId)

        data class MuteReport(
            override val direction: FrameDirection,
            override val commandId: Int,
            val enabled: Boolean
        ) : EmsV2Message(direction, commandId)

        data class Generic(
            override val direction: FrameDirection,
            override val commandId: Int,
            val rawPayload: ByteArray
        ) : EmsV2Message(direction, commandId)
    }

    /**
     * Sealed hierarchy describing EMS v2 commands our client can transmit.
     */
    sealed class EmsV2Command : ProtocolCommand {
        object ReadStatus : EmsV2Command()
        data class SetMode(val mode: Int) : EmsV2Command()
        data class SetBodyZone(val zone: Int) : EmsV2Command()
        data class SetLevel(val level: Int) : EmsV2Command()
        data class SetTimer(val seconds: Int) : EmsV2Command()
        object RequestHeartbeat : EmsV2Command()
        data class SetRunState(val running: Boolean, val durationSeconds: Int) : EmsV2Command()
        data class RunProgram(
            val zone: Int,
            val mode: Int,
            val level: Int,
            val timerMinutes: Int
        ) : EmsV2Command()
        object StopProgram : EmsV2Command()
        data class ToggleMute(val enabled: Boolean) : EmsV2Command()
        data class Raw(
            val direction: FrameDirection,
            val commandId: Int,
            val payload: ByteArray
        ) : EmsV2Command()
    }
}
