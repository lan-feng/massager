package com.massager.app.data.bluetooth.protocol

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry in charge of mapping `productId` identifiers to their BLE protocol adapters.
 *
 * Adapters are contributed via Hilt multibindings. When a device advertisement is parsed and
 * the `productId` is known we can look up the corresponding adapter and delegate all encoding /
 * decoding responsibilities to it.
 */
@Singleton
class ProtocolRegistry @Inject constructor(
    private val adapters: Set<@JvmSuppressWildcards BleProtocolAdapter>
) {

    private val mappedAdapters: Map<Int, BleProtocolAdapter> = adapters.associateBy { it.productId }

    fun defaultAdapter(): BleProtocolAdapter? = mappedAdapters[DEFAULT_PRODUCT_ID] ?: mappedAdapters.values.firstOrNull()

    /**
     * Resolves a [BleProtocolAdapter] for the provided [productId].
     *
     * @return The adapter registered for the id or `null` when we do not support the product yet.
     */
    fun findAdapter(productId: Int?): BleProtocolAdapter? = productId?.let(mappedAdapters::get)

    companion object {
        private const val DEFAULT_PRODUCT_ID = 1
    }
}
