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
import com.massager.app.domain.model.ComboInfoSerializer
import com.massager.app.domain.model.DeviceMetadata
import com.massager.app.domain.model.TemperatureRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
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
            val ensured = backfillDeviceIndexes(devices)
            ensured
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
            val previous = database.deviceDao().listDevicesForOwner(ownerId)
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
            val merged = mergeDeviceIndexes(entities, previous)
                database.withTransaction {
                    database.deviceDao().clearForOwner(ownerId)
                    database.deviceDao().upsertAll(merged)
                }
            merged.map { entity -> entity.toDeviceMetadata(deviceCatalog) }
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
            val normalizedAlias = normalizeAlias(displayName) ?: displayName.takeIf { it.isNotBlank() }
            if (sessionManager.isGuestMode()) {
                val now = Instant.now()
                val ownerId = sessionManager.activeOwnerId()
                val generatedAlias = generateWindowsAlias(
                    baseName = normalizedAlias ?: displayName,
                    existingNames = existingDisplayNames(ownerId)
                )
                val nextIndex = nextDeviceIndex(ownerId)
                val generatedId = deviceSerial
                    .takeIf { it.isNotBlank() }
                    ?: uniqueId
                    ?: now.toEpochMilli().toString()
                val comboWithIndex = ComboInfoSerializer.withIndex(null, nextIndex)
                val entity = DeviceEntity(
                    id = generatedId,
                    name = generatedAlias.ifBlank { normalizedAlias ?: displayName.ifBlank { deviceSerial.ifBlank { "Local Device" } } },
                    serial = deviceSerial.ifBlank { uniqueId },
                    uniqueId = uniqueId,
                    ownerId = ownerId,
                    comboInfo = comboWithIndex,
                    status = "online",
                    batteryLevel = 100,
                    lastSeenAt = now
                )
                database.deviceDao().upsert(entity)
                entity.toDeviceMetadata(deviceCatalog)
            } else {
                val ownerId = sessionManager.accountOwnerId()
                    ?: throw IllegalStateException("Missing account owner id")
                val generatedAlias = generateWindowsAlias(
                    baseName = normalizedAlias ?: displayName,
                    existingNames = existingDisplayNames(ownerId)
                )
                val type = deviceCatalog.resolveType(productId, displayName)
                val nextIndex = nextDeviceIndex(ownerId)
                val comboWithIndex = ComboInfoSerializer.withIndex(null, nextIndex)
                val response = api.bindDevice(
                    DeviceBindRequest(
                        deviceSerial = deviceSerial,
                        deviceType = type,
                        nameAlias = generatedAlias,
                        firmwareVersion = firmwareVersion,
                        uniqueId = uniqueId,
                        comboInfo = comboWithIndex
                    )
                )
                if (response.success.not()) {
                    throw IllegalStateException(response.message ?: "Failed to bind device")
                }
                val dto = response.data ?: throw IllegalStateException("Empty bind response")
                val comboRaw = dto.comboInfo ?: comboWithIndex
                val entity = dto.toEntity(
                    defaultAlias = generatedAlias,
                    ownerIdOverride = ownerId
                )
                val indexedEntity = entity.copy(
                    name = generatedAlias.ifBlank { entity.name },
                    comboInfo = comboWithIndex ?: comboRaw
                )
                database.deviceDao().upsert(indexedEntity)
                // Persist combo_info keyed by id and serial to avoid being cleared by subsequent refresh.
                comboWithIndex?.let { updateLocalComboInfo(deviceSerial, it, resolvedId = dto.id) }
                indexedEntity.toDeviceMetadata(deviceCatalog)
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
                val normalizedName = normalizeAlias(newName) ?: newName
                if (sessionManager.isGuestMode()) {
                    val existing = database.deviceDao().findById(deviceId)
                        ?: throw IllegalStateException("Device not found locally")
                    database.deviceDao().updateName(deviceId, normalizedName)
                    return@runCatching existing.copy(name = normalizedName).toDeviceMetadata(deviceCatalog)
                }
                val ownerId = sessionManager.accountOwnerId()
                    ?: throw IllegalStateException("Missing account owner id")
                val idLong = resolveServerDeviceId(deviceId)
                    ?: throw IllegalArgumentException("Invalid device id: $deviceId")
                val response = api.updateDevice(
                    com.massager.app.data.remote.dto.DeviceUpdateRequest(
                        id = idLong,
                        nameAlias = normalizedName
                    )
                )
                if (response.success.not()) {
                    throw IllegalStateException(response.message ?: "Failed to rename device")
                }
                val dto = response.data
                if (dto != null) {
                    val entity = dto.toEntity(
                        defaultAlias = normalizedName,
                        ownerIdOverride = ownerId
                    )
                    val existing = database.deviceDao().findById(deviceId)
                        ?: database.deviceDao().findBySerial(deviceId)
                    val merged = mergeDeviceIndexes(listOf(entity), existing?.let { listOf(it) } ?: emptyList()).first()
                    database.deviceDao().upsert(merged)
                    merged.toDeviceMetadata(deviceCatalog)
                } else {
                    database.deviceDao().updateName(deviceId, normalizedName)
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

    private suspend fun existingDisplayNames(ownerId: String): List<String> {
        val devices = database.deviceDao().listDevicesForOwner(ownerId)
        if (devices.isEmpty()) return emptyList()
        return buildList {
            devices.forEach { device ->
                val normalized = normalizeAlias(device.name) ?: device.name
                if (!normalized.isNullOrBlank()) add(normalized.trim())
                val payload = ComboInfoSerializer.parse(device.comboInfo)
                payload.devices.forEach { attached ->
                    val alias = normalizeAlias(attached.nameAlias) ?: attached.deviceSerial
                    if (!alias.isNullOrBlank()) add(alias.trim())
                }
            }
        }
    }

    private fun generateWindowsAlias(
        baseName: String?,
        existingNames: Collection<String>
    ): String {
        val sanitizedBase = stripIndexSuffix(baseName).ifBlank { "N8" }
        val pattern = Regex("^${Regex.escape(sanitizedBase)}\\s*\\((\\d+)\\)$", RegexOption.IGNORE_CASE)
        var isBaseTaken = false
        val usedIndexes = mutableSetOf<Int>()
        existingNames.forEach { existing ->
            val candidate = existing.trim()
            if (candidate.equals(sanitizedBase, ignoreCase = true)) {
                isBaseTaken = true
                usedIndexes.add(1)
            } else {
                val match = pattern.matchEntire(candidate)
                val idx = match?.groupValues?.getOrNull(1)?.toIntOrNull()
                if (idx != null && idx > 0) usedIndexes.add(idx)
            }
        }
        if (!isBaseTaken) return sanitizedBase
        var index = 2
        while (usedIndexes.contains(index)) {
            index += 1
        }
        return "$sanitizedBase ($index)"
    }

    private fun stripIndexSuffix(name: String?): String {
        if (name.isNullOrBlank()) return ""
        val regex = Regex("""\s*\(\d+\)$""")
        return name.replace(regex, "").trim()
    }

    private fun normalizeAlias(alias: String?): String? {
        if (alias.isNullOrBlank()) return null
        return if (alias.equals("BLE", ignoreCase = true)) "N8" else alias
    }

    private suspend fun nextDeviceIndex(ownerId: String): Int {
        val existing = database.deviceDao().listDevicesForOwner(ownerId)
        val maxStored = existing
            .mapNotNull { device -> ComboInfoSerializer.parse(device.comboInfo).index }
            .maxOrNull()
        val currentMax = maxStored ?: existing.size
        return currentMax + 1
    }

    private fun DeviceDto.toEntity(
        defaultAlias: String? = null,
        ownerIdOverride: String? = null
    ): DeviceEntity =
        DeviceEntity(
            id = id.toString(),
            name = normalizeAlias(nameAlias)
                ?: normalizeAlias(defaultAlias)
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
        val displayName = normalizeAlias(name) ?: name
        val type = deviceCatalog.resolveType(productId = null, name = displayName)
        val payload = ComboInfoSerializer.parse(comboInfo)
        val attached = payload.devices
        return DeviceMetadata(
            id = id,
            name = displayName,
            serialNo = uniqueId ?: id,
            macAddress = serial,
            isConnected = status?.equals("online", ignoreCase = true) == true,
            index = payload.index,
            deviceType = type,
            iconResId = deviceCatalog.iconForType(type),
            attachedDevices = attached
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

    private fun backfillDeviceIndexes(
        devices: List<DeviceEntity>
    ): List<DeviceEntity> {
        if (devices.isEmpty()) return devices
        val missing = devices.any { ComboInfoSerializer.parse(it.comboInfo).index == null }
        if (!missing) return devices

        var maxIndex = devices.mapNotNull { ComboInfoSerializer.parse(it.comboInfo).index }.maxOrNull() ?: 0
        val updated = devices.map { device ->
            val payload = ComboInfoSerializer.parse(device.comboInfo)
            val assignedIndex = payload.index ?: run {
                maxIndex += 1
                maxIndex
            }
            val comboWithIndex = ComboInfoSerializer.withIndex(device.comboInfo, assignedIndex)
            device.copy(
                name = normalizeAlias(device.name) ?: device.name,
                comboInfo = comboWithIndex
            )
        }
        // Persist the backfilled combo_info so numbering is stable for future launches.
        runBlocking(ioDispatcher) {
            database.deviceDao().upsertAll(updated)
        }
        return updated
    }

    private fun mergeDeviceIndexes(
        incoming: List<DeviceEntity>,
        previous: List<DeviceEntity>
    ): List<DeviceEntity> {
        val indexByKey = mutableMapOf<String, Int>()
        previous.forEach { device ->
            val payload = ComboInfoSerializer.parse(device.comboInfo)
            payload.index?.let { idx ->
                indexByKey[device.id] = idx
                device.serial?.let { indexByKey[it] = idx }
            }
        }
        var maxIndex = indexByKey.values.maxOrNull() ?: 0
        return incoming.map { entity ->
            val currentIndex = ComboInfoSerializer.parse(entity.comboInfo).index
            val preserved = indexByKey[entity.id] ?: entity.serial?.let { indexByKey[it] }
            val finalIndex = currentIndex ?: preserved ?: run {
                maxIndex += 1
                maxIndex
            }
            val comboWithIndex = ComboInfoSerializer.withIndex(entity.comboInfo, finalIndex)
            entity.copy(
                name = normalizeAlias(entity.name) ?: entity.name,
                comboInfo = comboWithIndex
            )
        }
    }

}
