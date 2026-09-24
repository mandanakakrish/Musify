package com.gaminghub.musicplayer.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gaminghub.musicplayer.ui.theme.MusifyGreen
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DynamicAudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 32,
    accentColor: Color = MusifyGreen
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VisualizerTransition")
    
    // Fast rhythmic beat pulse animation
    val beatPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) (Math.PI * 2).toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beatPhase"
    )

    // Secondary mid-frequency harmonic wave
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) (Math.PI * 4).toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    // High frequency treble wave
    val treblePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) (Math.PI * 6).toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "treblePhase"
    )

    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val barWidth = (width / (barCount * 1.6f)).coerceAtLeast(3f)
        val gap = (width - (barCount * barWidth)) / (barCount - 1).coerceAtLeast(1)

        val neonGradient = Brush.verticalGradient(
            colors = listOf(
                accentColor,
                Color(0xFF00E5FF),
                Color(0xFF7C4DFF).copy(alpha = 0.8f),
                Color.Transparent
            )
        )

        // 1. Draw dynamic 32-band reactive frequency equalizer bars
        for (i in 0 until barCount) {
            val x = i * (barWidth + gap)
            val normalizedI = i.toFloat() / barCount

            val bassMod = sin(beatPhase + normalizedI * 3.5).toFloat()
            val midMod = sin(wavePhase * 1.3f + normalizedI * 6.0 + 0.8).toFloat()
            val trebleMod = cos(treblePhase * 0.9f + normalizedI * 12.0).toFloat()

            val combined = if (isPlaying) {
                ((bassMod * 0.5f + midMod * 0.35f + trebleMod * 0.15f + 1f) / 2f).coerceIn(0.08f, 1f)
            } else {
                0.05f
            }

            val barHeight = (height * 0.85f * combined).coerceAtLeast(4f)
            val y = (height - barHeight) / 2f

            drawRoundRect(
                brush = neonGradient,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }

        // 2. Draw dual glowing audio oscillogram waves (Center Line)
        if (isPlaying) {
            val upperPath = Path()
            val lowerPath = Path()

            for (x in 0..width.toInt() step 4) {
                val normX = x / width
                val w1 = sin(beatPhase * 2.0 + normX * 8.0).toFloat() * (height * 0.15f)
                val w2 = cos(wavePhase + normX * 14.0).toFloat() * (height * 0.08f)
                
                val currentY1 = centerY + (w1 + w2)
                val currentY2 = centerY - (w1 + w2) * 0.7f

                if (x == 0) {
                    upperPath.moveTo(x.toFloat(), currentY1)
                    lowerPath.moveTo(x.toFloat(), currentY2)
                } else {
                    upperPath.lineTo(x.toFloat(), currentY1)
                    lowerPath.lineTo(x.toFloat(), currentY2)
                }
            }

            drawPath(
                path = upperPath,
                color = Color.White.copy(alpha = 0.85f),
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )

            drawPath(
                path = lowerPath,
                color = Color(0xFF00E5FF).copy(alpha = 0.5f),
                style = Stroke(width = 1.5f, cap = StrokeCap.Round)
            )
        }
    }
}
