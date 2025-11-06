package com.massager.app.core

object DeviceTypeConfig {
    val RESERVED_DEVICE_TYPES: List<Int> = (10..19).toList()

    private val deviceNameToType: Map<String, Int> = mapOf(
        "smartpulse" to 10,
        "Massager" to 11,
        "massager" to 12
    )

    private val productIdToType: Map<Int, Int> = mapOf(
        1 to 12 // EMS v2 product id maps to massager slot
    )

    fun resolveType(productId: Int?, name: String?): Int {
        val fromProduct = productId?.let(productIdToType::get)
        if (fromProduct != null) return fromProduct
        return resolveTypeForName(name)
    }

    fun resolveTypeForName(name: String?): Int {
        val normalized = name.orEmpty().lowercase()
        return deviceNameToType.entries.firstOrNull { (keyword, _) ->
            normalized.contains(keyword)
        }?.value ?: RESERVED_DEVICE_TYPES.first()
    }
}
