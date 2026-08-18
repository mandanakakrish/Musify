package com.gaminghub.musify.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.gaminghub.musify.LyricLine
import com.gaminghub.musify.ui.viewmodels.PlaybackViewModel
import com.gaminghub.musify.ui.viewmodels.LibraryViewModel
import com.gaminghub.musify.TrackModel
import java.util.Locale

@Composable
fun PlaylistSelectionSheetContent(
    playlists: List<com.gaminghub.musicplayer.data.PlaylistEntity>,
    favoriteTracks: List<TrackModel>,
    playlistThumbnails: Map<Int, List<String?>>,
    onCreateClick: () -> Unit,
    onPlaylistClick: (Int) -> Unit,
    onFavoriteClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCreateClick() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF333333)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.padding(12.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                @Suppress("DEPRECATION")
                Text("Create Playlist", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            PlaylistRow(
                title = "Favorite Songs",
                trackUrls = favoriteTracks.take(4).map { it.albumArtUrl },
                onClick = onFavoriteClick
            )
        }

        items(playlists) { playlist ->
            val thumbs = playlistThumbnails[playlist.id] ?: emptyList()
            PlaylistRow(
                title = playlist.name,
                trackUrls = thumbs,
                onClick = { onPlaylistClick(playlist.id) }
            )
        }
    }
}

@Composable
fun PlaylistGridThumbnail(urls: List<String?>, size: androidx.compose.ui.unit.Dp = 56.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF333333))
    ) {
        if (urls.isEmpty()) {
            Icon(Icons.Default.MusicNote, null, tint = Color.Gray, modifier = Modifier.align(Alignment.Center))
        } else {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f)) {
                    ThumbnailItem(urls.getOrNull(0), Modifier.weight(1f))
                    ThumbnailItem(urls.getOrNull(1), Modifier.weight(1f))
                }
                Row(Modifier.weight(1f)) {
                    ThumbnailItem(urls.getOrNull(2), Modifier.weight(1f))
                    ThumbnailItem(urls.getOrNull(3), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ThumbnailItem(url: String?, modifier: Modifier) {
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        placeholder = rememberVectorPainter(Icons.Default.MusicNote)
    )
}

@Composable
fun PlaylistRow(title: String, trackUrls: List<String?>, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaylistGridThumbnail(urls = trackUrls)
        Spacer(modifier = Modifier.width(16.dp))
        @Suppress("DEPRECATION")
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SpeedControlDialog(
    currentSpeed: Float,
    onDismiss: () -> Unit,
    onSpeedSet: (Float) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var sliderSpeed by remember { mutableFloatStateOf(currentSpeed) }
    var textInput by remember { mutableStateOf(String.format(Locale.getDefault(), "%.2f", currentSpeed)) }
    var isTextError by remember { mutableStateOf(false) }
    val speedPresets = listOf(0.50f, 0.75f, 1.0f, 1.25f, 1.50f, 1.75f, 2.0f)
    val themeColor = Color(0xFF00E5FF)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1E1E1E),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                @Suppress("DEPRECATION")
                Text("Playback Speed", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                @Suppress("DEPRECATION")
                Text("${String.format(Locale.getDefault(), "%.2f", sliderSpeed)}x", color = themeColor, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = sliderSpeed,
                    onValueChange = { v ->
                        sliderSpeed = (Math.round(v / 0.05f) * 0.05f).coerceIn(0.50f, 2.0f)
                        textInput = String.format(Locale.getDefault(), "%.2f", sliderSpeed)
                        isTextError = false
                    },
                    valueRange = 0.50f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White, activeTrackColor = themeColor, inactiveTrackColor = themeColor.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    @Suppress("DEPRECATION")
                    Text("0.50x", color = Color.Gray, fontSize = 11.sp)
                    @Suppress("DEPRECATION")
                    Text("2.00x", color = Color.Gray, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    speedPresets.forEach { preset ->
                        val active = Math.abs(sliderSpeed - preset) < 0.01f
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (active) themeColor else Color.Gray.copy(alpha = 0.1f),
                            modifier = Modifier.clickable { 
                                sliderSpeed = preset
                                textInput = String.format(Locale.getDefault(), "%.2f", preset)
                                isTextError = false 
                            }
                        ) {
                            @Suppress("DEPRECATION")
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.2f", preset).trimEnd('0').trimEnd('.')}x", 
                                color = if (active) Color.Black else Color.White, 
                                fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { input ->
                        textInput = input
                        val parsed = input.toFloatOrNull()
                        if (parsed != null && parsed in 0.50f..2.0f) { 
                            sliderSpeed = parsed; isTextError = false 
                        } else { isTextError = true }
                    },
                    label = { @Suppress("DEPRECATION") Text("Custom Speed (0.5 – 2.0)", color = Color.Gray, fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    isError = isTextError,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = themeColor, unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        errorBorderColor = Color.Red, cursorColor = themeColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isTextError) {
                    @Suppress("DEPRECATION")
                    Text("Enter a value between 0.50 and 2.00", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    @Suppress("DEPRECATION")
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { if (!isTextError) onSpeedSet(sliderSpeed) }, 
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        enabled = !isTextError, shape = RoundedCornerShape(12.dp)
                    ) {
                        @Suppress("DEPRECATION")
                        Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LyricsPanel(
    syncedLyrics: List<LyricLine>,
    lyrics: String?,
    currentLyricIndex: Int,
    playbackViewModel: PlaybackViewModel
) {
    if (syncedLyrics.isNotEmpty()) {
        val listState = rememberLazyListState()
        
        LaunchedEffect(currentLyricIndex) {
            if (currentLyricIndex != -1) {
                listState.animateScrollToItem(
                    index = (currentLyricIndex).coerceAtLeast(0),
                    scrollOffset = -150
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(top = 100.dp, bottom = 450.dp)
            ) {
                itemsIndexed(syncedLyrics) { index, line ->
                    val isActive = index == currentLyricIndex
                    val textColor by animateColorAsState(
                        targetValue = if (isActive) Color.White else Color.White.copy(alpha = 0.3f),
                        label = "LyricColor"
                    )
                    val textSize by animateFloatAsState(
                        targetValue = if (isActive) 24f else 19f,
                        label = "LyricSize"
                    )

                    @Suppress("DEPRECATION")
                    Text(
                        text = line.text,
                        color = textColor,
                        fontSize = textSize.sp,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp,
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .fillMaxWidth()
                            .clickable { playbackViewModel.seekTo(line.timeMs) }
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent)))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f))))
            )
        }
    } else {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier.verticalScroll(scrollState).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            @Suppress("DEPRECATION")
            Text(
                text = lyrics ?: "Searching for lyrics...", 
                color = Color.White.copy(alpha = 0.9f), 
                fontSize = 20.sp, textAlign = TextAlign.Center, 
                fontWeight = FontWeight.Bold, lineHeight = 32.sp
            )
        }
    }
}

@Composable
fun ControlBar(
    isPlaying: Boolean,
    isFavorite: Boolean,
    isShuffleOn: Boolean,
    isRepeatOn: Boolean,
    currentTrack: TrackModel?,
    playbackViewModel: PlaybackViewModel,
    libraryViewModel: LibraryViewModel,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    FrostedGlassContainer(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        blurRadius = 30.dp,
        cornerRadius = 32.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                IconButton(onClick = { currentTrack?.let { libraryViewModel.toggleFavorite(it) } }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite", tint = if (isFavorite) Color.Red else Color.White, modifier = Modifier.size(22.dp)
                    )
                }
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(Icons.Default.Shuffle, "Shuffle", tint = if (isShuffleOn) Color.White else Color.White.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
                }
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                IconButton(onClick = { playbackViewModel.playPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
            Box(modifier = Modifier.weight(1.2f), contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = { playbackViewModel.togglePlayPause() },
                    modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White)
                ) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = Color.Black, modifier = Modifier.size(32.dp))
                }
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                IconButton(onClick = { playbackViewModel.playNext() }) {
                    Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                IconButton(onClick = onToggleRepeat) {
                    Icon(if (isRepeatOn) Icons.Default.RepeatOne else Icons.Default.Repeat, "Repeat", tint = if (isRepeatOn) Color.White else Color.White.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
                }
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                IconButton(onClick = onOpenEqualizer) {
                    Icon(Icons.Default.Equalizer, "Equalizer", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}
