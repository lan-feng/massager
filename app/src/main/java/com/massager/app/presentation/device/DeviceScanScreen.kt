package com.massager.app.presentation.device

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DeviceScanScreen(
    viewModel: DeviceViewModel,
    onBack: () -> Unit
) {
    val devices by viewModel.pairedDevices.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Nearby Massager Devices",
            style = MaterialTheme.typography.titleLarge
        )
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(devices) { deviceName ->
                Text(text = deviceName, modifier = Modifier.padding(vertical = 8.dp))
            }
            if (devices.isEmpty()) {
                item {
                    Text(
                        text = "No paired devices detected. Enable Bluetooth to start scanning.",
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
        }
        Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
            Text("Back")
        }
    }
}
