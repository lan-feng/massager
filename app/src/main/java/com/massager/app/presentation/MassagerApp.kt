package com.massager.app.presentation

// 文件说明：Compose 应用入口，包裹主题并加载全局导航主机。
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.massager.app.presentation.navigation.MassagerNavHost

@Composable
fun MassagerApp() {
    val navController = rememberNavController()
    Surface {
        MassagerNavHost(navController = navController, pendingRoute = null)
    }
}
