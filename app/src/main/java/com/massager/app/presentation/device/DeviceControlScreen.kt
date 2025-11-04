package com.massager.app.presentation.device

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.massager.app.R
import androidx.compose.material3.SnackbarHostState

private val AccentRed = Color(0xFFE54335)
private val RenameTeal = Color(0xFF16A085)

@Composable
fun DeviceControlScreen(
    viewModel: DeviceControlViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    DeviceControlContent(
        state = state,
        onBack = onBack,
        onReconnect = viewModel::reconnect,
        onSelectZone = viewModel::selectZone,
        onSelectMode = viewModel::selectMode,
        onSelectTimer = viewModel::selectTimer,
        onIncreaseLevel = viewModel::increaseLevel,
        onDecreaseLevel = viewModel::decreaseLevel,
        onToggleMute = viewModel::toggleMute,
        onToggleSession = viewModel::toggleSession,
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
    onReconnect: () -> Unit,
    onSelectZone: (BodyZone) -> Unit,
    onSelectMode: (Int) -> Unit,
    onSelectTimer: (Int) -> Unit,
    onIncreaseLevel: () -> Unit,
    onDecreaseLevel: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSession: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onConsumeMessage: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    var showInfoDialog by remember { mutableStateOf(false) }
    var timerDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        when (val message = state.message) {
            is DeviceMessage.SessionStarted -> {
                val text = context.getString(
                    R.string.device_session_started,
                    message.level,
                    context.getString(modeLabelRes(message.mode))
                )
                snackbarHostState.showSnackbar(text)
            }

            DeviceMessage.SessionStopped -> {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.device_session_stopped)
                )
            }

            DeviceMessage.BatteryLow -> {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.device_battery_low)
                )
            }

            is DeviceMessage.MuteChanged -> {
                val resId = if (message.enabled) {
                    R.string.device_mute_enabled
                } else {
                    R.string.device_mute_disabled
                }
                snackbarHostState.showSnackbar(context.getString(resId))
            }

            is DeviceMessage.CommandFailed -> {
                snackbarHostState.showSnackbar(context.getString(message.messageRes))
            }

            null -> Unit
        }
        if (state.message != null) {
            onConsumeMessage()
        }
    }

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
                    containerColor = Color(0xFFF7F7F7)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF7F7F7)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DeviceDisplaySection(
                state = state,
                onReconnect = onReconnect,
                onToggleMute = onToggleMute
            )
            BodyZoneTabs(
                selectedZone = state.zone,
                isRunning = state.isRunning,
                onSelectZone = onSelectZone
            )
            ModeSelectionGrid(
                selectedMode = state.mode,
                isRunning = state.isRunning,
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
    onToggleMute: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = state.deviceName.ifBlank { stringResource(id = R.string.device_title) },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
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
                        color = if (state.isConnected) Color(0xFF4CAF50) else AccentRed
                    )
                }
                IconButton(
                    onClick = onReconnect,
                    enabled = !state.isConnected,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            color = if (state.isConnected) Color(0xFFE0E0E0) else Color(0xFFFFEBEE)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(id = R.string.device_reconnect),
                        tint = if (state.isConnected) Color.Gray else AccentRed
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(160.dp),
                    shape = CircleShape,
                    color = Color(0xFFF2F2F2)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_massager_logo),
                        contentDescription = null,
                        modifier = Modifier.padding(36.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = if (state.isMuted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                        contentDescription = if (state.isMuted) {
                            stringResource(id = R.string.device_mute_disabled)
                        } else {
                            stringResource(id = R.string.device_mute_enabled)
                        },
                        tint = AccentRed
                    )
                }
            }
            BatteryStatusRow(batteryPercent = state.batteryPercent)
        }
    }
}

@Composable
private fun BatteryStatusRow(batteryPercent: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF9F9F9))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.BatteryFull,
            contentDescription = null,
            tint = if (batteryPercent > 20) Color(0xFF4CAF50) else Color(0xFFE53935)
        )
        Text(
            text = stringResource(id = R.string.device_battery_label, batteryPercent),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BodyZoneTabs(
    selectedZone: BodyZone,
    isRunning: Boolean,
    onSelectZone: (BodyZone) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BodyZone.values().forEach { zone ->
            val isSelected = zone == selectedZone
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(enabled = !isRunning) { onSelectZone(zone) },
                color = if (isSelected) AccentRed else Color.Transparent,
                tonalElevation = if (isSelected) 4.dp else 0.dp
            ) {
                Text(
                    text = stringResource(id = zone.labelRes),
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .align(Alignment.CenterVertically),
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ModeSelectionGrid(
    selectedMode: Int,
    isRunning: Boolean,
    onSelectMode: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
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
                            enabled = !isRunning,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) AccentRed else Color(0xFFF4F4F4),
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
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
        color = Color.White
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
                            color = Color(0xFFE54335)
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
                color = if (enabled) Color(0xFFFFF0EF) else Color(0xFFE0E0E0),
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) Color(0xFFE54335) else Color.Gray
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
                .background(Color.White)
                .clickable(enabled = !state.isRunning, onClick = onToggleTimerMenu)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Outlined.Timer, contentDescription = null, tint = Color(0xFFE54335))
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
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isRunning) AccentRed else RenameTeal,
                contentColor = Color.White
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
