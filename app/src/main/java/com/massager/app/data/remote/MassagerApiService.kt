package com.massager.app.data.remote

import com.massager.app.data.remote.dto.ApiEnvelope
import com.massager.app.data.remote.dto.DeviceBindRequest
import com.massager.app.data.remote.dto.DeviceDto
import com.massager.app.data.remote.dto.MeasurementDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface MassagerApiService {
    @GET("device/v1/listByType")
    suspend fun fetchDevicesByType(
        @Query("deviceType") deviceType: String
    ): ApiEnvelope<List<DeviceDto>>

    @POST("device/v1/bind")
    suspend fun bindDevice(
        @Body request: DeviceBindRequest
    ): ApiEnvelope<DeviceDto>

    @GET("business/v1/measurement/user/device/list")
    suspend fun fetchMeasurements(
        @Query("deviceId") deviceId: Long
    ): ApiEnvelope<List<MeasurementDto>>
}
