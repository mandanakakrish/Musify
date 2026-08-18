package com.gaminghub.musify.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.gaminghub.musify.ui.viewmodels.AudioEngineViewModel
import com.gaminghub.musify.ui.viewmodels.PlaybackViewModel
import kotlin.math.log10

enum class VisualizerStyle {
    BARS
}

@Composable
fun ModernVisualizerOverlay(
    audioEngineViewModel: AudioEngineViewModel,
    playbackViewModel: PlaybackViewModel,
    modifier: Modifier = Modifier,
    style: VisualizerStyle = VisualizerStyle.BARS,
    barColor: Color = Color(0xFF00E5FF)
) {
    val fftData by audioEngineViewModel.fftData.collectAsState()
    val isPlaying by playbackViewModel.isPlaying.collectAsState()

    // Smooth transition for bars when music stops
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer_glow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val barCount = 40
        val barSpacing = width / barCount
        val maxBarHeight = height * 0.4f

        if (isPlaying && fftData.isNotEmpty()) {
            drawBars(fftData, width, height, barCount, maxBarHeight, barSpacing, barColor, alpha)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBars(
    fftData: ByteArray,
    width: Float,
    height: Float,
    barCount: Int,
    maxBarHeight: Float,
    barSpacing: Float,
    barColor: Color,
    alpha: Float
) {
    for (i in 0 until barCount) {
        val index = (i * (fftData.size / 2 / barCount)) * 2
        if (index + 1 >= fftData.size) break
        
        val real = fftData[index].toFloat()
        val imag = fftData[index + 1].toFloat()
        val magnitude = Math.sqrt((real * real + imag * imag).toDouble()).toFloat()
        
        val scaledHeight = (log10(magnitude.coerceAtLeast(1f)) * 40f).coerceIn(4f, maxBarHeight)
        
        val x = i * barSpacing + (barSpacing / 2)
        val startY = height - 10f
        val endY = height - scaledHeight
        
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(barColor.copy(alpha = alpha), barColor.copy(alpha = 0.1f)),
                startY = endY,
                endY = startY
            ),
            start = Offset(x, startY),
            end = Offset(x, endY),
            strokeWidth = barSpacing * 0.6f,
            cap = StrokeCap.Round
        )
    }
}
