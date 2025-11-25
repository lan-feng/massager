package com.massager.app.data.remote.dto

// 文件说明：用户相关 API 的响应与请求 DTO。
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserInfoResponse(
    val id: Long,
    val email: String,
    val name: String,
    @SerialName("avatarUrl") val avatarUrl: String? = null,
    @SerialName("appid") val appId: String? = null,
    val timezone: String? = null,
    val country: String? = null
)

@Serializable
data class UpdateUserRequest(
    val name: String? = null,
    @SerialName("avatarUrl") val avatarUrl: String? = null
)

@Serializable
data class ChangePasswordRequest(
    @SerialName("oldPassword") val oldPassword: String,
    @SerialName("newPassword") val newPassword: String
)

@Serializable
data class FileUploadResponse(
    @SerialName("url") val url: String
)

@Serializable
data class DeleteAccountRequest(
    @SerialName("id") val userId: Long
)
