package com.massager.app.domain.model

// 文件说明：保存设备组合信息的领域模型。
import org.json.JSONArray
import org.json.JSONObject

data class ComboDeviceInfo(
    val deviceSerial: String,
    val deviceType: Int? = null,
    val firmwareVersion: String? = null,
    val uniqueId: String? = null,
    val nameAlias: String? = null,
    val index: Int? = null
)

data class ComboInfoPayload(
    val devices: List<ComboDeviceInfo> = emptyList(),
    val index: Int? = null
) {
    fun toJson(): String = ComboInfoSerializer.toJson(this)
}

object ComboInfoSerializer {
    fun parse(raw: String?): ComboInfoPayload {
        if (raw.isNullOrBlank()) return ComboInfoPayload()
        return runCatching {
            val root = JSONObject(raw)
            val array = root.optJSONArray(DEVICES_KEY) ?: JSONArray()
            val rootIndex = root.optInt(INDEX_KEY, 0).takeIf { root.has(INDEX_KEY) && it > 0 }
            ComboInfoPayload(
                devices = buildList {
                    for (index in 0 until array.length()) {
                        val item = array.optJSONObject(index) ?: continue
                        val serial = item.optString(SERIAL_KEY)
                        if (serial.isNullOrBlank()) continue
                        val displayIndex = item.optInt(INDEX_KEY, 0).takeIf { item.has(INDEX_KEY) && it > 0 }
                        add(
                            ComboDeviceInfo(
                                deviceSerial = serial,
                                deviceType = item.optInt(DEVICE_TYPE_KEY).takeIf { item.has(DEVICE_TYPE_KEY) },
                                firmwareVersion = item.optString(FIRMWARE_KEY).takeIf { it.isNotBlank() },
                                uniqueId = item.optString(UNIQUE_ID_KEY).takeIf { it.isNotBlank() },
                                nameAlias = normalizeAlias(item.optString(NAME_ALIAS_KEY)),
                                index = displayIndex
                            )
                        )
                    }
                },
                index = rootIndex
            )
        }.getOrElse { ComboInfoPayload() }
    }

    fun append(raw: String?, entry: ComboDeviceInfo): ComboInfoPayload {
        val existing = parse(raw)
        val replaced = existing.devices.firstOrNull { device ->
            device.deviceSerial.equals(entry.deviceSerial, ignoreCase = true)
        }
        val filtered = existing.devices.filterNot { device ->
            device.deviceSerial.equals(entry.deviceSerial, ignoreCase = true)
        }
        val nextIndexBase = filtered.maxOfOrNull { it.index ?: 0 } ?: 0
        val normalized = when {
            entry.index != null && entry.index > 0 -> entry
            replaced?.index != null && replaced.index > 0 -> entry.copy(index = replaced.index)
            else -> entry.copy(index = nextIndexBase + 1)
        }
        return ComboInfoPayload(filtered + normalized, index = existing.index)
    }

    fun remove(raw: String?, serial: String): ComboInfoPayload {
        val existing = parse(raw)
        val filtered = existing.devices.filterNot { device ->
            device.deviceSerial.equals(serial, ignoreCase = true)
        }
        return ComboInfoPayload(filtered, index = existing.index)
    }

    fun withIndex(raw: String?, index: Int): String {
        val payload = parse(raw)
        val adjusted = payload.copy(index = index.takeIf { it > 0 })
        return toJson(adjusted)
    }

    fun toJson(payload: ComboInfoPayload): String {
        val root = JSONObject()
        payload.index?.let { idx -> root.put(INDEX_KEY, idx) }
        val array = JSONArray()
        payload.devices.forEach { device ->
            val obj = JSONObject()
            obj.put(SERIAL_KEY, device.deviceSerial)
            device.deviceType?.let { obj.put(DEVICE_TYPE_KEY, it) }
            device.firmwareVersion?.let { obj.put(FIRMWARE_KEY, it) }
            device.uniqueId?.let { obj.put(UNIQUE_ID_KEY, it) }
            normalizeAlias(device.nameAlias)?.let { obj.put(NAME_ALIAS_KEY, it) }
            device.index?.let { obj.put(INDEX_KEY, it) }
            array.put(obj)
        }
        root.put(DEVICES_KEY, array)
        return root.toString()
    }

    private const val DEVICES_KEY = "devices"
    private const val SERIAL_KEY = "device_serial"
    private const val DEVICE_TYPE_KEY = "device_type"
    private const val FIRMWARE_KEY = "firmware_version"
    private const val UNIQUE_ID_KEY = "unique_id"
    private const val NAME_ALIAS_KEY = "name_alias"
    private const val INDEX_KEY = "index"

    private fun normalizeAlias(alias: String?): String? {
        if (alias.isNullOrBlank()) return null
        return if (alias.equals("BLE", ignoreCase = true)) "N8" else alias
    }
}
