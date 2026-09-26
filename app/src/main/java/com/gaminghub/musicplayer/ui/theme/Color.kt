package com.gaminghub.musicplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Spotify Pure Green Fallback constant
val DefaultSpotifyGreen = Color(0xFF1DB954)
val DefaultSpotifyGreenDark = Color(0xFF1ED760)

// Dynamic Musify Accent Color — Automatically reflects chosen Accent Color & Hue!
val MusifyGreen: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary

val MusifyGreenDark: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)

val MusifyPurple: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary

val MusifyCyan: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary

val MusifyDarkBg = Color(0xFF000000)
val MusifyCardBg = Color(0xFF121212)
val MusifyGlassSurface = Color(0xFF181818)
val MusifyGlassBorder = Color(0x22FFFFFF)
val MusifyTextPrimary = Color(0xFFFFFFFF)
val MusifyTextSecondary = Color(0xFFB3B3B3)
val MusifyAccentGlow: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)