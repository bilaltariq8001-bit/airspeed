package com.velocity.speedometer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.velocity.speedometer.domain.model.ThemeMode

@Immutable
data class VelocityExtendedColors(
    val isDark: Boolean,
    val divider: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val secondaryAccent: Color,
    val surfaceVariantRaised: Color,
    val gaugeTrack: Color
)

val LocalVelocityColors = staticCompositionLocalOf {
    VelocityExtendedColors(
        isDark = true,
        divider = DarkDivider,
        success = VelocitySuccess,
        warning = VelocityWarning,
        danger = VelocityDanger,
        secondaryAccent = DarkSecondary,
        surfaceVariantRaised = DarkSurfaceVariant,
        gaugeTrack = DarkDivider
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBackground,
    secondary = DarkSecondary,
    onSecondary = DarkPrimaryText,
    background = DarkBackground,
    onBackground = DarkPrimaryText,
    surface = DarkSurface,
    onSurface = DarkPrimaryText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkSecondaryText,
    outline = DarkDivider,
    error = VelocityDanger,
    onError = DarkPrimaryText
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    secondary = LightSecondary,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightPrimaryText,
    surface = LightSurface,
    onSurface = LightPrimaryText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightSecondaryText,
    outline = LightDivider,
    error = VelocityDanger,
    onError = Color.White
)

@Composable
fun VelocityTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (useDarkTheme) {
        VelocityExtendedColors(
            isDark = true,
            divider = DarkDivider,
            success = VelocitySuccess,
            warning = VelocityWarning,
            danger = VelocityDanger,
            secondaryAccent = DarkSecondary,
            surfaceVariantRaised = DarkSurfaceVariant,
            gaugeTrack = DarkDivider
        )
    } else {
        VelocityExtendedColors(
            isDark = false,
            divider = LightDivider,
            success = Color(0xFF00B85C),
            warning = Color(0xFFD98A00),
            danger = VelocityDanger,
            secondaryAccent = LightSecondary,
            surfaceVariantRaised = LightSurfaceVariant,
            gaugeTrack = LightDivider
        )
    }

    CompositionLocalProvider(LocalVelocityColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VelocityTypography,
            shapes = VelocityShapes,
            content = content
        )
    }
}
