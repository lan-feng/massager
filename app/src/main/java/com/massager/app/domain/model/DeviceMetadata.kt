package com.massager.app.domain.model

data class DeviceMetadata(
    val id: String,
    val name: String,
    val macAddress: String?,
    val isConnected: Boolean
)

data class RecoveryMassagerOption(
    val id: String,
    val title: String,
    val description: String
)

data class TemperatureRecord(
    val id: String,
    val celsius: Double,
    val recordedAt: java.time.Instant
)
