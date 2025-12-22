package com.massager.app.presentation.auth

// 文件说明：Compose 实现的登录、注册、重置密码界面与导航。
import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Facebook
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.massager.app.R
import com.massager.app.presentation.components.ThemedSnackbarHost
import com.massager.app.presentation.theme.massagerExtendedColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    state: AuthUiState,
    onLogin: (email: String, password: String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onForgotPassword: () -> Unit = {},
    onGuestLogin: () -> Unit = {},
    onGoogleLogin: () -> Unit = {},
    onFacebookLogin: () -> Unit = {},
    onOpenUserAgreement: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onConsumeError: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed || state.isLoading) 0.97f else 1f,
        label = "login_button_scale"
    )
    val scrollState = rememberScrollState()
    val brand = MaterialTheme.massagerExtendedColors.band
    val brandDeep = MaterialTheme.massagerExtendedColors.bandDeep
    val fieldShape = RoundedCornerShape(14.dp)
    val fieldBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
    val fieldContent = MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AuthLogo()
                    Spacer(modifier = Modifier.height(24.dp))

                    state.errorMessage?.let { message ->
                        LaunchedEffect(message) {
                            snackbarHostState.showSnackbar(message)
                            onConsumeError()
                        }
                    }

                LoginTextField(
                    value = email,
                    onValueChange = { updated ->
                        email = updated.trimEnd { ch -> ch.isWhitespace() }
                        emailError = null
                    },
                    label = stringResource(id = R.string.login_label_email),
                    placeholder = stringResource(id = R.string.login_placeholder_email),
                    leadingIcon = Icons.Default.MailOutline,
                    isError = emailError != null,
                    supportingText = emailError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    shape = fieldShape,
                    background = fieldBackground,
                    contentColor = fieldContent
                )

                Spacer(modifier = Modifier.height(18.dp))

                LoginTextField(
                    value = password,
                    onValueChange = { updated ->
                        password = updated.trimEnd { ch -> ch.isWhitespace() }
                        passwordError = null
                    },
                    label = stringResource(id = R.string.login_label_password),
                    placeholder = stringResource(id = R.string.login_placeholder_password),
                    leadingIcon = Icons.Default.Person,
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (passwordVisible) {
                                    stringResource(id = R.string.login_hide_password)
                                } else {
                                    stringResource(id = R.string.login_show_password)
                                }
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (!state.isLoading) {
                                    attemptLogin(
                                        context,
                                        email,
                                        password,
                                    onValidationFailed = { eError, pError ->
                                        emailError = eError
                                        passwordError = pError
                                    },
                                    onSuccess = onLogin
                                )
                            }
                        }
                    ),
                    isError = passwordError != null,
                    supportingText = passwordError,
                    shape = fieldShape,
                    background = fieldBackground,
                    contentColor = fieldContent
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onNavigateToRegister) {
                        Text(
                            text = stringResource(id = R.string.login_action_signup),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = brand,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    TextButton(onClick = onForgotPassword) {
                        Text(
                            text = stringResource(id = R.string.login_action_forgot),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = brand,
                                fontWeight = FontWeight.Medium
                            ),
                            textAlign = TextAlign.End
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        attemptLogin(
                            context,
                            email,
                            password,
                            onValidationFailed = { eError, pError ->
                                emailError = eError
                                passwordError = pError
                            },
                            onSuccess = onLogin
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = buttonScale
                            scaleY = buttonScale
                        },
                    enabled = !state.isLoading,
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brand,
                        disabledContainerColor = brand.copy(alpha = 0.4f),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 2.dp
                    ),
                    interactionSource = interactionSource,
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.login_action_login),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onGuestLogin,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(
                        width = 1.6.dp,
                        brush = Brush.linearGradient(listOf(brandDeep, brand))
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = brand
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(id = R.string.login_action_guest),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(id = R.string.login_action_guest),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        text = stringResource(id = R.string.login_separator_or),
                        modifier = Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)
                ) {
                    SocialIconButton(
                        icon = painterResource(id = R.drawable.ic_google),
                        contentDescription = stringResource(id = R.string.login_google_desc),
                        onClick = onGoogleLogin,
                        enabled = !state.isLoading,
                        tint = null
                    )
                    SocialIconButton(
                        icon = rememberVectorPainter(image = Icons.Default.Facebook),
                        contentDescription = stringResource(id = R.string.login_facebook_desc),
                        onClick = onFacebookLogin,
                        enabled = !state.isLoading,
                        tint = Color(0xFF1877F2)
                    )
                }

                    Spacer(modifier = Modifier.weight(1f, fill = true))

                    AuthFooter(
                        onOpenUserAgreement = onOpenUserAgreement,
                        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 1.dp)
                    )
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
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String, // 用于 contentDescription，不展示 label
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    shape: RoundedCornerShape,
    background: Color,
    contentColor: Color
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(background)
                .defaultMinSize(minHeight = 64.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = label,
                    tint = contentColor.copy(alpha = 0.9f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 32.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = singleLine,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = contentColor),
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    cursorBrush = SolidColor(contentColor),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            trailingIcon?.let { icon ->
                Box(contentAlignment = Alignment.Center) {
                    icon()
                }
            }
        }
        supportingText?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 10.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun SocialIconButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    tint: Color?
) {
    Surface(
        shape = CircleShape,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .size(60.dp)
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                painter = icon,
                contentDescription = contentDescription,
                tint = tint ?: Color.Unspecified,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun RegisterScreen(
    state: AuthUiState,
    onRegister: (name: String, email: String, password: String, verificationCode: String) -> Unit,
    onSendVerificationCode: suspend (email: String) -> Result<Unit>,
    onNavigateToLogin: () -> Unit,
    onRegistrationHandled: () -> Unit,
    onOpenUserAgreement: (() -> Unit)? = null,
    onOpenPrivacyPolicy: (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val brand = MaterialTheme.massagerExtendedColors.band
    val brandDeep = MaterialTheme.massagerExtendedColors.bandDeep
    val fieldShape = RoundedCornerShape(14.dp)
    val fieldBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
    val fieldContent = MaterialTheme.colorScheme.onSurface
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var isSendingCode by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1_000)
            countdown -= 1
        }
    }

    LaunchedEffect(state.registrationSuccess) {
        if (state.registrationSuccess) {
            onRegistrationHandled()
            onNavigateToLogin()
        }
    }

    val scrollState = rememberScrollState()
    val canSubmit = emailError == null && passwordError == null && nameError == null &&
        name.isNotBlank() && email.isNotBlank() && password.isNotBlank() &&
        verificationCode.isNotBlank() && !state.isLoading

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            AuthLogo()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.login_action_signup),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = brand
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Start),
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = state.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    state.errorMessage?.let { message ->
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                LoginTextField(
                    value = email,
                    onValueChange = { input ->
                        val sanitized = input.trimEnd { ch -> ch.isWhitespace() }
                        email = sanitized
                        val suggestedName = sanitized.substringBefore("@", "").trim()
                        if (suggestedName.isNotEmpty()) {
                            name = suggestedName
                            nameError = null
                        } else {
                            name = ""
                        }
                        emailError = when {
                            sanitized.isBlank() -> context.getString(R.string.error_email_empty)
                            !Patterns.EMAIL_ADDRESS.matcher(sanitized).matches() -> context.getString(R.string.error_email_invalid)
                            else -> null
                        }
                    },
                    label = stringResource(id = R.string.login_label_email),
                    placeholder = stringResource(id = R.string.login_placeholder_email),
                    leadingIcon = Icons.Default.MailOutline,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    isError = emailError != null,
                    supportingText = emailError,
                    shape = fieldShape,
                    background = fieldBackground,
                    contentColor = fieldContent
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                LoginTextField(
                    modifier = Modifier.weight(1f),
                    value = verificationCode,
                    onValueChange = { input ->
                        verificationCode = input.trimEnd { ch -> ch.isWhitespace() }
                        },
                        label = stringResource(id = R.string.register_label_verification_code),
                        placeholder = stringResource(id = R.string.register_placeholder_verification_code),
                        leadingIcon = Icons.Default.MailOutline,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        shape = fieldShape,
                        background = fieldBackground,
                        contentColor = fieldContent,
                        trailingIcon = {
                            OutlinedButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    if (email.isBlank() || emailError != null) {
                                        emailError = context.getString(R.string.error_email_invalid)
                                        return@OutlinedButton
                                    }
                                    if (isSendingCode || countdown > 0) return@OutlinedButton
                                    coroutineScope.launch {
                                        isSendingCode = true
                                        countdown = 60
                                        val result = onSendVerificationCode(email.trim())
                                        if (result.isSuccess) {
                                            snackbarHostState.showSnackbar(context.getString(R.string.verification_code_sent))
                                        } else {
                                            countdown = 0
                                            snackbarHostState.showSnackbar(
                                                result.exceptionOrNull()?.message
                                                    ?: context.getString(R.string.verification_code_failed)
                                            )
                                        }
                                        isSendingCode = false
                                    }
                                },
                                enabled = countdown == 0 && !state.isLoading && !isSendingCode,
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = brand,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    brush = Brush.linearGradient(listOf(brandDeep, brand))
                                )
                            ) {
                                Text(
                                    text = if (countdown == 0) stringResource(id = R.string.register_send_code)
                                    else stringResource(id = R.string.register_send_code_countdown, countdown)
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                LoginTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = password,
                    onValueChange = { input ->
                        val sanitized = input.trimEnd { ch -> ch.isWhitespace() }
                        password = sanitized
                        passwordError = validatePassword(context, sanitized)
                    },
                    label = stringResource(id = R.string.login_label_password),
                    placeholder = stringResource(id = R.string.login_placeholder_password),
                    leadingIcon = Icons.Default.Person,
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (passwordVisible) {
                                    stringResource(id = R.string.login_hide_password)
                                } else {
                                    stringResource(id = R.string.login_show_password)
                                }
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    isError = passwordError != null,
                    supportingText = passwordError,
                    shape = fieldShape,
                    background = fieldBackground,
                    contentColor = fieldContent
                )
                Text(
                    text = stringResource(R.string.password_rule),
                    style = MaterialTheme.typography.bodySmall.copy(color = brand),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (name.isBlank()) {
                            nameError = context.getString(R.string.error_name_empty)
                            return@Button
                        }
                        if (nameError != null) return@Button
                        if (email.isBlank()) {
                            emailError = context.getString(R.string.error_email_empty)
                            return@Button
                        }
                        if (emailError != null) return@Button
                        passwordError = validatePassword(context, password)
                        if (passwordError != null) return@Button
                        if (verificationCode.isBlank()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.register_placeholder_verification_code)
                                )
                            }
                            return@Button
                        }
                        onRegister(
                            name.trim(),
                            email.trim(),
                            password.trim(),
                            verificationCode.trim()
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canSubmit,
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brand,
                        disabledContainerColor = brand.copy(alpha = 0.4f),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 2.dp
                    ),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.action_submit),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                TextButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = brand)
                ) {
                    Text(
                        text = stringResource(id = R.string.action_back_to_login),
                        style = MaterialTheme.typography.bodyMedium.copy(color = brand, fontWeight = FontWeight.Medium)
                    )
                }

                Spacer(modifier = Modifier.weight(1f, fill = true))

                AuthFooter(
                    onOpenUserAgreement = onOpenUserAgreement,
                    onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 1.dp)
                )
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
fun ForgetPasswordScreen(
    state: ForgetPasswordUiState,
    onBack: () -> Unit,
    onSendCode: (String) -> Unit,
    onSubmit: (String, String, String) -> Unit,
    onConsumeToast: () -> Unit,
    onConsumeError: () -> Unit,
    onConsumeSnackbar: () -> Unit,
    onPasswordResetSuccess: () -> Unit,
    onOpenUserAgreement: (() -> Unit)? = null,
    onOpenPrivacyPolicy: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val brand = MaterialTheme.massagerExtendedColors.band
    val brandDeep = MaterialTheme.massagerExtendedColors.bandDeep
    val fieldShape = RoundedCornerShape(14.dp)
    val fieldBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
    val fieldContent = MaterialTheme.colorScheme.onSurface

    var email by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var localCountdown by remember { mutableIntStateOf(0) }
    var isSendingLocal by remember { mutableStateOf(false) }
    var logoVisible by remember { mutableStateOf(false) }
    var emailErrorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val passwordPattern = remember { Regex("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,12}$") }
    val trimmedEmail = email.trim()
    val isEmailValid = trimmedEmail.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()
    val emailHasError = trimmedEmail.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()
    val isPasswordValid = passwordPattern.matches(password)
    val isSubmitEnabled = isEmailValid && verificationCode.isNotBlank() && isPasswordValid && !state.isResetting

    LaunchedEffect(Unit) { logoVisible = true }

    state.toastMessageRes?.let { resId ->
        LaunchedEffect(resId) {
            snackbarHostState.showSnackbar(context.getString(resId))
            onConsumeToast()
        }
    }

    state.errorMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            onConsumeError()
        }
    }

    state.snackbarMessageRes?.let { resId ->
        LaunchedEffect(resId) {
            val message = context.getString(resId)
            snackbarHostState.showSnackbar(message)
            onConsumeSnackbar()
            if (resId == R.string.password_reset_success) onPasswordResetSuccess()
        }
    }

    LaunchedEffect(localCountdown) {
        if (localCountdown > 0) {
            delay(1_000)
            localCountdown -= 1
        } else if (isSendingLocal) {
            isSendingLocal = false
        }
    }

    val sendCodeEnabled = state.countdownSeconds == 0 && localCountdown == 0 && !state.isSendingCode && !state.isResetting && !isSendingLocal

    fun handleSendCode() {
        val currentEmail = email.trim()
        if (currentEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(currentEmail).matches()) {
            emailErrorMessage = if (currentEmail.isEmpty()) {
                context.getString(R.string.error_email_empty)
            } else {
                context.getString(R.string.error_email_invalid)
            }
            email = currentEmail
        } else if (sendCodeEnabled) {
            isSendingLocal = true
            localCountdown = 60
            emailErrorMessage = null
            coroutineScope.launch {
                runCatching { onSendCode(currentEmail) }
                    .onFailure {
                        localCountdown = 0
                        isSendingLocal = false
                        snackbarHostState.showSnackbar(
                            it.message ?: context.getString(R.string.verification_code_failed)
                        )
                    }
            }
        }
    }

    fun handleSubmit() {
        if (isSubmitEnabled) onSubmit(email.trim(), verificationCode.trim(), password)
    }

    val getCodeText = if (localCountdown > 0) {
        stringResource(R.string.resend_in_seconds, localCountdown)
    } else {
        stringResource(R.string.get_code)
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                AnimatedVisibility(visible = logoVisible, enter = fadeIn()) { AuthLogo() }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.forget_password_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = brand
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Start),
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(24.dp))

                AnimatedVisibility(
                    visible = state.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    state.errorMessage?.let { message ->
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                LoginTextField(
                    value = email,
                    onValueChange = { input ->
                        email = input.trimEnd { ch -> ch.isWhitespace() }
                        emailErrorMessage = null
                    },
                    label = stringResource(id = R.string.login_label_email),
                    placeholder = stringResource(id = R.string.email_hint),
                    leadingIcon = Icons.Filled.MailOutline,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = emailErrorMessage != null || emailHasError,
                    supportingText = emailErrorMessage ?: if (emailHasError) stringResource(id = R.string.error_email_invalid) else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    shape = fieldShape,
                    background = fieldBackground,
                    contentColor = fieldContent
                )

                Spacer(modifier = Modifier.height(16.dp))

                LoginTextField(
                    value = verificationCode,
                    onValueChange = { input -> verificationCode = input.trimEnd { ch -> ch.isWhitespace() } },
                    label = stringResource(id = R.string.register_label_verification_code),
                    placeholder = stringResource(id = R.string.verification_hint),
                    leadingIcon = Icons.Filled.MailOutline,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        OutlinedButton(
                            onClick = ::handleSendCode,
                            enabled = sendCodeEnabled,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = brand,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = BorderStroke(
                                1.dp,
                                brush = Brush.linearGradient(listOf(brandDeep, brand))
                            )
                        ) {
                            if (state.isSendingCode) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = getCodeText,
                                    color = if (sendCodeEnabled) brand else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    shape = fieldShape,
                    background = fieldBackground,
                    contentColor = fieldContent
                )

                Spacer(modifier = Modifier.height(16.dp))

                LoginTextField(
                    value = password,
                    onValueChange = { input -> password = input.trimEnd { ch -> ch.isWhitespace() } },
                    label = stringResource(id = R.string.login_label_password),
                    placeholder = stringResource(id = R.string.password_hint),
                    leadingIcon = Icons.Filled.Person,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = password.isNotEmpty() && !isPasswordValid,
                    enabled = !state.isResetting,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isSubmitEnabled) handleSubmit() else focusManager.clearFocus()
                        }
                    ),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            Icon(imageVector = icon, contentDescription = null)
                        }
                    },
                    shape = fieldShape,
                    background = fieldBackground,
                    contentColor = fieldContent
                )

                Text(
                    text = stringResource(R.string.password_rule),
                    style = MaterialTheme.typography.bodySmall.copy(color = brand),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = ::handleSubmit,
                    enabled = isSubmitEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brand,
                        disabledContainerColor = brand.copy(alpha = 0.4f),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 2.dp
                    ),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    if (state.isResetting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.submit),
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                        )
                    }
                }

                TextButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = brand)
                ) {
                    Text(
                        text = stringResource(id = R.string.action_back_to_login),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = brand,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f, fill = true))

                AuthFooter(
                    onOpenUserAgreement = onOpenUserAgreement,
                    onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 1.dp)
                )
            }

            ThemedSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun AuthLogo() {
    Image(
        painter = painterResource(id = R.drawable.ic_massager_logo),
        contentDescription = "MASSAGER logo",
        modifier = Modifier.size(192.dp)
    )
}

@Composable
private fun AuthFooter(
    onOpenUserAgreement: (() -> Unit)?,
    onOpenPrivacyPolicy: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    fun openUrl(resId: Int) {
        val url = context.getString(resId)
        runCatching {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.auth_terms_prefix),
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(
                onClick = { onOpenUserAgreement?.invoke() ?: openUrl(R.string.user_agreement_url) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.auth_terms_user_agreement),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = accent,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Text(
                text = "|",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            TextButton(
                onClick = { onOpenPrivacyPolicy?.invoke() ?: openUrl(R.string.privacy_policy_url) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.auth_terms_privacy_policy),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = accent,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun SocialButton(
    buttonText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = buttonText
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = buttonText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun attemptLogin(
    context: android.content.Context,
    email: String,
    password: String,
    onValidationFailed: (emailError: String?, passwordError: String?) -> Unit,
    onSuccess: (String, String) -> Unit
) {
    val trimmedEmail = email.trim()
    val trimmedPassword = password

    var emailError: String? = null
    var passwordError: String? = null

    if (trimmedEmail.isBlank()) {
        emailError = context.getString(R.string.error_email_empty)
    } else if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
        emailError = context.getString(R.string.error_email_invalid)
    }

    if (trimmedPassword.isBlank()) {
        passwordError = context.getString(R.string.error_password_empty)
    }

    if (emailError == null && passwordError == null) {
        onSuccess(trimmedEmail, trimmedPassword)
    } else {
        onValidationFailed(emailError, passwordError)
    }
}

private fun validatePassword(context: android.content.Context, password: String): String? {
    if (password.isBlank()) return context.getString(R.string.error_password_empty)
    if (password.length !in 6..12) {
        return context.getString(R.string.error_new_password_invalid)
    }
    val hasLetter = password.any { it.isLetter() }
    val hasDigit = password.any { it.isDigit() }
    if (!hasLetter || !hasDigit) {
        return context.getString(R.string.error_password_letters_and_numbers)
    }
    return null
}
