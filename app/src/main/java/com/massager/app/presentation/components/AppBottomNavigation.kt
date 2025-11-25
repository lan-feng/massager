package com.massager.app.presentation.components

// 文件说明：底部导航栏组件，承载主要 Tab 切换。
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.massager.app.presentation.home.AppBottomTab
import com.massager.app.presentation.theme.massagerExtendedColors

@Composable
fun AppBottomNavigation(
    currentTab: AppBottomTab,
    onTabSelected: (AppBottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.massagerExtendedColors.danger
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
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
                    indicatorColor = accent.copy(alpha = 0.12f),
                    selectedIconColor = accent,
                    selectedTextColor = accent,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
