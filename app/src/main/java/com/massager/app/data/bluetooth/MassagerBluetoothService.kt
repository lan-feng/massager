package com.massager.app.data.bluetooth

// 文件说明：多设备并发 GATT 服务实现，按地址维护独立会话，连接/写入/状态流按地址分发。
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.massager.app.R
import com.massager.app.core.logging.logTag
import com.massager.app.data.bluetooth.protocol.BleProtocolAdapter
import com.massager.app.data.bluetooth.protocol.ProtocolCommand
import com.massager.app.data.bluetooth.protocol.ProtocolMessage
import com.massager.app.data.bluetooth.protocol.ProtocolRegistry
import com.massager.app.data.bluetooth.scan.BleScanCoordinator
import com.massager.app.data.bluetooth.scan.BleScanCoordinator.CachedScanDevice
import com.massager.app.data.bluetooth.session.EmsFrameExtractor
import com.massager.app.data.bluetooth.session.ProtocolPayloadBuffer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SERVICE_DISCOVERY_RETRY_DELAY_MS = 600L
private const val CONNECTION_PRIORITY_RESET_DELAY_MS = 7_000L
private const val EMPTY_DECODE_LOG_THROTTLE_MS = 1_000L
private const val INITIAL_MTU_TIMEOUT_MS = 2_000L
private const val WRITE_TIMEOUT_MS = 5_000L
private const val MAX_SERVICE_DISCOVERY_RETRIES = 3
private val TAG = logTag("MassagerBleService")
private val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
    UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

data class BleScanResult(
    val name: String,
    val address: String,
    val rssi: Int,
    val isConnected: Boolean,
    val productId: Int? = null,
    val firmwareVersion: String? = null,
    val uniqueId: String? = null
)

data class BleConnectionState(
    val status: Status = Status.Idle,
    val deviceName: String? = null,
    val deviceAddress: String? = null,
    val errorMessage: String? = null,
    val isProtocolReady: Boolean = false
) {
    enum class Status {
        Idle,
        Scanning,
        Connecting,
        Connected,
        Disconnected,
        Failed,
        BluetoothUnavailable
    }
}

interface BleConnectionListener {
    fun onConnected(device: BluetoothDevice)
    fun onDisconnected(device: BluetoothDevice)
    fun onConnectionFailed(device: BluetoothDevice?, reason: String?)
}

private data class PendingWrite(
    val gatt: BluetoothGatt,
    val characteristic: BluetoothGattCharacteristic,
    val payload: ByteArray,
    val writeType: Int,
    val completion: CompletableDeferred<Boolean>
)

private data class GattSession(
    val gatt: BluetoothGatt,
    val adapter: BleProtocolAdapter,
    val address: String,
    val payloadBuffer: ProtocolPayloadBuffer = ProtocolPayloadBuffer(EmsFrameExtractor()),
    var connectionState: BleConnectionState = BleConnectionState(status = BleConnectionState.Status.Connecting, deviceAddress = address),
    var activeServiceUuid: UUID? = null,
    var activeWriteCharacteristicUuid: UUID? = null,
    var activeNotifyCharacteristicUuid: UUID? = null,
    var pendingServiceRetry: Runnable? = null,
    var pendingPriorityReset: Runnable? = null,
    var pendingMtuTimeout: Runnable? = null,
    var pendingWriteTimeout: Runnable? = null,
    var pendingNotifyEnableCharacteristicUuid: UUID? = null,
    var awaitingInitialMtu: Boolean = false,
    var serviceDiscoveryRetries: Int = 0,
    val writeQueue: ArrayDeque<PendingWrite> = ArrayDeque(),
    var activeWrite: PendingWrite? = null
)

@Singleton
class MassagerBluetoothService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?,
    private val protocolRegistry: ProtocolRegistry,
    private val scanCoordinator: BleScanCoordinator
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listeners = CopyOnWriteArraySet<BleConnectionListener>()
    private val sessions = mutableMapOf<String, GattSession>()
    private val lastEmptyDecodeLogAt = mutableMapOf<String, Long>()
    private var preferredAddress: String? = null
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_TURNING_OFF,
                BluetoothAdapter.STATE_OFF -> handleBluetoothTurnedOff()
                BluetoothAdapter.STATE_ON -> handleBluetoothTurnedOn()
            }
        }
    }

    val scanResults: StateFlow<List<BleScanResult>> = scanCoordinator.scanResults

    private val _connectionState = MutableStateFlow(BleConnectionState())
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _connectionStates = MutableStateFlow<Map<String, BleConnectionState>>(emptyMap())
    val connectionStates: StateFlow<Map<String, BleConnectionState>> = _connectionStates.asStateFlow()

    private val _protocolMessages = MutableSharedFlow<ProtocolMessage>(extraBufferCapacity = 32)
    val protocolMessages: SharedFlow<ProtocolMessage> = _protocolMessages.asSharedFlow()
    private val _deviceProtocolMessages = MutableSharedFlow<DeviceProtocolMessage>(extraBufferCapacity = 32)
    val deviceProtocolMessages: SharedFlow<DeviceProtocolMessage> = _deviceProtocolMessages.asSharedFlow()

    private val scanFailureListener = object : BleScanCoordinator.ScanFailureListener {
        override fun onScanFailure(errorCode: Int) {
            _connectionStates.update { current ->
                current.mapValues { (_, state) ->
                    state.copy(
                        status = BleConnectionState.Status.Failed,
                        errorMessage = "Scan failed ($errorCode)",
                        isProtocolReady = false
                    )
                }
            }
        }
    }

    init {
        scanCoordinator.addFailureListener(scanFailureListener)
        @Suppress("DEPRECATION")
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
    }

    private fun sessionFor(address: String?): GattSession? = address?.let { sessions[it] }

    private fun updateState(address: String, newState: BleConnectionState) {
        val stateWithAddr = newState.copy(deviceAddress = address)
        _connectionStates.update { it + (address to stateWithAddr) }
        _connectionState.value = stateWithAddr
        sessions[address]?.connectionState = stateWithAddr
    }

    fun addConnectionListener(listener: BleConnectionListener) {
        listeners.add(listener)
    }

    fun removeConnectionListener(listener: BleConnectionListener) {
        listeners.remove(listener)
    }

    fun clearConnectionListeners() {
        listeners.clear()
    }

    fun clearError() {
        _connectionState.update { it.copy(errorMessage = null) }
    }

    @SuppressLint("MissingPermission")
    fun startScan(): BleScanCoordinator.ScanStartResult {
        val result = scanCoordinator.startScan()
        when (result) {
            is BleScanCoordinator.ScanStartResult.Started -> _connectionState.update {
                it.copy(status = BleConnectionState.Status.Scanning, errorMessage = null, isProtocolReady = false)
            }
            is BleScanCoordinator.ScanStartResult.Error -> _connectionState.value =
                BleConnectionState(status = result.status, errorMessage = result.message)
        }
        return result
    }

    fun restartScan(): BleScanCoordinator.ScanStartResult {
        scanCoordinator.clearCache()
        return startScan()
    }

    fun stopScan() {
        scanCoordinator.stopScan()
        _connectionState.update {
            if (it.status == BleConnectionState.Status.Scanning) it.copy(status = BleConnectionState.Status.Idle, isProtocolReady = false) else it
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String): Boolean {
        val adapter = bluetoothAdapter ?: run {
            _connectionState.update {
                it.copy(
                    status = BleConnectionState.Status.BluetoothUnavailable,
                    errorMessage = context.getString(R.string.device_error_bluetooth_disabled)
                )
            }
            return false
        }
        if (!adapter.isEnabled) {
            _connectionState.update {
                it.copy(
                    status = BleConnectionState.Status.BluetoothUnavailable,
                    errorMessage = context.getString(R.string.device_error_bluetooth_disabled)
                )
            }
            return false
        }
        if (!hasConnectPermission()) {
            _connectionState.update {
                it.copy(
                    status = BleConnectionState.Status.BluetoothUnavailable,
                    errorMessage = context.getString(R.string.device_error_bluetooth_permission)
                )
            }
            return false
        }
        sessions[address]?.let { session ->
            if (session.connectionState.status == BleConnectionState.Status.Connected ||
                session.connectionState.status == BleConnectionState.Status.Connecting
            ) return true
        }

        val cachedEntry = scanCoordinator.getCachedDevice(address)
        val device = cachedEntry?.device ?: runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: return false
        val resolvedAdapter = protocolRegistry.findAdapter(cachedEntry?.productId) ?: protocolRegistry.defaultAdapter()
            ?: return false

        val callback = createCallback(address)
        val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, callback)
        } ?: return false

        val initialState = BleConnectionState(
            status = BleConnectionState.Status.Connecting,
            deviceName = resolveDeviceName(device, cachedEntry, device.address),
            deviceAddress = address,
            isProtocolReady = false
        )
        val session = GattSession(gatt = gatt, adapter = resolvedAdapter, address = address, connectionState = initialState)
        sessions[address] = session
        updateState(address, initialState)
        scanCoordinator.updateConnectedAddress(address)
        return true
    }

    @SuppressLint("MissingPermission")
    fun disconnect(address: String? = null) {
        val targets = if (address != null) listOf(address) else sessions.keys.toList()
        targets.forEach { addr ->
            val session = sessions.remove(addr) ?: return@forEach
            runCatching { session.gatt.disconnect() }
                .onFailure { Log.w(TAG, "disconnect: gatt.disconnect failed for $addr", it) }
            cleanupSession(session)
            updateState(addr, BleConnectionState(status = BleConnectionState.Status.Disconnected, deviceAddress = addr))
            scanCoordinator.updateConnectedAddress(null)
        }
        if (address == null) clearConnectionListeners()
    }

    fun shutdown() {
        stopScan()
        disconnect()
        ioScope.cancel()
        scanCoordinator.removeFailureListener(scanFailureListener)
        runCatching { context.unregisterReceiver(bluetoothStateReceiver) }
    }

    @SuppressLint("MissingPermission")
    fun readCharacteristic(serviceUuid: UUID, characteristicUuid: UUID, address: String? = null): Boolean {
        val session = sessionFor(address) ?: sessionFor(connectionState.value.deviceAddress) ?: return false
        val characteristic = session.gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid) ?: return false
        @Suppress("DEPRECATION")
        return session.gatt.readCharacteristic(characteristic)
    }

    @SuppressLint("MissingPermission")
    suspend fun writeCharacteristic(
        serviceUuid: UUID,
        characteristicUuid: UUID,
        payload: ByteArray,
        writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
        address: String? = null
    ): Boolean {
        val session = sessionFor(address) ?: sessionFor(connectionState.value.deviceAddress) ?: return false
        val characteristic = session.gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid) ?: run {
            Log.w(TAG, "writeCharacteristic: characteristic not found service=$serviceUuid characteristic=$characteristicUuid")
            return false
        }
        val request = PendingWrite(
            gatt = session.gatt,
            characteristic = characteristic,
            payload = payload.copyOf(),
            writeType = writeType,
            completion = CompletableDeferred()
        )
        enqueueWrite(session, request)
        return request.completion.await()
    }

    suspend fun sendProtocolCommand(
        command: ProtocolCommand,
        writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
        address: String? = null
    ): Boolean {
        val targetAddress = address ?: preferredAddress ?: connectionState.value.deviceAddress
        val session = sessionFor(targetAddress) ?: return false
        val adapter = session.adapter
        val serviceUuid = session.activeServiceUuid ?: adapter.serviceUuidCandidates.firstOrNull()
        val characteristicUuid = session.activeWriteCharacteristicUuid ?: adapter.writeCharacteristicUuidCandidates.firstOrNull()
        if (serviceUuid == null || characteristicUuid == null) {
            scheduleServiceRediscovery(session, "missing_handle_for_command")
            return false
        }
        val payload = adapter.encode(command)
        Log.d(
            TAG,
            "sendProtocolCommand: address=$targetAddress command=$command payload=${payload.toDebugHexString()}"
        )
        val result = writeCharacteristic(serviceUuid, characteristicUuid, payload, writeType, session.address)
        if (!result) Log.e(TAG, "sendProtocolCommand: write failed service=$serviceUuid characteristic=$characteristicUuid command=$command")
        return result
    }

    fun isProtocolReady(address: String? = null): Boolean =
        sessionFor(address)?.activeWriteCharacteristicUuid != null

    fun activeProtocolKey(address: String? = null): String? =
        sessionFor(address)?.adapter?.protocolKey

    fun cachedDeviceName(address: String?): String? = scanCoordinator.cachedDeviceName(address)

    fun setPreferredAddress(address: String?) {
        preferredAddress = address
    }

    private fun createCallback(address: String): BluetoothGattCallback {
        return object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                val session = sessionFor(address) ?: return
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> handleConnected(session, gatt)
                    BluetoothProfile.STATE_DISCONNECTED -> handleDisconnected(session, status)
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                val session = sessionFor(address) ?: return
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    val failed = session.connectionState.copy(
                        status = BleConnectionState.Status.Failed,
                        errorMessage = "Service discovery failed ($status)"
                    )
                    updateState(address, failed)
                    notifyConnectionFailed(gatt.device, "Service discovery failed")
                    return
                }
                val adapter = session.adapter
                val resolveResult = resolveProtocolHandles(session, gatt, adapter, enableNotifications = true)
                if (!resolveResult.ready && resolveResult.missingHandles) {
                    Log.e(TAG, "onServicesDiscovered: unable to resolve protocol handles for ${gatt.device.address}")
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                val session = sessionFor(address) ?: return
                session.awaitingInitialMtu = false
                session.pendingMtuTimeout?.let(mainHandler::removeCallbacks)
                session.pendingMtuTimeout = null
                queueServiceDiscovery(session, "mtu_changed", delayMs = 0L)
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int
            ) {
                val session = sessionFor(address) ?: return
                if (descriptor.uuid == CLIENT_CHARACTERISTIC_CONFIG_UUID &&
                    session.pendingNotifyEnableCharacteristicUuid == descriptor.characteristic.uuid
                ) {
                    session.pendingNotifyEnableCharacteristicUuid = null
                    val ready = status == BluetoothGatt.GATT_SUCCESS
                    updateState(address, session.connectionState.copy(isProtocolReady = ready))
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                val session = sessionFor(address) ?: return
                val completed = synchronized(session.writeQueue) {
                    if (session.activeWrite != null && session.activeWrite?.characteristic == characteristic) session.activeWrite else null
                }
                completed?.completion?.complete(status == BluetoothGatt.GATT_SUCCESS)
                if (completed != null) {
                    synchronized(session.writeQueue) {
                        if (session.activeWrite === completed) session.activeWrite = null
                    }
                    session.pendingWriteTimeout?.let(mainHandler::removeCallbacks)
                    session.pendingWriteTimeout = null
                    advanceWriteQueue(session)
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                handleProtocolPayload(address, value)
            }

            @Suppress("DEPRECATION")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                characteristic.value?.let { payload -> handleProtocolPayload(address, payload) }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    characteristic.value?.let { payload -> handleProtocolPayload(address, payload) }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleConnected(session: GattSession, gatt: BluetoothGatt) {
        val state = BleConnectionState(
            status = BleConnectionState.Status.Connected,
            deviceName = resolveDeviceName(
                device = gatt.device,
                cachedEntry = scanCoordinator.getCachedDevice(gatt.device.address),
                fallback = gatt.device.address
            ),
            deviceAddress = session.address,
            errorMessage = null,
            isProtocolReady = false
        )
        updateState(session.address, state)
        notifyConnected(gatt.device)
        boostConnectionPriority(session, gatt)
        val mtuRequested = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && hasConnectPermission()) {
            runCatching { gatt.requestMtu(session.adapter.preferredMtu) }.getOrDefault(false)
        } else false
        session.awaitingInitialMtu = mtuRequested
        if (mtuRequested) {
            session.pendingMtuTimeout?.let(mainHandler::removeCallbacks)
            val timeoutRunnable = Runnable { handleInitialMtuTimeout(session, gatt) }
            session.pendingMtuTimeout = timeoutRunnable
            mainHandler.postDelayed(timeoutRunnable, INITIAL_MTU_TIMEOUT_MS)
        } else {
            queueServiceDiscovery(session, "initial_connect_no_mtu", delayMs = 0L)
        }
    }

    private fun handleDisconnected(session: GattSession, status: Int) {
        val failed = status != BluetoothGatt.GATT_SUCCESS
        val newState = session.connectionState.copy(
            status = if (failed) BleConnectionState.Status.Failed else BleConnectionState.Status.Disconnected,
            errorMessage = if (failed) "Disconnected ($status)" else null,
            isProtocolReady = false
        )
        updateState(session.address, newState)
        cleanupSession(session)
        if (failed) notifyConnectionFailed(session.gatt.device, "Disconnected ($status)") else notifyDisconnected(session.gatt.device)
        sessions.remove(session.address)
    }

    private fun cleanupSession(session: GattSession) {
        session.pendingServiceRetry?.let(mainHandler::removeCallbacks)
        session.pendingPriorityReset?.let(mainHandler::removeCallbacks)
        session.pendingMtuTimeout?.let(mainHandler::removeCallbacks)
        session.pendingWriteTimeout?.let(mainHandler::removeCallbacks)
        session.pendingNotifyEnableCharacteristicUuid = null
        session.activeWrite = null
        session.writeQueue.clear()
        if (hasConnectPermission()) {
            runCatching { session.gatt.close() }
                .onFailure { Log.w(TAG, "cleanupSession: gatt.close failed for ${session.address}", it) }
        } else {
            // Even without permission, best effort close to avoid leaking the Binder.
            runCatching { session.gatt.close() }
                .onFailure { Log.w(TAG, "cleanupSession: gatt.close without permission failed for ${session.address}", it) }
        }
    }

    private fun handleBluetoothTurnedOff() {
        Log.d(TAG, "handleBluetoothTurnedOff")
        stopScan()
        // Mark all sessions unavailable and clean them up.
        val addresses = sessions.keys.toList()
        addresses.forEach { addr ->
            sessions.remove(addr)?.let { session ->
                runCatching { session.gatt.disconnect() }
                cleanupSession(session)
            }
        }
        _connectionStates.update { map ->
            map.mapValues { (_, state) ->
                state.copy(
                    status = BleConnectionState.Status.BluetoothUnavailable,
                    errorMessage = context.getString(R.string.device_error_bluetooth_disabled),
                    isProtocolReady = false
                )
            }
        }
        _connectionState.update {
            it.copy(
                status = BleConnectionState.Status.BluetoothUnavailable,
                errorMessage = context.getString(R.string.device_error_bluetooth_disabled),
                isProtocolReady = false
            )
        }
    }

    private fun handleBluetoothTurnedOn() {
        Log.d(TAG, "handleBluetoothTurnedOn")
        _connectionStates.update { map ->
            map.mapValues { (_, state) ->
                state.copy(
                    status = BleConnectionState.Status.Disconnected,
                    errorMessage = null,
                    isProtocolReady = false
                )
            }
        }
        _connectionState.update {
            it.copy(
                status = BleConnectionState.Status.Idle,
                errorMessage = null,
                isProtocolReady = false
            )
        }
    }

    private data class ResolveResult(
        val ready: Boolean,
        val missingHandles: Boolean
    )

    private fun resolveProtocolHandles(
        session: GattSession,
        gatt: BluetoothGatt,
        adapter: BleProtocolAdapter,
        enableNotifications: Boolean
    ): ResolveResult {
        val notifyUuid = adapter.notifyCharacteristicUuidCandidates.firstOrNull()
        val writeUuid = adapter.writeCharacteristicUuidCandidates.firstOrNull()
        val serviceUuid = adapter.serviceUuidCandidates.firstOrNull()

        var writeCharacteristic = when {
            writeUuid != null -> gatt.findCharacteristic(writeUuid)
            serviceUuid != null -> gatt.getService(serviceUuid)?.characteristics?.firstOrNull { it.hasWriteProperty() }
            else -> null
        }
        var notifyCharacteristic = when {
            notifyUuid != null -> gatt.findCharacteristic(notifyUuid)
            serviceUuid != null -> gatt.getService(serviceUuid)?.characteristics?.firstOrNull { it.hasNotifyProperty() }
            else -> null
        }

        if (writeCharacteristic == null) {
            // Fallback: scan all services for the first writable characteristic
            gatt.services.forEach { service ->
                service.characteristics.firstOrNull { it.hasWriteProperty() }?.let { candidate ->
                    writeCharacteristic = candidate
                    notifyCharacteristic = notifyCharacteristic ?: service.characteristics.firstOrNull { it.hasNotifyProperty() }
                    return@forEach
                }
            }
        }

        val resolvedService = writeCharacteristic?.service ?: notifyCharacteristic?.service
        session.activeServiceUuid = resolvedService?.uuid
        session.activeWriteCharacteristicUuid = writeCharacteristic?.uuid
        session.activeNotifyCharacteristicUuid = notifyCharacteristic?.uuid

        val notifyChar = notifyCharacteristic
        val notificationsReady = if (enableNotifications && notifyChar != null) {
            enableNotifications(gatt, notifyChar, session)
        } else true

        val ready = session.activeWriteCharacteristicUuid != null && notificationsReady
        updateState(session.address, session.connectionState.copy(isProtocolReady = ready))
        val missingHandles = session.activeWriteCharacteristicUuid == null ||
            (enableNotifications && notifyChar == null)
        return ResolveResult(ready = ready, missingHandles = missingHandles)
    }

    private fun BluetoothGatt.findCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
        services.forEach { service ->
            service.getCharacteristic(uuid)?.let { return it }
        }
        return null
    }

    private fun BluetoothGattCharacteristic.hasWriteProperty(): Boolean {
        val writeMask = BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
        return properties and writeMask != 0
    }

    private fun BluetoothGattCharacteristic.hasNotifyProperty(): Boolean {
        val notifyMask = BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE
        return properties and notifyMask != 0
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        session: GattSession
    ): Boolean {
        val enabled = gatt.setCharacteristicNotification(characteristic, true)
        if (!enabled) return false
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
        if (descriptor != null) {
            val writeSuccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothStatusCodes.SUCCESS
            } else {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
            if (writeSuccess) {
                session.pendingNotifyEnableCharacteristicUuid = characteristic.uuid
                return false
            }
        }
        return true
    }

    private fun queueServiceDiscovery(session: GattSession, reason: String, delayMs: Long = SERVICE_DISCOVERY_RETRY_DELAY_MS) {
        if (session.serviceDiscoveryRetries >= MAX_SERVICE_DISCOVERY_RETRIES) return
        session.serviceDiscoveryRetries += 1
        session.pendingServiceRetry?.let(mainHandler::removeCallbacks)
        val runnable = Runnable {
            if (hasConnectPermission()) {
                val started = runCatching { session.gatt.discoverServices() }.getOrDefault(false)
                if (!started) scheduleServiceRediscovery(session, "retry_after_failed_start")
            }
        }
        session.pendingServiceRetry = runnable
        if (delayMs <= 0L) {
            mainHandler.post(runnable)
        } else {
            mainHandler.postDelayed(runnable, delayMs)
        }
    }

    private fun scheduleServiceRediscovery(session: GattSession, reason: String): Boolean {
        if (session.serviceDiscoveryRetries >= MAX_SERVICE_DISCOVERY_RETRIES) return false
        queueServiceDiscovery(session, reason)
        return true
    }

    private fun boostConnectionPriority(session: GattSession, gatt: BluetoothGatt) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
        if (hasConnectPermission()) {
            runCatching { gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH) }
                .onFailure { Log.w(TAG, "boostConnectionPriority: high priority request denied", it) }
        }
        session.pendingPriorityReset?.let(mainHandler::removeCallbacks)
        val resetRunnable = Runnable {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && hasConnectPermission()) {
                runCatching { gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED) }
                    .onFailure { Log.w(TAG, "boostConnectionPriority: reset priority denied", it) }
            }
            session.pendingPriorityReset = null
        }
        session.pendingPriorityReset = resetRunnable
        mainHandler.postDelayed(resetRunnable, CONNECTION_PRIORITY_RESET_DELAY_MS)
    }

    private fun handleInitialMtuTimeout(session: GattSession, gatt: BluetoothGatt) {
        if (!session.awaitingInitialMtu) return
        session.awaitingInitialMtu = false
        session.pendingMtuTimeout = null
        queueServiceDiscovery(session, "initial_connect_timeout")
    }

    private fun enqueueWrite(session: GattSession, request: PendingWrite) {
        val shouldStart = synchronized(session.writeQueue) {
            if (session.activeWrite == null) {
                session.activeWrite = request
                true
            } else {
                session.writeQueue.addLast(request)
                false
            }
        }
        if (shouldStart) {
            performWrite(session, request)
        } else if (session.gatt != request.gatt) {
            synchronized(session.writeQueue) { session.writeQueue.remove(request) }
            request.completion.complete(false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun performWrite(session: GattSession, request: PendingWrite) {
        if (session.gatt != request.gatt) {
            request.completion.complete(false)
            advanceWriteQueue(session)
            return
        }
        val characteristic = request.characteristic
        val payload = request.payload
        Log.d(TAG, "writeCharacteristic: payload=${payload.toDebugHexString()}")
        val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            request.gatt.writeCharacteristic(characteristic, payload, request.writeType) == BluetoothStatusCodes.SUCCESS
        } else {
            characteristic.value = payload
            characteristic.writeType = request.writeType
            @Suppress("DEPRECATION")
            request.gatt.writeCharacteristic(characteristic)
        }
        if (!success) {
            request.completion.complete(false)
            advanceWriteQueue(session)
        } else {
            session.pendingWriteTimeout?.let(mainHandler::removeCallbacks)
            val timeout = Runnable {
                if (session.activeWrite === request) {
                    Log.w(TAG, "writeCharacteristic: timing out write for ${request.characteristic.uuid}")
                    request.completion.complete(false)
                    advanceWriteQueue(session)
                }
            }
            session.pendingWriteTimeout = timeout
            mainHandler.postDelayed(timeout, WRITE_TIMEOUT_MS)
        }
    }

    private fun advanceWriteQueue(session: GattSession) {
        session.pendingWriteTimeout?.let(mainHandler::removeCallbacks)
        session.pendingWriteTimeout = null
        val next = synchronized(session.writeQueue) {
            if (session.writeQueue.isNotEmpty()) {
                session.writeQueue.removeFirst().also { session.activeWrite = it }
            } else {
                session.activeWrite = null
                null
            }
        }
        if (next != null) performWrite(session, next)
    }

    private fun handleProtocolPayload(address: String, payload: ByteArray) {
        if (payload.isEmpty()) return
        val session = sessionFor(address) ?: return
        val adapter = session.adapter
        val payloadBuffer = session.payloadBuffer
        val frames = payloadBuffer.append(payload)
        if (frames.isEmpty()) {
            val now = SystemClock.elapsedRealtime()
            val last = lastEmptyDecodeLogAt[address] ?: 0L
            if (now - last >= EMPTY_DECODE_LOG_THROTTLE_MS) {
                lastEmptyDecodeLogAt[address] = now
                Log.v(TAG, "handleProtocolPayload: buffered payload length=${payload.size} address=$address")
            }
            return
        }
        ioScope.launch {
            frames.forEachIndexed { index, frame ->
                Log.d(TAG, "handleProtocolPayload: decoding frame#$index hex=${frame.toDebugHexString()} length=${frame.size}")
                val messages = adapter.decode(frame)
                if (messages.isEmpty()) {
                    val now = SystemClock.elapsedRealtime()
                    val last = lastEmptyDecodeLogAt[address] ?: 0L
                    if (now - last >= EMPTY_DECODE_LOG_THROTTLE_MS) {
                        lastEmptyDecodeLogAt[address] = now
                        Log.v(TAG, "handleProtocolPayload: no decodable messages (length=${frame.size})")
                    }
                    return@forEachIndexed
                }
                messages.forEach { message ->
                    _protocolMessages.tryEmit(message)
                    _deviceProtocolMessages.tryEmit(DeviceProtocolMessage(address = address, message = message))
                }
            }
        }
    }

    private fun notifyConnected(device: BluetoothDevice) {
        listeners.forEach { it.onConnected(device) }
    }

    private fun notifyDisconnected(device: BluetoothDevice) {
        listeners.forEach { it.onDisconnected(device) }
    }

    private fun notifyConnectionFailed(device: BluetoothDevice?, reason: String?) {
        listeners.forEach { it.onConnectionFailed(device, reason) }
    }

    private fun hasConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }

    private fun ByteArray.toDebugHexString(limit: Int = 32): String {
        val actualLimit = min(size, limit)
        val builder = StringBuilder(actualLimit * 3)
        for (index in 0 until actualLimit) {
            if (index > 0) builder.append(' ')
            builder.append(String.format("%02X", this[index]))
        }
        if (size > limit) builder.append(" …(${size} bytes)")
        return builder.toString()
    }

    private fun resolveDeviceName(
        device: BluetoothDevice? = null,
        cachedEntry: CachedScanDevice? = null,
        fallback: String? = null
    ): String? {
        cachedEntry?.name?.takeIf { it.isNotBlank() }?.let { return it }
        if (device != null && hasConnectPermission()) {
            val candidate = runCatching { device.name }.getOrNull()
            if (!candidate.isNullOrBlank()) return candidate
        }
        return fallback
    }
}
