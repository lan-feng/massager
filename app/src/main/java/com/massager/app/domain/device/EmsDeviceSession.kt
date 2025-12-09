package com.massager.app.domain.device

// 文件说明：定义 EMS 设备会话，实现连接、命令与数据管道。
import com.massager.app.data.bluetooth.MassagerBluetoothService
import com.massager.app.data.bluetooth.protocol.EmsV2ProtocolAdapter
import com.massager.app.data.bluetooth.protocol.EmsV2ProtocolAdapter.EmsV2Command
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmsDeviceSession @Inject constructor(
    private val bluetoothService: MassagerBluetoothService
) : DeviceSession {

    override val supportedProtocolKeys: Set<String> = setOf(EmsV2ProtocolAdapter.PROTOCOL_KEY)

    override suspend fun selectZone(zoneIndex: Int): Boolean =
        bluetoothService.sendProtocolCommand(EmsV2Command.SetBodyZone(zoneIndex))

    override suspend fun selectMode(mode: Int): Boolean =
        bluetoothService.sendProtocolCommand(EmsV2Command.SetMode(mode))

    override suspend fun selectLevel(level: Int): Boolean =
        bluetoothService.sendProtocolCommand(EmsV2Command.SetLevel(level))

    override suspend fun selectTimer(minutes: Int): Boolean =
        bluetoothService.sendProtocolCommand(EmsV2Command.SetTimer(minutes * 60))

    override suspend fun runProgram(zone: Int, mode: Int, level: Int, timerMinutes: Int): Boolean =
        bluetoothService.sendProtocolCommand(EmsV2Command.SetRunState(running = true, durationSeconds = timerMinutes * 60))

    override suspend fun stopProgram(): Boolean =
        bluetoothService.sendProtocolCommand(EmsV2Command.SetRunState(running = false, durationSeconds = 0))

    override suspend fun toggleMute(enabled: Boolean): Boolean =
        bluetoothService.sendProtocolCommand(EmsV2Command.ToggleMute(enabled))

    override suspend fun requestStatus(): Boolean =
        bluetoothService.sendProtocolCommand(EmsV2Command.ReadStatus)

    override suspend fun requestHeartbeat(): Boolean =
        bluetoothService.sendProtocolCommand(EmsV2Command.RequestHeartbeat)
}
