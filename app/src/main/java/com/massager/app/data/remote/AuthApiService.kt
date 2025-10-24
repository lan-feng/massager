package com.massager.app.data.remote

import com.massager.app.data.remote.dto.ApiEnvelope
import com.massager.app.data.remote.dto.AuthRequest
import com.massager.app.data.remote.dto.AuthResponse
import com.massager.app.data.remote.dto.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/v1/login")
    suspend fun login(@Body request: AuthRequest): ApiEnvelope<AuthResponse>

    @POST("auth/v1/register")
    suspend fun register(@Body request: RegisterRequest): ApiEnvelope<AuthResponse>
}
