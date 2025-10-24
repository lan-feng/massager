package com.massager.app.data.repository

import com.massager.app.data.bluetooth.MassagerBluetoothService
import com.massager.app.data.local.MassagerDatabase
import com.massager.app.data.local.entity.DeviceEntity
import com.massager.app.data.local.entity.MeasurementEntity
import com.massager.app.data.remote.MassagerApiService
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
    private val bluetoothService: MassagerBluetoothService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    val pairedDevices: Flow<List<String>> = bluetoothService.scanNearbyDevices().map { devices ->
        devices.map { device -> device.name ?: device.address ?: "Unknown device" }
    }

    fun deviceMetadata(): Flow<List<DeviceMetadata>> =
        database.deviceDao().getDevices().map { devices ->
            devices.map {
                DeviceMetadata(
                    id = it.id,
                    name = it.name,
                    macAddress = it.serial,
                    isConnected = it.status?.equals("online", ignoreCase = true) == true
                )
            }
        }

    suspend fun refreshDevices(): Result<List<DeviceMetadata>> = withContext(ioDispatcher) {
        runCatching {
            val response = api.fetchDevices()
            if (response.success.not()) {
                throw IllegalStateException(response.message ?: "Failed to load devices")
            }
            val devices = response.data.orEmpty()
            val entities = devices.map { dto ->
                DeviceEntity(
                    id = dto.id.toString(),
                    name = dto.nameAlias?.takeIf { it.isNotBlank() } ?: dto.deviceSerial.orEmpty(),
                    serial = dto.deviceSerial,
                    ownerId = dto.userId?.toString().orEmpty(),
                    status = dto.status,
                    batteryLevel = dto.batteryLevel,
                    lastSeenAt = parseInstant(dto.lastSeenAt)
                )
            }
            database.deviceDao().upsertAll(entities)
            entities.map { entity ->
                DeviceMetadata(
                    id = entity.id,
                    name = entity.name,
                    macAddress = entity.serial,
                    isConnected = entity.status?.equals("online", ignoreCase = true) == true
                )
            }
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
}
