package com.massager.app.domain.device

// 文件说明：设备会话接口，规范连接、发送命令与关闭流程。
interface DeviceSession {
    val supportedProtocolKeys: Set<String>

    suspend fun selectZone(zoneIndex: Int): Boolean
    suspend fun selectMode(mode: Int): Boolean
    suspend fun selectLevel(level: Int): Boolean
    suspend fun selectTimer(minutes: Int): Boolean
    suspend fun runProgram(zone: Int, mode: Int, level: Int, timerMinutes: Int): Boolean
    suspend fun stopProgram(): Boolean
    suspend fun toggleMute(enabled: Boolean): Boolean
    suspend fun requestStatus(): Boolean
}
