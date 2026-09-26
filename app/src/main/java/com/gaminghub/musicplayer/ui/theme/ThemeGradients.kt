package com.gaminghub.musicplayer.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class AppThemeGradients(
    val backgroundBrush: Brush,
    val cardBrush: Brush,
    val bottomSheetBrush: Brush,
    val backgroundColor: Color,
    val cardColor: Color
)

val LocalAppGradients = compositionLocalOf {
    AppThemeGradients(
        backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF121212), Color(0xFF121212))),
        cardBrush = Brush.verticalGradient(listOf(Color(0xFF1E1E1E), Color(0xFF1E1E1E))),
        bottomSheetBrush = Brush.verticalGradient(listOf(Color(0xFF181818), Color(0xFF181818))),
        backgroundColor = Color(0xFF121212),
        cardColor = Color(0xFF1E1E1E)
    )
}

object ThemeGradientHelper {
    fun getDarkPresets(): List<List<Color>> = listOf(
        listOf(Color(0xFF2C2C34), Color(0xFF0F0F14)),
        listOf(Color(0xFF35353C), Color(0xFF141418)),
        listOf(Color(0xFF1F2538), Color(0xFF0B0E18)),
        listOf(Color(0xFF301D38), Color(0xFF110816)),
        listOf(Color(0xFF1C3026), Color(0xFF09140E))
    )

    fun getLightPresets(): List<List<Color>> = listOf(
        listOf(Color(0xFFFFFFFF), Color(0xFFD6DDE4)),
        listOf(Color(0xFFFFFDF8), Color(0xFFE2D6C5)),
        listOf(Color(0xFFF2F8FC), Color(0xFFCDE1F0)),
        listOf(Color(0xFFFAF2FC), Color(0xFFE0CEEC)),
        listOf(Color(0xFFF2FAF5), Color(0xFFCCEBD7))
    )

    fun getPresets(isDark: Boolean): List<List<Color>> =
        if (isDark) getDarkPresets() else getLightPresets()
}
