package com.massager.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String? = null
)

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val serial: String?,
    val ownerId: String,
    val status: String? = null,
    val batteryLevel: Int? = null,
    val lastSeenAt: Instant? = null
)

@Entity(tableName = "things")
data class ThingEntity(
    @PrimaryKey val id: String,
    val label: String,
    val deviceId: String
)

@Entity(tableName = "records")
data class RecordEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val createdAt: Instant
)

@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey val id: String,
    val deviceId: String,
    val type: String? = null,
    val value: Double? = null,
    val unit: String? = null,
    val recordedAt: Instant,
    val rawData: String? = null
)

@Entity(tableName = "massager_devices")
data class MassagerDeviceEntity(
    @PrimaryKey val id: String,
    val nickname: String,
    val intensityLevel: Int,
    val isFavorite: Boolean
)
