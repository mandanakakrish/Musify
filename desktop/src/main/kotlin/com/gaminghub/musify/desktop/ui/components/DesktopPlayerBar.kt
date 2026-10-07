package com.gaminghub.musify.desktop.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaminghub.musify.desktop.model.RepeatMode
import com.gaminghub.musify.desktop.network.rememberAsyncImage
import com.gaminghub.musify.desktop.ui.theme.*
import com.gaminghub.musify.desktop.viewmodel.DesktopMusicViewModel

@Composable
fun DesktopPlayerBar(viewModel: DesktopMusicViewModel) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isLyricsOpen by viewModel.isLyricsOpen.collectAsState()
    val isQueueOpen by viewModel.isQueueOpen.collectAsState()
    val isMiniPlayer by viewModel.isMiniPlayer.collectAsState()

    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableStateOf(0f) }

    Surface(
        color = SurfaceDark,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().height(90.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Track Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(280.dp)
            ) {
                if (currentTrack != null) {
                    val bitmap = rememberAsyncImage(currentTrack?.albumArtUrl)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTrack?.title ?: "",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = currentTrack?.artist ?: "",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val isFav = currentTrack?.let { viewModel.isTrackFavorite(it) } ?: false
                    IconButton(onClick = { currentTrack?.let { viewModel.toggleFavorite(it) } }) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFav) MusifyEmerald else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Text(
                        text = "No track playing",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // Center: Playback Controls & Progress Bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp)
            ) {
                // Control Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Shuffle
                    IconButton(onClick = { viewModel.toggleShuffle() }) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) MusifyEmerald else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Previous
                    IconButton(onClick = { viewModel.playPrevious() }) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Play / Pause Circle
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MusifyEmerald)
                            .clickable { viewModel.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Next
                    IconButton(onClick = { viewModel.playNext() }) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Repeat
                    IconButton(onClick = { viewModel.toggleRepeat() }) {
                        Icon(
                            imageVector = when (repeatMode) {
                                RepeatMode.ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (repeatMode != RepeatMode.OFF) MusifyEmerald else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Progress Bar & Timestamps
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(22.dp)
                ) {
                    val currentPos = if (isDraggingSlider) dragPositionMs.toLong() else positionMs
                    Text(
                        text = formatTime(currentPos),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.width(36.dp)
                    )

                    val maxDuration = if (durationMs > 0) durationMs.toFloat() else 1f
                    val sliderVal = (if (isDraggingSlider) dragPositionMs else positionMs.toFloat()).coerceIn(0f, maxDuration)

                    Slider(
                        value = sliderVal,
                        onValueChange = {
                            isDraggingSlider = true
                            dragPositionMs = it
                        },
                        onValueChangeFinished = {
                            viewModel.seekTo(dragPositionMs.toLong())
                            isDraggingSlider = false
                        },
                        valueRange = 0f..maxDuration,
                        colors = SliderDefaults.colors(
                            thumbColor = TextPrimary,
                            activeTrackColor = MusifyEmerald,
                            inactiveTrackColor = SurfaceElevated
                        ),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )

                    Text(
                        text = formatTime(durationMs),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }

            // Right: Volume & Auxiliary Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.width(280.dp)
            ) {
                // Lyrics button
                IconButton(onClick = { viewModel.toggleLyrics() }) {
                    Icon(
                        imageVector = Icons.Default.Notes,
                        contentDescription = "Lyrics",
                        tint = if (isLyricsOpen) MusifyEmerald else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Queue button
                IconButton(onClick = { viewModel.toggleQueue() }) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Queue",
                        tint = if (isQueueOpen) MusifyEmerald else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Mini player button
                IconButton(onClick = { viewModel.toggleMiniPlayer() }) {
                    Icon(
                        imageVector = Icons.Default.PictureInPictureAlt,
                        contentDescription = "Mini Player",
                        tint = if (isMiniPlayer) MusifyEmerald else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Mute / Unmute
                IconButton(onClick = { viewModel.toggleMute() }) {
                    Icon(
                        imageVector = when {
                            isMuted || volume == 0f -> Icons.Default.VolumeOff
                            volume < 0.5f -> Icons.Default.VolumeDown
                            else -> Icons.Default.VolumeUp
                        },
                        contentDescription = "Volume",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Volume slider
                Slider(
                    value = if (isMuted) 0f else volume,
                    onValueChange = { viewModel.setVolume(it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = TextPrimary,
                        activeTrackColor = MusifyEmerald,
                        inactiveTrackColor = SurfaceElevated
                    ),
                    modifier = Modifier.width(100.dp)
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
