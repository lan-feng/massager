package com.massager.app.domain.device

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
