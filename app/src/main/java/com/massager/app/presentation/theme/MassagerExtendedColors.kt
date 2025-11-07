package com.massager.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class MassagerExtendedColors(
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textDisabled: Color,
    val textOnAccent: Color,
    val iconPrimary: Color,
    val iconMuted: Color,
    val surfaceBright: Color,
    val surfaceSubtle: Color,
    val surfaceStrong: Color,
    val divider: Color,
    val outline: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val accentSoft: Color
)

internal val LightExtendedColors = MassagerExtendedColors(
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    textMuted = TextMutedLight,
    textDisabled = TextDisabledLight,
    textOnAccent = Color.White,
    iconPrimary = TextSecondaryLight,
    iconMuted = TextMutedLight,
    surfaceBright = SurfaceLight,
    surfaceSubtle = SurfaceSubtleLight,
    surfaceStrong = SurfaceStrongLight,
    divider = DividerLight,
    outline = OutlineLight,
    success = SuccessLight,
    warning = WarningLight,
    danger = DangerLight,
    info = InfoLight,
    accentSoft = AccentSoftLight
)

internal val DarkExtendedColors = MassagerExtendedColors(
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    textMuted = TextMutedDark,
    textDisabled = TextDisabledDark,
    textOnAccent = Color.White,
    iconPrimary = TextSecondaryDark,
    iconMuted = TextMutedDark,
    surfaceBright = SurfaceDark,
    surfaceSubtle = SurfaceSubtleDark,
    surfaceStrong = SurfaceStrongDark,
    divider = DividerDark,
    outline = OutlineDark,
    success = SuccessDark,
    warning = WarningDark,
    danger = DangerDark,
    info = InfoDark,
    accentSoft = AccentSoftDark
)

val LocalMassagerExtendedColors = staticCompositionLocalOf { LightExtendedColors }

val MaterialTheme.massagerExtendedColors: MassagerExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalMassagerExtendedColors.current
