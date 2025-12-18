package com.massager.app.presentation.settings

// 文件说明：账户安全界面，提供密码找回、邮件验证等操作入口。
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.massager.app.R
import com.massager.app.presentation.theme.massagerExtendedColors

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
    onBindGoogle: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val guestRestrictionText = stringResource(id = R.string.guest_mode_cloud_restricted)
    var contentVisible by remember { mutableStateOf(false) }
    var showBoundDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        contentVisible = true
    }
    val restrictedClick: () -> Unit = {
        Toast.makeText(context, guestRestrictionText, Toast.LENGTH_SHORT).show()
    }
    val setPasswordAction = if (state.isGuestMode) restrictedClick else onSetPassword
    val deleteAccountAction = if (state.isGuestMode) restrictedClick else onDeleteAccount
    val displayedEmail = if (state.isGuestMode && state.userEmail.isBlank()) {
        stringResource(id = R.string.guest_placeholder_email)
    } else {
        state.userEmail
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.massagerExtendedColors.surfaceSubtle
    ) {
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column {
                            AccountInfoRow(
                                title = stringResource(id = R.string.email_label),
                                value = displayedEmail,
                                onClick = null
                            )
                            Divider()
                            AccountInfoRow(
                                title = stringResource(id = R.string.set_password),
                                value = null,
                                onClick = setPasswordAction
                            )
                        }
                    }

                    Column {
                        Text(
                            text = stringResource(id = R.string.third_party_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 2.dp
                        ) {
                            Column {
                                state.thirdPartyAccounts.forEachIndexed { index, binding ->
                                    ThirdPartyAccountRow(
                                        binding = binding,
                                        onClick = {
                                            if (state.isGuestMode) {
                                                restrictedClick()
                                            } else if (state.facebookBound) {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.third_party_binding_blocked_fb),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else if (binding.isBound) {
                                                showBoundDialog = true
                                            } else {
                                                onBindGoogle()
                                            }
                                        }
                                    )
                                    if (index != state.thirdPartyAccounts.lastIndex) {
                                        Divider()
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 2.dp
                    ) {
                        AccountInfoRow(
                            title = stringResource(id = R.string.delete_account),
                            value = null,
                            onClick = deleteAccountAction
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onRequestLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .background(Color.Transparent),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.logout),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
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

    if (showBoundDialog) {
        AlertDialog(
            onDismissRequest = { showBoundDialog = false },
            title = { Text(text = stringResource(id = R.string.third_party_bound_title)) },
            text = { Text(text = stringResource(id = R.string.third_party_bound_message)) },
            confirmButton = {
                Button(onClick = { showBoundDialog = false }) {
                    Text(text = stringResource(id = R.string.third_party_bound_ok))
                }
            },
            dismissButton = {}
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThirdPartyAccountRow(
    binding: ThirdPartyAccountBinding,
    onClick: () -> Unit
) {
    ListItem(
        leadingContent = {
            PlatformBadge(
                text = binding.platform.badgeLabel,
                background = binding.platform.badgeColor()
            )
        },
        headlineContent = {
            Text(text = stringResource(id = binding.platform.displayNameRes))
        },
        supportingContent = {
            val textRes = if (binding.isBound) R.string.third_party_bound else R.string.go_to_binding
            Text(
                text = stringResource(id = textRes),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun PlatformBadge(
    text: String,
    background: Color
) {
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.Transparent),
        color = background,
        contentColor = Color.White,
        shape = CircleShape,
        tonalElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color.White,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun ThirdPartyPlatform.badgeColor(): Color = when (this) {
    ThirdPartyPlatform.Google -> Color(0xFF34A853)
}
