package com.massager.app.presentation.auth

import android.util.Patterns
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val AccentRed = Color(0xFFE53935)

@Composable
fun LoginScreen(
    state: AuthUiState,
    onLogin: (email: String, password: String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onForgotPassword: () -> Unit = {},
    onGuestLogin: () -> Unit = {},
    onGoogleLogin: () -> Unit = {},
    onFacebookLogin: () -> Unit = {}
) {
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
                    text = "Log In",
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
                    onValueChange = { updated ->
                        email = updated.trimEnd { ch -> ch.isWhitespace() }
                        emailError = null
                    },
                    label = { Text("Email") },
                    placeholder = { Text("Please enter your email") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.MailOutline,
                            contentDescription = "Email"
                        )
                    },
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

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = password,
                    onValueChange = { updated ->
                        password = updated.trimEnd { ch -> ch.isWhitespace() }
                        passwordError = null
                    },
                    label = { Text("Password") },
                    placeholder = { Text("Please enter your password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Password"
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (passwordVisible) {
                                    "Hide password"
                                } else {
                                    "Show password"
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
                        attemptLogin(
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
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed,
                        disabledContainerColor = AccentRed.copy(alpha = 0.4f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp
                    ),
                    interactionSource = interactionSource
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Log In",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onNavigateToRegister) {
                        Text("Sign Up")
                    }
                    TextButton(onClick = onForgotPassword) {
                        Text("Forget your password")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Divider(modifier = Modifier.weight(1f))
                    Text(
                        text = "Or continue with",
                        modifier = Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Divider(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(24.dp))

                SocialButton(
                    buttonText = "Continue as Guest",
                    icon = Icons.Default.Person,
                    onClick = onGuestLogin,
                    enabled = !state.isLoading
                )
                Spacer(modifier = Modifier.height(16.dp))
                SocialButton(
                    buttonText = "Continue with Google",
                    icon = Icons.Default.MailOutline,
                    onClick = onGoogleLogin,
                    enabled = !state.isLoading
                )
                Spacer(modifier = Modifier.height(16.dp))
                SocialButton(
                    buttonText = "Continue with Facebook",
                    icon = Icons.Default.Facebook,
                    onClick = onFacebookLogin,
                    enabled = !state.isLoading
                )

                Spacer(modifier = Modifier.height(32.dp))

                AuthFooter()
            }
        }
    }
}

@Composable
fun RegisterScreen(
    state: AuthUiState,
    onRegister: (name: String, email: String, password: String, verificationCode: String) -> Unit,
    onSendVerificationCode: suspend (email: String) -> Result<Unit>,
    onNavigateToLogin: () -> Unit,
    onRegistrationHandled: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
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
                    text = "Sign Up",
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
                            sanitized.isBlank() -> "Email cannot be empty"
                            !Patterns.EMAIL_ADDRESS.matcher(sanitized).matches() -> "Please enter a valid email"
                            else -> null
                        }
                    },
                    label = { Text("Email") },
                    placeholder = { Text("Please enter your email") },
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
                        label = { Text("Verification code") },
                        placeholder = { Text("Please enter the verification code") },
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
                                emailError = "Please enter a valid email"
                                return@OutlinedButton
                            }
                            coroutineScope.launch {
                                val result = onSendVerificationCode(email.trim())
                                if (result.isSuccess) {
                                    countdown = 60
                                    Toast.makeText(context, "Verification code sent", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        result.exceptionOrNull()?.message ?: "Failed to send verification code",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        enabled = countdown == 0 && !state.isLoading,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(if (countdown == 0) "Get code" else "${countdown}s")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = password,
                    onValueChange = { input ->
                        val sanitized = input.trimEnd { ch -> ch.isWhitespace() }
                        password = sanitized
                        passwordError = validatePassword(sanitized)
                    },
                    label = { Text("Password") },
                    placeholder = { Text("Please enter your password") },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (passwordVisible) {
                                    "Hide password"
                                } else {
                                    "Show password"
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
                            nameError = "Name cannot be empty"
                            return@Button
                        }
                        if (nameError != null) return@Button
                        if (email.isBlank()) {
                            emailError = "Email cannot be empty"
                            return@Button
                        }
                        if (emailError != null) return@Button
                        passwordError = validatePassword(password)
                        if (passwordError != null) return@Button
                        if (verificationCode.isBlank()) {
                            Toast.makeText(context, "Verification code cannot be empty", Toast.LENGTH_SHORT).show()
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
                        containerColor = AccentRed,
                        disabledContainerColor = AccentRed.copy(alpha = 0.4f)
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

                AuthFooter()
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
    val accentRed = Color(0xFFE54335)
    val accentDarkRed = Color(0xFFD22020)
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

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
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(listOf(accentRed, accentDarkRed))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_massager_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(12.dp)
                        )
                    }
                }
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
                                    accentRed
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
                style = MaterialTheme.typography.bodySmall.copy(color = accentRed),
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
                colors = ButtonDefaults.buttonColors(containerColor = accentRed),
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

            AuthFooter()
        }
    }
}

@Composable
private fun AuthLogo() {
    Surface(
        modifier = Modifier.size(96.dp),
        shape = CircleShape,
        color = AccentRed
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_massager_logo),
            contentDescription = "MASSAGER logo",
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        )
    }
}

@Composable
private fun AuthFooter() {
    Text(
        text = "By continuing you agree with xyj's User Agreement and Privacy Policy",
        style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        ),
        textAlign = TextAlign.Center
    )
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
        emailError = "Email cannot be empty"
    } else if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
        emailError = "Please enter a valid email"
    }

    if (trimmedPassword.isBlank()) {
        passwordError = "Password cannot be empty"
    }

    if (emailError == null && passwordError == null) {
        onSuccess(trimmedEmail, trimmedPassword)
    } else {
        onValidationFailed(emailError, passwordError)
    }
}

private fun validatePassword(password: String): String? {
    if (password.isBlank()) return "Password cannot be empty"
    if (password.length !in 6..12) {
        return "Password must be 6-12 characters"
    }
    val hasLetter = password.any { it.isLetter() }
    val hasDigit = password.any { it.isDigit() }
    if (!hasLetter || !hasDigit) {
        return "Use letters and numbers (no pure numbers)"
    }
    return null
}




