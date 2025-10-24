package com.massager.app.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.massager.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.massager.app.presentation.home.AppBottomTab

private val AccentRed = Color(0xFFE53935)
private val BackgroundColor = Color(0xFFF8F8F8)

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    currentTab: AppBottomTab,
    onTabSelected: (AppBottomTab) -> Unit,
    onToggleTemperature: () -> Unit,
    onClearCache: () -> Unit,
    onNavigatePersonalInfo: () -> Unit,
    onNavigateAccountSecurity: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateFavorites: () -> Unit,
    onNavigateAbout: () -> Unit,
    onLogout: () -> Unit,
    onConsumeToast: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(it)
                delay(1500)
                onConsumeToast()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundColor
    ) {
        Box {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    val avatarGreeting = stringResource(
                        R.string.settings_avatar_greeting,
                        state.user.name
                    )
                    HeaderSection(
                        user = state.user,
                        onAvatarTap = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(message = avatarGreeting)
                            }
                        },
                        onLogout = onLogout
                    )
                }

                item {
                    SettingsGroup(
                        title = stringResource(R.string.settings_profile_section),
                        items = listOf(
                            SettingsItem(
                                icon = Icons.Outlined.PersonOutline,
                                title = stringResource(R.string.settings_personal_info),
                                onClick = onNavigatePersonalInfo
                            ),
                            SettingsItem(
                                icon = Icons.Outlined.Security,
                                title = stringResource(R.string.settings_account_security),
                                onClick = onNavigateAccountSecurity
                            ),
                            SettingsItem(
                                icon = Icons.Outlined.Thermostat,
                                title = stringResource(R.string.settings_temperature_unit),
                                trailingContent = {
                                    TemperatureToggle(
                                        unit = state.user.tempUnit,
                                        onToggle = onToggleTemperature
                                    )
                                }
                            ),
                            SettingsItem(
                                icon = Icons.Outlined.Delete,
                                title = stringResource(R.string.settings_clear_cache),
                                trailingText = state.user.cacheSize,
                                onClick = onClearCache
                            )
                        )
                    )
                }

                item {
                    SettingsGroup(
                        title = stringResource(R.string.settings_other_section),
                        items = listOf(
                            SettingsItem(
                                icon = Icons.Outlined.History,
                                title = stringResource(R.string.settings_history),
                                onClick = onNavigateHistory
                            ),
                            SettingsItem(
                                icon = Icons.Outlined.FavoriteBorder,
                                title = stringResource(R.string.settings_favorites),
                                onClick = onNavigateFavorites
                            ),
                            SettingsItem(
                                icon = Icons.Filled.Info,
                                title = stringResource(R.string.settings_about),
                                onClick = onNavigateAbout
                            )
                        )
                    )
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp)
            )

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
                                tint = if (currentTab == tab) {
                                    AccentRed
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
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
}

@Composable
private fun HeaderSection(
    user: SettingsUser,
    onAvatarTap: () -> Unit,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_header_greeting),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            var isPressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 1.08f else 1f,
                animationSpec = tween(durationMillis = 240),
                label = "avatar_scale"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    shadowElevation = 8.dp,
                    color = AccentRed.copy(alpha = 0.15f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_massager_logo),
                        contentDescription = stringResource(R.string.settings_avatar_content_desc),
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isPressed = true
                                onAvatarTap()
                                isPressed = false
                            }
                    )
                }
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = stringResource(R.string.settings_user_id, "1008611"),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                TextButton(onClick = onLogout) {
                    Text(text = stringResource(R.string.settings_logout))
                }
            }
        }
    }
}

private data class SettingsItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val trailingText: String? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val onClick: (() -> Unit)? = null
)

@Composable
private fun SettingsGroup(
    title: String,
    items: List<SettingsItem>
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .shadow(2.dp, RoundedCornerShape(24.dp))
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(modifier = Modifier.height(12.dp))

        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(enabled = item.onClick != null) { item.onClick?.invoke() }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = AccentRed
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                when {
                    item.trailingContent != null -> item.trailingContent.invoke()
                    item.trailingText != null -> Text(
                        text = item.trailingText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    else -> Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = Color.Transparent
                    )
                }
            }
            if (index != items.lastIndex) {
                Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun TemperatureToggle(
    unit: TemperatureUnit,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = unit.display, style = MaterialTheme.typography.bodySmall)
        Switch(
            checked = unit == TemperatureUnit.Fahrenheit,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentRed,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
