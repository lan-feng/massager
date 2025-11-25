package com.massager.app.presentation.theme

// 文件说明：封装 Massager 的主题装饰，注入颜色、排版并提供给全局界面。
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue40,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlue90,
    onPrimaryContainer = PrimaryBlue10,
    secondary = SecondaryMint60,
    onSecondary = Color.White,
    secondaryContainer = SecondaryMint90,
    onSecondaryContainer = SecondaryMint10,
    tertiary = TertiaryOrange60,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryOrange90,
    onTertiaryContainer = TertiaryOrange10,
    error = Error40,
    onError = Color.White,
    errorContainer = Error90,
    onErrorContainer = Error10,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    inverseOnSurface = TextPrimaryDark,
    inverseSurface = SurfaceDark,
    inversePrimary = PrimaryBlue80
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue80,
    onPrimary = PrimaryBlue20,
    primaryContainer = PrimaryBlue35,
    onPrimaryContainer = PrimaryBlue90,
    secondary = SecondaryMint80,
    onSecondary = SecondaryMint10,
    secondaryContainer = SecondaryMint40,
    onSecondaryContainer = SecondaryMint90,
    tertiary = TertiaryOrange80,
    onTertiary = TertiaryOrange10,
    tertiaryContainer = TertiaryOrange40,
    onTertiaryContainer = TertiaryOrange90,
    error = Error80,
    onError = Error10,
    errorContainer = Error40,
    onErrorContainer = Error90,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    inverseOnSurface = TextPrimaryLight,
    inverseSurface = SurfaceLight,
    inversePrimary = PrimaryBlue40
)

@Composable
fun MassagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            // Use surface for both bars to ensure good contrast with icon appearance
            val barsColor = colorScheme.surface.toArgb()
            window?.statusBarColor = barsColor
            window?.navigationBarColor = barsColor
            val controller = ViewCompat.getWindowInsetsController(view)
            controller?.isAppearanceLightStatusBars = !darkTheme
            controller?.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalMassagerExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MassagerTypography,
            content = content
        )
    }
}
