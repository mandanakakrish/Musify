package com.gaminghub.musify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A reusable container that provides a 'Frosted Glass' (Glassmorphism) effect.
 * Uses background blurring and semi-transparent gradients for a premium look.
 */
@Composable
fun FrostedGlassContainer(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 20.dp,
    cornerRadius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                ),
                RoundedCornerShape(cornerRadius)
            )
    ) {
        // Subtle internal glow/blur
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(blurRadius)
                .background(Color.White.copy(alpha = 0.02f))
        )
        
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
