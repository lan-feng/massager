package com.massager.app.presentation.components

// 文件说明：统一的 Snackbar 样式，使用主题主色并自动适配导航栏内边距。
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ThemedSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    applyNavigationPadding: Boolean = true
) {
    // Use inverse colors so snackbar contrasts with both light & dark themes, slightly lightened
    val baseContainer = MaterialTheme.colorScheme.inverseSurface
    val container = baseContainer.copy(alpha = 0.92f)
    val content = MaterialTheme.colorScheme.inverseOnSurface
    SnackbarHost(
        hostState = hostState,
        modifier = if (applyNavigationPadding) {
            modifier.navigationBarsPadding()
        } else {
            modifier
        },
        snackbar = { data ->
            Snackbar(
                snackbarData = data,
                containerColor = container,
                contentColor = content,
                actionColor = content,
                dismissActionContentColor = content
            )
        }
    )
}
