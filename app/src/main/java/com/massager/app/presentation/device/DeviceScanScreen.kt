package com.massager.app.presentation.device

// 文件说明：展示扫描到的设备列表供选择连接。UI 重构仅影响展示，不改动 BLE 逻辑。
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.massager.app.R
import com.massager.app.data.bluetooth.BleConnectionState
import com.massager.app.presentation.theme.BandDeep
import com.massager.app.presentation.theme.BandPrimary
import com.massager.app.presentation.theme.DangerDark
import com.massager.app.presentation.theme.DangerLight
import com.massager.app.presentation.theme.massagerExtendedColors
import com.massager.app.presentation.components.ThemedSnackbarHost
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
        // viewModel will retry scan when permissions granted
    }
    val enableBtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshScan()
    }
    var permissionRequested by remember { mutableStateOf(false) }
    var bluetoothRequested by remember { mutableStateOf(false) }
    var permissionRequestFailed by remember { mutableStateOf(false) }
    var btEnableFailed by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = uiState.connectionState.errorMessage
    val missingPermissions = runtimePermissions.filter {
        ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    var settingsDialogMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(missingPermissions) {
        if (missingPermissions.isNotEmpty()) {
            permissionRequested = true
            runCatching {
                permissionLauncher.launch(missingPermissions.toTypedArray())
            }.onFailure {
                permissionRequestFailed = true
                val text = context.getString(R.string.device_error_bluetooth_scan_permission)
                snackbarHostState.showSnackbar(text)
                settingsDialogMessage = text
            }
        } else {
            permissionRequested = false
            permissionRequestFailed = false
            settingsDialogMessage = null
        }
    }
    LaunchedEffect(missingPermissions, permissionRequested) {
        if (missingPermissions.isNotEmpty() && permissionRequested) {
            settingsDialogMessage = settingsDialogMessage
                ?: context.getString(R.string.device_error_bluetooth_scan_permission)
        }
    }
    val isBluetoothOff = uiState.connectionState.status == BleConnectionState.Status.BluetoothUnavailable
    LaunchedEffect(isBluetoothOff) {
        if (isBluetoothOff && missingPermissions.isEmpty()) {
            bluetoothRequested = true
            runCatching {
                enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }.onFailure {
                btEnableFailed = true
                val text = context.getString(R.string.device_error_bluetooth_disabled)
                snackbarHostState.showSnackbar(text)
                settingsDialogMessage = text
            }
        } else {
            bluetoothRequested = false
            btEnableFailed = false
            if (!permissionRequestFailed) {
                settingsDialogMessage = null
            }
        }
    }
    val isDark = isSystemInDarkTheme()
    val accent = if (isDark) BandDeep else BandPrimary
    val signalColor = if (isDark) DangerDark else DangerLight

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
                title = { Text(text = stringResource(id = R.string.add_device_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = accent.copy(alpha = 0.08f)
                )
            )
        },
        snackbarHost = { ThemedSnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.massagerExtendedColors.surfaceSubtle)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            RadarScanView(
                isScanning = uiState.isScanning,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (uiState.isScanning) {
                        viewModel.toggleScan()
                    } else {
                        viewModel.refreshScan()
                    }
                },
                accent = accent
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (uiState.isScanning) {
                    stringResource(id = R.string.device_scanning_status)
                } else {
                    stringResource(id = R.string.device_found_count, uiState.devices.size)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))

            if ((isBluetoothOff && (bluetoothRequested || btEnableFailed)) ||
                ((missingPermissions.isNotEmpty() && permissionRequested) || permissionRequestFailed)
            ) {
                settingsDialogMessage = settingsDialogMessage ?: context.getString(
                    if (isBluetoothOff) R.string.device_error_bluetooth_disabled
                    else R.string.device_error_bluetooth_scan_permission
                )
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
                            color = MaterialTheme.massagerExtendedColors.textMuted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    items(uiState.devices, key = { it.address }) { device ->
                        DeviceCard(
                            device = device,
                            isProcessing = uiState.processingAddress == device.address,
                            signalColor = signalColor,
                            onClick = { viewModel.onDeviceSelected(device) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // 按钮移除，交互已收敛至雷达中心
        }
    }

    settingsDialogMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { settingsDialogMessage = null },
            title = { Text(text = stringResource(id = R.string.permission_bluetooth_settings)) },
            text = { Text(text = message) },
            confirmButton = {
                Button(
                    onClick = {
                        settingsDialogMessage = null
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                ) {
                    Text(text = stringResource(id = R.string.permission_bluetooth_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { settingsDialogMessage = null }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun RadarScanView(
    isScanning: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFF3AA1A0)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_transition")
    val sweepAngleState = if (isScanning) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "radar_sweep"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val sweepAngle by sweepAngleState

    val primary = accent
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f) }

    Box(
        modifier = modifier.size(270.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2
            val outerStrokeWidth = 1.5.dp.toPx()
            val innerStrokeWidth = 1.dp.toPx()
            val crosshairStroke = 0.8.dp.toPx()

            // Outer circle
            drawCircle(
                color = primary.copy(alpha = 0.5f),
                radius = radius,
                style = Stroke(width = outerStrokeWidth)
            )

            // Inner dashed circle
            drawCircle(
                color = primary.copy(alpha = 0.35f),
                radius = radius * 0.68f,
                style = Stroke(width = innerStrokeWidth, pathEffect = dashEffect)
            )

            // Crosshair lines
            drawLine(
                color = primary.copy(alpha = 0.25f),
                start = Offset(0f, center.y),
                end = Offset(size.width, center.y),
                strokeWidth = crosshairStroke
            )
            drawLine(
                color = primary.copy(alpha = 0.25f),
                start = Offset(center.x, 0f),
                end = Offset(center.x, size.height),
                strokeWidth = crosshairStroke
            )
            // Concentric rings for tap target (gap ≈ 4dp)
            val innerRingRadius = radius * 0.30f
            val ringGap = 8.dp.toPx()
            drawCircle(
                color = primary.copy(alpha = 0.30f),
                radius = innerRingRadius,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = primary.copy(alpha = 0.20f),
                radius = innerRingRadius + ringGap,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Sweep: gradient fade from center to edge, visible only when scanning
            if (isScanning) {
                val sweepBrush = Brush.sweepGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        0.4f to primary.copy(alpha = 0.18f),
                        0.75f to primary.copy(alpha = 0.38f),
                        1f to primary.copy(alpha = 0.68f)
                    ),
                    center = center
                )
                rotate(degrees = sweepAngle, pivot = center) {
                    drawArc(
                        brush = sweepBrush,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                }
            }

        }
        // Center content: square when scanning, search icon when idle
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(
                    color = if (isScanning) primary.copy(alpha = 0.14f) else MaterialTheme.massagerExtendedColors.surfaceBright,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isScanning) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(primary, shape = RoundedCornerShape(4.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceCard(
    device: DeviceListItem,
    isProcessing: Boolean,
    signalColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Bluetooth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(id = R.string.device_serial_label, device.uniqueId.toString()),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.bigtop_updates_24),
                        contentDescription = null,
                        tint = signalColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "${device.signalStrength}",
                        style = MaterialTheme.typography.labelMedium.copy(color = signalColor)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

            }
        }
    }
}
