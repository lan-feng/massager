package com.massager.app.presentation

// 文件说明：Compose 应用入口，包裹主题并加载全局导航主机。
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.massager.app.presentation.navigation.MassagerNavHost
import com.massager.app.presentation.theme.MassagerTheme

@Composable
fun MassagerApp() {
    MassagerTheme {
        Surface {
            MassagerNavHost()
        }
    }
}
