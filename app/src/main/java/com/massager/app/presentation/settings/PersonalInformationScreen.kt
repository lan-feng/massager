package com.massager.app.presentation.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.massager.app.R
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
    val context = LocalContext.current

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    PersonalInformationContent(
        state = uiState,
        onBack = onBack,
        onAvatarSelected = viewModel::updateAvatar,
        onNameUpdated = viewModel::updateName
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalInformationContent(
    state: PersonalInfoUiState,
    onBack: () -> Unit,
    onAvatarSelected: (ByteArray) -> Unit,
    onNameUpdated: (String) -> Unit
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
        containerColor = MaterialTheme.massagerExtendedColors.surfaceSubtle
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
                ProfileListCard {
                    ProfileRow(
                        title = stringResource(id = R.string.avatar_label),
                        trailingContent = {
                            AvatarPreview(
                                state = state,
                                onClick = { showAvatarDialog = true }
                            )
                        },
                        onClick = { showAvatarDialog = true }
                    )
                    Divider()
                    ProfileRow(
                        title = stringResource(id = R.string.email_label),
                        trailingText = state.email,
                        onClick = null
                    )
                    Divider()
                    ProfileRow(
                        title = stringResource(id = R.string.name_label),
                        trailingText = state.name,
                        onClick = { showNameDialog = true }
                    )
                }
            }
        }
    }

    if (showAvatarDialog) {
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

    if (showNameDialog) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileListCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.massagerExtendedColors.surfaceBright)
    ) {
        Column(content = content)
    }
}

@Composable
private fun ProfileRow(
    title: String,
    trailingText: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)?
) {
    val modifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    } else {
        Modifier.fillMaxWidth()
    }

    ListItem(
        headlineContent = {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            trailingText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End
                )
            }
        },
        trailingContent = {
            when {
                trailingContent != null -> trailingContent.invoke()
                onClick != null -> Icon(
                    imageVector = Icons.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = modifier
    )
}

@Composable
private fun AvatarPreview(
    state: PersonalInfoUiState,
    onClick: () -> Unit
) {
    val avatarBitmap = remember(state.avatarBytes) {
        state.avatarBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
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
            if (avatarBitmap != null) {
                Image(
                    bitmap = avatarBitmap,
                    contentDescription = stringResource(id = R.string.avatar_label),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.LightGray)
                        .size(48.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = rememberAsyncImagePainter(model = state.avatarUrl),
                    contentDescription = stringResource(id = R.string.avatar_label),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.LightGray)
                        .size(48.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
    val isValid = trimmed.length in 2..20 && trimmed.all { it.isLetter() }

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

private fun compressBitmap(source: Bitmap, quality: Int = 85): ByteArray {
    val stream = ByteArrayOutputStream()
    source.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    return stream.toByteArray()
}

