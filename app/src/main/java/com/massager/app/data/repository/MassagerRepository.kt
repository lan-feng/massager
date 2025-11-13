package com.massager.app.data.repository

import androidx.room.withTransaction
import com.massager.app.core.DeviceTypeConfig
import com.massager.app.data.local.MassagerDatabase
import com.massager.app.data.local.entity.DeviceEntity
import com.massager.app.data.local.entity.MeasurementEntity
import com.massager.app.data.remote.MassagerApiService
import com.massager.app.data.remote.dto.DeviceBindRequest
import com.massager.app.data.remote.dto.DeviceDto
import com.massager.app.di.IoDispatcher
import com.massager.app.domain.model.DeviceMetadata
import com.massager.app.domain.model.TemperatureRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MassagerRepository @Inject constructor(
    private val api: MassagerApiService,
    private val database: MassagerDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    fun deviceMetadata(): Flow<List<DeviceMetadata>> =
        database.deviceDao().getDevices().map { devices ->
            devices
                .sortedWith(
                    compareByDescending<DeviceEntity> {
                        it.status?.equals("online", ignoreCase = true) == true
                    }.thenByDescending { it.lastSeenAt ?: Instant.EPOCH }
                )
                .map { it.toDeviceMetadata() }
        }

    suspend fun refreshDevices(
        deviceTypes: List<Int> = DeviceTypeConfig.RESERVED_DEVICE_TYPES
    ): Result<List<DeviceMetadata>> = withContext(ioDispatcher) {
        runCatching {
            val typesToRequest = deviceTypes.ifEmpty { DeviceTypeConfig.RESERVED_DEVICE_TYPES }
            val response = api.fetchDevicesByType(typesToRequest.joinToString(","))
            if (response.success.not()) {
                throw IllegalStateException(response.message ?: "Failed to load devices")
            }
            val allowedTypeSet = typesToRequest.toSet()
            val devices = response.data.orEmpty().filter { dto ->
                val type = dto.deviceType
                type == null || allowedTypeSet.contains(type)
            }
            val entities = devices.map { dto -> dto.toEntity() }
            database.withTransaction {
                database.deviceDao().clear()
                database.deviceDao().upsertAll(entities)
            }
            entities.map { entity -> entity.toDeviceMetadata() }
        }
    }

    suspend fun bindDevice(
        deviceSerial: String,
        displayName: String,
        firmwareVersion: String? = null,
        uniqueId: String? = null
    ): Result<DeviceMetadata> = withContext(ioDispatcher) {
        runCatching {
            val type = DeviceTypeConfig.resolveTypeForName(displayName)
            val response = api.bindDevice(
                DeviceBindRequest(
                    deviceSerial = deviceSerial,
                    deviceType = type,
                    nameAlias = displayName.takeIf { it.isNotBlank() },
                    firmwareVersion = firmwareVersion,
                    uniqueId = uniqueId
                )
            )
            if (response.success.not()) {
                throw IllegalStateException(response.message ?: "Failed to bind device")
            }
            val dto = response.data ?: throw IllegalStateException("Empty bind response")
            val entity = dto.toEntity(defaultAlias = displayName)
            database.deviceDao().upsert(entity)
            entity.toDeviceMetadata()
        }
    }

    fun recentMeasurements(): Flow<List<TemperatureRecord>> =
        database.measurementDao().getMeasurements().map { measurements ->
            measurements.map {
                TemperatureRecord(
                    id = it.id,
                    celsius = it.value ?: 0.0,
                    recordedAt = it.recordedAt
                )
            }
        }

    suspend fun renameDevice(deviceId: String, newName: String): Result<DeviceMetadata> =
        withContext(ioDispatcher) {
            runCatching {
                val idLong = deviceId.toLongOrNull()
                    ?: throw IllegalArgumentException("Invalid device id: $deviceId")
                val response = api.updateDevice(
                    com.massager.app.data.remote.dto.DeviceUpdateRequest(
                        id = idLong,
                        nameAlias = newName
                    )
                )
                if (response.success.not()) {
                    throw IllegalStateException(response.message ?: "Failed to rename device")
                }
                val dto = response.data
                if (dto != null) {
                    val entity = dto.toEntity(defaultAlias = newName)
                    database.deviceDao().upsert(entity)
                    entity.toDeviceMetadata()
                } else {
                    database.deviceDao().updateName(deviceId, newName)
                    database.deviceDao().findById(deviceId)?.toDeviceMetadata()
                        ?: throw IllegalStateException("Device not found locally after rename")
                }
            }
        }

    suspend fun removeDevice(deviceId: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val idLong = deviceId.toLongOrNull()
                    ?: throw IllegalArgumentException("Invalid device id: $deviceId")
                val response = api.deleteDevice(idLong)
                if (response.success.not()) {
                    throw IllegalStateException(response.message ?: "Failed to remove device")
                }
                database.withTransaction {
                    database.deviceDao().deleteById(deviceId)
                    database.measurementDao().deleteForDevice(deviceId)
                }
            }
        }

    suspend fun refreshMeasurements(deviceId: String): Result<List<TemperatureRecord>> =
        withContext(ioDispatcher) {
            runCatching {
                val deviceIdLong = deviceId.toLongOrNull()
                    ?: throw IllegalArgumentException("Invalid device id: $deviceId")
                val response = api.fetchMeasurements(deviceIdLong)
                if (response.success.not()) {
                    throw IllegalStateException(response.message ?: "Failed to load measurements")
                }
                val measurements = response.data.orEmpty()
                database.measurementDao().deleteForDevice(deviceId)
                val entities = measurements.map { dto ->
                    val recordedAt = parseInstant(dto.timestamp) ?: Instant.now()
                    val value = extractTemperature(dto.data)
                    MeasurementEntity(
                        id = dto.id.toString(),
                        deviceId = dto.deviceId.toString(),
                        type = "temperature",
                        value = value,
                        unit = "°C",
                        recordedAt = recordedAt,
                        rawData = dto.data
                    )
                }
                database.measurementDao().upsertAll(entities)
                entities.map {
                    TemperatureRecord(
                        id = it.id,
                        celsius = it.value ?: 0.0,
                        recordedAt = it.recordedAt
                    )
                }
            }
        }

    private fun parseInstant(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
        }.getOrElse {
            runCatching { Instant.parse(value) }.getOrNull()
        }
    }

    private fun extractTemperature(data: String?): Double? {
        if (data.isNullOrBlank()) return null
        return try {
            val json = JSONObject(data)
            listOf("temperature", "temp", "value")
                .asSequence()
                .map { key -> json.optDouble(key, Double.NaN) }
                .firstOrNull { !it.isNaN() }
        } catch (ex: JSONException) {
            null
        }
    }

    private fun DeviceDto.toEntity(defaultAlias: String? = null): DeviceEntity =
        DeviceEntity(
            id = id.toString(),
            name = nameAlias?.takeIf { it.isNotBlank() }
                ?: defaultAlias?.takeIf { it.isNotBlank() }
                ?: deviceSerial?.takeIf { it.isNotBlank() }
                ?: id.toString(),
            serial = deviceSerial,
            ownerId = userId?.toString().orEmpty(),
            status = status,
            batteryLevel = batteryLevel,
            lastSeenAt = parseInstant(lastSeenAt)
        )

    private fun DeviceEntity.toDeviceMetadata(): DeviceMetadata =
        DeviceMetadata(
            id = id,
            name = name,
            macAddress = serial,
            isConnected = status?.equals("online", ignoreCase = true) == true
        )

}
