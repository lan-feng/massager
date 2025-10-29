package com.massager.app.presentation.device

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
                    message.mode
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
                        text = stringResource(id = R.string.device_title),
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DeviceDisplaySection(
                batteryPercent = state.batteryPercent,
                isMuted = state.isMuted,
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
                isRunning = state.isRunning,
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
    batteryPercent: Int,
    isMuted: Boolean,
    onToggleMute: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .shadow(6.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                modifier = Modifier
                    .padding(20.dp)
                    .height(180.dp)
                    .fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier
                        .size(150.dp)
                        .align(Alignment.Center),
                    color = Color(0xFFF0F0F0),
                    shape = CircleShape,
                    tonalElevation = 6.dp
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_massager_logo),
                        contentDescription = stringResource(R.string.device_title),
                        modifier = Modifier.padding(32.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                        contentDescription = if (isMuted) {
                            stringResource(R.string.device_unmute)
                        } else {
                            stringResource(R.string.device_mute)
                        }
                    )
                }
            }
            BatteryStatusRow(batteryPercent = batteryPercent)
        }
        DottedPlaceholder(
            modifier = Modifier
                .width(120.dp)
                .height(180.dp),
            label = stringResource(id = R.string.device_add_placeholder)
        )
    }
}

@Composable
private fun BatteryStatusRow(batteryPercent: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
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
private fun DottedPlaceholder(
    modifier: Modifier = Modifier,
    label: String
) {
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .drawBehind {
                val strokeWidth = 3.dp.toPx()
                drawRoundRect(
                    color = Color(0xFFE0E0E0),
                    style = Stroke(width = strokeWidth, pathEffect = dashEffect),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx())
                )
            }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = Color(0xFFE53935),
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
                color = if (isSelected) Color(0xFFE54335) else Color.Transparent,
                tonalElevation = if (isSelected) 4.dp else 0.dp
            ) {
                Text(
                    text = when (zone) {
                        BodyZone.SHLDR -> stringResource(id = R.string.device_zone_shldr)
                        BodyZone.WAIST -> stringResource(id = R.string.device_zone_waist)
                        BodyZone.LEGS -> stringResource(id = R.string.device_zone_legs)
                        BodyZone.ARMS -> stringResource(id = R.string.device_zone_arms)
                        BodyZone.JC -> stringResource(id = R.string.device_zone_jc)
                    },
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
            val modeRows = (1..5).toList().chunked(3)
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
                                containerColor = if (isSelected) Color(0xFFE54335) else Color(0xFFF4F4F4),
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            Text(
                                text = stringResource(id = R.string.device_mode_label, mode),
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
    isRunning: Boolean,
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
                    enabled = isRunning && level > 0,
                    onClick = onDecrease
                )
                AnimatedContent(
                    targetState = level,
                    transitionSpec = { fadeIn() with fadeOut() },
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
                    enabled = isRunning && level < 20,
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
                    text = if (state.isRunning && state.remainingSeconds > 0) {
                        formatMinutes(state.remainingSeconds)
                    } else if (state.timerMinutes == 0) {
                        stringResource(id = R.string.device_timer_placeholder)
                    } else {
                        "${state.timerMinutes} ${stringResource(id = R.string.device_minutes_suffix)}"
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
                                text = "$minutes ${stringResource(id = R.string.device_minutes_suffix)}"
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
                containerColor = if (state.isRunning) Color(0xFFE54335) else Color(0xFFFFC107),
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

private fun formatMinutes(remainingSeconds: Int): String {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
