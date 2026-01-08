package com.massager.app.data.bluetooth.scan

// 文件说明：协调 BLE 扫描流程与过滤逻辑的组件。
import android.Manifest
import android.annotation.SuppressLint
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
import android.os.SystemClock
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
    private var pendingStart: Runnable? = null
    private var lastScanStartElapsed = 0L
    private val recentStarts = ArrayDeque<Long>()
    private var nextAllowedStartAt = 0L
    private var isScanning = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::handleScanResult)
        }

        override fun onScanFailed(errorCode: Int) {
            handleScanFailure(errorCode)
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
        pendingStart?.let(mainHandler::removeCallbacks)
        pendingStart = null
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
        if (requiresLocationPermission() && !hasLocationPermission()) {
            return ScanStartResult.Error(
                status = BleConnectionState.Status.BluetoothUnavailable,
                message = context.getString(R.string.device_error_location_permission)
            )
        }
        val scanner = adapter.bluetoothLeScanner ?: return ScanStartResult.Error(
            status = BleConnectionState.Status.BluetoothUnavailable,
            message = context.getString(R.string.device_error_bluetooth_disabled)
        )
        val now = SystemClock.elapsedRealtime()
        if (now < nextAllowedStartAt) {
            val cooldownDelay = (nextAllowedStartAt - now).coerceAtMost(MAX_THROTTLE_DELAY_MS)
            Log.i(TAG, "startScan: respecting cooldown, delaying ${cooldownDelay}ms")
            scheduleThrottledStart(cooldownDelay)
            return ScanStartResult.Started
        }
        val elapsed = now - lastScanStartElapsed
        val delay = computeRequiredDelay(elapsed, now)
        if (delay > 0L) {
            Log.i(TAG, "startScan: throttling scan request, delaying ${delay}ms")
            scheduleThrottledStart(delay)
            return ScanStartResult.Started
        }
        return startScanInternal(scanner)
    }

    private fun startScanInternal(scanner: BluetoothLeScanner): ScanStartResult {
        synchronized(cachedDevices) { cachedDevices.clear() }
        emitDeviceSnapshot()
        isScanning = true
        lastScanStartElapsed = SystemClock.elapsedRealtime()
        recordStartTimestamp(lastScanStartElapsed)
        return try {
            scanner.startScan(null, scanSettings, scanCallback)
            scheduleScanTimeout(scanner)
            ScanStartResult.Started
        } catch (securityException: SecurityException) {
            Log.w(TAG, "startScan: missing permission", securityException)
            isScanning = false
            ScanStartResult.Error(
                status = BleConnectionState.Status.BluetoothUnavailable,
                message = context.getString(R.string.device_error_bluetooth_scan_permission)
            )
        }
    }

    fun restartScan(): ScanStartResult {
        synchronized(cachedDevices) { cachedDevices.clear() }
        emitDeviceSnapshot()
        if (isScanning) return ScanStartResult.Started
        stopScan()
        return startScan()
    }

    fun stopScan() {
        pendingStart?.let(mainHandler::removeCallbacks)
        pendingStart = null
        if (!isScanning) return
        bluetoothAdapter?.bluetoothLeScanner?.stopSafeScan(scanCallback)
        isScanning = false
        scanTimeout?.let(mainHandler::removeCallbacks)
        scanTimeout = null
    }

    fun clearCache() {
        synchronized(cachedDevices) { cachedDevices.clear() }
        emitDeviceSnapshot()
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
        synchronized(cachedDevices) {
            val existing = cachedDevices[device.address]
            val cachedDevice = CachedScanDevice(
                device = device,
                name = buildDisplayName(safeDeviceName(device), advertisement),
                address = device.address,
                rssi = result.rssi,
                lastSeen = System.currentTimeMillis(),
                firstSeen = existing?.firstSeen ?: SystemClock.elapsedRealtime(),
                productId = advertisement.productId,
                firmwareVersion = advertisement.firmwareVersion,
                uniqueId = advertisement.uniqueId,
                protocolKey = adapter.protocolKey
            )
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
                .sortedBy { it.firstSeen }
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
        // Show only the base name; avoid appending firmware/uniqueId to keep label concise.
        return rawName?.takeIf { it.isNotBlank() }
            ?: advertisement.uniqueId.orEmpty()
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

    private fun scheduleThrottledStart(delayMs: Long) {
        val runnable = Runnable {
            pendingStart = null
            if (isScanning) return@Runnable
            val adapter = bluetoothAdapter ?: return@Runnable
            if (!adapter.isEnabled) return@Runnable
            if (!hasScanPermission()) return@Runnable
            if (requiresLocationPermission() && !hasLocationPermission()) return@Runnable
            val scanner = adapter.bluetoothLeScanner ?: return@Runnable
            startScanInternal(scanner)
        }
        pendingStart = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    private fun computeRequiredDelay(elapsedSinceLast: Long, now: Long): Long {
        val minGapDelay = (MIN_SCAN_RESTART_INTERVAL_MS - elapsedSinceLast).coerceAtLeast(0L)
        pruneOldStarts(now)
        val overLimit = recentStarts.size >= MAX_STARTS_PER_WINDOW
        val windowDelay = if (overLimit) {
            val oldest = recentStarts.firstOrNull() ?: now
            (oldest + SCAN_WINDOW_MS - now).coerceAtLeast(0L)
        } else 0L
        val delay = maxOf(minGapDelay, windowDelay)
        return delay.coerceAtMost(MAX_THROTTLE_DELAY_MS)
    }

    private fun pruneOldStarts(now: Long) {
        while (recentStarts.isNotEmpty() && now - recentStarts.first() > SCAN_WINDOW_MS) {
            recentStarts.removeFirst()
        }
    }

    private fun recordStartTimestamp(timestamp: Long) {
        pruneOldStarts(timestamp)
        recentStarts.addLast(timestamp)
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothLeScanner.stopSafeScan(callback: ScanCallback) {
        if (!hasScanPermission()) return
        runCatching { stopScan(callback) }
    }

    private fun handleScanFailure(errorCode: Int) {
        listeners.forEach { it.onScanFailure(errorCode) }
        // 针对系统“scanning too frequently”报错添加冷却期，避免短时间内再次触发。
        if (errorCode == ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED ||
            errorCode == ScanCallback.SCAN_FAILED_INTERNAL_ERROR || // 部分机型也用此码表示频率限制
            errorCode == 6 /* 部分厂商返回裸 6 */) {
            nextAllowedStartAt = SystemClock.elapsedRealtime() + COOLDOWN_AFTER_FAILURE_MS
        }
        stopScan()
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

    private fun requiresLocationPermission(): Boolean {
        // Android 12+ (S) 允许使用 BLUETOOTH_SCAN/CONNECT（neverForLocation）在无定位权限下扫描；
        // 旧版本仍需要定位权限才能返回扫描结果。
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    }

    private fun safeDeviceName(device: BluetoothDevice): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                return null
            }
        }
        return runCatching { device.name }.getOrNull()
    }

    data class CachedScanDevice(
        val device: BluetoothDevice,
        val name: String,
        val address: String,
        val rssi: Int,
        val lastSeen: Long,
        val firstSeen: Long,
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
        const val SCAN_RESULT_TTL_MS = 20_000L
        private const val SCAN_TIMEOUT_MS = 15_000L
        private const val MIN_SCAN_RESTART_INTERVAL_MS = 3_000L
        private const val SCAN_WINDOW_MS = 30_000L
        private const val MAX_STARTS_PER_WINDOW = 3
        private const val MAX_THROTTLE_DELAY_MS = 5_000L
        private const val COOLDOWN_AFTER_FAILURE_MS = 15_000L
        private val TAG = logTag("BleScanCoordinator")
    }

    fun updateConnectedAddress(address: String?) {
        connectedAddress = address
        emitDeviceSnapshot()
    }
}
