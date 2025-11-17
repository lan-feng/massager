package com.massager.app.presentation.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.massager.app.presentation.components.AppBottomNavigation
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.ripple.rememberRipple
import coil.compose.rememberAsyncImagePainter
import com.massager.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.massager.app.presentation.home.AppBottomTab
import com.massager.app.presentation.theme.massagerExtendedColors
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt


@Composable
fun SettingsScreen(
    state: SettingsUiState,
    currentTab: AppBottomTab,
    onTabSelected: (AppBottomTab) -> Unit,
    onToggleTemperature: () -> Unit,
    onClearCache: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateAvatar: (ByteArray) -> Unit,
    onNavigatePersonalInfo: () -> Unit,
    onNavigateAccountSecurity: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateFavorites: () -> Unit,
    onNavigateAbout: () -> Unit,
    onLogout: () -> Unit,
    onConsumeToast: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            coroutineScope.launch {
                val compressed = withContext(Dispatchers.Default) {
                    compressBitmap(bitmap)
                }
                onUpdateAvatar(compressed)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val bitmap = loadBitmapFromUri(context, uri)
            bitmap?.let {
                val compressed = withContext(Dispatchers.Default) {
                    compressBitmap(it)
                }
                onUpdateAvatar(compressed)
            }
        }
    }

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(it)
                delay(1500)
                onConsumeToast()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.massagerExtendedColors.surfaceSubtle
    ) {
        Box {
            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    HeaderSection(
                        user = state.user,
                        onAvatarTap = { showAvatarDialog = true }
                    )
                }

                item {
                    SettingsGroup(
                        title = stringResource(R.string.settings_profile_section),
                        items = listOf(
                            SettingsItem(
                                icon = Icons.Outlined.PersonOutline,
                                title = stringResource(R.string.settings_personal_info),
                                onClick = onNavigatePersonalInfo
                            ),
                            SettingsItem(
                                icon = Icons.Outlined.Security,
                                title = stringResource(R.string.settings_account_security),
                                onClick = onNavigateAccountSecurity
                            ),
                            SettingsItem(
                                icon = Icons.Outlined.Delete,
                                title = stringResource(R.string.settings_clear_cache),
                                trailingText = state.user.cacheSize,
                                onClick = onClearCache
                            )
                        )
                    )
                }

                item {
                    SettingsGroup(
                        title = stringResource(R.string.settings_other_section),
                        items = listOf(
                            SettingsItem(
                                icon = Icons.Filled.Info,
                                title = stringResource(R.string.settings_about),
                                onClick = onNavigateAbout
                            )
                        )
                    )
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp)
            )

            AppBottomNavigation(
                currentTab = currentTab,
                onTabSelected = onTabSelected,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (showEditNameDialog) {
            EditNameDialog(
                currentName = state.user.name,
                onDismiss = { showEditNameDialog = false },
                onSave = {
                    onUpdateName(it)
                    showEditNameDialog = false
                }
            )
        }

        if (showAvatarDialog) {
            AvatarOptionsDialog(
                onDismiss = { showAvatarDialog = false },
                onTakePhoto = {
                    showAvatarDialog = false
                    cameraLauncher.launch(null)
                },
                onChooseGallery = {
                    showAvatarDialog = false
                    galleryLauncher.launch("image/*")
                }
            )
        }
    }
}

@Composable
private fun HeaderSection(
    user: SettingsUser,
    onAvatarTap: () -> Unit
) {
    val avatarBitmap = remember(user.avatarBytes) {
        user.avatarBytes?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    }

    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateIn = true }

    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.massagerExtendedColors.surfaceBright,
                        MaterialTheme.massagerExtendedColors.surfaceSubtle
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            AnimatedVisibility(
                visible = animateIn,
                enter = fadeIn(animationSpec = tween(360)) + slideInVertically { it / 3 },
                exit = fadeOut()
            ) {
                val remoteAvatarPainter = user.avatarUrl?.takeIf { it.isNotBlank() }?.let {
                    rememberAsyncImagePainter(model = it)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    var isPressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 1.08f else 1f,
                        animationSpec = tween(durationMillis = 240),
                        label = "avatar_scale"
                    )

                    Surface(
                        shape = CircleShape,
                        shadowElevation = 12.dp,
                        color = MaterialTheme.massagerExtendedColors.surfaceBright,
                        modifier = Modifier.size(86.dp)
                    ) {
                        val imageModifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isPressed = true
                                onAvatarTap()
                                isPressed = false
                            }

                        when {
                            avatarBitmap != null -> Image(
                                bitmap = avatarBitmap,
                                contentDescription = stringResource(R.string.settings_avatar_content_desc),
                                modifier = imageModifier,
                                contentScale = ContentScale.Crop
                            )
                            remoteAvatarPainter != null -> Image(
                                painter = remoteAvatarPainter,
                                contentDescription = stringResource(R.string.settings_avatar_content_desc),
                                modifier = imageModifier,
                                contentScale = ContentScale.Crop
                            )
                            else -> Image(
                                painter = painterResource(id = R.drawable.ic_massager_logo),
                                contentDescription = stringResource(R.string.settings_avatar_content_desc),
                                modifier = imageModifier.padding(12.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (user.email.isNotBlank()) {
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        if (user.id > 0) {
                            Text(
                                text = stringResource(R.string.settings_user_id, user.id.toString()),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class SettingsItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val trailingText: String? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val onClick: (() -> Unit)? = null
)

@Composable
private fun SettingsGroup(
    title: String,
    items: List<SettingsItem>
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.massagerExtendedColors.surfaceBright),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            items.forEachIndexed { index, item ->
                val hasNavigation = item.onClick != null
                val interaction = remember(item.title) { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(
                            enabled = hasNavigation,
                            interactionSource = interaction,
                            indication = if (hasNavigation) {
                                rememberRipple(color = MaterialTheme.massagerExtendedColors.danger.copy(alpha = 0.16f))
                            } else null
                        ) { item.onClick?.invoke() }
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = MaterialTheme.massagerExtendedColors.danger
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    when {
                        item.trailingContent != null -> item.trailingContent.invoke()
                        item.trailingText != null -> Text(
                            text = item.trailingText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (hasNavigation) {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                    }
                }
                if (index != items.lastIndex) {
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                }
            }
        }
    }
}

@Composable
private fun TemperatureToggle(
    unit: TemperatureUnit,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = unit.display, style = MaterialTheme.typography.bodySmall)
        Switch(
            checked = unit == TemperatureUnit.Fahrenheit,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.massagerExtendedColors.textOnAccent,
                checkedTrackColor = MaterialTheme.massagerExtendedColors.danger,
                uncheckedThumbColor = MaterialTheme.massagerExtendedColors.surfaceBright,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun EditNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember(currentName) { mutableStateOf(currentName) }
    val trimmed = name.trim()
    val isValid = trimmed.length >= 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.edit_name_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(text = stringResource(id = R.string.edit_name_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isValid) {
                    Text(
                        text = stringResource(id = R.string.edit_name_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(trimmed) },
                enabled = isValid
            ) {
                Text(text = stringResource(id = R.string.edit_name_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.edit_name_cancel))
            }
        }
    )
}

@Composable
private fun AvatarOptionsDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseGallery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.avatar_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onTakePhoto,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.avatar_take_photo))
                }
                TextButton(
                    onClick = onChooseGallery,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.avatar_choose_gallery))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.avatar_option_cancel))
            }
        }
    )
}

private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }

private fun compressBitmap(source: Bitmap): ByteArray {
    val maxSize = 512
    val largestSide = max(source.width, source.height)
    val scaledBitmap = if (largestSide > maxSize) {
        val scale = maxSize.toFloat() / largestSide
        Bitmap.createScaledBitmap(
            source,
            (source.width * scale).roundToInt(),
            (source.height * scale).roundToInt(),
            true
        )
    } else {
        source
    }
    val stream = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
    return stream.toByteArray()
}

