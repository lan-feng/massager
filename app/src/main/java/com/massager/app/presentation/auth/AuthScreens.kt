package com.massager.app.presentation.auth

// 文件说明：Compose 实现的登录、注册、重置密码界面与导航。
import android.util.Patterns
import android.widget.Toast
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.massager.app.R
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
    onOpenPrivacyPolicy: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
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

                Spacer(modifier = Modifier.height(28.dp))

                AuthFooter(
                    onOpenUserAgreement = onOpenUserAgreement,
                    onOpenPrivacyPolicy = onOpenPrivacyPolicy
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    supportingText: String? = null,
    shape: RoundedCornerShape,
    background: Color,
    contentColor: Color
) {
    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background),
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.8f)
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.6f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = contentColor.copy(alpha = 0.9f)
            )
        },
        trailingIcon = trailingIcon,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = contentColor),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        isError = isError,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            errorBorderColor = MaterialTheme.colorScheme.error,
            containerColor = Color.Transparent,
            cursorColor = contentColor
        ),
        shape = shape,
        supportingText = {
            supportingText?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    )
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
    onOpenUserAgreement: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val brand = MaterialTheme.massagerExtendedColors.band
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
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
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AuthLogo()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.login_action_signup),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
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

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
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
                    label = { Text(stringResource(id = R.string.login_label_email)) },
                    placeholder = { Text(stringResource(id = R.string.login_placeholder_email)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    isError = emailError != null,
                    supportingText = {
                        emailError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = verificationCode,
                        onValueChange = { input ->
                            verificationCode = input.trimEnd { ch -> ch.isWhitespace() }
                        },
                        label = { Text(stringResource(id = R.string.register_label_verification_code)) },
                        placeholder = { Text(stringResource(id = R.string.register_placeholder_verification_code)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = {
                            focusManager.clearFocus()
                            if (email.isBlank() || emailError != null) {
                                emailError = context.getString(R.string.error_email_invalid)
                                return@OutlinedButton
                            }
                            coroutineScope.launch {
                                val result = onSendVerificationCode(email.trim())
                                if (result.isSuccess) {
                                    countdown = 60
                                    Toast.makeText(context, context.getString(R.string.verification_code_sent), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        result.exceptionOrNull()?.message ?: context.getString(R.string.verification_code_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        enabled = countdown == 0 && !state.isLoading,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            if (countdown == 0) stringResource(id = R.string.register_send_code)
                            else stringResource(id = R.string.register_send_code_countdown, countdown)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = password,
                    onValueChange = { input ->
                        val sanitized = input.trimEnd { ch -> ch.isWhitespace() }
                        password = sanitized
                        passwordError = validatePassword(context, sanitized)
                    },
                    label = { Text(stringResource(id = R.string.login_label_password)) },
                    placeholder = { Text(stringResource(id = R.string.login_placeholder_password)) },
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
                    supportingText = {
                        passwordError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

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
                            Toast.makeText(context, context.getString(R.string.register_placeholder_verification_code), Toast.LENGTH_SHORT).show()
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
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brand,
                        disabledContainerColor = brand.copy(alpha = 0.4f),
                        contentColor = MaterialTheme.massagerExtendedColors.textOnAccent
                    )
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Submit",
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
                        .padding(top = 12.dp)
                ) {
                    Text("Back to Log In")
                }

                Spacer(modifier = Modifier.height(32.dp))

                AuthFooter(
                    onOpenUserAgreement = onOpenUserAgreement,
                    onOpenPrivacyPolicy = onOpenPrivacyPolicy
                )
            }
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
    onPasswordResetSuccess: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val brand = MaterialTheme.massagerExtendedColors.band

    var email by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var logoVisible by remember { mutableStateOf(false) }

    val passwordPattern = remember { Regex("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,12}$") }
    val trimmedEmail = email.trim()
    val isEmailValid = trimmedEmail.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()
    val emailHasError = trimmedEmail.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()
    val isPasswordValid = passwordPattern.matches(password)
    val isSubmitEnabled = isEmailValid && verificationCode.isNotBlank() && isPasswordValid && !state.isResetting

    LaunchedEffect(Unit) {
        logoVisible = true
    }

    state.toastMessageRes?.let { resId ->
        LaunchedEffect(resId) {
            Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
            onConsumeToast()
        }
    }

    state.errorMessage?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onConsumeError()
        }
    }

    state.snackbarMessageRes?.let { resId ->
        LaunchedEffect(resId) {
            val message = context.getString(resId)
            snackbarHostState.showSnackbar(message)
            onConsumeSnackbar()
            if (resId == R.string.password_reset_success) {
                onPasswordResetSuccess()
            }
        }
    }

    val sendCodeEnabled = state.countdownSeconds == 0 && !state.isSendingCode && !state.isResetting

    fun handleSendCode() {
        val currentEmail = email.trim()
        if (currentEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(currentEmail).matches()) {
            Toast.makeText(context, context.getString(R.string.invalid_email), Toast.LENGTH_SHORT).show()
        } else {
            onSendCode(currentEmail)
        }
    }

    fun handleSubmit() {
        if (!isSubmitEnabled) return
        onSubmit(email.trim(), verificationCode.trim(), password)
    }

    val getCodeText = if (state.countdownSeconds > 0) {
        stringResource(R.string.resend_in_seconds, state.countdownSeconds)
    } else {
        stringResource(R.string.get_code)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.forget_password_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(visible = logoVisible, enter = fadeIn()) {
                AuthLogo()
            }

            OutlinedTextField(
                value = email,
                onValueChange = { input ->
                    email = input.trimEnd { ch -> ch.isWhitespace() }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = stringResource(id = R.string.email_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.MailOutline,
                        contentDescription = null
                    )
                },
                singleLine = true,
                isError = emailHasError,
                enabled = !state.isResetting,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            OutlinedTextField(
                value = verificationCode,
                onValueChange = { input ->
                    verificationCode = input.trimEnd { ch -> ch.isWhitespace() }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = stringResource(id = R.string.verification_hint)) },
                singleLine = true,
                enabled = !state.isResetting,
                trailingIcon = {
                    TextButton(
                        onClick = ::handleSendCode,
                        enabled = sendCodeEnabled
                    ) {
                        if (state.isSendingCode) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = getCodeText,
                                color = if (sendCodeEnabled) {
                                    brand
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            OutlinedTextField(
                value = password,
                onValueChange = { input ->
                    password = input.trimEnd { ch -> ch.isWhitespace() }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = stringResource(id = R.string.password_hint)) },
                singleLine = true,
                isError = password.isNotEmpty() && !isPasswordValid,
                enabled = !state.isResetting,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (isSubmitEnabled) {
                            handleSubmit()
                        } else {
                            focusManager.clearFocus()
                        }
                    }
                ),
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        val icon = if (passwordVisible) {
                            Icons.Filled.Visibility
                        } else {
                            Icons.Filled.VisibilityOff
                        }
                        Icon(imageVector = icon, contentDescription = null)
                    }
                }
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
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = brand,
                    disabledContainerColor = brand.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.massagerExtendedColors.textOnAccent
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

            Spacer(modifier = Modifier.height(32.dp))

            AuthFooter(
                onOpenUserAgreement = {},
                onOpenPrivacyPolicy = {}
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
    onOpenUserAgreement: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "By continuing you agree with",
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
                onClick = onOpenUserAgreement,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "User Agreement",
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
                onClick = onOpenPrivacyPolicy,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Privacy Policy",
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
        return "Use letters and numbers (no pure numbers)"
    }
    return null
}

