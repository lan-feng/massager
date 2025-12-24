package com.massager.app.presentation.settings

// 文件说明：账户安全界面，提供密码找回、邮件验证等操作入口。
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.massager.app.presentation.components.ThemedSnackbarHost
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.massager.app.R
import com.massager.app.presentation.theme.massagerExtendedColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSecurityScreen(
    state: AccountSecurityUiState,
    onBack: () -> Unit,
    onSetPassword: () -> Unit,
    onDeleteAccount: () -> Unit,
    onRequestLogout: () -> Unit,
    onConfirmLogout: () -> Unit,
    onDismissLogoutDialog: () -> Unit,
    onBindGoogle: () -> Unit,
    onUnbind: (ThirdPartyPlatform) -> Unit,
    onConsumeUnbindResult: () -> Unit,
    onConsumeBindResult: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val guestRestrictionText = stringResource(id = R.string.guest_mode_cloud_restricted)
    var contentVisible by remember { mutableStateOf(false) }
    var unbindTarget by remember { mutableStateOf<ThirdPartyPlatform?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val showBlockingLoader = state.isBinding || state.isUnbinding

    LaunchedEffect(Unit) {
        contentVisible = true
    }
    val restrictedClick: () -> Unit = {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(guestRestrictionText)
        }
    }
    val setPasswordAction = if (state.isGuestMode) restrictedClick else onSetPassword
    val deleteAccountAction = if (state.isGuestMode) restrictedClick else onDeleteAccount

    LaunchedEffect(state.unbindSucceeded, state.unbindError) {
        if (state.unbindSucceeded) {
            snackbarHostState.showSnackbar(context.getString(R.string.third_party_unbind_success))
            onConsumeUnbindResult()
        } else if (state.unbindError != null) {
            snackbarHostState.showSnackbar(state.unbindError ?: context.getString(R.string.third_party_unbind_failed))
            onConsumeUnbindResult()
        }
    }

    LaunchedEffect(state.bindSucceeded, state.bindError) {
        if (state.bindSucceeded) {
            snackbarHostState.showSnackbar(context.getString(R.string.third_party_bind_success))
            onConsumeBindResult()
        } else if (state.bindError != null) {
            snackbarHostState.showSnackbar(state.bindError ?: context.getString(R.string.third_party_unbind_failed))
            onConsumeBindResult()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.massagerExtendedColors.surfaceSubtle
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.account_security_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.massagerExtendedColors.surfaceSubtle,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )

            AnimatedVisibility(
                visible = contentVisible,
                enter = slideInVertically { it / 3 } + fadeIn(),
                exit = slideOutVertically { it / 3 } + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    SectionCard(
                        title = stringResource(id = R.string.security_settings_section).uppercase(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SettingRow(
                            icon = Icons.Filled.Lock,
                            label = stringResource(id = R.string.set_password),
                            onClick = setPasswordAction
                        )
                        Divider(color = MaterialTheme.massagerExtendedColors.divider.copy(alpha = 0.6f))
                        SettingRow(
                            icon = Icons.Filled.Delete,
                            label = stringResource(id = R.string.delete_account),
                            onClick = deleteAccountAction
                        )
                    }

                    SectionCard(
                        title = stringResource(id = R.string.account_binding_section).uppercase(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        state.thirdPartyAccounts.forEachIndexed { index, binding ->
                            ThirdPartyAccountRow(
                                binding = binding,
                                onBind = {
                                    if (state.isGuestMode) {
                                        restrictedClick()
                                    } else if (state.facebookBound) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                context.getString(R.string.third_party_binding_blocked_fb)
                                            )
                                        }
                                    } else {
                                        if (binding.platform == ThirdPartyPlatform.Google) {
                                            onBindGoogle()
                                        } else {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    context.getString(R.string.third_party_binding_coming_soon)
                                                )
                                            }
                                        }
                                    }
                                },
                                onUnbind = {
                                    if (state.isGuestMode) {
                                        restrictedClick()
                                    } else {
                                        unbindTarget = binding.platform
                                    }
                                },
                                isProcessing = state.isUnbinding
                            )
                            if (index != state.thirdPartyAccounts.lastIndex) {
                                Divider(color = MaterialTheme.massagerExtendedColors.divider.copy(alpha = 0.6f))
                            }
                        }
                    }

                }
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                ThemedSnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (showBlockingLoader) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (state.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = onDismissLogoutDialog,
            title = { Text(text = stringResource(id = R.string.logout)) },
            text = { Text(text = stringResource(id = R.string.logout_confirm)) },
            confirmButton = {
                Button(onClick = onConfirmLogout) {
                    Text(text = stringResource(id = R.string.logout_confirm_confirm))
                }
            },
            dismissButton = {
                Button(onClick = onDismissLogoutDialog) {
                    Text(text = stringResource(id = R.string.logout_confirm_cancel))
                }
            }
        )
    }

    if (unbindTarget != null) {
        AlertDialog(
            onDismissRequest = { unbindTarget = null },
            title = { Text(text = stringResource(id = R.string.third_party_unbind_confirm_title)) },
            text = { Text(text = stringResource(id = R.string.third_party_unbind_confirm_message)) },
            confirmButton = {
                Button(
                    enabled = !state.isUnbinding,
                    onClick = {
                        unbindTarget?.let(onUnbind)
                        unbindTarget = null
                    }
                ) {
                    Text(text = stringResource(id = R.string.third_party_unbind))
                }
            },
            dismissButton = {
                Button(onClick = { unbindTarget = null }) {
                    Text(text = stringResource(id = R.string.third_party_unbind_cancel))
                }
            }
        )
    }
}

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountInfoRow(
    title: String,
    value: String?,
    onClick: (() -> Unit)?
) {
    val modifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    }

    val supportingContent: (@Composable () -> Unit)? = value?.let { textValue ->
        {
            Text(text = textValue, fontWeight = FontWeight.SemiBold)
        }
    }

    ListItem(
        headlineContent = {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = supportingContent,
        trailingContent = {
            if (onClick != null) {
                Icon(
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
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.massagerExtendedColors.cardBackground
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.massagerExtendedColors.band
            )
        },
        headlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThirdPartyAccountRow(
    binding: ThirdPartyAccountBinding,
    onBind: () -> Unit,
    onUnbind: () -> Unit,
    isProcessing: Boolean
) {
    ListItem(
        leadingContent = {
            PlatformBadge(
                platform = binding.platform
            )
        },
        headlineContent = {
            Text(text = stringResource(id = binding.platform.displayNameRes))
        },
        supportingContent = {
            if (binding.isBound) {
                Column {
                    val details = listOfNotNull(binding.displayName, binding.email).distinct()
                    details.forEach { detail ->
                        Text(
                            text = detail,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(id = R.string.go_to_binding),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { if (binding.isBound) onUnbind() else onBind() })
            .padding(horizontal = 16.dp, vertical = 8.dp),
        trailingContent = {
            if (binding.isBound) {
                TextButton(
                    onClick = onUnbind,
                    enabled = !isProcessing,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(text = stringResource(id = R.string.third_party_unbind))
                }
            } else {
                Text(
                    text = stringResource(id = R.string.go_to_binding),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.massagerExtendedColors.band
                )
            }
        }
    )
}

@Composable
private fun PlatformBadge(
    platform: ThirdPartyPlatform
) {
    val (iconRes, tint) = when (platform) {
        ThirdPartyPlatform.Google -> R.drawable.ic_google to Color(0xFF4285F4)
    }
    Surface(
        modifier = Modifier.size(36.dp),
        shape = CircleShape,
        color = tint.copy(alpha = 0.12f),
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
