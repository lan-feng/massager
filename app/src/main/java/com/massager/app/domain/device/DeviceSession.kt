package com.massager.app.domain.device

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
