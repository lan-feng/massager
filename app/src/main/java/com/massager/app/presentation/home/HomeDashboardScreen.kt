package com.massager.app.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.annotation.StringRes
import com.massager.app.R
import com.massager.app.domain.model.DeviceMetadata
import kotlinx.coroutines.delay
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity

private val AccentRed = Color(0xFFE53935)

@Composable
fun HomeDashboardScreen(
    state: HomeUiState,
    currentTab: AppBottomTab,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    onAddDevice: () -> Unit,
    onDeviceClick: (DeviceMetadata) -> Unit,
    onConsumeDeviceAddedToast: () -> Unit,
    onTabSelected: (AppBottomTab) -> Unit
) {
    val density = LocalDensity.current
    val pullThresholdPx = with(density) { 120.dp.toPx() }
    var pullOffset by remember { mutableStateOf(0f) }
    val nestedScrollConnection = remember(state.isRefreshing) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.Drag && available.y > 0f && !state.isRefreshing) {
                    pullOffset += available.y
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.Drag) {
                    pullOffset = (pullOffset + available.y).coerceAtLeast(0f)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!state.isRefreshing && pullOffset > pullThresholdPx) {
                    onRefresh()
                }
                pullOffset = 0f
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                pullOffset = 0f
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(state.isRefreshing) {
        if (!state.isRefreshing) {
            pullOffset = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        HomeContent(
            state = state,
            onAddDevice = onAddDevice,
            onDismissError = onDismissError,
            onDeviceClick = onDeviceClick
        )

        val showPullIndicator = pullOffset > 0f || state.isRefreshing
        if (showPullIndicator) {
            val progress = (pullOffset / pullThresholdPx).coerceIn(0f, 1f)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                tonalElevation = 4.dp,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.isRefreshing) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = AccentRed
                        )
                    } else {
                        androidx.compose.material3.CircularProgressIndicator(
                            progress = progress,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = AccentRed
                        )
                    }
                    Text(
                        text = when {
                            state.isRefreshing -> "Refreshing..."
                            progress >= 1f -> "Release to refresh"
                            else -> "Pull to refresh"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = state.showDeviceAddedToast,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AccentRed,
                    contentColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    text = "Success",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        LaunchedEffect(state.showDeviceAddedToast) {
            if (state.showDeviceAddedToast) {
                delay(2000)
                onConsumeDeviceAddedToast()
            }
        }

        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 4.dp,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AppBottomTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = currentTab == tab,
                    onClick = { onTabSelected(tab) },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = stringResource(tab.labelRes),
                            tint = when {
                                tab == AppBottomTab.Profile -> AccentRed
                                currentTab == tab -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(tab.labelRes),
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = AccentRed.copy(alpha = 0.12f),
                        selectedIconColor = AccentRed,
                        selectedTextColor = AccentRed
                    )
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onAddDevice: () -> Unit,
    onDismissError: () -> Unit,
    onDeviceClick: (DeviceMetadata) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 72.dp)
    ) {
        val isTablet = maxWidth > 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isTablet) 32.dp else 20.dp)
        ) {
            TopBar(onAddDevice = onAddDevice)

            state.errorMessage?.let { message ->
                ToastCard(
                    message = message,
                    actionLabel = "Dismiss",
                    onAction = onDismissError
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = "Common Device",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))

            val devices = if (state.devices.isEmpty()) {
                listOf(
                    DeviceMetadata(
                        id = "demo",
                        name = "SmartPulse TENS Unit",
                        macAddress = "SmartPulse TENS Device",
                        isConnected = true
                    )
                )
            } else {
                state.devices
            }

            if (isTablet) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(280.dp),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(devices) { device ->
                        DeviceCard(
                            device = device,
                            subtitle = device.macAddress ?: "SmartPulse TENS Device",
                            onClick = onDeviceClick
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = devices,
                        key = { it.id }
                    ) { device ->
                        DeviceCard(
                            device = device,
                            subtitle = device.macAddress ?: "SmartPulse TENS Device",
                            onClick = onDeviceClick
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(96.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(onAddDevice: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Welcome to MASSAGER",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Manage your wellness devices",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        IconButton(
            onClick = onAddDevice,
            modifier = Modifier
                .size(48.dp)
                .background(AccentRed.copy(alpha = 0.12f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add device",
                tint = AccentRed
            )
        }
    }
}

@Composable
private fun DeviceCard(
    device: DeviceMetadata,
    subtitle: String,
    onClick: (DeviceMetadata) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clickable { onClick(device) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = AccentRed.copy(alpha = 0.12f),
                modifier = Modifier.size(56.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_massager_logo),
                    contentDescription = device.name,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            val statusColor = if (device.isConnected) Color(0xFF4CAF50) else Color(0xFFFFA000)
            Text(
                text = if (device.isConnected) "Online" else "Offline",
                color = statusColor,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun ToastCard(
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

enum class AppBottomTab(@StringRes val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Home(R.string.settings_tab_home, Icons.Filled.Home),
    Manual(R.string.settings_tab_manual, Icons.Filled.Book),
    Devices(R.string.settings_tab_devices, Icons.Filled.Devices),
    Profile(R.string.settings_tab_profile, Icons.Filled.Person)
}
