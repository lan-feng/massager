package com.massager.app.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.massager.app.presentation.home.AppBottomTab

private val AccentRed = Color(0xFFE54335)

@Composable
fun AppBottomNavigation(
    currentTab: AppBottomTab,
    onTabSelected: (AppBottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = Color.White,
        tonalElevation = 4.dp
    ) {
        AppBottomTab.visibleTabs.forEach { tab ->
            NavigationBarItem(
                selected = tab == currentTab,
                onClick = { onTabSelected(tab) },
                icon = {
                    androidx.compose.material3.Icon(
                        imageVector = tab.icon,
                        contentDescription = stringResource(id = tab.labelRes)
                    )
                },
                label = {
                    Text(text = stringResource(id = tab.labelRes))
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = AccentRed.copy(alpha = 0.12f),
                    selectedIconColor = AccentRed,
                    selectedTextColor = AccentRed,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
