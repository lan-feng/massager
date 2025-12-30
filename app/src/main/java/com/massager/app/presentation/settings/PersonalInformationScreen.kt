package com.massager.app.presentation.settings

// 文件说明：个人信息编辑界面，提供姓名、邮箱、头像等表单交互。
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.massager.app.R
import com.massager.app.presentation.components.ThemedSnackbarHost
import com.massager.app.presentation.settings.components.SettingsEntry
import com.massager.app.presentation.settings.components.SettingsSectionCard
import com.massager.app.presentation.theme.massagerExtendedColors
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun PersonalInformationScreen(
    viewModel: PersonalInformationViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeToast()
        }
    }

    PersonalInformationContent(
        state = uiState,
        onBack = onBack,
        onAvatarSelected = viewModel::updateAvatar,
        onNameUpdated = viewModel::updateName,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalInformationContent(
    state: PersonalInfoUiState,
    onBack: () -> Unit,
    onAvatarSelected: (ByteArray) -> Unit,
    onNameUpdated: (String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoaded = true
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap ?: return@rememberLauncherForActivityResult
        processBitmap(bitmap, onAvatarSelected)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val resolver = context.contentResolver
        val bitmap = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
        bitmap?.let { processBitmap(it, onAvatarSelected) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.personal_info_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.massagerExtendedColors.surfaceSubtle,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.massagerExtendedColors.surfaceSubtle,
        snackbarHost = { ThemedSnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        if (state.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
        }
        AnimatedVisibility(visible = isLoaded) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val displayName = if (state.name.isBlank() && state.isGuestMode) {
                    stringResource(id = R.string.guest_placeholder_name)
                } else {
                    state.name
                }
                val displayEmail = if (state.email.isBlank() && state.isGuestMode) {
                    stringResource(id = R.string.guest_placeholder_email)
                } else {
                    state.email
                }
                SettingsSectionCard(
                    title = null,
                    items = listOf(
                        SettingsEntry(
                            title = stringResource(id = R.string.email_label),
                            trailingText = displayEmail
                        ),
                        SettingsEntry(
                            title = stringResource(id = R.string.avatar_label),
                            trailingContent = {
                                AvatarPreview(
                                    state = state,
                                    onClick = { showAvatarDialog = true }
                                )
                            },
                            onClick = { showAvatarDialog = true }
                        ),
                        SettingsEntry(
                            title = stringResource(id = R.string.name_label),
                            trailingText = displayName,
                            onClick = { showNameDialog = true }
                        )
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 20.dp,
                        vertical = 16.dp
                    ),
                    cornerRadius = 24
                )
            }
        }
    }

    if (showAvatarDialog && !state.isGuestMode) {
        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            title = { Text(text = stringResource(id = R.string.change_avatar)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            showAvatarDialog = false
                            cameraLauncher.launch(null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.avatar_take_photo))
                    }
                    TextButton(
                        onClick = {
                            showAvatarDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.avatar_choose_gallery))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAvatarDialog = false }) {
                    Text(text = stringResource(id = R.string.profile_cancel))
                }
            }
        )
    }

    if (showNameDialog && !state.isGuestMode) {
        EditNameDialog(
            currentName = state.name,
            onDismiss = { showNameDialog = false },
            onSave = {
                onNameUpdated(it)
                showNameDialog = false
            }
        )
    }
}

@Composable
private fun AvatarPreview(
    state: PersonalInfoUiState,
    onClick: () -> Unit
) {
    val avatarBitmap = remember(state.avatarBytes) {
        state.avatarBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
    val hasRemote = !state.avatarUrl.isNullOrBlank()
    Row(
        modifier = Modifier
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            shape = CircleShape,
            color = Color.Transparent,
            tonalElevation = 2.dp,
            shadowElevation = 6.dp
        ) {
            val modifier = Modifier
                .clip(CircleShape)
                .background(Color.LightGray)
                .size(48.dp)
            when {
                avatarBitmap != null -> {
                    Image(
                        bitmap = avatarBitmap,
                        contentDescription = stringResource(id = R.string.avatar_label),
                        modifier = modifier,
                        contentScale = ContentScale.Crop
                    )
                }
                hasRemote -> {
                    Image(
                        painter = rememberAsyncImagePainter(model = state.avatarUrl),
                        contentDescription = stringResource(id = R.string.avatar_label),
                        modifier = modifier,
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Image(
                        painter = painterResource(id = R.drawable.ic_massager_logo),
                        contentDescription = stringResource(id = R.string.avatar_label),
                        modifier = modifier,
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
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
    val isValid = trimmed.length in 2..20

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.edit_name)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = stringResource(id = R.string.profile_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isValid) {
                    Text(
                        text = stringResource(id = R.string.profile_name_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(trimmed) }, enabled = isValid) {
                Text(text = stringResource(id = R.string.profile_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.profile_cancel))
            }
        }
    )
}

private fun processBitmap(
    bitmap: Bitmap,
    onResult: (ByteArray) -> Unit
) {
    val scaled = scaleBitmap(bitmap)
    val compressed = compressBitmap(scaled)
    onResult(compressed)
}

private fun scaleBitmap(source: Bitmap, maxSize: Int = 512): Bitmap {
    val largestSide = max(source.width, source.height)
    if (largestSide <= maxSize) return source
    val scale = maxSize.toFloat() / largestSide
    return Bitmap.createScaledBitmap(
        source,
        (source.width * scale).roundToInt(),
        (source.height * scale).roundToInt(),
        true
    )
}

private fun compressBitmap(source: Bitmap, quality: Int = 80): ByteArray {
    val stream = ByteArrayOutputStream()
    source.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    return stream.toByteArray()
}

