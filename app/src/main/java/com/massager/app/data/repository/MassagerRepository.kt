package com.massager.app.data.repository

// 文件说明：管理设备与测量数据的获取、同步、组合信息更新等核心数据流。
import androidx.room.withTransaction
import com.massager.app.core.DeviceCatalog
import com.massager.app.data.local.MassagerDatabase
import com.massager.app.data.local.SessionManager
import com.massager.app.data.local.entity.DeviceEntity
import com.massager.app.data.local.entity.MeasurementEntity
import com.massager.app.data.remote.MassagerApiService
import com.massager.app.data.remote.dto.DeviceBindRequest
import com.massager.app.data.remote.dto.DeviceComboInfoUpdateRequest
import com.massager.app.data.remote.dto.DeviceDto
import com.massager.app.di.IoDispatcher
import com.massager.app.domain.model.DeviceMetadata
import com.massager.app.domain.model.TemperatureRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    private val deviceCatalog: DeviceCatalog,
    private val sessionManager: SessionManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    fun observeDeviceComboInfo(deviceId: String): Flow<String?> =
        database.deviceDao().getComboInfo(deviceId)

    fun deviceMetadata(): Flow<List<DeviceMetadata>> {
        val ownerId = sessionManager.activeOwnerId()
        return database.deviceDao().getDevicesForOwner(ownerId).map { devices ->
            devices
                .sortedWith(
                    compareByDescending<DeviceEntity> {
                        it.status?.equals("online", ignoreCase = true) == true
                    }.thenByDescending { it.lastSeenAt ?: Instant.EPOCH }
                )
                .map { it.toDeviceMetadata(deviceCatalog) }
        }
    }

    suspend fun refreshDevices(
        deviceTypes: List<Int> = deviceCatalog.reservedDeviceTypes
    ): Result<List<DeviceMetadata>> = withContext(ioDispatcher) {
        runCatching {
            if (sessionManager.isGuestMode()) {
                return@runCatching localDevicesSnapshot()
            }
            val ownerId = sessionManager.accountOwnerId()
                ?: throw IllegalStateException("Missing account owner id")
            val typesToRequest = deviceTypes.ifEmpty { deviceCatalog.reservedDeviceTypes }
            val response = api.fetchDevicesByType(typesToRequest.joinToString(","))
            if (response.success.not()) {
                throw IllegalStateException(response.message ?: "Failed to load devices")
            }
            val allowedTypeSet = typesToRequest.toSet()
            val devices = response.data.orEmpty().filter { dto ->
                val type = dto.deviceType
                type == null || allowedTypeSet.contains(type)
            }
            val entities = devices.map { dto -> dto.toEntity(ownerIdOverride = ownerId) }
            database.withTransaction {
                database.deviceDao().clearForOwner(ownerId)
                database.deviceDao().upsertAll(entities)
            }
            entities.map { entity -> entity.toDeviceMetadata(deviceCatalog) }
        }
    }

    suspend fun bindDevice(
        deviceSerial: String,
        displayName: String,
        productId: Int? = null,
        firmwareVersion: String? = null,
        uniqueId: String? = null
    ): Result<DeviceMetadata> = withContext(ioDispatcher) {
        runCatching {
            if (sessionManager.isGuestMode()) {
                val now = Instant.now()
                val generatedId = deviceSerial
                    .takeIf { it.isNotBlank() }
                    ?: uniqueId
                    ?: now.toEpochMilli().toString()
            val ownerId = sessionManager.activeOwnerId()
            val entity = DeviceEntity(
                id = generatedId,
                name = displayName.ifBlank { deviceSerial.ifBlank { "Local Device" } },
                serial = deviceSerial.ifBlank { uniqueId },
                uniqueId = uniqueId,
                ownerId = ownerId,
                status = "online",
                batteryLevel = 100,
                lastSeenAt = now
            )
                database.deviceDao().upsert(entity)
                entity.toDeviceMetadata(deviceCatalog)
            } else {
                val ownerId = sessionManager.accountOwnerId()
                    ?: throw IllegalStateException("Missing account owner id")
                val type = deviceCatalog.resolveType(productId, displayName)
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
                val entity = dto.toEntity(
                    defaultAlias = displayName,
                    ownerIdOverride = ownerId
                )
                database.deviceDao().upsert(entity)
                entity.toDeviceMetadata(deviceCatalog)
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

    suspend fun renameDevice(deviceId: String, newName: String): Result<DeviceMetadata> =
        withContext(ioDispatcher) {
            runCatching {
                if (sessionManager.isGuestMode()) {
                    val existing = database.deviceDao().findById(deviceId)
                        ?: throw IllegalStateException("Device not found locally")
                    database.deviceDao().updateName(deviceId, newName)
                    return@runCatching existing.copy(name = newName).toDeviceMetadata(deviceCatalog)
                }
                val ownerId = sessionManager.accountOwnerId()
                    ?: throw IllegalStateException("Missing account owner id")
                val idLong = resolveServerDeviceId(deviceId)
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
                    val entity = dto.toEntity(
                        defaultAlias = newName,
                        ownerIdOverride = ownerId
                    )
                    database.deviceDao().upsert(entity)
                    entity.toDeviceMetadata(deviceCatalog)
                } else {
                    database.deviceDao().updateName(deviceId, newName)
                    database.deviceDao().findById(deviceId)?.toDeviceMetadata(deviceCatalog)
                        ?: throw IllegalStateException("Device not found locally after rename")
                }
            }
        }

    suspend fun updateDeviceComboInfo(
        deviceId: String,
        comboInfo: String
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            if (sessionManager.isGuestMode()) {
                updateLocalComboInfo(deviceId, comboInfo)
                return@runCatching
            }
            val idLong = resolveServerDeviceId(deviceId)
                ?: throw IllegalArgumentException("Invalid device id: $deviceId")
            val response = api.updateComboInfo(
                DeviceComboInfoUpdateRequest(
                    id = idLong,
                    comboInfo = comboInfo
                )
            )
            if (response.success.not()) {
                throw IllegalStateException(response.message ?: "Failed to update combination info")
            }
            updateLocalComboInfo(deviceId, comboInfo, resolvedId = idLong)
        }
    }

    suspend fun removeDevice(deviceId: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                if (sessionManager.isGuestMode()) {
                    database.withTransaction {
                        database.deviceDao().deleteById(deviceId)
                        database.measurementDao().deleteForDevice(deviceId)
                    }
                    return@runCatching Unit
                }
                val idLong = resolveServerDeviceId(deviceId)
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
                if (sessionManager.isGuestMode()) {
                    val existing = database.measurementDao().getMeasurementsForDevice(deviceId).first()
                    val records = if (existing.isEmpty()) {
                        val seeded = seedGuestMeasurements(deviceId)
                        database.measurementDao().upsertAll(seeded)
                        seeded
                    } else {
                        existing
                    }
                    return@runCatching records.map { it.toDomain() }
                }
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

    private suspend fun localDevicesSnapshot(): List<DeviceMetadata> {
        val ownerId = sessionManager.activeOwnerId()
        return database.deviceDao().getDevicesForOwner(ownerId).first()
            .map { it.toDeviceMetadata(deviceCatalog) }
    }

    private suspend fun seedGuestMeasurements(deviceId: String): List<MeasurementEntity> {
        val now = Instant.now()
        return listOf(0, 1, 2, 3, 4).map { index ->
            MeasurementEntity(
                id = "$deviceId-sample-$index",
                deviceId = deviceId,
                type = "temperature",
                value = 35.5 + index * 0.3,
                unit = "C",
                recordedAt = now.minusSeconds(index * 3600L),
                rawData = null
            )
        }
    }

    private fun MeasurementEntity.toDomain(): TemperatureRecord =
        TemperatureRecord(
            id = id,
            celsius = value ?: 0.0,
            recordedAt = recordedAt
        )

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

    private fun DeviceDto.toEntity(
        defaultAlias: String? = null,
        ownerIdOverride: String? = null
    ): DeviceEntity =
        DeviceEntity(
            id = id.toString(),
            name = nameAlias?.takeIf { it.isNotBlank() }
                ?: defaultAlias?.takeIf { it.isNotBlank() }
                ?: deviceSerial?.takeIf { it.isNotBlank() }
                ?: id.toString(),
            serial = deviceSerial,
            uniqueId = uniqueId,
            comboInfo = comboInfo,
            ownerId = ownerIdOverride ?: userId?.toString().orEmpty(),
            status = status,
            batteryLevel = batteryLevel,
            lastSeenAt = parseInstant(lastSeenAt)
        )

    private fun DeviceEntity.toDeviceMetadata(deviceCatalog: DeviceCatalog): DeviceMetadata {
        val type = deviceCatalog.resolveType(productId = null, name = name)
        return DeviceMetadata(
            id = id,
            name = name,
            serialNo = uniqueId ?: id,
            macAddress = serial,
            isConnected = status?.equals("online", ignoreCase = true) == true,
            deviceType = type,
            iconResId = deviceCatalog.iconForType(type)
        )
    }

    private suspend fun updateLocalComboInfo(deviceId: String, comboInfo: String, resolvedId: Long? = null) {
        // Update by id, resolved numeric id, and serial to ensure local row matches the server update.
        val idKey = resolvedId?.toString() ?: deviceId
        database.deviceDao().updateComboInfo(idKey, comboInfo)
        database.deviceDao().updateComboInfo(deviceId, comboInfo)
        database.deviceDao().updateComboInfoBySerial(deviceId, comboInfo)
    }

    private suspend fun resolveServerDeviceId(deviceId: String): Long? {
        deviceId.toLongOrNull()?.let { return it }
        val byId = database.deviceDao().findById(deviceId)?.id?.toLongOrNull()
        if (byId != null) return byId
        val bySerial = database.deviceDao().findBySerial(deviceId)?.id?.toLongOrNull()
        return bySerial
    }

}
