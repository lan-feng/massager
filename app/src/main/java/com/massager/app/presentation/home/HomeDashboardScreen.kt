package com.massager.app.presentation.home

// 文件说明：Compose 首页仪表盘界面，呈现设备状态与健康数据。
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.massager.app.R
import com.massager.app.domain.model.DeviceMetadata
import com.massager.app.presentation.components.AppBottomNavigation
import com.massager.app.presentation.theme.massagerExtendedColors
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

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

    LaunchedEffect(effects) {
        effects.collectLatest { effect ->
            when (effect) {
                is HomeEffect.ShowMessage -> {
                    Toast.makeText(context, context.getString(effect.messageRes), Toast.LENGTH_SHORT).show()
                }

                is HomeEffect.ShowMessageText -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onDismissError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                AnimatedVisibility(visible = state.isManagementActive) {
                    ManagementBottomBar(
                        isSelectionActive = state.isManagementActive,
                        selectionCount = state.selectedDeviceIds.size,
                        isProcessing = state.isActionInProgress,
                        onRenameClick = onRenameClick,
                        onRemoveClick = onRemoveClick,
                        onCancel = onCancelManagement
                    )
                }
                AppBottomNavigation(
                    currentTab = currentTab,
                    onTabSelected = onTabSelected
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            HeaderSection(onAddDevice = onAddDevice)
            Text(
                text = stringResource(id = R.string.home_management_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.massagerExtendedColors.textSecondary
                ),
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            if (state.devices.isEmpty()) {
                EmptyDeviceState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = state.devices,
                        key = { it.id }
                    ) { device ->
                        DeviceCardItem(
                            device = device,
                            isSelected = state.selectedDeviceIds.contains(device.id),
                            onClick = {
                                if (state.isManagementActive) {
                                    onDeviceToggle(device)
                                } else {
                                    onDeviceOpen(device)
                                }
                            },
                            onLongPress = { onDeviceToggle(device) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(120.dp))
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
            Text(
                text = stringResource(id = R.string.home_management_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.massagerExtendedColors.textPrimary
                )
            )
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
                tint = MaterialTheme.massagerExtendedColors.danger
            )
        }
    }
}

@Composable
private fun DeviceCardItem(
    device: DeviceMetadata,
    isSelected: Boolean,
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
                onLongPress = onLongPress
            )
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
                        .background(MaterialTheme.massagerExtendedColors.danger),
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun DeviceCard(
    device: DeviceMetadata,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val scale = if (isSelected) 1.02f else 1f
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.massagerExtendedColors.surfaceBright),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.massagerExtendedColors.danger.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_massager_logo),
                    contentDescription = device.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.massagerExtendedColors.textPrimary
                    )
                )
                Text(
                    text = stringResource(id = R.string.home_management_device_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.massagerExtendedColors.textMuted,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun ManagementBottomBar(
    isSelectionActive: Boolean,
    selectionCount: Int,
    isProcessing: Boolean,
    onRenameClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.massagerExtendedColors.surfaceBright,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        if (isProcessing) {
            Text(
                text = stringResource(id = R.string.home_management_processing),
                style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.massagerExtendedColors.textSecondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Center
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onRenameClick,
                enabled = selectionCount == 1 && !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.massagerExtendedColors.success,
                    contentColor = MaterialTheme.massagerExtendedColors.textOnAccent
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.rename))
            }
            Button(
                onClick = onRemoveClick,
                enabled = selectionCount > 0 && !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.massagerExtendedColors.danger,
                    contentColor = MaterialTheme.massagerExtendedColors.textOnAccent
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.remove_device))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(
            onClick = onCancel,
            enabled = isSelectionActive && !isProcessing,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = stringResource(id = R.string.cancel))
        }
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
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.massagerExtendedColors.danger),
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
                    color = MaterialTheme.massagerExtendedColors.danger
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
private fun EmptyDeviceState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.home_management_empty_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            textAlign = TextAlign.Center,
            color = MaterialTheme.massagerExtendedColors.textSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.home_management_empty_message),
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.massagerExtendedColors.textMuted),
            textAlign = TextAlign.Center
        )
    }
}
