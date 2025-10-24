package com.massager.app.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Configure your Massager application preferences.",
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = onLogout,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Log out")
        }
    }
}
