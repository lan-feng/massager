package com.massager.app.data.remote

// 文件说明：用户资料与账号安全相关的 Retrofit API 定义。
import com.massager.app.data.remote.dto.ApiEnvelope
import com.massager.app.data.remote.dto.ChangePasswordRequest
import com.massager.app.data.remote.dto.FileUploadResponse
import com.massager.app.data.remote.dto.UpdateUserRequest
import com.massager.app.data.remote.dto.UserInfoResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface UserApiService {
    @GET("auth/v1/user/info")
    suspend fun getUserInfo(): ApiEnvelope<UserInfoResponse>

    @POST("auth/v1/user/update")
    suspend fun updateUser(@Body request: UpdateUserRequest): ApiEnvelope<UserInfoResponse>

    @Multipart
    @POST("common/v1/files/upload")
    suspend fun uploadAvatar(
        @Part file: MultipartBody.Part,
        @Query("folder") folder: String = "avatars"
    ): ApiEnvelope<FileUploadResponse>

    @POST("auth/v1/user/change-password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): ApiEnvelope<Unit>

    @POST("auth/v1/user/del")
    suspend fun deleteAccount(
        @Query("id") userId: Long
    ): ApiEnvelope<Unit>

    @POST("auth/v1/user/unbind")
    suspend fun unbindProvider(
        @Query("provider") provider: String
    ): ApiEnvelope<Unit>
}
