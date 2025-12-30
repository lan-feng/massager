package com.massager.app.presentation.settings

// 文件说明：定义账户安全相关的导航路由与参数常量。
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.massager.app.R
import com.massager.app.presentation.theme.massagerExtendedColors
import com.massager.app.presentation.components.ThemedSnackbarHost
import com.massager.app.presentation.components.LoginTextField
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    viewModel: ChangePasswordViewModel = hiltViewModel(),
    requireOldPassword: Boolean = true,
    onBack: () -> Unit,
    onPasswordChanged: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val brand = MaterialTheme.massagerExtendedColors.band

    LaunchedEffect(requireOldPassword) {
        viewModel.configure(requireOldPassword)
    }

    LaunchedEffect(uiState.snackbarMessageRes, uiState.snackbarMessageText) {
        val message = uiState.snackbarMessageText
            ?: uiState.snackbarMessageRes?.let { context.getString(it) }
        message?.let {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.consumeSnackbar()
            }
        }
    }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            viewModel.consumeSuccess()
            onPasswordChanged()
        }
    }

    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { ThemedSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.set_new_password_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.massagerExtendedColors.surfaceSubtle,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.massagerExtendedColors.surfaceSubtle)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AnimatedVisibility(visible = requireOldPassword && uiState.requireOldPassword) {
                PasswordField(
                    label = stringResource(id = R.string.old_password_label),
                    value = uiState.oldPassword,
                    onValueChange = viewModel::onOldPasswordChanged,
                    isError = uiState.oldPasswordError,
                    placeholder = stringResource(id = R.string.old_password_label),
                    visible = oldPasswordVisible,
                    onToggleVisibility = { oldPasswordVisible = !oldPasswordVisible },
                    supportingText = when {
                        !uiState.oldPasswordError -> null
                        uiState.oldPassword.isBlank() -> stringResource(id = R.string.error_old_password_required)
                        else -> stringResource(id = R.string.error_old_password_incorrect)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    enabled = !uiState.isSubmitting
                )
            }

            PasswordField(
                label = stringResource(id = R.string.new_password_label),
                value = uiState.newPassword,
                onValueChange = viewModel::onNewPasswordChanged,
                isError = uiState.newPasswordError,
                placeholder = stringResource(id = R.string.new_password_label),
                visible = newPasswordVisible,
                onToggleVisibility = { newPasswordVisible = !newPasswordVisible },
                supportingText = if (uiState.newPasswordError) stringResource(id = R.string.error_new_password_invalid) else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                enabled = !uiState.isSubmitting
            )

            PasswordField(
                label = stringResource(id = R.string.confirm_password_label),
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChanged,
                isError = uiState.confirmPasswordError,
                placeholder = stringResource(id = R.string.confirm_password_label),
                visible = confirmPasswordVisible,
                onToggleVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
                supportingText = if (uiState.confirmPasswordError) stringResource(id = R.string.error_confirm_password_mismatch) else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.submit()
                        }
                ),
                enabled = !uiState.isSubmitting
            )

            Text(
                text = stringResource(R.string.password_rule),
                style = MaterialTheme.typography.bodySmall.copy(color = brand),
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.submit()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = uiState.isFormValid && !uiState.isSubmitting,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = brand,
                    disabledContainerColor = brand.copy(alpha = 0.4f),
                    contentColor = Color.White
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.submit_action),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                }
            }

            
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountConfirmScreen(
    viewModel: DeleteAccountViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            viewModel.consumeSuccess()
            onSuccess()
        }
    }

    LaunchedEffect(uiState.errorMessageRes, uiState.errorMessageText) {
        val message = uiState.errorMessageText
            ?: uiState.errorMessageRes?.let { context.getString(it) }
        message?.takeIf { it.isNotBlank() }?.let {
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.massagerExtendedColors.surfaceSubtle
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column {
                TopAppBar(
                    title = { Text(text = stringResource(id = R.string.delete_account_confirm_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.massagerExtendedColors.surfaceSubtle,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.delete_account_confirm_description),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (uiState.isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        )
                    }
                    Button(
                        onClick = { viewModel.deleteAccount() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.isLoading.not(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(text = stringResource(id = R.string.delete_account_confirm_button))
                    }
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.isLoading.not(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.massagerExtendedColors.band,
                            contentColor = MaterialTheme.massagerExtendedColors.textOnAccent
                        )
                    ) {
                        Text(text = stringResource(id = R.string.delete_account_cancel_button))
                    }
                }
            }
            ThemedSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    placeholder: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    supportingText: String?,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    enabled: Boolean,
    brand: Color = MaterialTheme.massagerExtendedColors.band,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
    shape: RoundedCornerShape = RoundedCornerShape(14.dp)
) {
//    Text(
//        text = label,
//        style = MaterialTheme.typography.bodyMedium.copy(
//            color = MaterialTheme.colorScheme.onSurface,
//            fontWeight = FontWeight.Medium
//        ),
//        modifier = Modifier.padding(bottom = 4.dp)
//    )
    LoginTextField(
        value = value,
        onValueChange = { input ->
            onValueChange(input.trimEnd { ch -> ch.isWhitespace() })
        },
        label = label,
        placeholder = placeholder,
        leadingIcon = Icons.Filled.Lock,
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = null
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText,
        shape = shape,
        background = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
}
