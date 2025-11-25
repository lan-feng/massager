package com.massager.app.presentation.device

// 文件说明：设备控制 UI，提供模式、强度等交互。
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import androidx.compose.material3.SnackbarHostState
import com.massager.app.presentation.theme.massagerExtendedColors


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
        onIncreaseLevel = viewModel::increaseLevel,
        onDecreaseLevel = viewModel::decreaseLevel,
        onToggleMute = viewModel::toggleMute,
        onToggleSession = viewModel::toggleSession,
        onRenameAttached = viewModel::renameAttachedDevice,
        onRemoveAttached = viewModel::removeAttachedDevice,
        snackbarHostState = snackbarHostState,
        onConsumeMessage = viewModel::consumeMessage
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class
)
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
    onIncreaseLevel: () -> Unit,
    onDecreaseLevel: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSession: () -> Unit,
    onRenameAttached: (String, String) -> Unit,
    onRemoveAttached: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    onConsumeMessage: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var showInfoDialog by remember { mutableStateOf(false) }
    var timerDropdownExpanded by remember { mutableStateOf(false) }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                onAddDevice = { onAddDevice(excludedSerials) }
            )
            AnimatedVisibility(visible = state.isComboUpdating) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
            DeviceDisplaySection(
                state = state,
                onReconnect = onReconnect,
                onToggleMute = onToggleMute,
                modifier = Modifier.fillMaxWidth()
            )
            BodyZoneTabs(
                selectedZone = state.zone,
                onSelectZone = onSelectZone
            )
            ModeSelectionGrid(
                selectedMode = state.mode,
                isEnabled = state.isProtocolReady && state.isConnected,
                onSelectMode = onSelectMode
            )
            LevelControlSection(
                level = state.level,
                isConnected = state.isConnected,
                onIncrease = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onIncreaseLevel()
                },
                onDecrease = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDecreaseLevel()
                }
            )
            TimerActionSection(
                state = state,
                timerDropdownExpanded = timerDropdownExpanded,
                onToggleTimerMenu = { timerDropdownExpanded = !timerDropdownExpanded },
                onDismissTimerMenu = { timerDropdownExpanded = false },
                onSelectTimer = {
                    timerDropdownExpanded = false
                    onSelectTimer(it)
                },
                onToggleSession = onToggleSession
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
private fun DeviceDisplaySection(
    state: DeviceControlUiState,
    onReconnect: () -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.massagerExtendedColors.surfaceBright)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = state.deviceName.ifBlank { stringResource(id = R.string.device_title) },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(
                                id = if (state.isConnected) {
                                    R.string.device_status_connected
                                } else {
                                    R.string.device_status_disconnected
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.isConnected) {
                                MaterialTheme.massagerExtendedColors.success
                            } else {
                                MaterialTheme.massagerExtendedColors.danger
                            }
                        )
                    }
                    IconButton(
                        onClick = onReconnect,
                        enabled = !state.isConnected && !state.isConnecting,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                color = when {
                                    state.isConnected -> MaterialTheme.massagerExtendedColors.surfaceStrong
                                    state.isConnecting -> MaterialTheme.massagerExtendedColors.danger.copy(alpha = 0.15f)
                                    else -> MaterialTheme.massagerExtendedColors.danger.copy(alpha = 0.08f)
                                }
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(id = R.string.device_reconnect),
                            tint = when {
                                state.isConnected -> MaterialTheme.massagerExtendedColors.iconMuted
                                state.isConnecting -> MaterialTheme.massagerExtendedColors.danger.copy(alpha = 0.6f)
                                else -> MaterialTheme.massagerExtendedColors.danger
                            }
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp, max = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val deviceBoxSize = 96.dp
                    val cornerRadius = 12.dp
                    Box(
                        modifier = Modifier
                            .size(deviceBoxSize)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(cornerRadius),
                                clip = false
                            )
                            .clip(RoundedCornerShape(cornerRadius))
                            .background(MaterialTheme.massagerExtendedColors.surfaceBright),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_massager_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                BatteryStatusRow(
                    batteryPercent = state.batteryPercent,
                    isMuted = state.isMuted,
                    onToggleMute = onToggleMute
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
    onAddDevice: () -> Unit
) {
    val cardHeight = 120.dp
    val cardSpacing = 12.dp
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight),
        horizontalArrangement = Arrangement.spacedBy(cardSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(cards, key = { card ->
            card.deviceSerial ?: if (card.isMainDevice) "main-device" else card.displayName
        }) { card ->
            DeviceSwitcherCard(
                card = card,
                modifier = Modifier
                    .height(cardHeight),
                onSelect = { onSelect(card.deviceSerial) },
                onLongPress = { onLongPress(card) }
            )
        }
        item {
            AddDeviceBox(
                onAddDevice = onAddDevice,
                size = cardHeight,
                corner = 16.dp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceSwitcherCard(
    card: DeviceCardState,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
    onLongPress: () -> Unit
) {
    val selectedColor = MaterialTheme.massagerExtendedColors.surfaceBright
    val unselectedColor = MaterialTheme.massagerExtendedColors.surfaceSubtle
    Surface(
        modifier = modifier
            .width(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onSelect,
                onLongClick = onLongPress
            ),
        color = if (card.isSelected) selectedColor else unselectedColor,
        tonalElevation = if (card.isSelected) 6.dp else 0.dp,
        shadowElevation = if (card.isSelected) 8.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = card.displayName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
        }
    }
}

@Composable
private fun AddDeviceBox(
    onAddDevice: () -> Unit,
    size: Dp = 88.dp,
    corner: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.massagerExtendedColors.surfaceBright)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BodyZone.values().forEach { zone ->
            val isSelected = zone == selectedZone
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelectZone(zone) },
                color = if (isSelected) MaterialTheme.massagerExtendedColors.danger else Color.Transparent,
                tonalElevation = if (isSelected) 4.dp else 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = zone.labelRes),
                        color = if (isSelected) {
                            MaterialTheme.massagerExtendedColors.textOnAccent
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(min = 72.dp),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeSelectionGrid(
    selectedMode: Int,
    isEnabled: Boolean,
    onSelectMode: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.massagerExtendedColors.surfaceBright,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.mode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            val modeRows = (0..7).toList().chunked(3)
            modeRows.forEachIndexed { rowIndex, rowModes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowModes.forEach { mode ->
                        val isSelected = mode == selectedMode
                        Button(
                            onClick = { onSelectMode(mode) },
                            enabled = isEnabled,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.massagerExtendedColors.danger
                                } else {
                                    MaterialTheme.massagerExtendedColors.surfaceSubtle
                                },
                                contentColor = if (isSelected) {
                                    MaterialTheme.massagerExtendedColors.textOnAccent
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            Text(
                                text = stringResource(id = modeLabelRes(mode)),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (rowModes.size < 3) {
                        repeat(3 - rowModes.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                if (rowIndex != modeRows.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun LevelControlSection(
    level: Int,
    isConnected: Boolean,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.massagerExtendedColors.surfaceBright
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.device_level_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlCircularButton(
                    icon = Icons.Filled.Remove,
                    enabled = isConnected && level > 0,
                    onClick = onDecrease
                )
                AnimatedContent(
                    targetState = level,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "level-counter"
                ) { value ->
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.massagerExtendedColors.danger
                        )
                    )
                }
                ControlCircularButton(
                    icon = Icons.Filled.Add,
                    enabled = isConnected && level < 19,
                    onClick = onIncrease
                )
            }
        }
    }
}

@Composable
private fun ControlCircularButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(56.dp)
            .background(
                color = if (enabled) {
                    MaterialTheme.massagerExtendedColors.accentSoft
                } else {
                    MaterialTheme.massagerExtendedColors.surfaceStrong
                },
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) {
                MaterialTheme.massagerExtendedColors.danger
            } else {
                MaterialTheme.massagerExtendedColors.iconMuted
            }
        )
    }
}

@Composable
private fun TimerActionSection(
    state: DeviceControlUiState,
    timerDropdownExpanded: Boolean,
    onToggleTimerMenu: () -> Unit,
    onDismissTimerMenu: () -> Unit,
    onSelectTimer: (Int) -> Unit,
    onToggleSession: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.massagerExtendedColors.surfaceBright)
                .clickable(enabled = !state.isRunning, onClick = onToggleTimerMenu)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.massagerExtendedColors.danger
                )
                Text(
                    text = when {
                        state.isRunning && state.remainingSeconds > 0 -> formatDuration(state.remainingSeconds)
                        state.timerMinutes > 0 -> stringResource(
                            id = R.string.device_timer_minutes_format,
                            state.timerMinutes
                        )
                        else -> stringResource(id = R.string.device_timer_placeholder)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            DropdownMenu(
                expanded = timerDropdownExpanded,
                onDismissRequest = onDismissTimerMenu
            ) {
                state.availableTimerOptions.forEach { minutes ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(
                                    id = R.string.device_timer_minutes_format,
                                    minutes
                                )
                            )
                        },
                        onClick = { onSelectTimer(minutes) }
                    )
                }
            }
        }

        Button(
            onClick = onToggleSession,
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
            enabled = state.isProtocolReady && state.isConnected,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isRunning) {
                    MaterialTheme.massagerExtendedColors.danger
                } else {
                    MaterialTheme.massagerExtendedColors.success
                },
                contentColor = MaterialTheme.massagerExtendedColors.textOnAccent
            )
        ) {
            Text(
                text = if (state.isRunning) {
                    stringResource(id = R.string.device_stop)
                } else {
                    stringResource(id = R.string.device_start)
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
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
