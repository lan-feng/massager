package com.massager.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    @SerialName("display_name") val displayName: String
)

@Serializable
data class AuthResponse(
    val token: String,
    @SerialName("tokenType") val tokenType: String? = null,
    @SerialName("expiresIn") val expiresIn: Long? = null,
    @SerialName("userInfo") val user: UserDto
)

@Serializable
data class UserDto(
    val id: Long,
    val email: String,
    val name: String,
    @SerialName("appid") val appId: String? = null,
    @SerialName("avatarUrl") val avatarUrl: String? = null,
    val country: String? = null,
    val timezone: String? = null,
    @SerialName("createdAt") val createdAt: String? = null
)

@Serializable
data class DeviceDto(
    val id: Long,
    @SerialName("userId") val userId: Long? = null,
    @SerialName("deviceSerial") val deviceSerial: String? = null,
    @SerialName("deviceType") val deviceType: Int? = null,
    @SerialName("firmwareVersion") val firmwareVersion: String? = null,
    @SerialName("nameAlias") val nameAlias: String? = null,
    @SerialName("lastSeenAt") val lastSeenAt: String? = null,
    @SerialName("batteryLevel") val batteryLevel: Int? = null,
    val status: String? = null,
    val enabled: Boolean? = null,
    @SerialName("bindStatus") val bindStatus: String? = null
)

@Serializable
data class MeasurementDto(
    val id: Long,
    @SerialName("deviceId") val deviceId: Long,
    @SerialName("thingId") val thingId: Long? = null,
    val data: String? = null,
    val status: String? = null,
    val quality: String? = null,
    @SerialName("timestamp") val timestamp: String,
    @SerialName("createdAt") val createdAt: String? = null
)

@Serializable
data class ApiEnvelope<T>(
    val code: Int? = null,
    val success: Boolean = false,
    val data: T? = null,
    val message: String? = null
)
