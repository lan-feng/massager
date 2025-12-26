package com.massager.app.data.repository

// 文件说明：封装用户资料相关的远端接口与本地缓存同步。
import com.massager.app.data.remote.UserApiService
import com.massager.app.data.remote.dto.ChangePasswordRequest
import com.massager.app.data.remote.dto.FileUploadResponse
import com.massager.app.data.remote.dto.UpdateUserRequest
import com.massager.app.data.remote.dto.UserInfoResponse
import com.massager.app.data.remote.upload.FilePart
import com.massager.app.data.remote.upload.MultipartRequestBodyUtil
import com.massager.app.domain.model.ThirdPartyProfile
import com.massager.app.domain.model.UserProfile
import com.massager.app.data.local.SessionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userApiService: UserApiService,
    private val sessionManager: SessionManager
) {

    suspend fun fetchProfile(): Result<UserProfile> = runCatching {
        val response = userApiService.getUserInfo()
        if (response.success.not()) {
            throw IllegalStateException(response.message ?: "Failed to load profile")
        }
        response.data?.let { data ->
            data.token?.takeIf { it.isNotBlank() }?.let { sessionManager.saveAuthToken(it) }
            data.resolvedUser()?.appId?.takeIf { it.isNotBlank() }?.let { sessionManager.saveAppId(it) }
            data.toDomain()
        } ?: throw IllegalStateException("Empty profile response")
    }

    suspend fun updateName(name: String): Result<UserProfile> = runCatching {
        val response = userApiService.updateUser(UpdateUserRequest(name = name))
        if (response.success.not()) {
            throw IllegalStateException(response.message ?: "Failed to update profile")
        }
        response.data?.toDomain() ?: throw IllegalStateException("Empty profile response")
    }

    suspend fun updateAvatar(avatarUrl: String): Result<UserProfile> = runCatching {
        val response = userApiService.updateUser(UpdateUserRequest(avatarUrl = avatarUrl))
        if (response.success.not()) {
            throw IllegalStateException(response.message ?: "Failed to update avatar")
        }
        response.data?.toDomain() ?: throw IllegalStateException("Empty profile response")
    }

    suspend fun uploadAvatar(bytes: ByteArray, fileName: String): Result<FileUploadResponse> = runCatching {
        val part = MultipartRequestBodyUtil.fromBytes(
            partName = "file",
            part = FilePart(fileName = fileName, bytes = bytes)
        )
        val response = userApiService.uploadAvatar(part)
        if (response.success.not()) {
            throw IllegalStateException(response.message ?: "Failed to upload avatar")
        }
        response.data ?: throw IllegalStateException("Upload response missing URL")
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> = runCatching {
        val response = userApiService.changePassword(
            ChangePasswordRequest(oldPassword = oldPassword, newPassword = newPassword)
        )
        if (response.success.not()) {
            throw IllegalStateException(response.message ?: "Failed to change password")
        }
    }

    suspend fun deleteAccount(userId: Long): Result<Unit> = runCatching {
        val response = userApiService.deleteAccount(userId)
        if (response.success.not()) {
            throw IllegalStateException(response.message ?: "Failed to delete account")
        }
    }

    suspend fun unbindProvider(provider: String): Result<Unit> = runCatching {
        val response = userApiService.unbindProvider(provider)
        if (response.success.not()) {
            throw IllegalStateException(response.message ?: "Failed to unbind provider")
        }
    }

    private fun UserInfoResponse.toDomain(): UserProfile {
        val payload = resolvedUser()
            ?: throw IllegalStateException("Missing user payload in response")
        return UserProfile(
            id = payload.id,
            name = payload.name,
            email = payload.email,
            avatarUrl = payload.avatarUrl,
            cacheSize = null,
            firebaseUid = payload.firebaseUid,
            appleUserId = payload.appleUserId,
            facebookUid = payload.facebookUid,
            thirdPartyProfiles = parseThirdPartyProfiles(payload.userSettings),
            hasPassword = payload.hasPassword ?: false
        )
    }

    private fun parseThirdPartyProfiles(settings: Map<String, com.massager.app.data.remote.dto.ThirdPartyProps?>?): Map<String, ThirdPartyProfile> {
        return settings.orEmpty().mapValues { (_, value) ->
            ThirdPartyProfile(name = value?.name, email = value?.email)
        }
    }
}
