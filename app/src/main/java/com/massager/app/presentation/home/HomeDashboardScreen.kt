package com.massager.app.presentation.home

// 文件说明：Compose 首页仪表盘界面，呈现设备状态与健康数据。
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import com.massager.app.R
import com.massager.app.domain.model.DeviceMetadata
import com.massager.app.presentation.components.AppBottomNavigation
import com.massager.app.presentation.components.ThemedSnackbarHost
import com.massager.app.presentation.theme.massagerExtendedColors
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

private data class SelectedCardLayout(val offset: Offset, val size: IntSize)

@Composable
fun HomeDashboardScreen(
    state: HomeUiState,
    effects: SharedFlow<HomeEffect>,
    currentTab: AppBottomTab,
    onAddDevice: () -> Unit,
    onDeviceToggle: (DeviceMetadata) -> Unit,
    onDeviceOpen: (DeviceMetadata) -> Unit,
    onRenameClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onCancelManagement: () -> Unit,
    onRenameInputChanged: (String) -> Unit,
    onRenameConfirm: () -> Unit,
    onRenameDismiss: () -> Unit,
    onRemoveConfirm: () -> Unit,
    onRemoveDismiss: () -> Unit,
    onDismissError: () -> Unit,
    onTabSelected: (AppBottomTab) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val focusedDeviceId = state.selectedDeviceIds.firstOrNull()
    val focusedDevice = state.devices.firstOrNull { it.id == focusedDeviceId }
    var selectedCardLayout by remember { mutableStateOf<SelectedCardLayout?>(null) }
    var contentRootOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(effects) {
        effects.collectLatest { effect ->
            when (effect) {
                is HomeEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(context.getString(effect.messageRes))
                }

                is HomeEffect.ShowMessageText -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    LaunchedEffect(state.errorMessageRes, state.errorMessageText) {
        val message = state.errorMessageText
            ?: state.errorMessageRes?.let { context.getString(it) }
        message?.let {
            snackbarHostState.showSnackbar(message)
            onDismissError()
        }
    }

    LaunchedEffect(focusedDeviceId) {
        if (focusedDeviceId == null) {
            selectedCardLayout = null
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            ThemedSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 72.dp)
            )
        },
        bottomBar = {
            AppBottomNavigation(
                currentTab = currentTab,
                onTabSelected = onTabSelected
            )
        }
    ) { padding ->
        val backgroundColor = MaterialTheme.colorScheme.background
        val scrimBase = MaterialTheme.colorScheme.onSurface
        val isFocusVisible = focusedDevice != null
        val blurRadius by animateDpAsState(targetValue = if (isFocusVisible) 12.dp else 0.dp, animationSpec = tween(220), label = "focusBlur")
        val scrimAlpha by animateFloatAsState(targetValue = if (isFocusVisible) 0.35f else 0f, animationSpec = tween(200), label = "focusScrim")
        val blurModifier = if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = backgroundColor)
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        contentRootOffset = coordinates.positionInRoot()
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(blurModifier)
                ) {
                    HeaderSection(onAddDevice = onAddDevice)
                    if (state.devices.isEmpty()) {
                        EmptyDeviceState(
                            modifier = Modifier.fillMaxSize(),
                            onAddDevice = onAddDevice
                        )
                    } else {
                        DeviceList(
                            devices = state.devices,
                            focusedDeviceId = focusedDeviceId,
                            isManagementActive = state.isManagementActive,
                            onDeviceToggle = onDeviceToggle,
                            onDeviceOpen = onDeviceOpen,
                            onSelectedBounds = { selectedCardLayout = it },
                            contentOrigin = contentRootOffset
                        )
                    }
                }
                if (scrimAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(scrimBase.copy(alpha = scrimAlpha))
                            .then(
                                if (focusedDevice != null) {
                                    Modifier.pointerInput(onCancelManagement) {
                                        detectTapGestures { onCancelManagement() }
                                    }
                                } else Modifier
                            )
                            .zIndex(1f)
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = focusedDevice != null,
                    enter = fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220)),
                    exit = fadeOut(animationSpec = tween(160)) + scaleOut(targetScale = 0.98f, animationSpec = tween(160)),
                    modifier = Modifier.zIndex(2f)
                ) {
                    focusedDevice?.let { device ->
                        SelectedDeviceOverlay(
                            device = device,
                            layout = selectedCardLayout,
                            onClick = {
                                if (state.isManagementActive) {
                                    onDeviceToggle(device)
                                } else {
                                    onDeviceOpen(device)
                                }
                            },
                            onLongPress = { onDeviceToggle(device) },
                            onRenameClick = onRenameClick,
                            onRemoveClick = onRemoveClick,
                            modifier = Modifier.zIndex(2f)
                        )
                    }
                }
            }
        }
    }

    if (state.isRenameDialogVisible) {
        RenameDeviceDialog(
            value = state.renameInput,
            isProcessing = state.isActionInProgress,
            errorRes = state.renameInputError,
            onValueChange = onRenameInputChanged,
            onDismiss = onRenameDismiss,
            onConfirm = onRenameConfirm
        )
    }

    if (state.isRemoveDialogVisible) {
        RemoveDeviceDialog(
            isProcessing = state.isActionInProgress,
            onDismiss = onRemoveDismiss,
            onConfirm = onRemoveConfirm
        )
    }
}

@Composable
private fun DeviceList(
    devices: List<DeviceMetadata>,
    focusedDeviceId: String?,
    isManagementActive: Boolean,
    onDeviceToggle: (DeviceMetadata) -> Unit,
    onDeviceOpen: (DeviceMetadata) -> Unit,
    onSelectedBounds: (SelectedCardLayout) -> Unit,
    contentOrigin: Offset
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = devices,
            key = { it.id }
        ) { device ->
            DeviceCardItem(
                device = device,
                isSelected = device.id == focusedDeviceId,
                showSelectionBadge = focusedDeviceId == null,
                onSelectedBounds = onSelectedBounds,
                contentOrigin = contentOrigin,
                onClick = {
                    if (isManagementActive) {
                        onDeviceToggle(device)
                    } else {
                        onDeviceOpen(device)
                    }
                },
                onLongPress = { onDeviceToggle(device) }
            )
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

enum class AppBottomTab(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val isVisible: Boolean = true
) {
    Home(R.string.settings_tab_home, Icons.Filled.Home, isVisible = true),
    Manual(R.string.settings_tab_manual, Icons.Filled.Book, isVisible = false),
    Devices(R.string.settings_tab_devices, Icons.Filled.Devices, isVisible = false),
    Profile(R.string.settings_tab_profile, Icons.Filled.Person, isVisible = true);

    companion object {
        val visibleTabs: List<AppBottomTab> = values().filter { it.isVisible }
    }
}

@Composable
private fun HeaderSection(onAddDevice: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 28.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
//            Text(
//                text = stringResource(id = R.string.home_management_title),
//                style = MaterialTheme.typography.titleLarge.copy(
//                    fontWeight = FontWeight.SemiBold,
//                    fontSize = 20.sp,
//                    color = MaterialTheme.massagerExtendedColors.textPrimary
//                )
//            )
        }
        IconButton(
            onClick = onAddDevice,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.massagerExtendedColors.surfaceBright)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(id = R.string.home_management_add_device),
                tint = MaterialTheme.massagerExtendedColors.success
            )
        }
    }
}

@Composable
private fun DeviceCardItem(
    device: DeviceMetadata,
    isSelected: Boolean,
    showSelectionBadge: Boolean = true,
    onSelectedBounds: (SelectedCardLayout) -> Unit = {},
    contentOrigin: Offset = Offset.Zero,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.95f)
    ) {
        Box {
            DeviceCard(
                device = device,
                isSelected = isSelected,
                onClick = onClick,
                onLongPress = onLongPress,
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    if (isSelected) {
                        val bounds = coordinates.boundsInRoot()
                        val offset = bounds.topLeft - contentOrigin
                        onSelectedBounds(
                            SelectedCardLayout(
                                offset = offset,
                                size = IntSize(
                                    bounds.width.roundToInt(),
                                    bounds.height.roundToInt()
                                )
                            )
                        )
                    }
                }
            )
            if (showSelectionBadge) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn()
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-12).dp, y = 12.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.massagerExtendedColors.success),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun DeviceCard(
    device: DeviceMetadata,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = if (isSelected) 1.02f else 1f

    Card(
        shape = RoundedCornerShape(if (isSelected) 22.dp else 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.massagerExtendedColors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.massagerExtendedColors.cardBackground)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp)),
//                    .background(MaterialTheme.massagerExtendedColors.surfaceSubtle),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = device.iconResId ?: R.drawable.ic_massager_logo),
                    contentDescription = device.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(64.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.massagerExtendedColors.textPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SerialNo ${device.serialNo ?: device.id}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.massagerExtendedColors.textMuted,
                        fontSize = 12.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
private fun SelectedDeviceOverlay(
    device: DeviceMetadata,
    layout: SelectedCardLayout?,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onRenameClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val cardWidth = layout?.let { with(density) { it.size.width.toDp() } }
    val cardOffset = layout?.let { with(density) { IntOffset(it.offset.x.roundToInt(), it.offset.y.roundToInt()) } }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        val columnModifier = (cardOffset?.let { offset ->
            Modifier.offset { offset }
        } ?: Modifier.align(Alignment.TopCenter).padding(horizontal = 20.dp, vertical = 16.dp))
            .then(
                if (cardWidth != null) Modifier.width(cardWidth) else Modifier.fillMaxWidth()
            )
        Column(
            modifier = columnModifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FocusedDeviceCard(
                device = device,
                onClick = onClick,
                onLongPress = onLongPress
            )
            Spacer(modifier = Modifier.height(16.dp))
            FloatingActionPanel(
                onRenameClick = onRenameClick,
                onRemoveClick = onRemoveClick,
                modifier = if (cardWidth != null && cardWidth > 100.dp) {
                    Modifier.width(cardWidth*3/4)
                } else {
                    Modifier.fillMaxWidth()
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FocusedDeviceCard(
    device: DeviceMetadata,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.massagerExtendedColors.surfaceBright),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        border = BorderStroke(1.dp, MaterialTheme.massagerExtendedColors.band.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.massagerExtendedColors.surfaceBright)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(20.dp)),
//                    .background(MaterialTheme.massagerExtendedColors.surfaceSubtle),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = device.iconResId ?: R.drawable.ic_massager_logo),
                    contentDescription = device.name,
                    modifier = Modifier.size(70.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = device.name.ifBlank { "BLE Device" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.massagerExtendedColors.textPrimary,
                        fontSize = 17.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "SerialNo ${device.serialNo ?: device.id}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.massagerExtendedColors.textMuted,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.massagerExtendedColors.iconMuted
            )
        }
    }
}

@Composable
private fun FloatingActionPanel(
    onRenameClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dividerColor = MaterialTheme.massagerExtendedColors.divider
    val panelBackground = MaterialTheme.massagerExtendedColors.surfaceBright
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = panelBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier
            .background(MaterialTheme.massagerExtendedColors.surfaceBright)
            .fillMaxWidth()) {
            PanelRow(
                text = stringResource(id = R.string.rename),
                textColor = MaterialTheme.massagerExtendedColors.textPrimary,
                icon = Icons.Filled.Edit,
                iconTint = MaterialTheme.massagerExtendedColors.band,
                onClick = onRenameClick
            )
            Divider(color = dividerColor.copy(alpha = 0.6f), thickness = 0.7.dp)
            PanelRow(
                text = stringResource(id = R.string.remove_device),
                textColor = MaterialTheme.massagerExtendedColors.danger,
                icon = Icons.Filled.Delete,
                iconTint = MaterialTheme.massagerExtendedColors.band,
                onClick = onRemoveClick
            )
        }
    }
}

@Composable
private fun PanelRow(
    text: String,
    textColor: Color,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(
                    bounded = true,
                    color = iconTint.copy(alpha = 0.35f)
                ),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                color = textColor
            ),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint
        )
    }
}

@Composable
private fun RenameDeviceDialog(
    value: String,
    isProcessing: Boolean,
    errorRes: Int?,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.rename_device_title))
        },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = !isProcessing,
                    placeholder = {
                        Text(text = stringResource(id = R.string.rename_placeholder))
                    },
                    singleLine = true,
                    isError = errorRes != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorRes != null) {
                    Text(
                        text = stringResource(id = errorRes),
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.massagerExtendedColors.success),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = value.isNotBlank() && !isProcessing && errorRes == null
            ) {
                Text(text = stringResource(id = R.string.save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing
            ) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun RemoveDeviceDialog(
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.remove_device_confirm_title)) },
        text = {
            Text(text = stringResource(id = R.string.remove_device_confirm_message))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isProcessing
            ) {
                Text(
                    text = stringResource(id = R.string.confirm),
                    color = MaterialTheme.massagerExtendedColors.success
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing
            ) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun EmptyDeviceState(
    modifier: Modifier = Modifier,
    onAddDevice: (() -> Unit)? = null
) {
    val cardShape = RoundedCornerShape(24.dp)
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val cardFillColor = MaterialTheme.massagerExtendedColors.surfaceBright

    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = cardFillColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val strokeWidth = 2.dp.toPx()
                    val dash = PathEffect.dashPathEffect(floatArrayOf(16f, 10f))
                    val radius = 24.dp.toPx()
                    drawRoundRect(
                        color = borderColor,
                        style = Stroke(width = strokeWidth, pathEffect = dash),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                    )
                }
                .clip(cardShape)
                .clickable(
                    enabled = onAddDevice != null,
                    onClick = { onAddDevice?.invoke() }
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(id = R.string.home_management_add_device),
                        tint = MaterialTheme.massagerExtendedColors.textOnAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = stringResource(id = R.string.home_saved_devices_empty_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 19.sp,
                        color = MaterialTheme.massagerExtendedColors.textPrimary
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(id = R.string.home_saved_devices_empty_subtitle),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.massagerExtendedColors.textMuted,
                        fontSize = 15.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(36.dp))
        Text(
            text = stringResource(id = R.string.home_saved_devices_empty_getting_started_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = MaterialTheme.massagerExtendedColors.textPrimary
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.home_saved_devices_empty_getting_started_body),
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.massagerExtendedColors.textMuted,
                fontSize = 15.sp,
                lineHeight = 20.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}
