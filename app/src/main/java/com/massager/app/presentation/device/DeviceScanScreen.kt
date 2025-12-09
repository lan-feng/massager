package com.massager.app.presentation.device

// 文件说明：展示扫描到的设备列表供选择连接。
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.massager.app.R
import com.massager.app.data.bluetooth.BleConnectionState
import com.massager.app.presentation.navigation.DeviceScanSource
import com.massager.app.presentation.theme.massagerExtendedColors
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScanScreen(
    viewModel: DeviceViewModel,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateControl: (String?) -> Unit
) {
    val context = LocalContext.current
    val runtimePermissions = remember {
        buildList {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                add(android.Manifest.permission.BLUETOOTH_SCAN)
                add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }
            // 兼容部分厂商仍要求位置权限
            add(android.Manifest.permission.ACCESS_FINE_LOCATION)
            add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // no-op; viewModel will retry scan when permissions granted
    }
    val enableBtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshScan()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = uiState.connectionState.errorMessage
    val missingPermissions = runtimePermissions.filter {
        ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    val isBluetoothOff = uiState.connectionState.status == BleConnectionState.Status.BluetoothUnavailable

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearErrorMessage()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                DeviceScanEffect.NavigateHome -> onNavigateHome()
                is DeviceScanEffect.ReturnToControl -> onNavigateControl(effect.deviceSerial)
                is DeviceScanEffect.ShowMessage -> {
                    val text = effect.messageRes?.let(context::getString) ?: effect.message
                    if (!text.isNullOrBlank()) {
                        snackbarHostState.showSnackbar(text)
                    }
                }
                DeviceScanEffect.RequestPermissions -> {
                    val missing = runtimePermissions.filter {
                        ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    }
                    if (missing.isNotEmpty()) {
                        permissionLauncher.launch(missing.toTypedArray())
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.device_scan_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::refreshScan,
                        enabled = !uiState.isScanning
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(id = R.string.device_scan_refresh)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.device_scan_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            DeviceStatusRow(
                connectionState = uiState.connectionState,
                isScanning = uiState.isScanning
            )
            Spacer(modifier = Modifier.height(12.dp))

            val showPermissionsFirst = missingPermissions.isNotEmpty()

            if (!showPermissionsFirst && isBluetoothOff) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.massagerExtendedColors.accentSoft),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(id = R.string.device_error_bluetooth_disabled),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.massagerExtendedColors.textPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = {
                                enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                            }) {
                                Text(text = stringResource(id = R.string.try_again))
                            }
                            OutlinedButton(onClick = {
                                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }) {
                                Text(text = stringResource(id = R.string.permission_bluetooth_settings))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (missingPermissions.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.massagerExtendedColors.accentSoft),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(id = R.string.device_error_bluetooth_scan_permission),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.massagerExtendedColors.textPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = {
                                permissionLauncher.launch(missingPermissions.toTypedArray())
                            }) {
                                Text(text = stringResource(id = R.string.try_again))
                            }
                            OutlinedButton(onClick = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }) {
                                Text(text = stringResource(id = R.string.permission_bluetooth_settings))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            val actionLabel = if (uiState.scanSource == DeviceScanSource.CONTROL) {
                stringResource(id = R.string.device_scan_action_combo)
            } else {
                stringResource(id = R.string.device_scan_action_bind)
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.devices.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(id = R.string.device_scan_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 24.dp)
                        )
                    }
                } else {
                    items(uiState.devices, key = { it.address }) { device ->
                        DeviceScanListItem(
                            device = device,
                            actionLabel = actionLabel,
                            isProcessing = uiState.processingAddress == device.address,
                            onAction = { viewModel.onDeviceSelected(device) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = viewModel::refreshScan,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isScanning
                ) {
                    Text(text = stringResource(id = R.string.device_scan_refresh))
                }
                Button(
                    onClick = viewModel::toggleScan,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isScanning) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Text(
                        text = stringResource(
                            id = if (uiState.isScanning) {
                                R.string.device_scan_stop
                            } else {
                                R.string.device_scan_scan
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceStatusRow(
    connectionState: BleConnectionState,
    isScanning: Boolean
) {
    val statusText = when (connectionState.status) {
        BleConnectionState.Status.Connected -> {
            val name = connectionState.deviceName ?: connectionState.deviceAddress.orEmpty()
            stringResource(id = R.string.device_scan_status_connected, name)
        }

        BleConnectionState.Status.Connecting -> stringResource(id = R.string.device_scan_status_connecting)
        BleConnectionState.Status.Scanning -> stringResource(id = R.string.device_scan_status_scanning)
        BleConnectionState.Status.Disconnected -> stringResource(id = R.string.device_scan_status_disconnected)
        BleConnectionState.Status.BluetoothUnavailable -> stringResource(id = R.string.device_scan_error_bluetooth_off)
        else -> stringResource(id = R.string.device_scan_status_idle)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            AnimatedVisibility(visible = !connectionState.errorMessage.isNullOrBlank()) {
                Text(
                    text = connectionState.errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        AnimatedVisibility(visible = isScanning) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun DeviceScanListItem(
    device: DeviceListItem,
    actionLabel: String,
    isProcessing: Boolean,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(
                        id = R.string.device_scan_signal_strength,
                        device.signalStrength
                    ),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing,
                colors = if (device.isConnected) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(text = actionLabel)
                }
            }
        }
    }
}
