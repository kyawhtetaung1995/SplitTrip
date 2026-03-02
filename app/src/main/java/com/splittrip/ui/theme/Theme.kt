package com.splittrip.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand Colors
val Primary = Color(0xFF6C63FF)
val PrimaryVariant = Color(0xFF3F3D89)
val Secondary = Color(0xFF03DAC6)
val Background = Color(0xFFF8F9FE)
val Surface = Color(0xFFFFFFFF)
val Error = Color(0xFFE53935)
val Success = Color(0xFF43A047)
val Warning = Color(0xFFFB8C00)

val OnPrimary = Color(0xFFFFFFFF)
val OnBackground = Color(0xFF1A1A2E)
val OnSurface = Color(0xFF1A1A2E)
val CardBackground = Color(0xFFFFFFFF)
val Divider = Color(0xFFEEEEEE)

// Dark theme
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E2E)
val DarkCard = Color(0xFF2A2A3E)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    background = Background,
    surface = Surface,
    error = Error,
    onBackground = OnBackground,
    onSurface = OnSurface,
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    background = DarkBackground,
    surface = DarkSurface,
    error = Error,
)

@Composable
fun SplitTripTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
