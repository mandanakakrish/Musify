package com.gaminghub.musify.desktop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MusifyGreen = Color(0xFF1DB954)
val MusifyEmerald = Color(0xFF00E676)
val MusifyNeon = Color(0xFF10B981)

val BackgroundDark = Color(0xFF0F0F12)
val SurfaceDark = Color(0xFF17171C)
val SurfaceElevated = Color(0xFF22222B)
val SurfaceHighlight = Color(0xFF2E2E3A)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA1A1AA)
val TextTertiary = Color(0xFF71717A)

private val DarkColorScheme = darkColorScheme(
    primary = MusifyGreen,
    onPrimary = Color.Black,
    primaryContainer = MusifyEmerald.copy(alpha = 0.2f),
    onPrimaryContainer = MusifyEmerald,
    secondary = MusifyNeon,
    onSecondary = Color.Black,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary
)

@Composable
fun MusifyDesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
