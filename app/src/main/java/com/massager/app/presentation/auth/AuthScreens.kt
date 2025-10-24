package com.massager.app.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    state: AuthUiState,
    onLogin: (email: String, password: String) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    AuthScaffold(
        title = "Massager Login",
        state = state,
        primaryActionLabel = "Log in",
        secondaryActionLabel = "Create account",
        onSecondaryAction = onNavigateToRegister,
        onPrimaryAction = onLogin
    )
}

@Composable
fun RegisterScreen(
    state: AuthUiState,
    onRegister: (name: String, email: String, password: String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    AuthScaffold(
        title = "Create Massager Account",
        state = state,
        primaryActionLabel = "Register",
        secondaryActionLabel = "Already have an account?",
        onSecondaryAction = onNavigateToLogin,
        onPrimaryAction = { email, password ->
            onRegister("Massager User", email, password)
        }
    )
}

@Composable
private fun AuthScaffold(
    title: String,
    state: AuthUiState,
    primaryActionLabel: String,
    secondaryActionLabel: String,
    onSecondaryAction: () -> Unit,
    onPrimaryAction: (email: String, password: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Button(
            onClick = { onPrimaryAction(email, password) },
            enabled = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(4.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(primaryActionLabel)
            }
        }
        Button(
            onClick = onSecondaryAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text(secondaryActionLabel)
        }
    }
}
