package com.massager.app.data.remote

import com.massager.app.data.remote.dto.ApiEnvelope
import com.massager.app.data.remote.dto.DeviceDto
import com.massager.app.data.remote.dto.MeasurementDto
import retrofit2.http.GET
import retrofit2.http.Query

interface MassagerApiService {
    @GET("device/v1/list")
    suspend fun fetchDevices(): ApiEnvelope<List<DeviceDto>>

    @GET("business/v1/measurement/user/device/list")
    suspend fun fetchMeasurements(
        @Query("deviceId") deviceId: Long
    ): ApiEnvelope<List<MeasurementDto>>
}
