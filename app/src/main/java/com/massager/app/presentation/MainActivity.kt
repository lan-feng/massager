package com.massager.app.presentation

// 文件说明：应用主 Activity，展示启动页并挂载 Compose 应用入口。
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.massager.app.core.preferences.LanguageManager
import com.massager.app.core.preferences.ProvideAppLocale
import com.massager.app.core.preferences.ThemeManager
import com.massager.app.presentation.theme.MyAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var languageManager: LanguageManager
    @Inject lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val appLocale by languageManager.appLocale.collectAsStateWithLifecycle()
            val appTheme by themeManager.appTheme.collectAsStateWithLifecycle()

            ProvideAppLocale(appLocale) {
                MyAppTheme(appTheme = appTheme) {
                    MassagerApp()
                }
            }
        }
    }
}
