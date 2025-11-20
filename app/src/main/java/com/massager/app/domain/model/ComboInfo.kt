package com.massager.app.domain.model

import org.json.JSONArray
import org.json.JSONObject

data class ComboDeviceInfo(
    val deviceSerial: String,
    val deviceType: Int? = null,
    val firmwareVersion: String? = null,
    val uniqueId: String? = null,
    val nameAlias: String? = null
)

data class ComboInfoPayload(
    val devices: List<ComboDeviceInfo> = emptyList()
) {
    fun toJson(): String = ComboInfoSerializer.toJson(this)
}

object ComboInfoSerializer {
    fun parse(raw: String?): ComboInfoPayload {
        if (raw.isNullOrBlank()) return ComboInfoPayload()
        return runCatching {
            val root = JSONObject(raw)
            val array = root.optJSONArray(DEVICES_KEY) ?: JSONArray()
            ComboInfoPayload(
                devices = buildList {
                    for (index in 0 until array.length()) {
                        val item = array.optJSONObject(index) ?: continue
                        val serial = item.optString(SERIAL_KEY)
                        if (serial.isNullOrBlank()) continue
                        add(
                            ComboDeviceInfo(
                                deviceSerial = serial,
                                deviceType = item.optInt(DEVICE_TYPE_KEY).takeIf { item.has(DEVICE_TYPE_KEY) },
                                firmwareVersion = item.optString(FIRMWARE_KEY).takeIf { it.isNotBlank() },
                                uniqueId = item.optString(UNIQUE_ID_KEY).takeIf { it.isNotBlank() },
                                nameAlias = item.optString(NAME_ALIAS_KEY).takeIf { it.isNotBlank() }
                            )
                        )
                    }
                }
            )
        }.getOrElse { ComboInfoPayload() }
    }

    fun append(raw: String?, entry: ComboDeviceInfo): ComboInfoPayload {
        val existing = parse(raw)
        val filtered = existing.devices.filterNot { device ->
            device.deviceSerial.equals(entry.deviceSerial, ignoreCase = true)
        }
        return ComboInfoPayload(filtered + entry)
    }

    fun remove(raw: String?, serial: String): ComboInfoPayload {
        val existing = parse(raw)
        val filtered = existing.devices.filterNot { device ->
            device.deviceSerial.equals(serial, ignoreCase = true)
        }
        return ComboInfoPayload(filtered)
    }

    fun toJson(payload: ComboInfoPayload): String {
        val root = JSONObject()
        val array = JSONArray()
        payload.devices.forEach { device ->
            val obj = JSONObject()
            obj.put(SERIAL_KEY, device.deviceSerial)
            device.deviceType?.let { obj.put(DEVICE_TYPE_KEY, it) }
            device.firmwareVersion?.let { obj.put(FIRMWARE_KEY, it) }
            device.uniqueId?.let { obj.put(UNIQUE_ID_KEY, it) }
            device.nameAlias?.let { obj.put(NAME_ALIAS_KEY, it) }
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
}
