package com.massager.app.data.remote.dto

// 文件说明：定义通用网络响应与请求的数据传输对象。
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
    val name: String,
    val verificationCode: String,
    @SerialName("appid") val appId: String? = null
)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    @SerialName("newPassword") val newPassword: String,
    val verificationCode: String
)

@Serializable
data class FirebaseLoginRequest(
    @SerialName("idToken") val idToken: String,
    @SerialName("appid") val appId: String? = null
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
    @SerialName("uniqueId") val uniqueId: String? = null,
    @SerialName("deviceType") val deviceType: Int? = null,
    @SerialName("firmwareVersion") val firmwareVersion: String? = null,
    @SerialName("nameAlias") val nameAlias: String? = null,
    @SerialName("comboInfo") val comboInfo: String? = null,
    @SerialName("lastSeenAt") val lastSeenAt: String? = null,
    @SerialName("batteryLevel") val batteryLevel: Int? = null,
    val status: String? = null,
    val enabled: Boolean? = null,
    @SerialName("bindStatus") val bindStatus: String? = null
)

@Serializable
data class DeviceBindRequest(
    @SerialName("deviceSerial") val deviceSerial: String,
    @SerialName("deviceType") val deviceType: Int,
    @SerialName("nameAlias") val nameAlias: String? = null,
    @SerialName("firmwareVersion") val firmwareVersion: String? = null,
    @SerialName("uniqueId") val uniqueId: String? = null
)

@Serializable
data class DeviceUpdateRequest(
    val id: Long,
    @SerialName("nameAlias") val nameAlias: String
)

@Serializable
data class DeviceComboInfoUpdateRequest(
    val id: Long,
    @SerialName("comboInfo") val comboInfo: String
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
