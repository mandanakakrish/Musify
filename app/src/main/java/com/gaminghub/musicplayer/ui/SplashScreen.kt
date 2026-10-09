package com.gaminghub.musicplayer.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaminghub.musicplayer.R
import com.gaminghub.musicplayer.ui.components.DynamicAudioVisualizer
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ConcentricAcousticWaves(
    modifier: Modifier = Modifier,
    logoCenterFractionY: Float = 0.42f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "acousticWaves")
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleProgress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width * 0.50f, size.height * logoCenterFractionY)
        val minRadius = 80.dp.toPx()
        val maxRadius = 240.dp.toPx()
        val ringSpacing = 8.dp.toPx()
        val numRings = ((maxRadius - minRadius) / ringSpacing).toInt()

        for (i in 0 until numRings) {
            val r = minRadius + ((i + rippleProgress) % numRings) * ringSpacing
            val norm = (r - minRadius) / (maxRadius - minRadius)

            val alpha = (kotlin.math.sin(norm * Math.PI).toFloat().coerceIn(0f, 1f) * 0.35f)

            drawCircle(
                color = Color(0xFF143048).copy(alpha = alpha),
                radius = r,
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}

@Composable
fun SplashScreen(
    isAppReady: Boolean = true,
    onSplashFinished: () -> Unit
) {
    // 0ms gap: Logo is rendered immediately at full opacity matching the window starting preview
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    // Ensure ripple waves play for at least a minimum smooth period (e.g. 1000ms),
    // and hold until all initial data (controller, local tracks, feeds) finishes loading!
    var minDurationPassed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1000L.milliseconds)
        minDurationPassed = true
    }

    LaunchedEffect(isAppReady, minDurationPassed) {
        if (isAppReady && minDurationPassed) {
            onSplashFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF04060C)),
        contentAlignment = Alignment.Center
    ) {
        // Animated concentric acoustic wave rings emanating outward from center
        ConcentricAcousticWaves(
            modifier = Modifier.fillMaxSize(),
            logoCenterFractionY = 0.42f
        )

        // Center Content: Logo + Typography
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale)
                .padding(bottom = 60.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                // Soft ambient radial glow
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(glowPulse)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00E5FF).copy(alpha = 0.22f),
                                    Color(0xFFE040FB).copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Luminous Neon Musify Logo
                Image(
                    painter = painterResource(id = R.drawable.musify_logo_transparent),
                    contentDescription = "Musify Logo",
                    modifier = Modifier.size(170.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Musify",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Pure Sound • Lossless Streaming",
                color = Color(0xFF9EA3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.6.sp
            )
        }

        // Bottom Animated Visualizer Indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 54.dp)
        ) {
            DynamicAudioVisualizer(
                isPlaying = true,
                barCount = 20,
                modifier = Modifier
                    .width(180.dp)
                    .height(38.dp)
            )
        }
    }
}
