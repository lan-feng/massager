package com.massager.app.core

object DeviceTypeConfig {
    val RESERVED_DEVICE_TYPES: List<Int> = (10..19).toList()

    private val deviceNameToType: Map<String, Int> = mapOf(
        "smartpulse" to 10,
        "comfytemp" to 11,
        "massager" to 12
    )

    fun resolveTypeForName(name: String): Int {
        val normalized = name.lowercase()
        return deviceNameToType.entries.firstOrNull { (keyword, _) ->
            normalized.contains(keyword)
        }?.value ?: RESERVED_DEVICE_TYPES.first()
    }
}
