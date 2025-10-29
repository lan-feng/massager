package com.massager.app.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SCAN_TIMEOUT_MS = 15_000L

data class BleScanResult(
    val name: String,
    val address: String,
    val rssi: Int,
    val isConnected: Boolean
)

data class BleConnectionState(
    val status: Status = Status.Idle,
    val deviceName: String? = null,
    val deviceAddress: String? = null,
    val errorMessage: String? = null
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
    private val bluetoothAdapter: BluetoothAdapter?
) {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listeners = CopyOnWriteArraySet<BleConnectionListener>()
    private val cachedDevices = mutableMapOf<String, CachedScanDevice>()

    private val _scanResults = MutableStateFlow<List<BleScanResult>>(emptyList())
    val scanResults: StateFlow<List<BleScanResult>> = _scanResults.asStateFlow()

    private val _connectionState = MutableStateFlow(BleConnectionState())
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    @Volatile
    private var connectedAddress: String? = null

    private var scanTimeoutJob: Job? = null
    private var currentGatt: BluetoothGatt? = null
    private var isScanning = false

    private val targetKeywords = listOf("massager", "smartpulse")

    private val scanSettings: ScanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
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
                    errorMessage = "Scan failed ($errorCode)"
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
                            deviceName = gatt.device.name,
                            deviceAddress = gatt.device.address,
                            errorMessage = null
                        )
                    }
                    notifyConnected(gatt.device)
                    emitDeviceSnapshot()
                    gatt.discoverServices() // prepare for GATT ops
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
            _connectionState.value = BleConnectionState(
                status = BleConnectionState.Status.BluetoothUnavailable,
                errorMessage = "Bluetooth is disabled"
            )
            return
        }
        val scanner = adapter.bluetoothLeScanner ?: return
        isScanning = true
        synchronized(cachedDevices) { cachedDevices.clear() }
        emitDeviceSnapshot()
        _connectionState.update {
            it.copy(
                status = BleConnectionState.Status.Scanning,
                errorMessage = null
            )
        }
        scanner.startScan(null, scanSettings, scanCallback)
        scheduleScanTimeout(scanner)
    }

    fun restartScan() {
        stopScan()
        startScan()
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning) return
        bluetoothAdapter?.bluetoothLeScanner?.stopSafeScan(scanCallback)
        isScanning = false
        scanTimeoutJob?.cancel()
        _connectionState.update {
            if (it.status == BleConnectionState.Status.Scanning) {
                it.copy(status = BleConnectionState.Status.Idle)
            } else {
                it
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String): Boolean {
        val adapter = bluetoothAdapter ?: return false
        val device = getCachedDevice(address)?.device ?: runCatching {
            adapter.getRemoteDevice(address)
        }.getOrNull() ?: return false

        stopScan()
        _connectionState.update {
            it.copy(
                status = BleConnectionState.Status.Connecting,
                deviceName = device.name,
                deviceAddress = device.address,
                errorMessage = null
            )
        }
        disposeGatt()
        connectedAddress = device.address
        currentGatt = device.connectGatt(context, false, gattCallback)
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
        currentGatt?.disconnect()
        disposeGatt()
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
    fun writeCharacteristic(
        serviceUuid: UUID,
        characteristicUuid: UUID,
        payload: ByteArray,
        writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
    ): Boolean {
        val gatt = currentGatt ?: return false
        val characteristic = gatt
            .getService(serviceUuid)
            ?.getCharacteristic(characteristicUuid) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, payload, writeType) == BluetoothStatusCodes.SUCCESS
        } else {
            characteristic.value = payload
            characteristic.writeType = writeType
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }
    }

    private fun handleDisconnection(device: BluetoothDevice, status: Int) {
        val failed = status != BluetoothGatt.GATT_SUCCESS
        if (connectedAddress == device.address) {
            connectedAddress = null
        }
        disposeGatt()
        _connectionState.update {
            it.copy(
                status = if (failed) BleConnectionState.Status.Failed else BleConnectionState.Status.Disconnected,
                deviceName = device.name,
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
            _connectionState.update {
                if (it.status == BleConnectionState.Status.Scanning) {
                    it.copy(status = BleConnectionState.Status.Idle)
                } else it
            }
        }
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

    private fun handleScanResult(result: ScanResult) {
        val name = result.device?.name ?: result.scanRecord?.deviceName
        if (!name.isMassagerDevice()) return
        val device = result.device ?: return
        val cached = CachedScanDevice(
            device = device,
            name = name.orEmpty(),
            address = device.address,
            rssi = result.rssi,
            lastSeen = System.currentTimeMillis()
        )
        synchronized(cachedDevices) {
            cachedDevices[cached.address] = cached
        }
        emitDeviceSnapshot()
    }

    private fun String?.isMassagerDevice(): Boolean {
        val value = this?.lowercase(Locale.US)?.trim().orEmpty()
        if (value.isEmpty()) return false
        return targetKeywords.any { value.contains(it) }
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
                        isConnected = connectedAddress == it.address
                    )
                }
        }
        _scanResults.value = snapshot
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

    private data class CachedScanDevice(
        val device: BluetoothDevice,
        val name: String,
        val address: String,
        val rssi: Int,
        val lastSeen: Long
    )
}
