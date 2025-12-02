package com.massager.app.presentation.device

// 文件说明：新增设备的扫描/输入界面。
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.massager.app.R
import com.massager.app.presentation.theme.massagerExtendedColors
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceScreen(
    viewModel: AddDeviceViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateManual: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val runtimePermissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
    }
    val missingPermissions = runtimePermissions.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }

    var autoPermissionRequested by rememberSaveable { mutableStateOf(false) }
    var hasTriggeredScan by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val stillMissing = runtimePermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (stillMissing.isEmpty()) {
            hasTriggeredScan = false
            viewModel.clearError()
            viewModel.startScan()
        }
    }

    val packageName = context.packageName

    LaunchedEffect(missingPermissions) {
        if (missingPermissions.isEmpty()) {
            if (!hasTriggeredScan) {
                hasTriggeredScan = true
                viewModel.startScan()
            }
        } else {
            hasTriggeredScan = false
            if (!autoPermissionRequested) {
                autoPermissionRequested = true
                permissionLauncher.launch(missingPermissions.toTypedArray())
            }
        }
    }

    fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    var permissionRetryLaunched by rememberSaveable { mutableStateOf(false) }
    val permissionErrorText = stringResource(id = R.string.device_error_bluetooth_scan_permission)
    val currentMissingPermissions by rememberUpdatedState(newValue = missingPermissions)
    val showSettingsShortcut = permissionRetryLaunched && missingPermissions.isNotEmpty()
    LaunchedEffect(uiState.errorMessage) {
        if (
            uiState.errorMessage == permissionErrorText &&
            currentMissingPermissions.isNotEmpty() &&
            !permissionRetryLaunched
        ) {
            permissionRetryLaunched = true
            permissionLauncher.launch(currentMissingPermissions.toTypedArray())
        }
        if (uiState.errorMessage == null) {
            permissionRetryLaunched = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                AddDeviceEffect.NavigateHome -> onNavigateHome()
                is AddDeviceEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
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
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::retryScan) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(id = R.string.try_again)
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
                .background(MaterialTheme.massagerExtendedColors.surfaceSubtle)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            RadarAnimation(isScanning = uiState.isScanning)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.searching_devices),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.massagerExtendedColors.textMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = uiState.statusMessageRes != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.statusMessageRes?.let { resId ->
                    StatusMessageCard(
                        text = stringResource(id = resId),
                        onDismiss = viewModel::clearStatusMessage
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (missingPermissions.isNotEmpty()) {
                PermissionRequestCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    onRequestPermission = {
                        permissionLauncher.launch(missingPermissions.toTypedArray())
                    },
                    onOpenSettings = if (showSettingsShortcut) {
                        { openAppSettings() }
                    } else {
                        null
                    }
                )
                Spacer(modifier = Modifier.weight(1f))
            } else if (uiState.devices.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = uiState.devices,
                        key = { it.id }
                    ) { device ->
                        val isConnecting = uiState.connectingDeviceId == device.id
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
                        ) {
                            DeviceCardItem(
                                device = device,
                                isConnecting = isConnecting,
                                onClick = { viewModel.connectToDevice(device) }
                            )
                        }
                    }
                }
            } else if (uiState.showEmpty) {
                AddDeviceEmptyState(onRetry = viewModel::retryScan)
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            OutlinedButton(
                onClick = onNavigateManual,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = stringResource(id = R.string.add_manually),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun RadarAnimation(isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_transition")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_sweep"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radar_pulse"
    )
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val radialColors = remember(primary, primaryContainer) {
        listOf(primaryContainer, primary.copy(alpha = 0.85f))
    }
    val pulseStrokeColor = MaterialTheme.massagerExtendedColors.surfaceBright

    Box(
        modifier = Modifier
            .size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2
            val gradient = Brush.radialGradient(
                colors = radialColors,
                center = center,
                radius = radius
            )
            drawCircle(brush = gradient, radius = radius)
            drawCircle(
                color = pulseStrokeColor.copy(alpha = 0.35f),
                radius = radius * pulse,
                style = Stroke(width = 6f)
            )
            drawArc(
                color = primary.copy(alpha = if (isScanning) 0.35f else 0.15f),
                startAngle = sweepAngle,
                sweepAngle = 60f,
                useCenter = true
            )
        }
        Box(
            modifier = Modifier
                .size(92.dp)
                .background(MaterialTheme.massagerExtendedColors.surfaceBright, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Bluetooth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceCardItem(
    device: NearbyDevice,
    isConnecting: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = device.macAddress,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Text(
                text = if (isConnecting) {
                    stringResource(id = R.string.connecting)
                } else {
                    stringResource(id = R.string.add_device_tap_to_connect)
                },
                style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary)
            )
            Text(
                text = stringResource(id = R.string.add_device_signal_strength, device.signalStrength),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddDeviceEmptyState(
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Bluetooth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.no_devices_found),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.try_again),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = stringResource(id = R.string.try_again))
        }
    }
}

@Composable
private fun PermissionRequestCard(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
    onOpenSettings: (() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.permission_bluetooth_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.permission_bluetooth_message),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = onRequestPermission) {
                    Text(text = stringResource(id = R.string.permission_bluetooth_grant))
                }
                if (onOpenSettings != null) {
                    Spacer(modifier = Modifier.size(4.dp))
                    TextButton(onClick = onOpenSettings) {
                        Text(text = stringResource(id = R.string.permission_bluetooth_settings))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMessageCard(
    text: String,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.common_ok))
            }
        }
    }
}
