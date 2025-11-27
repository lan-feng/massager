package com.massager.app.presentation.device

// 文件说明：设备控制 UI，提供模式、强度等交互。
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderPositions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.massager.app.R
import com.massager.app.presentation.theme.massagerExtendedColors
import kotlin.math.roundToInt
import kotlin.math.atan2
import kotlin.math.PI


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
    onReconnect: () -> Unit,
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
    val brand = Color(0xFF2BA39D)
    val brandSoft = Color(0xFF3FB6AE)
    var showInfoDialog by remember { mutableStateOf(false) }
    var manageTarget by remember { mutableStateOf<DeviceCardState?>(null) }
    var renameTarget by remember { mutableStateOf<DeviceCardState?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var renameErrorRes by remember { mutableStateOf<Int?>(null) }
    var deleteTarget by remember { mutableStateOf<DeviceCardState?>(null) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.deviceName.ifBlank { stringResource(id = R.string.device_title) },
                        style = MaterialTheme.typography.titleLarge
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.massagerExtendedColors.surfaceSubtle
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            val excludedSerials = remember(state.deviceCards) {
                val hostSerial = state.deviceCards.firstOrNull { it.isMainDevice }?.deviceSerial
                val combos = state.deviceCards
                    .filterNot { it.isMainDevice }
                    .mapNotNull { it.deviceSerial }
                buildList {
                    hostSerial?.let { add(it) }
                    addAll(combos)
                }
            }
            DeviceSwitcherRow(
                cards = state.deviceCards,
                onSelect = onSelectDevice,
                onLongPress = { card ->
                    if (!card.isMainDevice && card.deviceSerial != null) {
                        manageTarget = card
                    }
                },
                onAddDevice = { onAddDevice(excludedSerials) },
                batteryPercent = state.batteryPercent,
                isMuted = state.isMuted,
                onToggleMute = onToggleMute,
                isConnected = state.isConnected,
                isConnecting = state.isConnecting,
                onReconnect = onReconnect
            )
            AnimatedVisibility(visible = state.isComboUpdating) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
            TimerRing(
                isRunning = state.isRunning,
                remainingSeconds = state.remainingSeconds,
                timerMinutes = state.timerMinutes,
                brand = brand,
                brandSoft = brandSoft,
                enabled = state.isProtocolReady && state.isConnected,
                onSelectTimer = { minutes -> onSelectTimer(minutes.coerceIn(0, 60)) },
                onToggleSession = onToggleSession
            )
            LevelControlSection(
                level = state.level,
                isConnected = state.isConnected,
                brand = brand,
                onPreviewLevel = onPreviewLevel,
                onCommitLevel = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCommitLevel(it)
                }
            )
            ModeSelectionGrid(
                selectedMode = state.mode,
                isEnabled = state.isProtocolReady && state.isConnected,
                onSelectMode = onSelectMode,
                brand = brand
            )
            BodyZoneGrid(
                selectedZone = state.zone,
                onSelectZone = onSelectZone,
                brand = brand
            )
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
}

@Composable
private fun TimerRing(
    isRunning: Boolean,
    remainingSeconds: Int,
    timerMinutes: Int,
    brand: Color,
    brandSoft: Color,
    enabled: Boolean,
    onSelectTimer: (Int) -> Unit,
    onToggleSession: () -> Unit
) {
    val baseMinutes = timerMinutes.coerceIn(0, 60)
    val remainingMinutes = if (remainingSeconds > 0) (remainingSeconds + 59) / 60 else baseMinutes
    var isDragging by remember { mutableStateOf(false) }
    var selectedMinutes by remember { mutableStateOf(baseMinutes.coerceIn(0, 60)) }
    var committedMinutes by remember { mutableStateOf(baseMinutes.coerceIn(0, 60)) }

    LaunchedEffect(isRunning, baseMinutes, remainingMinutes) {
        if (!isDragging) {
            if (!isRunning) {
                committedMinutes = baseMinutes
                selectedMinutes = baseMinutes
            } else {
                selectedMinutes = remainingMinutes
            }
        }
    }

    val ringSize = 165.dp
    val stroke = 12.dp
    val displayMinutes = if (isRunning) remainingMinutes else selectedMinutes
    val arcMinutes = when {
        isDragging -> selectedMinutes
        else -> committedMinutes
    }.coerceIn(0, 60)
    val progress = (arcMinutes.toFloat() / 60f).coerceIn(0f, 1f)
    val interactionEnabled = enabled && !isRunning
    val ringSizePx = with(LocalDensity.current) { ringSize.toPx() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(ringSize)
                .pointerInput(interactionEnabled) {
                    if (!interactionEnabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            committedMinutes = selectedMinutes.coerceIn(0, 60)
                            onSelectTimer(selectedMinutes.coerceIn(0, 60))
                        },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, _ ->
                            val center = Offset(ringSizePx / 2f, ringSizePx / 2f)
                            val vector = change.position - center
                            val angle = (Math.toDegrees(atan2(vector.y.toDouble(), vector.x.toDouble())) + 450.0) % 360.0
                            val minutes = (angle / 360.0 * 60.0).toInt().coerceIn(0, 60)
                            selectedMinutes = minutes
                        }
                    )
                }
        ) {
            val diameter = size.minDimension
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = brand.copy(alpha = 0.12f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = brandSoft,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .size(ringSize * 0.7f)
                .clip(CircleShape)
                .clickable(
                    enabled = enabled,
                    onClick = onToggleSession
                ),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${displayMinutes.coerceAtLeast(0)} min",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isRunning) stringResource(id = R.string.device_stop) else stringResource(id = R.string.device_start),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isRunning) {
                        MaterialTheme.massagerExtendedColors.danger
                    } else {
                        brand
                    },
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun DeviceSwitcherRow(
    cards: List<DeviceCardState>,
    onSelect: (String?) -> Unit,
    onLongPress: (DeviceCardState) -> Unit,
    onAddDevice: () -> Unit,
    batteryPercent: Int,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    isConnected: Boolean,
    isConnecting: Boolean,
    onReconnect: () -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val cardSpacing = 14.dp
    val horizontalPadding = 8.dp
    val availableWidth = (screenWidth - horizontalPadding * 2 - cardSpacing*2).coerceAtLeast(200.dp)
    val selectedWidth = availableWidth * 0.6f
    val unselectedWidth = availableWidth * 0.35f
    val cardHeight = 120.dp
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
            DeviceSwitcherCard(
                card = card,
                batteryPercent = batteryPercent,
                isMuted = isMuted,
                onToggleMute = onToggleMute,
                isConnected = isConnected,
                isConnecting = isConnecting,
                onReconnect = onReconnect,
                selectedWidth = selectedWidth,
                unselectedWidth = unselectedWidth,
                modifier = Modifier
                    .height(cardHeight),
                onSelect = { onSelect(card.deviceSerial) },
                onLongPress = { onLongPress(card) }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceSwitcherCard(
    card: DeviceCardState,
    batteryPercent: Int,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    isConnected: Boolean,
    isConnecting: Boolean,
    onReconnect: () -> Unit,
    selectedWidth: Dp,
    unselectedWidth: Dp,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
    onLongPress: () -> Unit
) {
    val selectedColor = MaterialTheme.massagerExtendedColors.surfaceBright
    val unselectedColor = Color(0xFF2BA39D).copy(alpha = 0.12f)
    val accent = Color(0xFF2BA39D)
    val displayText = if (batteryPercent < 0) "--%" else "${batteryPercent}%"
    val width by animateDpAsState(
        targetValue = if (card.isSelected) selectedWidth else unselectedWidth,
        label = "device_switcher_width"
    )
    Box(
        modifier = modifier.width(width)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .combinedClickable(
                    onClick = onSelect,
                    onLongClick = onLongPress
                ),
            color = if (card.isSelected) selectedColor else unselectedColor,
            tonalElevation = if (card.isSelected) 8.dp else 0.dp,
            shadowElevation = if (card.isSelected) 10.dp else 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (card.isSelected) accent else MaterialTheme.massagerExtendedColors.iconMuted)
                    )
                    Text(
                        text = stringResource(
                            id = if (card.isMainDevice) {
                                R.string.device_combo_main_label
                            } else {
                                R.string.device_combo_member_label
                            }
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.massagerExtendedColors.textMuted
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Filled.Bluetooth,
                        contentDescription = null,
                        tint = if (isConnected) accent else MaterialTheme.massagerExtendedColors.iconMuted
                    )
                }
                Spacer(modifier = Modifier.weight(1f, fill = true))
                if (card.isSelected) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.BatteryFull,
                                contentDescription = null,
                                tint = when {
                                    batteryPercent < 0 -> MaterialTheme.massagerExtendedColors.iconMuted
                                    batteryPercent > 20 -> MaterialTheme.massagerExtendedColors.success
                                    else -> accent
                                }
                            )
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.massagerExtendedColors.surfaceBright)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
                                contentDescription = if (isMuted) {
                                    stringResource(id = R.string.device_mute_enabled)
                                } else {
                                    stringResource(id = R.string.device_mute_disabled)
                                },
                                tint = accent
                            )
                        }
                    }
                } else if (isConnected) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BatteryFull,
                            contentDescription = null,
                            tint = when {
                                batteryPercent < 0 -> MaterialTheme.massagerExtendedColors.iconMuted
                                batteryPercent > 20 -> MaterialTheme.massagerExtendedColors.success
                                else -> accent
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Bluetooth,
                                contentDescription = null,
                                tint = MaterialTheme.massagerExtendedColors.iconMuted
                            )
                            Text(
                                text = stringResource(id = R.string.device_status_disconnected),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.massagerExtendedColors.textMuted
                            )
                        }
                        IconButton(
                            onClick = onReconnect,
                            enabled = !isConnecting,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.massagerExtendedColors.surfaceBright)
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = accent
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = stringResource(id = R.string.device_reconnect),
                                    tint = accent
                                )
                            }
                        }
                    }
                }
            }
        }
        val showOverlay = card.isMainDevice && !isConnected
        if (showOverlay) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
                    .clickable(enabled = !isConnecting, onClick = onReconnect),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.massagerExtendedColors.surfaceBright,
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = accent
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Bluetooth,
                                contentDescription = null,
                                tint = accent
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.device_reconnect),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
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
private fun BatteryStatusRow(
    batteryPercent: Int,
    isMuted: Boolean,
    onToggleMute: () -> Unit
) {
    val isUnknown = batteryPercent < 0
    val displayText = if (isUnknown) "--%" else "${batteryPercent}%"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.massagerExtendedColors.surfaceBright)
            .border(
                1.dp,
                MaterialTheme.massagerExtendedColors.surfaceStrong.copy(alpha = 0.35f),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.BatteryFull,
            contentDescription = null,
            tint = when {
                isUnknown -> MaterialTheme.massagerExtendedColors.iconMuted
                batteryPercent > 20 -> MaterialTheme.massagerExtendedColors.success
                else -> MaterialTheme.massagerExtendedColors.danger
            }
        )
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = onToggleMute,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.massagerExtendedColors.surfaceBright)
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
                contentDescription = if (isMuted) {
                    stringResource(id = R.string.device_mute_enabled)
                } else {
                    stringResource(id = R.string.device_mute_disabled)
                },
                tint = MaterialTheme.massagerExtendedColors.danger
            )
        }
    }
}

@Composable
private fun BodyZoneTabs(
    selectedZone: BodyZone,
    onSelectZone: (BodyZone) -> Unit
) {
    BodyZoneGrid(
        selectedZone = selectedZone,
        onSelectZone = onSelectZone,
        brand = MaterialTheme.massagerExtendedColors.danger
    )
}

@Composable
private fun BodyZoneGrid(
    selectedZone: BodyZone,
    onSelectZone: (BodyZone) -> Unit,
    brand: Color
) {
    val zoneItems = listOf(
        BodyZone.SHOULDER to R.drawable.ic_deck,
        BodyZone.WAIST to R.drawable.ic_airline_seat_recline_normal,
        BodyZone.LEGS to R.drawable.ic_hiking,
        BodyZone.ARMS to R.drawable.ic_sports_gymnastics,
        BodyZone.JOINT to R.drawable.ic_settings_accessibility,
        BodyZone.BODY to R.drawable.ic_person
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(id = R.string.body_part_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(zoneItems) { (zone, iconRes) ->
                val isSelected = zone == selectedZone
                SelectionTile(
                    title = stringResource(id = zone.labelRes),
                    painter = painterResource(id = iconRes),
                    selected = isSelected,
                    enabled = true,
                    brand = brand,
                    modifier = Modifier.height(80.dp),
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(id = R.string.mode),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(modeItems) { (id, title, iconRes) ->
                val isSelected = id == selectedMode
                SelectionTile(
                    title = title,
                    painter = painterResource(id = iconRes),
                    selected = isSelected,
                    enabled = isEnabled,
                    brand = brand,
                    modifier = Modifier.height(84.dp),
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
private fun                                            LevelControlSection(
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.device_level_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${stringResource(id = R.string.device_level_label)}: $displayValue",
                style = MaterialTheme.typography.titleMedium,
                color = brand,
                fontWeight = FontWeight.Bold
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.massagerExtendedColors.textMuted
            )
            Spacer(modifier = Modifier.width(12.dp))
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
                        enabled = isConnected
                    )
                },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.massagerExtendedColors.surfaceBright,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    disabledThumbColor = MaterialTheme.massagerExtendedColors.surfaceSubtle
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "19",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.massagerExtendedColors.textMuted
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LevelSliderTrack(
    sliderPositions: SliderPositions,
    enabled: Boolean
) {
    val trackHeight = 12.dp
    val cornerRadius = trackHeight / 2
    val baseActive = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val baseInactive = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val activeColor = if (enabled) baseActive else baseActive.copy(alpha = 0.4f)
    val inactiveColor = if (enabled) baseInactive else baseInactive.copy(alpha = 0.5f)

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
            color = activeColor,
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
    val thumbColor = Color(0xFF2BA39D)
    SliderDefaults.Thumb(
        interactionSource = interactionSource,
        colors = SliderDefaults.colors(
            thumbColor = if (enabled) thumbColor else thumbColor.copy(alpha = 0.4f),
            disabledThumbColor = thumbColor.copy(alpha = 0.4f)
        )
    )
}

@StringRes
private fun modeLabelRes(mode: Int): Int = when (mode.coerceIn(0, 7)) {
    0 -> R.string.device_mode_0
    1 -> R.string.device_mode_1
    2 -> R.string.device_mode_2
    3 -> R.string.device_mode_3
    4 -> R.string.device_mode_4
    5 -> R.string.device_mode_5
    6 -> R.string.device_mode_6
    else -> R.string.device_mode_7
}

private fun formatDuration(remainingSeconds: Int): String {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
