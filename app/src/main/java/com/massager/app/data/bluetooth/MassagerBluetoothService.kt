package com.massager.app.data.bluetooth

// 文件说明：封装蓝牙扫描、连接与指令发送的核心服务。
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
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.massager.app.data.bluetooth.protocol.BleProtocolAdapter
import com.massager.app.data.bluetooth.protocol.EmsV2ProtocolAdapter
import com.massager.app.data.bluetooth.protocol.ProtocolCommand
import com.massager.app.data.bluetooth.protocol.ProtocolMessage
import com.massager.app.data.bluetooth.protocol.ProtocolRegistry
import com.massager.app.data.bluetooth.scan.BleScanCoordinator
import com.massager.app.data.bluetooth.scan.BleScanCoordinator.CachedScanDevice
import com.massager.app.data.bluetooth.session.EmsFrameExtractor
import com.massager.app.data.bluetooth.session.ProtocolPayloadBuffer
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val INITIAL_SERVICE_DISCOVERY_DELAY_MS = 200L
private const val MAX_SERVICE_DISCOVERY_RETRIES = 3
private const val SERVICE_DISCOVERY_RETRY_DELAY_MS = 600L
private const val CONNECTION_PRIORITY_RESET_DELAY_MS = 7_000L
private const val EMPTY_DECODE_LOG_THROTTLE_MS = 1_000L
private const val INITIAL_MTU_TIMEOUT_MS = 2_000L
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
    private val protocolRegistry: ProtocolRegistry,
    private val scanCoordinator: BleScanCoordinator
) {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastEmptyDecodeLogAt = 0L
    private val listeners = CopyOnWriteArraySet<BleConnectionListener>()
    private var payloadBuffer: ProtocolPayloadBuffer = ProtocolPayloadBuffer(EmsFrameExtractor())

    private val scanFailureListener = object : BleScanCoordinator.ScanFailureListener {
        override fun onScanFailure(errorCode: Int) {
            _connectionState.update {
                it.copy(
                    status = BleConnectionState.Status.Failed,
                    errorMessage = "Scan failed ($errorCode)",
                    isProtocolReady = false
                )
            }
        }
    }

    val scanResults: StateFlow<List<BleScanResult>> = scanCoordinator.scanResults

    private val _connectionState = MutableStateFlow(BleConnectionState())
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _protocolMessages = MutableSharedFlow<ProtocolMessage>(extraBufferCapacity = 32)
    val protocolMessages: SharedFlow<ProtocolMessage> = _protocolMessages.asSharedFlow()

    init {
        scanCoordinator.addFailureListener(scanFailureListener)
    }

    private fun resetPayloadBuffer(adapter: BleProtocolAdapter?) {
        payloadBuffer = ProtocolPayloadBuffer(
            when (adapter?.protocolKey) {
                EmsV2ProtocolAdapter.PROTOCOL_KEY -> EmsFrameExtractor()
                else -> EmsFrameExtractor()
            }
        )
    }

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

    private var currentGatt: BluetoothGatt? = null
    private var serviceDiscoveryRetries = 0
    private var pendingServiceRetry: Runnable? = null
    private var pendingPriorityReset: Runnable? = null
    private var pendingMtuTimeout: Runnable? = null
    private var awaitingInitialMtu = false
    private var pendingNotifyEnableCharacteristicUuid: UUID? = null
    private val writeQueue = ArrayDeque<PendingWrite>()
    private var activeWrite: PendingWrite? = null
    private val mainHandler = Handler(Looper.getMainLooper())

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
                                cachedEntry = scanCoordinator.getCachedDevice(gatt.device.address),
                                fallback = gatt.device.address
                            ),
                            deviceAddress = gatt.device.address,
                            errorMessage = null,
                            isProtocolReady = false
                        )
                    }
                    notifyConnected(gatt.device)
                    boostConnectionPriority(gatt)
                    val mtuRequested = activeAdapter?.let { adapter ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            val requested = if (hasConnectPermission()) {
                                try {
                                    gatt.requestMtu(adapter.preferredMtu)
                                } catch (securityException: SecurityException) {
                                    Log.w(TAG, "onConnectionStateChange: requestMtu denied", securityException)
                                    false
                                }
                            } else {
                                Log.w(TAG, "onConnectionStateChange: missing BLUETOOTH_CONNECT permission for requestMtu")
                                false
                            }
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
            Log.d(TAG, "onCharacteristicChanged: mac=${gatt.device.address} value=${value.toDebugHexString()}")
            handleProtocolPayload(value)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.d(TAG, "onCharacteristicChanged: mac=${gatt.device.address}")
            characteristic.value?.let(::handleProtocolPayload)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onCharacteristicRead: mac=${gatt.device.address}")
                characteristic.value?.let(::handleProtocolPayload)
            }
        }
    }

    // 监听器注册：用于上报连接、断开、失败等事件。
    fun addConnectionListener(listener: BleConnectionListener) {
        listeners.add(listener)
    }

    // 监听器移除。
    fun removeConnectionListener(listener: BleConnectionListener) {
        listeners.remove(listener)
    }

    // 清空当前错误消息，便于重新展示 UI 状态。
    fun clearError() {
        _connectionState.update { it.copy(errorMessage = null) }
    }

    @SuppressLint("MissingPermission")
    // 启动扫描：协调器开始搜索设备并更新连接状态。
    fun startScan() {
        when (val result = scanCoordinator.startScan()) {
            is BleScanCoordinator.ScanStartResult.Started -> {
                _connectionState.update {
                    it.copy(
                        status = BleConnectionState.Status.Scanning,
                        errorMessage = null,
                        isProtocolReady = false
                    )
                }
            }
            is BleScanCoordinator.ScanStartResult.Error -> {
                _connectionState.value = BleConnectionState(
                    status = result.status,
                    errorMessage = result.message
                )
            }
        }
    }

    // 重启扫描：清理缓存后重新开始，出错时回退为普通扫描。
    fun restartScan() {
        Log.d(TAG, "restartScan: restarting BLE scan")
        scanCoordinator.clearCache()
        when (scanCoordinator.restartScan()) {
            is BleScanCoordinator.ScanStartResult.Started -> _connectionState.update {
                it.copy(
                    status = BleConnectionState.Status.Scanning,
                    errorMessage = null,
                    isProtocolReady = false
                )
            }
            is BleScanCoordinator.ScanStartResult.Error -> startScan()
        }
    }

    @SuppressLint("MissingPermission")
    // 停止扫描：终止扫描任务并将状态恢复空闲。
    fun stopScan() {
        Log.d(TAG, "stopScan: stopping BLE scan")
        scanCoordinator.stopScan()
        _connectionState.update {
            if (it.status == BleConnectionState.Status.Scanning) {
                it.copy(status = BleConnectionState.Status.Idle, isProtocolReady = false)
            } else {
                it
            }
        }
    }

    @SuppressLint("MissingPermission")
    // 发起连接：根据扫描缓存或地址获取设备，选择协议适配器并建立 GATT 连接。
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
        val cachedEntry = scanCoordinator.getCachedDevice(address)
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
        resetPayloadBuffer(resolvedAdapter)
        activeServiceUuid = null
        activeWriteCharacteristicUuid = null
        activeNotifyCharacteristicUuid = null
        connectedAddress = device.address
        scanCoordinator.updateConnectedAddress(connectedAddress)
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
            activeAdapter = null
            resetPayloadBuffer(null)
            scanCoordinator.updateConnectedAddress(null)
            notifyConnectionFailed(device, "Unable to connect")
            return false
        }
        return true
    }

    @SuppressLint("MissingPermission")
    // 主动断开：清理会话状态、重置适配器并关闭 GATT。
    fun disconnect() {
        connectedAddress = null
        activeAdapter = null
        resetPayloadBuffer(null)
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
        scanCoordinator.updateConnectedAddress(null)
    }

    // 关闭服务：停止扫描、断开连接并取消协程。
    fun shutdown() {
        stopScan()
        disconnect()
        ioScope.cancel()
        scanCoordinator.removeFailureListener(scanFailureListener)
    }

    @SuppressLint("MissingPermission")
    // 读取特征：按给定 Service/Characteristic UUID 发起一次 GATT 读取。
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

    // 入队写请求：若当前无写操作则立即执行，否则排队。
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
    // 执行写入：根据系统版本调用对应 API 并在失败时推进队列。
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

    // 发送协议命令：若缺少句柄则尝试重新解析，编码后写入当前特征。
    suspend fun sendProtocolCommand(
        command: ProtocolCommand,
        writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
    ): Boolean {
        val adapter = activeAdapter ?: run {
            Log.w(TAG, "sendProtocolCommand: no active adapter, command=$command")
            return false
        }
        Log.d(TAG, "sendProtocolCommand: preparing command=$command writeType=$writeType")
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
        Log.d(
            TAG,
            "sendProtocolCommand: encoded payload=${payload.toDebugHexString()} " +
                "service=$resolvedService characteristic=$resolvedCharacteristic"
        )
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

    // 协议就绪判断：已解析到写特征则认为可发指令。
    fun isProtocolReady(): Boolean = activeAdapter != null && activeWriteCharacteristicUuid != null

    // 当前协议标识。
    fun activeProtocolKey(): String? = activeAdapter?.protocolKey

    // 处理断开：重置会话、清理队列并更新 UI 状态，同时通知监听器。
    private fun handleDisconnection(device: BluetoothDevice, status: Int) {
        val failed = status != BluetoothGatt.GATT_SUCCESS
        if (connectedAddress == device.address) {
            connectedAddress = null
            scanCoordinator.updateConnectedAddress(null)
        }
        activeAdapter = null
        resetPayloadBuffer(null)
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
                    cachedEntry = scanCoordinator.getCachedDevice(device.address),
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
    }

    // 安排服务发现：按延迟重试 discoverServices，记录原因避免过度重试。
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
                val started = if (hasConnectPermission()) {
                    try {
                        gatt.discoverServices()
                    } catch (securityException: SecurityException) {
                        Log.w(TAG, "queueServiceDiscovery: discoverServices denied", securityException)
                        false
                    }
                } else {
                    Log.w(TAG, "queueServiceDiscovery: missing BLUETOOTH_CONNECT permission")
                    false
                }
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

    // 临时提升连接优先级：初连阶段请求高优先级，稍后恢复平衡模式。
    private fun boostConnectionPriority(gatt: BluetoothGatt) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
        if (hasConnectPermission()) {
            try {
                gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
            } catch (securityException: SecurityException) {
                Log.w(TAG, "boostConnectionPriority: high priority request denied", securityException)
            }
        } else {
            Log.w(TAG, "boostConnectionPriority: missing BLUETOOTH_CONNECT permission")
        }
        pendingPriorityReset?.let(mainHandler::removeCallbacks)
        val resetRunnable = Runnable {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (hasConnectPermission()) {
                    try {
                        gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED)
                    } catch (securityException: SecurityException) {
                        Log.w(TAG, "boostConnectionPriority: reset priority denied", securityException)
                    }
                } else {
                    Log.w(TAG, "boostConnectionPriority: missing permission for reset")
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

    private fun disposeGatt() {
        val gatt = currentGatt
        currentGatt = null
        if (gatt == null) return
        if (!hasConnectPermission()) {
            Log.w(TAG, "disposeGatt: missing BLUETOOTH_CONNECT permission")
            return
        }
        try {
            gatt.close()
        } catch (securityException: SecurityException) {
            Log.w(TAG, "disposeGatt: failed to close GATT", securityException)
        }
    }

    fun cachedDeviceName(address: String?): String? {
        return scanCoordinator.cachedDeviceName(address)
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

    // 处理协议负载：累积分片、解码帧并向上游分发协议消息。
    private fun handleProtocolPayload(payload: ByteArray) {
        if (payload.isEmpty()) return
        val adapter = activeAdapter ?: return
        Log.d(
            TAG,
            "handleProtocolPayload: incoming raw=${payload.toDebugHexString()} length=${payload.size}"
        )
        val frames = payloadBuffer.append(payload)
        if (frames.isEmpty()) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastEmptyDecodeLogAt >= EMPTY_DECODE_LOG_THROTTLE_MS) {
                lastEmptyDecodeLogAt = now
                Log.v(TAG, "handleProtocolPayload: buffered payload length=${payload.size}")
            }
            return
        }
        ioScope.launch {
            frames.forEachIndexed { index, frame ->
                Log.d(
                    TAG,
                    "handleProtocolPayload: decoding frame#$index hex=${frame.toDebugHexString()} length=${frame.size}"
                )
                val messages = adapter.decode(frame)
                if (messages.isEmpty()) {
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastEmptyDecodeLogAt >= EMPTY_DECODE_LOG_THROTTLE_MS) {
                        lastEmptyDecodeLogAt = now
                        Log.v(TAG, "handleProtocolPayload: no decodable messages (length=${frame.size})")
                    }
                    return@forEachIndexed
                }
                Log.d(TAG, "handleProtocolPayload: decoded ${messages.toString()} message(s)")
                messages.forEach { _protocolMessages.tryEmit(it) }
            }
        }
    }

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

    // 解析协议句柄：匹配服务与读写/通知特征，必要时重映射协议并更新状态。
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

    private data class PendingWrite(
        val gatt: BluetoothGatt,
        val characteristic: BluetoothGattCharacteristic,
        val payload: ByteArray,
        val writeType: Int,
        val completion: CompletableDeferred<Boolean>
    )

    private fun hasConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
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


    private fun resolveDeviceName(
        device: BluetoothDevice?,
        cachedEntry: CachedScanDevice? = null,
        fallback: String? = null
    ): String? {
        cachedEntry?.name?.takeIf { it.isNotBlank() }?.let { return it }
        if (device != null && hasConnectPermission()) {
            val candidate = try {
                device.name
            } catch (securityException: SecurityException) {
                Log.w(TAG, "resolveDeviceName: unable to read device.name", securityException)
                null
            }
            if (!candidate.isNullOrBlank()) return candidate
        }
        return fallback ?: cachedEntry?.address ?: device?.address
    }

    private data class GattEndpoint(
        val serviceUuid: UUID,
        val writeCharacteristic: BluetoothGattCharacteristic,
        val notifyCharacteristic: BluetoothGattCharacteristic?
    )

}


