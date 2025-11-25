package com.massager.app.data.remote

// 文件说明：提供设备、测量等业务接口的 Retrofit 定义。
import com.massager.app.data.remote.dto.ApiEnvelope
import com.massager.app.data.remote.dto.DeviceBindRequest
import com.massager.app.data.remote.dto.DeviceComboInfoUpdateRequest
import com.massager.app.data.remote.dto.DeviceDto
import com.massager.app.data.remote.dto.MeasurementDto
import com.massager.app.data.remote.dto.DeviceUpdateRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path

interface MassagerApiService {
    @GET("device/v1/listByType")
    suspend fun fetchDevicesByType(
        @Query("deviceType") deviceType: String
    ): ApiEnvelope<List<DeviceDto>>

    @POST("device/v1/bind")
    suspend fun bindDevice(
        @Body request: DeviceBindRequest
    ): ApiEnvelope<DeviceDto>

    @POST("device/v1/update")
    suspend fun updateDevice(
        @Body request: DeviceUpdateRequest
    ): ApiEnvelope<DeviceDto?>

    @POST("device/v1/updateComboInfo")
    suspend fun updateComboInfo(
        @Body request: DeviceComboInfoUpdateRequest
    ): ApiEnvelope<DeviceDto?>

    @POST("device/v1/delById/{id}")
    suspend fun deleteDevice(
        @Path("id") id: Long
    ): ApiEnvelope<Unit?>

    @GET("business/v1/measurement/user/device/list")
    suspend fun fetchMeasurements(
        @Query("deviceId") deviceId: Long
    ): ApiEnvelope<List<MeasurementDto>>
}
