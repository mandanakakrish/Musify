package com.gaminghub.musify.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LiquidAmbientBackground(
    ambientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    val color1 by animateColorAsState(
        targetValue = ambientColors.getOrNull(0) ?: Color.Black,
        animationSpec = tween(2000),
        label = "color1"
    )
    val color2 by animateColorAsState(
        targetValue = ambientColors.getOrNull(1) ?: Color.DarkGray,
        animationSpec = tween(2000),
        label = "color2"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "liquid_bg")
    
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase2"
    )

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Layer 1: Base slow moving gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(color1.copy(alpha = 0.6f), color2.copy(alpha = 0.4f)),
                        startY = 0f,
                        endY = 2000f * phase1
                    )
                )
                .blur(80.dp)
        )

        // Layer 2: Radial "Blobs" that move
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(color2.copy(alpha = 0.5f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(
                            x = 500f + (200f * kotlin.math.sin(phase2 * 2 * Math.PI.toFloat())),
                            y = 800f + (300f * kotlin.math.cos(phase2 * 2 * Math.PI.toFloat()))
                        ),
                        radius = 1200f
                    )
                )
                .blur(100.dp)
        )

        // Layer 3: Subtle shimmer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.Transparent, color1.copy(alpha = 0.2f), Color.Transparent),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(2000f * phase1, 2000f * phase1)
                    )
                )
        )
    }
}
