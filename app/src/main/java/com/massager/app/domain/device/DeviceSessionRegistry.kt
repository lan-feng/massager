package com.massager.app.domain.device

// 文件说明：管理已注册设备会话实例的查找与获取。
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceSessionRegistry @Inject constructor(
    private val sessions: Set<@JvmSuppressWildcards DeviceSession>
) {

    fun sessionFor(protocolKey: String?): DeviceSession? {
        if (sessions.isEmpty()) return null
        return sessions.firstOrNull { session ->
            session.supportedProtocolKeys.isEmpty() ||
                protocolKey == null ||
                session.supportedProtocolKeys.contains(protocolKey)
        } ?: sessions.firstOrNull()
    }
}
