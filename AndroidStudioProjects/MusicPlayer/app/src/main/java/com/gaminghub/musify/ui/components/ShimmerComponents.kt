package com.gaminghub.musify.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerItem(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "translate"
    )

    val shimmerColors = listOf(
        Color.DarkGray.copy(alpha = 0.6f),
        Color.DarkGray.copy(alpha = 0.2f),
        Color.DarkGray.copy(alpha = 0.6f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

@Composable
fun ShimmerTrackList(count: Int = 5) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(count) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                ShimmerItem(modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerItem(modifier = Modifier.fillMaxWidth(0.6f).height(18.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    ShimmerItem(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp))
                }
            }
        }
    }
}

@Composable
fun ShimmerGrid(count: Int = 4) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(count) {
            Column(modifier = Modifier.width(140.dp)) {
                ShimmerItem(modifier = Modifier.size(140.dp))
                Spacer(modifier = Modifier.height(12.dp))
                ShimmerItem(modifier = Modifier.fillMaxWidth().height(16.dp))
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerItem(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp))
            }
        }
    }
}
