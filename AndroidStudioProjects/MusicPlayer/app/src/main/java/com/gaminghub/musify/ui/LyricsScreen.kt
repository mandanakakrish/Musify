package com.gaminghub.musify.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.gaminghub.musify.ui.viewmodels.PlaybackViewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@UnstableApi
@Composable
fun LyricsScreen(
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit
) {
    val currentTrack by playbackViewModel.currentTrack.collectAsState()
    val syncedLyrics by playbackViewModel.syncedLyrics.collectAsState()
    val plainLyrics by playbackViewModel.lyrics.collectAsState()
    val currentLyricIndex by playbackViewModel.currentLyricIndex.collectAsState()
    val isPlaying by playbackViewModel.isPlaying.collectAsState()
    val currentPosition by playbackViewModel.currentPosition.collectAsState()
    val duration by playbackViewModel.duration.collectAsState()
    
    // Fade in animation for the whole screen
    val alphaAnim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600),
        label = "fade_in"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black).graphicsLayer { alpha = alphaAnim }) {
        // ── Dynamic Animated Background ───────────────────────
        DynamicAmbientBackground(albumArtUrl = currentTrack?.albumArtUrl)

        // ── Main Content Layer ────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowDown, "Back", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentTrack?.title ?: "Unknown Title",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentTrack?.artist ?: "Unknown Artist",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(48.dp)) // Padding for symmetry
            }

            // Lyrics List
            Box(modifier = Modifier.weight(1f)) {
                if (syncedLyrics.isNotEmpty()) {
                    val listState = rememberLazyListState()
                    
                    LaunchedEffect(currentLyricIndex) {
                        if (currentLyricIndex != -1) {
                            // Scroll current line towards the top (150px offset)
                            listState.animateScrollToItem(
                                index = (currentLyricIndex).coerceAtLeast(0),
                                scrollOffset = -150
                            )
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(top = 150.dp, bottom = 500.dp)
                    ) {
                        itemsIndexed(syncedLyrics) { index, line ->
                            val isActive = index == currentLyricIndex
                            val scale by animateFloatAsState(if (isActive) 1.2f else 1.0f, label = "scale")
                            val alpha by animateFloatAsState(if (isActive) 1.0f else 0.4f, label = "alpha")

                            Text(
                                text = line.text,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 38.sp,
                                modifier = Modifier
                                    .padding(vertical = 16.dp, horizontal = 24.dp)
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        this.alpha = alpha
                                    }
                                    .clickable { playbackViewModel.seekTo(line.timeMs) }
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = plainLyrics ?: "Searching for lyrics...",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp).verticalScroll(rememberScrollState())
                        )
                    }
                }
            }

            // Minimalist Bottom Controls
            LyricsBottomControls(
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                onTogglePlay = { playbackViewModel.togglePlayPause() },
                onSeek = { playbackViewModel.seekTo(it) },
                onNext = { playbackViewModel.playNext() },
                onPrevious = { playbackViewModel.playPrevious() }
            )
        }
    }
}

@Composable
fun DynamicAmbientBackground(albumArtUrl: String?) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = albumArtUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().blur(80.dp).alpha(0.6f),
            contentScale = ContentScale.Crop
        )
        
        // Layered animated gradients for mesh effect
        val infiniteTransition = rememberInfiniteTransition(label = "mesh")
        val orbital1 by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
            label = "orb1"
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.1f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(500f, 500f),
                        radius = 1200f
                    )
                )
        )
    }
}

@Composable
fun LyricsBottomControls(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Slider
            val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
            Slider(
                value = progress,
                onValueChange = { onSeek((it * duration).toLong()) },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.height(20.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(52.dp).clickable { onTogglePlay() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            null, tint = Color.Black, modifier = Modifier.size(30.dp)
                        )
                    }
                }

                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

