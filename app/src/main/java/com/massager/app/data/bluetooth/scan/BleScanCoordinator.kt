package com.massager.app.data.bluetooth.scan

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.massager.app.R
import com.massager.app.core.logging.logTag
import com.massager.app.data.bluetooth.BleConnectionState
import com.massager.app.data.bluetooth.BleScanResult
import com.massager.app.data.bluetooth.advertisement.AdvertisementDecoder
import com.massager.app.data.bluetooth.advertisement.ProtocolAdvertisement
import com.massager.app.data.bluetooth.protocol.ProtocolRegistry
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.jvm.Volatile
import kotlin.math.min
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class BleScanCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?,
    private val protocolRegistry: ProtocolRegistry,
    private val decoders: Set<@JvmSuppressWildcards AdvertisementDecoder>
) {

    private val listeners = CopyOnWriteArraySet<ScanFailureListener>()
    private val cachedDevices = mutableMapOf<String, CachedScanDevice>()
    @Volatile
    private var connectedAddress: String? = null
    private val _scanResults = MutableStateFlow<List<BleScanResult>>(emptyList())
    val scanResults: StateFlow<List<BleScanResult>> = _scanResults.asStateFlow()

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

    private val mainHandler = Handler(Looper.getMainLooper())
    private var scanTimeout: Runnable? = null
    private var isScanning = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::handleScanResult)
        }

        override fun onScanFailed(errorCode: Int) {
            listeners.forEach { it.onScanFailure(errorCode) }
            stopScan()
        }
    }

    fun addFailureListener(listener: ScanFailureListener) {
        listeners.add(listener)
    }

    fun removeFailureListener(listener: ScanFailureListener) {
        listeners.remove(listener)
    }

    fun startScan(): ScanStartResult {
        if (isScanning) return ScanStartResult.Started
        val adapter = bluetoothAdapter ?: return ScanStartResult.Error(
            status = BleConnectionState.Status.BluetoothUnavailable,
            message = context.getString(R.string.device_error_bluetooth_disabled)
        )
        if (!adapter.isEnabled) {
            return ScanStartResult.Error(
                status = BleConnectionState.Status.BluetoothUnavailable,
                message = context.getString(R.string.device_error_bluetooth_disabled)
            )
        }
        if (!hasScanPermission()) {
            return ScanStartResult.Error(
                status = BleConnectionState.Status.BluetoothUnavailable,
                message = context.getString(R.string.device_error_bluetooth_scan_permission)
            )
        }
        if (!hasLocationPermission()) {
            return ScanStartResult.Error(
                status = BleConnectionState.Status.BluetoothUnavailable,
                message = context.getString(R.string.device_error_location_permission)
            )
        }
        val scanner = adapter.bluetoothLeScanner ?: return ScanStartResult.Error(
            status = BleConnectionState.Status.BluetoothUnavailable,
            message = context.getString(R.string.device_error_bluetooth_disabled)
        )
        synchronized(cachedDevices) { cachedDevices.clear() }
        emitDeviceSnapshot()
        isScanning = true
        scanner.startScan(null, scanSettings, scanCallback)
        scheduleScanTimeout(scanner)
        return ScanStartResult.Started
    }

    fun restartScan(): ScanStartResult {
        stopScan()
        return startScan()
    }

    fun stopScan() {
        if (!isScanning) return
        bluetoothAdapter?.bluetoothLeScanner?.stopSafeScan(scanCallback)
        isScanning = false
        scanTimeout?.let(mainHandler::removeCallbacks)
        scanTimeout = null
    }

    fun getCachedDevice(address: String): CachedScanDevice? =
        synchronized(cachedDevices) { cachedDevices[address] }

    fun cachedDeviceName(address: String?): String? {
        if (address.isNullOrBlank()) return null
        return getCachedDevice(address)?.name?.takeIf { it.isNotBlank() }
    }

    private fun handleScanResult(result: ScanResult) {
        val device = result.device ?: return
        val advertisement = identifyAdvertisement(result)
        if (advertisement == null) {
            Log.v(TAG, "handleScanResult: advertisement not recognized for ${device.address}")
            return
        }
        val adapter = protocolRegistry.resolveAdapter(advertisement)
        if (adapter == null) {
            Log.v(TAG, "handleScanResult: adapter missing for productId=${advertisement.productId}")
            return
        }
        val cachedDevice = CachedScanDevice(
            device = device,
            name = buildDisplayName(device.name.orEmpty(), advertisement),
            address = device.address,
            rssi = result.rssi,
            lastSeen = System.currentTimeMillis(),
            productId = advertisement.productId,
            firmwareVersion = advertisement.firmwareVersion,
            uniqueId = advertisement.uniqueId,
            protocolKey = adapter.protocolKey
        )
        synchronized(cachedDevices) {
            cachedDevices[cachedDevice.address] = cachedDevice
            pruneStaleScanResultsLocked()
        }
        emitDeviceSnapshot()
    }

    private fun identifyAdvertisement(result: ScanResult): ProtocolAdvertisement? {
        decoders.forEach { decoder ->
            val metadata = runCatching { decoder.decode(result) }.getOrNull()
            if (metadata != null) {
                return metadata
            }
        }
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
        _scanResults.value = snapshot
    }

    private fun buildDisplayName(rawName: String?, advertisement: ProtocolAdvertisement): String {
        val parts = mutableListOf<String>()
        if (!rawName.isNullOrBlank()) {
            parts += rawName
        }
        val firmware = advertisement.firmwareVersion?.takeIf { it.isNotBlank() }
        if (firmware != null) {
            parts += "fw:$firmware"
        }
        advertisement.uniqueId?.takeIf { it.isNotBlank() }?.let {
            parts += "#$it"
        }
        return parts.joinToString(" ").ifBlank { rawName.orEmpty() }
    }

    private fun pruneStaleScanResultsLocked() {
        val threshold = System.currentTimeMillis() - SCAN_RESULT_TTL_MS
        val iterator = cachedDevices.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.lastSeen < threshold) {
                iterator.remove()
            }
        }
    }

    private fun scheduleScanTimeout(scanner: BluetoothLeScanner) {
        val runnable = Runnable {
            Log.i(TAG, "scheduleScanTimeout: stopping scan after timeout")
            scanner.stopSafeScan(scanCallback)
            isScanning = false
        }
        scanTimeout?.let(mainHandler::removeCallbacks)
        scanTimeout = runnable
        mainHandler.postDelayed(runnable, SCAN_TIMEOUT_MS)
    }

    private fun BluetoothLeScanner.stopSafeScan(callback: ScanCallback) = runCatching {
        stopScan(callback)
    }

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

    data class CachedScanDevice(
        val device: BluetoothDevice,
        val name: String,
        val address: String,
        val rssi: Int,
        val lastSeen: Long,
        val productId: Int?,
        val firmwareVersion: String?,
        val uniqueId: String?,
        val protocolKey: String?
    )

    interface ScanFailureListener {
        fun onScanFailure(errorCode: Int)
    }

    sealed class ScanStartResult {
        data object Started : ScanStartResult()
        data class Error(
            val status: BleConnectionState.Status,
            val message: String
        ) : ScanStartResult()
    }

    companion object {
        private const val SCAN_TIMEOUT_MS = 15_000L
        private const val SCAN_RESULT_TTL_MS = 20_000L
        private val TAG = logTag("BleScanCoordinator")
    }

    fun updateConnectedAddress(address: String?) {
        connectedAddress = address
        emitDeviceSnapshot()
    }
}
