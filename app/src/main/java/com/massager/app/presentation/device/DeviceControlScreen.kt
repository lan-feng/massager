package com.massager.app.presentation.device

// 文件说明：设备控制 UI，提供模式、强度等交互。
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.massager.app.data.bluetooth.BleConnectionState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderPositions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.massager.app.R
import com.massager.app.presentation.theme.massagerExtendedColors
import com.massager.app.presentation.navigation.DeviceScanSource
import com.massager.app.presentation.device.DeviceViewModel
import com.massager.app.presentation.components.ThemedSnackbarHost
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


@Composable
fun DeviceControlScreen(
    viewModel: DeviceControlViewModel,
    onBack: () -> Unit,
    onAddDevice: (List<String>) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAfterReturning()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnectActiveDevice()
        }
    }

    LaunchedEffect(state.transientMessage) {
        val message = state.transientMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeTransientMessage()
        }
    }

    DeviceControlContent(
        state = state,
        onBack = onBack,
        onAddDevice = onAddDevice,
        onComboResult = viewModel::handleComboResult,
        onReconnect = viewModel::reconnect,
        onSelectDevice = viewModel::selectComboDevice,
        onSelectZone = viewModel::selectZone,
        onSelectMode = viewModel::selectMode,
        onSelectTimer = viewModel::selectTimer,
        onPreviewLevel = viewModel::previewLevel,
        onCommitLevel = viewModel::commitLevel,
        onToggleMute = viewModel::toggleMute,
        onToggleSession = viewModel::toggleSession,
        onRenameAttached = viewModel::renameAttachedDevice,
        onRemoveAttached = viewModel::removeAttachedDevice,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceControlContent(
    state: DeviceControlUiState,
    onBack: () -> Unit,
    onAddDevice: (List<String>) -> Unit = {},
    onComboResult: (String?) -> Unit,
    onReconnect: (String?) -> Unit,
    onSelectDevice: (String?) -> Unit,
    onSelectZone: (BodyZone) -> Unit,
    onSelectMode: (Int) -> Unit,
    onSelectTimer: (Int) -> Unit,
    onPreviewLevel: (Int) -> Unit,
    onCommitLevel: (Int) -> Unit,
    onToggleMute: () -> Unit,
    onToggleSession: () -> Unit,
    onRenameAttached: (String, String) -> Unit,
    onRemoveAttached: (String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val runtimePermissions = remember {
        buildList {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                add(android.Manifest.permission.BLUETOOTH_SCAN)
                add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }
            add(android.Manifest.permission.ACCESS_FINE_LOCATION)
            add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }
    val enableBtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }
    var permissionRequested by remember { mutableStateOf(false) }
    var bluetoothRequested by remember { mutableStateOf(false) }
    var settingsDialogMessage by remember { mutableStateOf<String?>(null) }
    val missingPermissions = runtimePermissions.filter {
        ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    var permissionRequestFailed by remember { mutableStateOf(false) }
    LaunchedEffect(missingPermissions) {
        if (missingPermissions.isNotEmpty()) {
            permissionRequested = true
            runCatching {
                permissionLauncher.launch(missingPermissions.toTypedArray())
            }.onFailure {
                permissionRequestFailed = true
                settingsDialogMessage = context.getString(R.string.device_error_bluetooth_scan_permission)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.device_error_bluetooth_scan_permission)
                    )
                }
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
    val isBluetoothOff = (BluetoothAdapter.getDefaultAdapter()?.isEnabled == false) ||
        state.deviceStatuses.values.any {
            it.connectionStatus == BleConnectionState.Status.BluetoothUnavailable
        }
    var btEnableFailed by remember { mutableStateOf(false) }
    LaunchedEffect(isBluetoothOff) {
        if (isBluetoothOff && missingPermissions.isEmpty()) {
            bluetoothRequested = true
            runCatching {
                enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }.onFailure {
                btEnableFailed = true
                settingsDialogMessage = context.getString(R.string.device_error_bluetooth_disabled)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.device_error_bluetooth_disabled)
                    )
                }
            }
        } else {
            bluetoothRequested = false
            btEnableFailed = false
            if (!permissionRequestFailed) {
                settingsDialogMessage = null
            }
        }
    }
    val selectedStatus = state.deviceCards.firstOrNull { it.isSelected }?.deviceSerial?.let { serial ->
        state.deviceStatuses[serial]
    }
    val isDevicePoweredOff = selectedStatus?.connectionStatus == BleConnectionState.Status.Disconnected ||
        selectedStatus?.connectionStatus == BleConnectionState.Status.Failed
    var hideConnectionDialog by remember { mutableStateOf(false) }
    LaunchedEffect(isDevicePoweredOff) {
        if (!isDevicePoweredOff) {
            hideConnectionDialog = false
        }
    }
    val hostSerial = state.deviceCards.firstOrNull { it.isMainDevice }?.deviceSerial
    val excludedSerials = remember(state.deviceCards) {
        val combos = state.deviceCards
            .filterNot { it.isMainDevice }
            .mapNotNull { it.deviceSerial }
        buildList {
            hostSerial?.let { add(it) }
            addAll(combos)
        }
    }
    val handleReconnect: (String?) -> Unit = { serial ->
        val currentMissingPermissions = runtimePermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        val bluetoothDisabled = (BluetoothAdapter.getDefaultAdapter()?.isEnabled == false) ||
            state.deviceStatuses.values.any { it.connectionStatus == BleConnectionState.Status.BluetoothUnavailable }
        when {
            currentMissingPermissions.isNotEmpty() -> {
                permissionLauncher.launch(currentMissingPermissions.toTypedArray())
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.device_error_bluetooth_scan_permission)
                    )
                }
            }
            bluetoothDisabled -> {
                enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.device_error_bluetooth_disabled)
                    )
                }
            }
            else -> onReconnect(serial)
        }
    }
    val brand = MaterialTheme.massagerExtendedColors.band
    val brandSoft = MaterialTheme.massagerExtendedColors.bandSoft
    var showInfoDialog by remember { mutableStateOf(false) }
    var manageTarget by remember { mutableStateOf<DeviceCardState?>(null) }
    var renameTarget by remember { mutableStateOf<DeviceCardState?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var renameErrorRes by remember { mutableStateOf<Int?>(null) }
    var deleteTarget by remember { mutableStateOf<DeviceCardState?>(null) }
    var showScanDialog by remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.deviceName.ifBlank { stringResource(id = R.string.device_title) },
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(Icons.Filled.HelpOutline, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.massagerExtendedColors.surfaceSubtle
                )
            )
        },
        snackbarHost = { ThemedSnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.massagerExtendedColors.surfaceSubtle
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
        val controlsEnabled = state.isConnected
        val controlsAlpha = if (controlsEnabled) 1f else 0.45f
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DeviceSwitcherRow(
                cards = state.deviceCards,
                onSelect = onSelectDevice,
                onLongPress = { card ->
                    if (!card.isMainDevice && card.deviceSerial != null) {
                        manageTarget = card
                    }
                },
                onAddDevice = { showScanDialog = true },
                deviceStatuses = state.deviceStatuses,
                isMuted = state.isMuted,
                onToggleMute = onToggleMute,
                onReconnect = handleReconnect
            )
            AnimatedVisibility(visible = state.isComboUpdating) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(modifier = Modifier.alpha(controlsAlpha)) {
                TimerDashboard(
                    isRunning = state.isRunning,
                    remainingSeconds = state.remainingSeconds,
                    timerMinutes = state.timerMinutes,
                    brand = brand,
                    brandSoft = brandSoft,
                    enabled = state.isProtocolReady && controlsEnabled,
                    onSelectTimer = { minutes -> onSelectTimer(minutes.coerceIn(0, 60)) },
                    onToggleSession = onToggleSession
                )
            }
            Box(modifier = Modifier.alpha(controlsAlpha)) {
                LevelControlSection(
                    level = state.level,
                    isConnected = controlsEnabled,
                    brand = brand,
                    onPreviewLevel = onPreviewLevel,
                    onCommitLevel = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCommitLevel(it)
                    }
                )
            }
            Box(modifier = Modifier.alpha(controlsAlpha)) {
                ModeSelectionGrid(
                    selectedMode = state.mode,
                    isEnabled = state.isProtocolReady && controlsEnabled,
                    onSelectMode = onSelectMode,
                    brand = brand
                )
            }
            BodyZoneGrid(
                selectedZone = state.zone,
                onSelectZone = onSelectZone,
                brand = brand,
                isEnabled = controlsEnabled,
                modifier = Modifier.alpha(controlsAlpha)
            )
        }
            if (isDevicePoweredOff && settingsDialogMessage == null && !hideConnectionDialog) {
                Dialog(
                    onDismissRequest = {},
                    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = 32.dp)
                                .widthIn(max = 360.dp),
                            shape = RoundedCornerShape(20.dp),
                            tonalElevation = 10.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 24.dp, vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.device_connection_error_title),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = stringResource(id = R.string.device_connection_timeout_message),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.massagerExtendedColors.textPrimary
                                    ),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.device_connection_instruction_power_on),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.massagerExtendedColors.textSecondary
                                        ),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                    Text(
                                        text = stringResource(id = R.string.device_connection_instruction_blink),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.massagerExtendedColors.textSecondary
                                        ),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                }
                                Divider(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            hideConnectionDialog = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.common_ok),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = state.showConnectingOverlay,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .matchParentSize()
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 6.dp,
                        color = MaterialTheme.massagerExtendedColors.surfaceBright
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = stringResource(id = R.string.connecting),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    settingsDialogMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { settingsDialogMessage = null },
            title = { Text(text = stringResource(id = R.string.permission_bluetooth_settings)) },
            text = { Text(text = message) },
            confirmButton = {
                TextButton(
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

    manageTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { manageTarget = null },
            title = { Text(text = stringResource(id = R.string.device_combo_manage_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(id = R.string.device_combo_manage_message, target.displayName),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(
                        onClick = {
                            renameTarget = target
                            renameValue = target.displayName
                            renameErrorRes = null
                            manageTarget = null
                        }
                    ) {
                        Text(text = stringResource(id = R.string.device_combo_manage_rename))
                    }
                    TextButton(
                        onClick = {
                            deleteTarget = target
                            manageTarget = null
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.device_combo_manage_remove),
                            color = MaterialTheme.massagerExtendedColors.danger
                        )
                    }
                    TextButton(onClick = { manageTarget = null }) {
                        Text(text = stringResource(id = R.string.device_combo_manage_cancel))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = {
                renameTarget = null
                renameErrorRes = null
            },
            title = { Text(text = stringResource(id = R.string.rename_device_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameValue,
                        onValueChange = {
                            renameValue = it
                            renameErrorRes = null
                        },
                        placeholder = { Text(text = stringResource(id = R.string.device_combo_rename_hint)) },
                        singleLine = true
                    )
                    renameErrorRes?.let { errorRes ->
                        Text(
                            text = stringResource(id = errorRes),
                            color = MaterialTheme.massagerExtendedColors.danger,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = renameValue.trim()
                        val error = validateLinkedDeviceName(trimmed)
                        if (error != null) {
                            renameErrorRes = error
                        } else {
                            target.deviceSerial?.let { serial ->
                                onRenameAttached(serial, trimmed)
                            }
                            renameTarget = null
                            renameErrorRes = null
                        }
                    },
                    enabled = !state.isComboUpdating
                ) {
                    Text(text = stringResource(id = R.string.rename))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    renameTarget = null
                    renameErrorRes = null
                }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(text = stringResource(id = R.string.device_combo_remove_confirm_title)) },
            text = {
                Text(
                    text = stringResource(id = R.string.device_combo_remove_confirm_message, target.displayName),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        target.deviceSerial?.let(onRemoveAttached)
                        deleteTarget = null
                    },
                    enabled = !state.isComboUpdating
                ) {
                    Text(
                        text = stringResource(id = R.string.device_combo_remove_action),
                        color = MaterialTheme.massagerExtendedColors.danger
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(text = stringResource(id = R.string.device_combo_cancel))
                }
            }
        )
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text(text = stringResource(id = R.string.device_info_title)) },
            text = { Text(text = stringResource(id = R.string.device_info_message)) },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text(text = stringResource(id = R.string.device_help_close))
                }
            }
        )
    }

    if (showScanDialog) {
        Dialog(
            onDismissRequest = { showScanDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.massagerExtendedColors.surfaceSubtle),
                color = MaterialTheme.massagerExtendedColors.surfaceSubtle
            ) {
                val scanViewModel: DeviceViewModel = hiltViewModel()
                DisposableEffect(showScanDialog) {
                    onDispose { scanViewModel.stopScan() }
                }
                LaunchedEffect(showScanDialog, hostSerial, excludedSerials) {
                    if (showScanDialog) {
                        scanViewModel.configureScanContext(
                            source = DeviceScanSource.CONTROL,
                            ownerId = hostSerial,
                            excludedSerialsInput = excludedSerials
                        )
                    }
                }
                DeviceScanScreen(
                    viewModel = scanViewModel,
                    onBack = { showScanDialog = false },
                    onNavigateHome = { showScanDialog = false },
                    onNavigateControl = { serial ->
                        onComboResult(serial)
                        showScanDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DeviceSwitcherRow(
    cards: List<DeviceCardState>,
    onSelect: (String?) -> Unit,
    onLongPress: (DeviceCardState) -> Unit,
    onAddDevice: () -> Unit,
    deviceStatuses: Map<String, DeviceStatus>,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onReconnect: (String?) -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val cardSpacing = 12.dp
    val horizontalPadding = 0.dp
    val availableWidth = (screenWidth - horizontalPadding * 2 - cardSpacing*2).coerceAtLeast(200.dp)
    val selectedWidth = availableWidth * 0.7f
    val unselectedWidth = availableWidth * 0.25f
    val cardHeight = 100.dp
    val hasAttached = cards.any { !it.isMainDevice }
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight),
        horizontalArrangement = Arrangement.spacedBy(cardSpacing),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = horizontalPadding)
    ) {
        items(cards, key = { card ->
            card.deviceSerial ?: if (card.isMainDevice) "main-device" else card.displayName
        }) { card ->
            val cardStatus = card.deviceSerial?.let { deviceStatuses[it] }
            val cardWidth by animateDpAsState(
                targetValue = if (card.isSelected) selectedWidth else unselectedWidth,
                label = "device_switcher_width"
            )
            val connectionState = mapConnectionState(cardStatus)
            val batteryPercent = cardStatus?.batteryPercent?.takeIf { it >= 0 }
            DeviceSwitchCard(
                name = card.displayName,
                subtitle = stringResource(
                    id = if (card.isMainDevice) {
                        R.string.device_combo_main_label
                    } else {
                        R.string.device_combo_member_label
                    }
                ),
                isSelected = card.isSelected,
                connectionState = connectionState,
                batteryPercent = batteryPercent,
                buzzerOn = isMuted.not(),
                isInteractive = cardStatus?.isProtocolReady == true,
                onReconnect = { card.deviceSerial?.let(onReconnect) },
                onBuzzerToggle = { _ -> onToggleMute() },
                onCardTap = { card.deviceSerial?.let(onSelect) },
                modifier = Modifier
                    .height(cardHeight)
                    .width(cardWidth),
                onLongPress = if (!card.isMainDevice) ({ onLongPress(card) }) else null
            )
        }
        if (!hasAttached) {
            item {
                AddDeviceBox(
                    onAddDevice = onAddDevice,
                    width = unselectedWidth,
                    height = cardHeight,
                    corner = 16.dp
                )
            }
        }
    }
}

private fun mapConnectionState(status: DeviceStatus?): ConnectionState = when {
    status == null -> ConnectionState.IDLE
    status.connectionStatus == BleConnectionState.Status.Scanning -> ConnectionState.CONNECTING
    status.connectionStatus == BleConnectionState.Status.Connecting -> ConnectionState.CONNECTING
    status.connectionStatus == BleConnectionState.Status.Connected && status.isProtocolReady -> ConnectionState.READY
    status.connectionStatus == BleConnectionState.Status.Connected && status.isProtocolReady.not() -> ConnectionState.CONNECTING
    status.connectionStatus == BleConnectionState.Status.Disconnected ||
        status.connectionStatus == BleConnectionState.Status.Failed ||
        status.connectionStatus == BleConnectionState.Status.BluetoothUnavailable -> ConnectionState.DISCONNECTED
    status.connectionStatus == BleConnectionState.Status.Idle -> ConnectionState.IDLE
    else -> ConnectionState.IDLE
}

@Composable
private fun AddDeviceBox(
    onAddDevice: () -> Unit,
    size: Dp = 88.dp,
    corner: Dp = 12.dp,
    modifier: Modifier = Modifier,
    width: Dp = size,
    height: Dp = size
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.massagerExtendedColors.surfaceBright.copy(alpha = 0.08f))
            .clickable(onClick = onAddDevice)
            .drawBehind {
                val stroke = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
                drawRoundRect(
                    color = Color(0xFF8AB5FF),
                    cornerRadius = CornerRadius(corner.toPx(), corner.toPx()),
                    style = stroke
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(id = R.string.home_add_device_content_desc),
            tint = Color(0xFF8AB5FF),
            modifier = Modifier.size(28.dp)
        )
    }
}

@StringRes
private fun validateLinkedDeviceName(value: String): Int? {
    val trimmed = value.trim()
    if (trimmed.length !in 2..30) return R.string.rename_error_length
    val regex = Regex("^[A-Za-z0-9\\s]+\$")
    return if (!regex.matches(trimmed)) R.string.rename_error_invalid else null
}

@Composable
private fun BodyZoneGrid(
    selectedZone: BodyZone,
    onSelectZone: (BodyZone) -> Unit,
    brand: Color,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val zoneItems = listOf(
        BodyZone.SHOULDER to R.drawable.ic_deck,
        BodyZone.WAIST to R.drawable.ic_airline_seat_recline_normal,
        BodyZone.LEGS to R.drawable.ic_hiking,
        BodyZone.ARMS to R.drawable.ic_sports_gymnastics,
        BodyZone.JOINT to R.drawable.ic_settings_accessibility,
        BodyZone.BODY to R.drawable.ic_person
    )
    val panelBackground = controlPanelBackground()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(panelBackground)
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = stringResource(id = R.string.body_part_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val maxVisibleTiles = 3.5f
        val horizontalPadding = 10.dp * 2 + 10.dp * (maxVisibleTiles - 1)
        val targetWidth = ((screenWidth - horizontalPadding) / maxVisibleTiles).coerceIn(76.dp, 110.dp)
        val tileHeight = (targetWidth - 20.dp).coerceAtLeast(60.dp)

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(tileHeight + 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            items(zoneItems) { (zone, iconRes) ->
                val isSelected = zone == selectedZone
                SelectionTile(
                    title = stringResource(id = zone.labelRes),
                    painter = painterResource(id = iconRes),
                    selected = isSelected,
                    enabled = isEnabled,
                    brand = brand,
                    modifier = Modifier
                        .width(targetWidth)
                        .height(tileHeight),
                    onClick = { onSelectZone(zone) }
                )
            }
        }
    }
}

@Composable
private fun ModeSelectionGrid(
    selectedMode: Int,
    isEnabled: Boolean,
    onSelectMode: (Int) -> Unit,
    brand: Color
) {
    val modeItems = listOf(
        Triple(0, stringResource(id = R.string.device_mode_0), R.drawable.ic_accessibility),
        Triple(1, stringResource(id = R.string.device_mode_1), R.drawable.ic_person),
        Triple(2, stringResource(id = R.string.device_mode_2), R.drawable.ic_gesture),
        Triple(3, stringResource(id = R.string.device_mode_3), R.drawable.ic_compress),
        Triple(4, stringResource(id = R.string.device_mode_4), R.drawable.ic_scatter_plot),
        Triple(5, stringResource(id = R.string.device_mode_5), R.drawable.ic_invert_colors),
        Triple(6, stringResource(id = R.string.device_mode_6), R.drawable.ic_local_fire_department),
        Triple(7, stringResource(id = R.string.device_mode_7), R.drawable.ic_woman)
    )
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxVisibleTiles = 3.5f
    val horizontalPadding = 10.dp * 2 + 10.dp * (maxVisibleTiles - 1)
    val targetWidth = ((screenWidth - horizontalPadding) / maxVisibleTiles).coerceIn(76.dp, 110.dp)
    val panelBackground = controlPanelBackground()
    val tileHeight = (targetWidth - 20.dp).coerceAtLeast(60.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(panelBackground)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.mode),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(tileHeight + 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            items(modeItems) { (id, title, iconRes) ->
                val isSelected = id == selectedMode
                SelectionTile(
                    title = title,
                    painter = painterResource(id = iconRes),
                    selected = isSelected,
                    enabled = isEnabled,
                    brand = brand,
                    modifier = Modifier
                        .width(targetWidth)
                        .height(tileHeight),
                    onClick = { onSelectMode(id) }
                )
            }
        }
    }
}

@Composable
private fun SelectionTile(
    title: String,
    icon: ImageVector? = null,
    painter: Painter? = null,
    selected: Boolean,
    enabled: Boolean,
    brand: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background = if (selected) {
        brand
    } else {
        brand.copy(alpha = 0.08f)
    }
    val contentColor = if (selected) Color.White else brand
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                painter != null -> {
                    Icon(
                        painter = painter,
                        contentDescription = null,
                        tint = contentColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                icon != null -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun  LevelControlSection(
    level: Int,
    isConnected: Boolean,
    brand: Color,
    onPreviewLevel: (Int) -> Unit,
    onCommitLevel: (Int) -> Unit
) {
    val sliderInteractionSource = remember { MutableInteractionSource() }
    var sliderValue by remember { mutableStateOf(level.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(level) {
        if (!isDragging) {
            sliderValue = level.toFloat()
        }
    }

    val displayValue = sliderValue.roundToInt().coerceIn(0, 19)
    val panelBackground = controlPanelBackground()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(panelBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Column(
                modifier = Modifier.widthIn(min = 60.dp, max = 120.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.device_level_label),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.massagerExtendedColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$displayValue / 0-19",
                    style = MaterialTheme.typography.bodyMedium,
                    color = brand,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            LevelAdjustButton(
                icon = Icons.Filled.Remove,
                tint = brand,
                enabled = isConnected,
                onClick = {
                    val next = (displayValue - 1).coerceIn(0, 19)
                    sliderValue = next.toFloat()
                    onPreviewLevel(next)
                    onCommitLevel(next)
                }
            )
            Slider(
                value = sliderValue,
                onValueChange = { next ->
                    isDragging = true
                    sliderValue = next.coerceIn(0f, 19f)
                    onPreviewLevel(sliderValue.roundToInt())
                },
                onValueChangeFinished = {
                    isDragging = false
                    sliderValue = displayValue.toFloat()
                    onCommitLevel(displayValue)
                },
                valueRange = 0f..19f,
                steps = 18,
                enabled = isConnected,
                modifier = Modifier.weight(1f),
                interactionSource = sliderInteractionSource,
                thumb = {
                    LevelSliderThumb(
                        enabled = isConnected,
                        interactionSource = sliderInteractionSource
                    )
                },
                track = { positions ->
                    LevelSliderTrack(
                        sliderPositions = positions,
                        enabled = isConnected,
                        activeColor = brand
                    )
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    disabledThumbColor = Color.White.copy(alpha = 0.6f)
                )
            )
            LevelAdjustButton(
                icon = Icons.Filled.Add,
                tint = brand,
                enabled = isConnected,
                onClick = {
                    val next = (displayValue + 1).coerceIn(0, 19)
                    sliderValue = next.toFloat()
                    onPreviewLevel(next)
                    onCommitLevel(next)
                }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LevelSliderTrack(
    sliderPositions: SliderPositions,
    enabled: Boolean,
    activeColor: Color
) {
    val trackHeight = 8.dp
    val cornerRadius = trackHeight / 2
    val baseInactive = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    val activeColorFinal = if (enabled) activeColor else activeColor.copy(alpha = 0.35f)
    val inactiveColor = if (enabled) baseInactive else baseInactive.copy(alpha = 0.35f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(trackHeight)
    ) {
        val trackHeightPx = trackHeight.toPx()
        val corner = cornerRadius.toPx()
        drawRoundRect(
            color = inactiveColor,
            cornerRadius = CornerRadius(corner, corner),
            size = Size(size.width, trackHeightPx)
        )
        val activeStart = sliderPositions.activeRange.start * size.width
        val activeEnd = sliderPositions.activeRange.endInclusive * size.width
        drawRoundRect(
            color = activeColorFinal,
            topLeft = Offset(activeStart, 0f),
            size = Size((activeEnd - activeStart).coerceAtLeast(0f), trackHeightPx),
            cornerRadius = CornerRadius(corner, corner)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LevelSliderThumb(
    enabled: Boolean,
    interactionSource: MutableInteractionSource
) {
    SliderDefaults.Thumb(
        interactionSource = interactionSource,
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            disabledThumbColor = Color.White.copy(alpha = 0.6f)
        )
    )
}

@Composable
private fun LevelAdjustButton(
    icon: ImageVector,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) tint else tint.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun controlPanelBackground(): Color = MaterialTheme.massagerExtendedColors.cardBackground
