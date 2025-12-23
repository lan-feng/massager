package com.massager.app.domain.model

// 文件说明：表征设备列表展示所需的元数据。
data class DeviceMetadata(
    val id: String,
    val name: String,
    val serialNo: String?,
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
