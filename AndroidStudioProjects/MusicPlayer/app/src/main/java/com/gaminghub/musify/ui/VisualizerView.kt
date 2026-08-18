package com.gaminghub.musify.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.hypot

@Composable
fun VisualizerView(
    fftData: ByteArray,
    waveformData: ByteArray,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF00E5FF),
    waveColor: Color = Color(0xFFFF4081)
) {
    Box(modifier = modifier) {
        // FFT Bars
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (fftData.isEmpty()) return@Canvas
            
            val barCount = 32
            val barWidth = size.width / barCount
            val maxAmplitude = 127f
            
            for (i in 0 until barCount) {
                // FFT data is [real, imag, real, imag, ...]
                val index = i * 2
                if (index + 1 >= fftData.size) break
                
                val real = fftData[index].toFloat()
                val imag = fftData[index + 1].toFloat()
                val magnitude = hypot(real, imag)
                
                val normalizedMagnitude = (magnitude / maxAmplitude).coerceIn(0f, 1f)
                val barHeight = size.height * normalizedMagnitude * 0.8f
                
                drawRect(
                    color = barColor,
                    topLeft = Offset(i * barWidth + 2f, size.height - barHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth - 4f, barHeight),
                    alpha = 0.6f
                )
            }
        }
        
        // Waveform
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (waveformData.isEmpty()) return@Canvas
            
            val points = mutableListOf<Offset>()
            val step = waveformData.size / 100 // 100 points
            
            for (i in 0 until waveformData.size step step) {
                val x = (i.toFloat() / waveformData.size.toFloat()) * size.width
                val y = (waveformData[i].toInt() + 128).toFloat() / 256f * size.height
                points.add(Offset(x, y))
            }
            
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = waveColor,
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
