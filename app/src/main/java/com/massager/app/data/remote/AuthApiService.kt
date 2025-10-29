package com.massager.app.data.remote

import com.massager.app.data.remote.dto.ApiEnvelope
import com.massager.app.data.remote.dto.AuthRequest
import com.massager.app.data.remote.dto.AuthResponse
import com.massager.app.data.remote.dto.RegisterRequest
import com.massager.app.data.remote.dto.ResetPasswordRequest
import com.massager.app.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApiService {
    @POST("auth/v1/login")
    suspend fun login(@Body request: AuthRequest): ApiEnvelope<AuthResponse>

    @POST("auth/v1/register")
    suspend fun register(@Body request: RegisterRequest): ApiEnvelope<UserDto>

    @POST("auth/v1/register/send-code")
    suspend fun sendRegisterCode(@Query("email") email: String): ApiEnvelope<Unit>

    @POST("auth/v1/password/reset")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): ApiEnvelope<Unit>
}
