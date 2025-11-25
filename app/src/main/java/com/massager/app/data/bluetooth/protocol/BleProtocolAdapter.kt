package com.massager.app.data.bluetooth.protocol

// 文件说明：蓝牙协议适配器接口，定义编码解码方法。
import java.util.UUID

/**
 * Contract describing how a particular product family speaks over BLE.
 *
 * Every product registers its adapter in [ProtocolRegistry]. The registry takes care of
 * selecting the right adapter once we have parsed the advertisement frame and resolved the
 * `productId`.
 */
interface BleProtocolAdapter {

    /**
     * Unique identifier for this protocol implementation.
     */
    val protocolKey: String
        get() = this::class.java.simpleName

    /**
     * Unique numeric identifier that matches the productId carried in the `hy` advertisement.
     */
    val productId: Int

    /**
     * List of candidate GATT service UUIDs that expose this protocol.
     *
     * Some firmwares ship different UUIDs while using the same protocol.
     */
    val serviceUuidCandidates: List<UUID>
        get() = emptyList()

    /**
     * List of candidate GATT characteristic UUIDs that accept write commands for this protocol.
     */
    val writeCharacteristicUuidCandidates: List<UUID>
        get() = emptyList()

    /**
     * List of candidate GATT characteristic UUIDs that emit notifications with protocol messages.
     */
    val notifyCharacteristicUuidCandidates: List<UUID>
        get() = emptyList()

    /**
     * Preferred MTU size for this protocol. Most JL6328 firmwares support 247 bytes.
     */
    val preferredMtu: Int
        get() = 247

    /**
     * Tries to decode a raw characteristic payload into one or more protocol level messages.
     *
     * @param payload Raw value dispatched by GATT notifications or reads.
     * @return Collection of decoded messages. The list is empty when the payload is not a valid frame.
     */
    fun decode(payload: ByteArray): List<ProtocolMessage>

    /**
     * Encodes a protocol level command into the wire format expected by the device.
     *
     * @param command High level command to be sent to the device.
     * @return Byte array ready to be written to the corresponding GATT characteristic.
     */
    fun encode(command: ProtocolCommand): ByteArray
}

/**
 * Marker interface for protocol level commands. Concrete protocol adapters should expose specific
 * command implementations describing the structure of the payload they expect.
 */
interface ProtocolCommand

/**
 * Base representation of a decoded protocol message.
 *
 * Implementations typically expose strongly typed fields (status, mode, intensity, etc.). We keep it
 * lightweight here so that callers can down-cast to protocol specific variants.
 */
interface ProtocolMessage {
    /**
     * Direction of travel for the frame that originated this message.
     */
    val direction: FrameDirection

    /**
     * Raw command identifier for the decoded frame.
     */
    val commandId: Int
}

/**
 * Frame direction flag defined by the EMS v2 protocol.
 */
enum class FrameDirection {
    /**
     * Payload originated from the mobile app.
     */
    AppToDevice,

    /**
     * Payload originated from the device firmware.
     */
    DeviceToApp
}
