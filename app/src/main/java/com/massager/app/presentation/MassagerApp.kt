package com.massager.app.presentation

// 文件说明：Compose 应用入口，包裹主题并加载全局导航主机。
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.Crossfade
import androidx.appcompat.app.AppCompatDelegate
import com.massager.app.presentation.navigation.MassagerNavHost
import com.massager.app.presentation.theme.MassagerTheme
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController

@Composable
fun MassagerApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("user_settings", Context.MODE_PRIVATE) }
    val pendingRouteState = remember { mutableStateOf<String?>(null) }
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        val route = prefs.getString("pref_pending_route", null)
        if (!route.isNullOrBlank()) {
            pendingRouteState.value = route
            prefs.edit().remove("pref_pending_route").apply()
        }
    }

    val themeKey = AppCompatDelegate.getDefaultNightMode()
    val localeKey = AppCompatDelegate.getApplicationLocales().toLanguageTags()

    MassagerTheme {
        Crossfade(targetState = "$themeKey-$localeKey", label = "theme_locale_crossfade") {
            Surface {
                MassagerNavHost(navController = navController, pendingRoute = pendingRouteState.value)
            }
        }
    }
}
