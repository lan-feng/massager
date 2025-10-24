package com.massager.app.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.massager.app.domain.model.DeviceMetadata
import com.massager.app.domain.model.TemperatureRecord

@Composable
fun HomeDashboardScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        state.errorMessage?.let { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    TextButton(onClick = onDismissError) {
                        Text("Dismiss")
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Massager Dashboard",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = onRefresh, enabled = !state.isRefreshing) {
                Text(if (state.isRefreshing) "Refreshing…" else "Refresh")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Connected Devices",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(state.devices) { device ->
                    DeviceRow(device)
                }
                if (state.devices.isEmpty()) {
                    item {
                        Text("No devices found.")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Recent Measurements",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(state.measurements) { measurement ->
                    MeasurementRow(measurement)
                }
                if (state.measurements.isEmpty()) {
                    item {
                        Text("No measurements available.")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: DeviceMetadata) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(device.name, fontWeight = FontWeight.Medium)
        Text("MAC: ${device.macAddress ?: "N/A"}")
        Text(if (device.isConnected) "Connected" else "Disconnected")
    }
}

@Composable
private fun MeasurementRow(record: TemperatureRecord) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("Temperature: ${record.celsius} ℃")
        Text("Time: ${record.recordedAt}")
    }
}
