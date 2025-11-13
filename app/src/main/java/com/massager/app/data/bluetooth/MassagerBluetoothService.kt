package com.massager.app.data.bluetooth

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
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.util.Log
import com.massager.app.data.bluetooth.protocol.BleProtocolAdapter
import com.massager.app.data.bluetooth.protocol.HyAdvertisement
import com.massager.app.data.bluetooth.protocol.ProtocolCommand
import com.massager.app.data.bluetooth.protocol.ProtocolMessage
import com.massager.app.data.bluetooth.protocol.ProtocolRegistry
import com.massager.app.R
import com.massager.app.core.logging.logTag
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SCAN_TIMEOUT_MS = 15_000L
private const val SCAN_RESULT_TTL_MS = 20_000L
private const val INITIAL_SERVICE_DISCOVERY_DELAY_MS = 200L
private const val MAX_SERVICE_DISCOVERY_RETRIES = 3
private const val SERVICE_DISCOVERY_RETRY_DELAY_MS = 600L
private const val CONNECTION_PRIORITY_RESET_DELAY_MS = 7_000L
private const val EMPTY_DECODE_LOG_THROTTLE_MS = 1_000L
private const val INITIAL_MTU_TIMEOUT_MS = 2_000L
private const val HY_PAYLOAD_LENGTH = 7
private const val HY_HEADER_FIRST: Byte = 0x68
private const val HY_HEADER_SECOND: Byte = 0x79
private const val EMS_HEADER_LENGTH = 2
private const val EMS_LENGTH_FIELD = 2
private const val EMS_TERMINATOR_LENGTH = 2
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

@Singleton
class MassagerBluetoothService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?,
    private val protocolRegistry: ProtocolRegistry
) {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastEmptyDecodeLogAt = 0L
    private val listeners = CopyOnWriteArraySet<BleConnectionListener>()
    private val cachedDevices = mutableMapOf<String, CachedScanDevice>()

    private val _scanResults = MutableStateFlow<List<BleScanResult>>(emptyList())
    val scanResults: StateFlow<List<BleScanResult>> = _scanResults.asStateFlow()

    private val _connectionState = MutableStateFlow(BleConnectionState())
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _protocolMessages = MutableSharedFlow<ProtocolMessage>(extraBufferCapacity = 32)
    val protocolMessages: SharedFlow<ProtocolMessage> = _protocolMessages.asSharedFlow()

    @Volatile
    private var connectedAddress: String? = null

    @Volatile
    private var activeAdapter: BleProtocolAdapter? = null
    @Volatile
    private var activeServiceUuid: UUID? = null
    @Volatile
    private var activeWriteCharacteristicUuid: UUID? = null
    @Volatile
    private var activeNotifyCharacteristicUuid: UUID? = null

    private var scanTimeoutJob: Job? = null
    private var currentGatt: BluetoothGatt? = null
    private var isScanning = false
    private var serviceDiscoveryRetries = 0
    private var pendingServiceRetry: Runnable? = null
    private var pendingPriorityReset: Runnable? = null
    private var pendingMtuTimeout: Runnable? = null
    private var awaitingInitialMtu = false
    private var pendingNotifyEnableCharacteristicUuid: UUID? = null
    private val writeQueue = ArrayDeque<PendingWrite>()
    private var activeWrite: PendingWrite? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val protocolBufferLock = Any()
    private var pendingProtocolBuffer = ByteArray(0)

    private val scanSettings: ScanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setLegacy(false)
            }
        }
        .build()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::handleScanResult)
        }

        override fun onScanFailed(errorCode: Int) {
            _connectionState.update {
                it.copy(
                    status = BleConnectionState.Status.Failed,
                    errorMessage = "Scan failed ($errorCode)",
                    isProtocolReady = false
                )
            }
            stopScan()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    currentGatt = gatt
                    connectedAddress = gatt.device.address
                    _connectionState.update {
                        it.copy(
                            status = BleConnectionState.Status.Connected,
                            deviceName = resolveDeviceName(
                                device = gatt.device,
                                cachedEntry = getCachedDevice(gatt.device.address),
                                fallback = gatt.device.address
                            ),
                            deviceAddress = gatt.device.address,
                            errorMessage = null,
                            isProtocolReady = false
                        )
                    }
                    notifyConnected(gatt.device)
                    emitDeviceSnapshot()
                    boostConnectionPriority(gatt)
                    val mtuRequested = activeAdapter?.let { adapter ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            val requested = gatt.requestMtu(adapter.preferredMtu)
                            awaitingInitialMtu = requested
                            if (requested) {
                                pendingMtuTimeout?.let(mainHandler::removeCallbacks)
                                val timeoutRunnable = Runnable { handleInitialMtuTimeout(gatt) }
                                pendingMtuTimeout = timeoutRunnable
                                mainHandler.postDelayed(timeoutRunnable, INITIAL_MTU_TIMEOUT_MS)
                            }
                            requested
                        } else {
                            false
                        }
                    } ?: false
                    if (!mtuRequested) {
                        awaitingInitialMtu = false
                        queueServiceDiscovery(gatt, INITIAL_SERVICE_DISCOVERY_DELAY_MS, "initial_connect_no_mtu")
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    handleDisconnection(gatt.device, status)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.update {
                    it.copy(
                        status = BleConnectionState.Status.Failed,
                        errorMessage = "Service discovery failed ($status)"
                    )
                }
                notifyConnectionFailed(gatt.device, "Service discovery failed")
                return
            }
            val services = gatt.services
            if (services.isEmpty()) {
                if (scheduleServiceRediscovery(gatt, "empty_service_list")) {
                    Log.i(
                        TAG,
                        "onServicesDiscovered: services empty, scheduling rediscovery attempt=$serviceDiscoveryRetries"
                    )
                    return
                } else {
                    Log.w(TAG, "onServicesDiscovered: services empty after retries exhausted")
                }
            } else {
                serviceDiscoveryRetries = 0
                cancelPendingServiceRetry()
                Log.d(TAG, "onServicesDiscovered: received ${services.size} service(s)")
            }
            val adapter = activeAdapter ?: run {
                Log.w(TAG, "onServicesDiscovered: activeAdapter is null")
                return
            }
            if (!resolveProtocolHandles(gatt, adapter, enableNotifications = true, updateConnectionState = true)) {
                Log.e(
                    TAG,
                    "onServicesDiscovered: unable to resolve protocol handles for ${gatt.device.address}"
                )
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            Log.d(TAG, "onMtuChanged: device=${gatt.device.address} mtu=$mtu status=$status")
            if (!awaitingInitialMtu) return
            awaitingInitialMtu = false
            pendingMtuTimeout?.let(mainHandler::removeCallbacks)
            pendingMtuTimeout = null
            val reason = if (status == BluetoothGatt.GATT_SUCCESS) {
                "initial_connect_mtu_ready"
            } else {
                "initial_connect_mtu_failed"
            }
            queueServiceDiscovery(gatt, INITIAL_SERVICE_DISCOVERY_DELAY_MS, reason)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (descriptor.uuid == CLIENT_CHARACTERISTIC_CONFIG_UUID &&
                pendingNotifyEnableCharacteristicUuid == descriptor.characteristic.uuid
            ) {
                pendingNotifyEnableCharacteristicUuid = null
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    _connectionState.update { it.copy(isProtocolReady = true) }
                } else {
                    Log.w(TAG, "onDescriptorWrite: CCC write failed status=$status")
                    _connectionState.update { it.copy(isProtocolReady = false) }
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val completed = synchronized(writeQueue) {
                if (activeWrite != null && activeWrite?.characteristic == characteristic) {
                    activeWrite
                } else {
                    null
                }
            }
            completed?.completion?.complete(status == BluetoothGatt.GATT_SUCCESS)
            if (completed != null) {
                synchronized(writeQueue) {
                    if (activeWrite === completed) {
                        activeWrite = null
                    }
                }
                advanceWriteQueue()
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleProtocolPayload(value)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            characteristic.value?.let(::handleProtocolPayload)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                characteristic.value?.let(::handleProtocolPayload)
            }
        }
    }

    fun addConnectionListener(listener: BleConnectionListener) {
        listeners.add(listener)
    }

    fun removeConnectionListener(listener: BleConnectionListener) {
        listeners.remove(listener)
    }

    fun clearError() {
        _connectionState.update { it.copy(errorMessage = null) }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (isScanning) return
        val adapter = bluetoothAdapter
        if (adapter == null || adapter.isEnabled.not()) {
            Log.w(TAG, "startScan: bluetooth adapter unavailable or disabled")
            _connectionState.value = BleConnectionState(
                status = BleConnectionState.Status.BluetoothUnavailable,
                errorMessage = context.getString(R.string.device_error_bluetooth_disabled)
            )
            return
        }
        if (!hasScanPermission()) {
            Log.w(TAG, "startScan: missing BLUETOOTH_SCAN permission")
            _connectionState.value = BleConnectionState(
                status = BleConnectionState.Status.BluetoothUnavailable,
                errorMessage = context.getString(R.string.device_error_bluetooth_scan_permission)
            )
            return
        }
        if (!hasLocationPermission()) {
            Log.w(TAG, "startScan: missing location permission")
            _connectionState.value = BleConnectionState(
                status = BleConnectionState.Status.BluetoothUnavailable,
                errorMessage = context.getString(R.string.device_error_location_permission)
            )
            return
        }
        val scanner = adapter.bluetoothLeScanner ?: run {
            Log.w(TAG, "startScan: bluetoothLeScanner is null")
            return
        }
        Log.d(TAG, "startScan: initiating BLE scan with settings=$scanSettings")
        isScanning = true
        synchronized(cachedDevices) { cachedDevices.clear() }
        emitDeviceSnapshot()
        _connectionState.update {
            it.copy(
                status = BleConnectionState.Status.Scanning,
                errorMessage = null,
                isProtocolReady = false
            )
        }
        scanner.startScan(null, scanSettings, scanCallback)
        scheduleScanTimeout(scanner)
    }

    fun restartScan() {
        Log.d(TAG, "restartScan: restarting BLE scan")
        stopScan()
        startScan()
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning) return
        Log.d(TAG, "stopScan: stopping BLE scan")
        bluetoothAdapter?.bluetoothLeScanner?.stopSafeScan(scanCallback)
        isScanning = false
        scanTimeoutJob?.cancel()
        _connectionState.update {
            if (it.status == BleConnectionState.Status.Scanning) {
                it.copy(status = BleConnectionState.Status.Idle, isProtocolReady = false)
            } else {
                it
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String): Boolean {
        val adapter = bluetoothAdapter ?: run {
            Log.w(TAG, "connect: bluetooth adapter is null")
            _connectionState.update {
                it.copy(
                    status = BleConnectionState.Status.BluetoothUnavailable,
                    errorMessage = context.getString(R.string.device_error_bluetooth_disabled)
                )
            }
            return false
        }
        if (!adapter.isEnabled) {
            Log.w(TAG, "connect: bluetooth adapter disabled")
            _connectionState.update {
                it.copy(
                    status = BleConnectionState.Status.BluetoothUnavailable,
                    errorMessage = context.getString(R.string.device_error_bluetooth_disabled)
                )
            }
            return false
        }
        if (!hasConnectPermission()) {
            Log.w(TAG, "connect: missing BLUETOOTH_CONNECT permission for address=$address")
            _connectionState.update {
                it.copy(
                    status = BleConnectionState.Status.BluetoothUnavailable,
                    errorMessage = context.getString(R.string.device_error_bluetooth_permission)
                )
            }
            return false
        }
        val cachedEntry = getCachedDevice(address)
        val device = cachedEntry?.device ?: runCatching {
            adapter.getRemoteDevice(address)
        }.getOrNull() ?: return false

        stopScan()
        cancelPendingServiceRetry()
        serviceDiscoveryRetries = 0
        val resolvedName = resolveDeviceName(device, cachedEntry, device.address)
        _connectionState.update {
            it.copy(
                status = BleConnectionState.Status.Connecting,
                deviceName = resolvedName,
                deviceAddress = device.address,
                errorMessage = null,
                isProtocolReady = false
            )
        }
        disposeGatt()
        val productId = cachedEntry?.productId
        val resolvedAdapter = protocolRegistry.findAdapter(productId) ?: run {
            Log.w(
                TAG,
                "connect: protocol adapter missing for productId=$productId, falling back to default for ${device.address}"
            )
            protocolRegistry.defaultAdapter()
        }
        if (resolvedAdapter == null) {
            Log.e(TAG, "connect: no protocol adapter available, aborting connection to ${device.address}")
            _connectionState.update {
                it.copy(
                    status = BleConnectionState.Status.Failed,
                    errorMessage = "Device not supported"
                )
            }
            return false
        }
        activeAdapter = resolvedAdapter
        activeServiceUuid = null
        activeWriteCharacteristicUuid = null
        activeNotifyCharacteristicUuid = null
        connectedAddress = device.address
        currentGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, gattCallback)
        }
        if (currentGatt == null) {
            _connectionState.update {
                it.copy(
                    status = BleConnectionState.Status.Failed,
                    errorMessage = "Unable to connect"
                )
            }
            connectedAddress = null
            notifyConnectionFailed(device, "Unable to connect")
            return false
        }
        emitDeviceSnapshot()
        return true
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        connectedAddress = null
        activeAdapter = null
        activeServiceUuid = null
        activeWriteCharacteristicUuid = null
        activeNotifyCharacteristicUuid = null
        awaitingInitialMtu = false
        pendingMtuTimeout?.let(mainHandler::removeCallbacks)
        pendingMtuTimeout = null
        pendingNotifyEnableCharacteristicUuid = null
        failPendingWrites()
        currentGatt?.disconnect()
        disposeGatt()
        cancelPendingServiceRetry()
        _connectionState.update {
            it.copy(
                status = BleConnectionState.Status.Disconnected,
                deviceAddress = null,
                deviceName = null
            )
        }
        emitDeviceSnapshot()
    }

    fun shutdown() {
        stopScan()
        disconnect()
        ioScope.cancel()
    }

    @SuppressLint("MissingPermission")
    fun readCharacteristic(serviceUuid: UUID, characteristicUuid: UUID): Boolean {
        val gatt = currentGatt ?: return false
        val characteristic = gatt
            .getService(serviceUuid)
            ?.getCharacteristic(characteristicUuid) ?: return false
        @Suppress("DEPRECATION")
        return gatt.readCharacteristic(characteristic)
    }

    @SuppressLint("MissingPermission")
    suspend fun writeCharacteristic(
        serviceUuid: UUID,
        characteristicUuid: UUID,
        payload: ByteArray,
        writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
    ): Boolean {
        val gatt = currentGatt ?: run {
            Log.w(TAG, "writeCharacteristic: no active GATT connection service=$serviceUuid characteristic=$characteristicUuid")
            return false
        }
        val characteristic = gatt
            .getService(serviceUuid)
            ?.getCharacteristic(characteristicUuid) ?: run {
            Log.w(TAG, "writeCharacteristic: characteristic not found service=$serviceUuid characteristic=$characteristicUuid")
            return false
        }
        val request = PendingWrite(
            gatt = gatt,
            characteristic = characteristic,
            payload = payload.copyOf(),
            writeType = writeType,
            completion = CompletableDeferred()
        )
        enqueueWrite(request)
        return request.completion.await()
    }

    private fun enqueueWrite(request: PendingWrite) {
        val shouldStart = synchronized(writeQueue) {
            if (activeWrite == null) {
                activeWrite = request
                true
            } else {
                writeQueue.addLast(request)
                false
            }
        }
        if (shouldStart) {
            performWrite(request)
        } else if (!currentGattMatches(request.gatt)) {
            synchronized(writeQueue) { writeQueue.remove(request) }
            request.completion.complete(false)
        }
    }

    private fun currentGattMatches(gatt: BluetoothGatt): Boolean = currentGatt == gatt

    @SuppressLint("MissingPermission")
    private fun performWrite(request: PendingWrite) {
        if (!currentGattMatches(request.gatt)) {
            request.completion.complete(false)
            advanceWriteQueue()
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
            advanceWriteQueue()
        }
    }

    private fun advanceWriteQueue() {
        val next = synchronized(writeQueue) {
            if (writeQueue.isNotEmpty()) {
                writeQueue.removeFirst().also { activeWrite = it }
            } else {
                activeWrite = null
                null
            }
        }
        if (next != null) {
            performWrite(next)
        }
    }

    private fun failPendingWrites() {
        val pending = mutableListOf<PendingWrite>()
        synchronized(writeQueue) {
            activeWrite?.let { pending.add(it) }
            activeWrite = null
            while (writeQueue.isNotEmpty()) {
                pending.add(writeQueue.removeFirst())
            }
        }
        pending.forEach { it.completion.complete(false) }
    }

    suspend fun sendProtocolCommand(
        command: ProtocolCommand,
        writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
    ): Boolean {
        val adapter = activeAdapter ?: run {
            Log.w(TAG, "sendProtocolCommand: no active adapter, command=$command")
            return false
        }
        var serviceUuid = activeServiceUuid
        var characteristicUuid = activeWriteCharacteristicUuid
        if (serviceUuid == null || characteristicUuid == null) {
            val gatt = currentGatt
            if (gatt != null) {
                val resolved = resolveProtocolHandles(
                    gatt = gatt,
                    adapter = adapter,
                    enableNotifications = true,
                    updateConnectionState = true
                )
                if (resolved) {
                    serviceUuid = activeServiceUuid
                    characteristicUuid = activeWriteCharacteristicUuid
                }
            }
        }

        val resolvedService = serviceUuid ?: run {
            val scheduled = currentGatt?.let { scheduleServiceRediscovery(it, "missing_service_uuid") } ?: false
            if (scheduled) {
                Log.i(TAG, "sendProtocolCommand: scheduled rediscovery due to missing service for command=$command")
            }
            Log.w(TAG, "sendProtocolCommand: service UUID not resolved after retry, command=$command")
            return false
        }
        val resolvedCharacteristic = characteristicUuid ?: run {
            val scheduled = currentGatt?.let { scheduleServiceRediscovery(it, "missing_characteristic_uuid") } ?: false
            if (scheduled) {
                Log.i(TAG, "sendProtocolCommand: scheduled rediscovery due to missing characteristic for command=$command")
            }
            Log.w(TAG, "sendProtocolCommand: write characteristic UUID not resolved after retry, command=$command")
            return false
        }
        val payload = adapter.encode(command)
        val result = writeCharacteristic(resolvedService, resolvedCharacteristic, payload, writeType)
        if (!result) {
            Log.e(
                TAG,
                "sendProtocolCommand: write failed service=$resolvedService characteristic=$resolvedCharacteristic command=$command length=${payload.size}"
            )
        } else {
            Log.v(
                TAG,
                "sendProtocolCommand: dispatched command=$command via service=$resolvedService characteristic=$resolvedCharacteristic length=${payload.size}"
            )
        }
        return result
    }

    fun isProtocolReady(): Boolean = activeAdapter != null && activeWriteCharacteristicUuid != null

    private fun handleDisconnection(device: BluetoothDevice, status: Int) {
        val failed = status != BluetoothGatt.GATT_SUCCESS
        if (connectedAddress == device.address) {
            connectedAddress = null
        }
        activeAdapter = null
        activeServiceUuid = null
        activeWriteCharacteristicUuid = null
        activeNotifyCharacteristicUuid = null
        awaitingInitialMtu = false
        pendingMtuTimeout?.let(mainHandler::removeCallbacks)
        pendingMtuTimeout = null
        pendingNotifyEnableCharacteristicUuid = null
        failPendingWrites()
        disposeGatt()
        cancelPendingServiceRetry()
        clearPriorityBoost()
        _connectionState.update {
            it.copy(
                status = if (failed) BleConnectionState.Status.Failed else BleConnectionState.Status.Disconnected,
                deviceName = resolveDeviceName(
                    device = device,
                    cachedEntry = getCachedDevice(device.address),
                    fallback = device.address
                ),
                deviceAddress = device.address,
                errorMessage = if (failed) "Disconnected ($status)" else null
            )
        }
        if (failed) {
            notifyConnectionFailed(device, "Disconnected ($status)")
        } else {
            notifyDisconnected(device)
        }
        emitDeviceSnapshot()
    }

    private fun scheduleScanTimeout(scanner: BluetoothLeScanner) {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = ioScope.launch {
            delay(SCAN_TIMEOUT_MS)
            scanner.stopSafeScan(scanCallback)
            isScanning = false
            Log.d(TAG, "scheduleScanTimeout: scan timed out after $SCAN_TIMEOUT_MS ms")
            _connectionState.update {
                if (it.status == BleConnectionState.Status.Scanning) {
                    it.copy(status = BleConnectionState.Status.Idle)
                } else it
            }
        }
    }

    private fun queueServiceDiscovery(
        gatt: BluetoothGatt,
        delayMs: Long,
        reason: String
    ) {
        if (serviceDiscoveryRetries >= MAX_SERVICE_DISCOVERY_RETRIES) {
            Log.w(
                TAG,
                "queueServiceDiscovery: max retries reached, skipping (reason=$reason)"
            )
            return
        }
        cancelPendingServiceRetry()
        val retryRunnable = Runnable {
            if (currentGatt == gatt && connectedAddress == gatt.device.address) {
                serviceDiscoveryRetries += 1
                val started = gatt.discoverServices()
                Log.i(
                    TAG,
                    "queueServiceDiscovery: discoverServices started=$started attempt=$serviceDiscoveryRetries reason=$reason"
                )
                if (!started) {
                    scheduleServiceRediscovery(gatt, "retry_after_failed_start")
                }
            } else {
                Log.v(
                    TAG,
                    "queueServiceDiscovery: skipping, GATT changed or disconnected (reason=$reason)"
                )
            }
        }
        pendingServiceRetry = retryRunnable
        if (delayMs <= 0) {
            mainHandler.post(retryRunnable)
        } else {
            mainHandler.postDelayed(retryRunnable, delayMs)
        }
    }

    private fun scheduleServiceRediscovery(gatt: BluetoothGatt, reason: String): Boolean {
        if (serviceDiscoveryRetries >= MAX_SERVICE_DISCOVERY_RETRIES) {
            Log.w(
                TAG,
                "scheduleServiceRediscovery: max retries ($MAX_SERVICE_DISCOVERY_RETRIES) exhausted for ${gatt.device.address}"
            )
            return false
        }
        queueServiceDiscovery(gatt, SERVICE_DISCOVERY_RETRY_DELAY_MS, reason)
        return true
    }

    private fun cancelPendingServiceRetry() {
        pendingServiceRetry?.let(mainHandler::removeCallbacks)
        pendingServiceRetry = null
    }

    private fun boostConnectionPriority(gatt: BluetoothGatt) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
        runCatching {
            gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
        }
        pendingPriorityReset?.let(mainHandler::removeCallbacks)
        val resetRunnable = Runnable {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                runCatching {
                    gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED)
                }
            }
            pendingPriorityReset = null
        }
        pendingPriorityReset = resetRunnable
        mainHandler.postDelayed(resetRunnable, CONNECTION_PRIORITY_RESET_DELAY_MS)
    }

    private fun clearPriorityBoost() {
        pendingPriorityReset?.let(mainHandler::removeCallbacks)
        pendingPriorityReset = null
    }

    private fun handleInitialMtuTimeout(gatt: BluetoothGatt) {
        if (!awaitingInitialMtu) return
        awaitingInitialMtu = false
        pendingMtuTimeout = null
        queueServiceDiscovery(gatt, INITIAL_SERVICE_DISCOVERY_DELAY_MS, "initial_connect_timeout")
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothLeScanner.stopSafeScan(callback: ScanCallback) = runCatching {
        stopScan(callback)
    }

    private fun disposeGatt() {
        currentGatt?.close()
        currentGatt = null
    }

    private fun getCachedDevice(address: String): CachedScanDevice? =
        synchronized(cachedDevices) { cachedDevices[address] }

    fun cachedDeviceName(address: String?): String? {
        if (address.isNullOrBlank()) return null
        return getCachedDevice(address)?.name?.takeIf { it.isNotBlank() }
    }

    private fun handleScanResult(result: ScanResult) {
        val device = result.device ?: run {
            Log.w(TAG, "handleScanResult: null device in scan result, skipping")
            return
        }
        val scanRecord = result.scanRecord ?: run {
            Log.v(TAG, "handleScanResult: missing scanRecord for ${device.address}, skipping")
            return
        }
        val rawBytes = scanRecord.bytes
        val advertisementPayload = extractHyPayload(rawBytes)
        val advertisement = HyAdvertisement.parse(advertisementPayload)
        if (advertisement == null) {
            Log.v(
                TAG,
                "handleScanResult: advertisement payload did not match EMS header (address=${device.address}), {rawBytes=${rawBytes.toDebugHexString()}}"
            )
            return
        }
        val adapter = protocolRegistry.findAdapter(advertisement.productId)
        if (adapter == null) {
            Log.v(
                TAG,
                "handleScanResult: no adapter registered for productId=${advertisement.productId}, ignoring device ${device.address}"
            )
            return
        }
        val cachedDevice = getCachedDevice(device.address)
        val displayName = buildDisplayName(
            resolveDeviceName(
                device = device,
                cachedEntry = cachedDevice,
                fallback = ""
            ),
            advertisement
        )
        val rawHex = rawBytes?.toDebugHexString()
        Log.d(
            TAG,
            "handleScanResult: device=${device.address}, name=$displayName, rssi=${result.rssi}, " +
                "advertisement=$advertisement, raw=$rawHex"
        )
        val cached = CachedScanDevice(
            device = device,
            name = displayName,
            address = device.address,
            rssi = result.rssi,
            lastSeen = System.currentTimeMillis(),
            productId = advertisement.productId,
            firmwareVersion = advertisement.firmwareVersion,
            uniqueId = advertisement.uniqueId
        )
        synchronized(cachedDevices) {
            cachedDevices[cached.address] = cached
            pruneStaleScanResultsLocked()
        }
        emitDeviceSnapshot()
    }

    private fun extractHyPayload(bytes: ByteArray?): ByteArray? {
        if (bytes == null) {
            Log.v(TAG, "extractHyPayload: raw bytes are null")
            return null
        }
        if (bytes.contentEquals(TEST_FALLBACK_PAYLOAD2)) {
            Log.v(TAG, "extractHyPayload: matched test fallback payload")
            return bytes
        }
        if (bytes.size < 3) {
            Log.v(TAG, "extractHyPayload: raw bytes too short length=${bytes.size}")
            return null
        }
        for (index in 0..bytes.size - 2) {
            if (bytes[index] == HY_HEADER_FIRST && bytes[index + 1] == HY_HEADER_SECOND) {
                val endExclusive = min(bytes.size, index + HY_PAYLOAD_LENGTH)
                Log.v(
                    TAG,
                    "extractHyPayload: found header at index=$index payloadLength=${endExclusive - index}"
                )
                return bytes.copyOfRange(index, endExclusive)
            }
        }
        Log.v(TAG, "extractHyPayload: HY header not found in raw bytes length=${bytes.size}")
        return null
    }

    private fun emitDeviceSnapshot() {
        val snapshot = synchronized(cachedDevices) {
            cachedDevices.values
                .sortedByDescending { it.rssi }
                .map {
                    BleScanResult(
                        name = it.name.ifBlank { it.address },
                        address = it.address,
                        rssi = it.rssi,
                        isConnected = connectedAddress == it.address,
                        productId = it.productId,
                        firmwareVersion = it.firmwareVersion,
                        uniqueId = it.uniqueId
                    )
                }
        }
        Log.d(TAG, "emitDeviceSnapshot: publishing ${snapshot.size} device(s)")
        _scanResults.value = snapshot
    }

    private fun notifyConnected(device: BluetoothDevice) {
        listeners.forEach { it.onConnected(device) }
    }

    private fun handleProtocolPayload(payload: ByteArray) {
        if (payload.isEmpty()) return
        val adapter = activeAdapter ?: return
        val frames = mutableListOf<ByteArray>()
        synchronized(protocolBufferLock) {
        pendingProtocolBuffer = pendingProtocolBuffer + payload
            while (true) {
                val extraction = extractFrame(pendingProtocolBuffer) ?: break
                if (extraction.frame != null) {
                    frames += extraction.frame
                }
                pendingProtocolBuffer = if (extraction.consumed >= pendingProtocolBuffer.size) {
                    ByteArray(0)
                } else {
                    pendingProtocolBuffer.copyOfRange(extraction.consumed, pendingProtocolBuffer.size)
                }
            }
            if (pendingProtocolBuffer.size > 256) {
                Log.w(TAG, "handleProtocolPayload: clearing oversized buffer size=${pendingProtocolBuffer.size}")
                pendingProtocolBuffer = ByteArray(0)
            }
        }
        if (frames.isEmpty()) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastEmptyDecodeLogAt >= EMPTY_DECODE_LOG_THROTTLE_MS) {
                lastEmptyDecodeLogAt = now
                Log.v(TAG, "handleProtocolPayload: awaiting complete frame (buffer=${pendingProtocolBuffer.size})")
            }
            return
        }
        frames.forEach { frame ->
            ioScope.launch {
                val messages = adapter.decode(frame)
                Log.d(TAG, "handleProtocolPayload: frame=${frame.toDebugHexString()} messages =${messages.toString()}")
                if (messages.isEmpty()) {
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastEmptyDecodeLogAt >= EMPTY_DECODE_LOG_THROTTLE_MS) {
                        lastEmptyDecodeLogAt = now
                        Log.v(TAG, "handleProtocolPayload: no decodable messages (length=${frame.size})")
                    }
                    return@launch
                }
                Log.d(TAG, "handleProtocolPayload: decoded ${messages.size} message(s) from device")
                messages.forEach { _protocolMessages.tryEmit(it) }
            }
        }
    }

    private data class FrameExtraction(val frame: ByteArray?, val consumed: Int)

    private fun extractFrame(buffer: ByteArray): FrameExtraction? {
        if (buffer.isEmpty()) return null
        val headerIndex = buffer.indexOfFirstHeader()
        if (headerIndex == -1) {
            return FrameExtraction(frame = null, consumed = buffer.size)
        }
        if (headerIndex > 0) {
            return FrameExtraction(frame = null, consumed = headerIndex)
        }
        if (buffer.size < 4) return null

        val terminatorIndex = buffer.indexOfTerminator(startIndex = EMS_HEADER_LENGTH + EMS_LENGTH_FIELD)
        if (terminatorIndex == -1) {
            return null
        }

        val totalLength = terminatorIndex + EMS_TERMINATOR_LENGTH
        if (buffer.size < totalLength) return null
        val frame = buffer.copyOfRange(0, totalLength)
        return FrameExtraction(frame = frame, consumed = totalLength)
    }

    private fun ByteArray.indexOfFirstHeader(): Int {
        for (i in indices) {
            if (this[i] == 0x68.toByte() && i + 1 < size && this[i + 1] == 0x79.toByte()) {
                return i
            }
        }
        return -1
    }

    private fun ByteArray.indexOfTerminator(startIndex: Int): Int {
        if (size < 2) return -1
        val start = startIndex.coerceAtLeast(0)
        for (i in start until size - 1) {
            if (this[i] == 0x0D.toByte() && this[i + 1] == 0x0A.toByte()) {
                return i
            }
        }
        return -1
    }

    private fun notifyDisconnected(device: BluetoothDevice) {
        listeners.forEach { it.onDisconnected(device) }
    }

    private fun notifyConnectionFailed(device: BluetoothDevice?, reason: String?) {
        listeners.forEach { it.onConnectionFailed(device, reason) }
    }

    private data class GattEndpoint(
        val serviceUuid: UUID,
        val writeCharacteristic: BluetoothGattCharacteristic,
        val notifyCharacteristic: BluetoothGattCharacteristic?
    )

    private fun BluetoothGattCharacteristic.hasWriteProperty(): Boolean {
        val writeMask = BluetoothGattCharacteristic.PROPERTY_WRITE or
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
        return properties and writeMask != 0
    }

    private fun BluetoothGattCharacteristic.hasNotifyProperty(): Boolean {
        val notifyMask = BluetoothGattCharacteristic.PROPERTY_NOTIFY or
            BluetoothGattCharacteristic.PROPERTY_INDICATE
        return properties and notifyMask != 0
    }

    private fun BluetoothGatt.findWritableEndpoint(): GattEndpoint? {
        services.forEach { service ->
            val writeCharacteristic = service.characteristics.firstOrNull { it.hasWriteProperty() }
            if (writeCharacteristic != null) {
                val notifyCharacteristic = service.characteristics.firstOrNull { it.hasNotifyProperty() }
                Log.d(
                    TAG,
                    "findWritableEndpoint: candidate service=${service.uuid} write=${writeCharacteristic.uuid} notify=${notifyCharacteristic?.uuid} " +
                        "writeProps=0x${writeCharacteristic.properties.toString(16)} notifyProps=0x${notifyCharacteristic?.properties?.toString(16)}"
                )
                return GattEndpoint(
                    serviceUuid = service.uuid,
                    writeCharacteristic = writeCharacteristic,
                    notifyCharacteristic = notifyCharacteristic
                )
            }
        }
        Log.v(TAG, "findWritableEndpoint: no writable characteristic found across ${services.size} service(s)")
        return null
    }

    private fun BluetoothGatt.findCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
        services.forEach { service ->
            service.getCharacteristic(uuid)?.let { return it }
        }
        return null
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ): Boolean {
        val enabled = gatt.setCharacteristicNotification(characteristic, true)
        if (!enabled) {
            Log.w(TAG, "enableNotifications: failed to enable notification for ${characteristic.uuid}")
            return false
        }
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
        if (descriptor != null) {
            val writeSuccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(
                    descriptor,
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
            if (writeSuccess) {
                pendingNotifyEnableCharacteristicUuid = characteristic.uuid
                return false
            } else {
                Log.w(TAG, "enableNotifications: failed to write CCC descriptor for ${characteristic.uuid}")
            }
        } else {
            Log.v(TAG, "enableNotifications: CCC descriptor missing for ${characteristic.uuid}")
        }
        return true
    }

    private fun resolveProtocolHandles(
        gatt: BluetoothGatt,
        adapter: BleProtocolAdapter,
        enableNotifications: Boolean = false,
        updateConnectionState: Boolean = false
    ): Boolean {
        val availableServices = gatt.services
        if (availableServices.isEmpty()) {
            Log.w(TAG, "resolveProtocolHandles: GATT services empty, attempting rediscovery")
            if (scheduleServiceRediscovery(gatt, "resolve_handles_empty")) {
                return false
            }
        }
        val serviceEntry = adapter.serviceUuidCandidates
            .asSequence()
            .mapNotNull { candidate ->
                gatt.getService(candidate)?.let { candidate to it }
            }
            .firstOrNull()
            ?: run {
                val allServiceUuids = gatt.services.map { it.uuid }
                Log.d(
                    TAG,
                    "resolveProtocolHandles: adapter=${adapter.productId} discoveredServices=${allServiceUuids.joinToString()}"
                )
                val remappedAdapter = protocolRegistry.findAdapterByServices(allServiceUuids)
                if (remappedAdapter != null && remappedAdapter != adapter) {
                    Log.i(
                        TAG,
                        "resolveProtocolHandles: remapping adapter from ${adapter.productId} to ${remappedAdapter.productId} " +
                            "based on GATT services=$allServiceUuids"
                    )
                    activeAdapter = remappedAdapter
                    return resolveProtocolHandles(
                        gatt = gatt,
                        adapter = remappedAdapter,
                        enableNotifications = enableNotifications,
                        updateConnectionState = updateConnectionState
                    )
                }
                val fallbackEndpoint = gatt.findWritableEndpoint()
                if (fallbackEndpoint != null) {
                    Log.i(
                        TAG,
                        "resolveProtocolHandles: using fallback writable endpoint service=${fallbackEndpoint.serviceUuid} " +
                            "write=${fallbackEndpoint.writeCharacteristic.uuid} notify=${fallbackEndpoint.notifyCharacteristic?.uuid}"
                    )
                    activeServiceUuid = fallbackEndpoint.serviceUuid
                    activeWriteCharacteristicUuid = fallbackEndpoint.writeCharacteristic.uuid
                    activeNotifyCharacteristicUuid = fallbackEndpoint.notifyCharacteristic?.uuid
                    val notifyReady = if (enableNotifications && fallbackEndpoint.notifyCharacteristic != null) {
                        enableNotifications(gatt, fallbackEndpoint.notifyCharacteristic)
                    } else {
                        true
                    }
                    if (updateConnectionState && notifyReady) {
                        _connectionState.update { it.copy(isProtocolReady = true) }
                    }
                    return true
                }
                Log.w(
                    TAG,
                    "resolveProtocolHandles: no fallback endpoint located for services=${allServiceUuids.joinToString()}"
                )
                Log.w(
                    TAG,
                    "resolveProtocolHandles: no matching service for adapter=${adapter.productId}. Available services will be dumped."
                )
                dumpGattServices(gatt)
                if (updateConnectionState) {
                    _connectionState.update { it.copy(isProtocolReady = false) }
                }
                return false
            }

        val (serviceUuid, service) = serviceEntry
        val writeCharacteristic = adapter.writeCharacteristicUuidCandidates
            .asSequence()
            .mapNotNull { candidate ->
                service.getCharacteristic(candidate) ?: gatt.findCharacteristic(candidate)
            }
            .firstOrNull()
            ?: run {
                Log.w(
                    TAG,
                    "resolveProtocolHandles: no matching write characteristic for adapter=${adapter.productId}. Dumping characteristics."
                )
                dumpGattServices(gatt)
                if (updateConnectionState) {
                    _connectionState.update { it.copy(isProtocolReady = false) }
                }
                return false
            }

        val notifyCharacteristic = adapter.notifyCharacteristicUuidCandidates
            .asSequence()
            .mapNotNull { candidate ->
                service.getCharacteristic(candidate) ?: gatt.findCharacteristic(candidate)
            }
            .firstOrNull()

        activeServiceUuid = serviceUuid
        activeWriteCharacteristicUuid = writeCharacteristic.uuid
        activeNotifyCharacteristicUuid = notifyCharacteristic?.uuid

        val notificationsReady = if (enableNotifications && notifyCharacteristic != null) {
            enableNotifications(gatt, notifyCharacteristic)
        } else {
            true
        }

        if (updateConnectionState && notificationsReady) {
            _connectionState.update { it.copy(isProtocolReady = true) }
        }

        Log.d(
            TAG,
            "resolveProtocolHandles: resolved service=$activeServiceUuid write=$activeWriteCharacteristicUuid notify=$activeNotifyCharacteristicUuid"
        )
        return true
    }

    private fun dumpGattServices(gatt: BluetoothGatt) {
        gatt.services.forEach { service ->
            Log.d(TAG, "GATT service=${service.uuid}")
            service.characteristics.forEach { characteristic ->
                Log.d(
                    TAG,
                    "  characteristic=${characteristic.uuid} properties=${characteristic.properties}"
                )
            }
        }
    }

    private data class CachedScanDevice(
        val device: BluetoothDevice,
        val name: String,
        val address: String,
        val rssi: Int,
        val lastSeen: Long,
        val productId: Int?,
        val firmwareVersion: String?,
        val uniqueId: String?
    )

    private data class PendingWrite(
        val gatt: BluetoothGatt,
        val characteristic: BluetoothGattCharacteristic,
        val payload: ByteArray,
        val writeType: Int,
        val completion: CompletableDeferred<Boolean>
    )

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun hasConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    private fun pruneStaleScanResultsLocked() {
        val threshold = System.currentTimeMillis() - SCAN_RESULT_TTL_MS
        val iterator = cachedDevices.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.lastSeen < threshold) {
                Log.v(TAG, "pruneStaleScanResultsLocked: removing stale device ${entry.key}")
                iterator.remove()
            }
        }
    }

    private fun ByteArray.toDebugHexString(limit: Int = 32): String {
        val actualLimit = min(size, limit)
        val builder = StringBuilder(actualLimit * 3)
        for (index in 0 until actualLimit) {
            if (index > 0) builder.append(' ')
            builder.append(String.format("%02X", this[index]))
        }
        if (size > limit) {
            builder.append(" …(${size} bytes)")
        }
        return builder.toString()
    }

    private fun buildDisplayName(rawName: String?, advertisement: HyAdvertisement): String {
        val sanitized = rawName?.takeIf { it.isNotBlank() }
        if (!sanitized.isNullOrBlank()) return sanitized
        val version = advertisement.firmwareVersion.takeUnless { it.equals("unknown", ignoreCase = true) }
        val suffix = version?.let { " v$it" }.orEmpty()
        return "EMS-${advertisement.productId}$suffix"
    }

    private fun resolveDeviceName(
        device: BluetoothDevice?,
        cachedEntry: CachedScanDevice? = null,
        fallback: String? = null
    ): String? {
        cachedEntry?.name?.takeIf { it.isNotBlank() }?.let { return it }
        if (device != null && hasConnectPermission()) {
            val candidate = device.name
            if (!candidate.isNullOrBlank()) return candidate
        }
        return fallback ?: cachedEntry?.address ?: device?.address
    }

    companion object {
        /**
         *
         * 特殊处理逻辑
         */
        private val TEST_FALLBACK_PAYLOAD = byteArrayOf(
            0x02,
            0x01,
            0x06,
            0x03,
            0x03,
            0xE0.toByte(),
            0xFF.toByte(),
            0x06,
            0x09,
            0x58,
            0x4C,
            0x42,
            0x4C,
            0x45
        )
        private val TEST_FALLBACK_PAYLOAD2 = byteArrayOf(
            0x08,
            0x09,
            0x42,
            0x4C.toByte(),
            0x45,
            0x5F.toByte(),
            0x45,
            0x4D.toByte(),
            0x53
        )
    }
}
