package com.gaminghub.musify.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DynamicAmbientBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val primaryColor by animateColorAsState(
        targetValue = colors.getOrElse(0) { Color.Black },
        animationSpec = tween(durationMillis = 1000),
        label = "PrimaryColor"
    )
    val secondaryColor by animateColorAsState(
        targetValue = colors.getOrElse(1) { Color.DarkGray },
        animationSpec = tween(durationMillis = 1000),
        label = "SecondaryColor"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Blur base layer
        Canvas(modifier = Modifier.fillMaxSize().blur(60.dp)) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.6f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.2f),
                    radius = size.maxDimension * 0.8f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(secondaryColor.copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.7f),
                    radius = size.maxDimension * 0.8f
                )
            )
        }
        
        // Frosted Glass Overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )
    }
}

@Composable
fun AnimatedEqualizer(
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    barCount: Int = 3,
    barWidth: Dp = 2.dp,
    spacing: Dp = 2.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Equalizer")
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        repeat(barCount) { index ->
            val heightScale by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400 + (index * 150), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "BarHeight_$index"
            )
            
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .fillMaxHeight(heightScale)
                    .background(color, RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp))
            )
        }
    }
}
