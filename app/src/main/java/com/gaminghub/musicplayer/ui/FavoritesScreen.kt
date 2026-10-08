package com.gaminghub.musicplayer.ui

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.gaminghub.musicplayer.MusicViewModel
import com.gaminghub.musicplayer.TrackModel
import com.gaminghub.musicplayer.ui.components.SongImage
import com.gaminghub.musicplayer.ui.theme.LocalAppGradients
import com.gaminghub.musicplayer.ui.theme.MusifyGreen
import com.gaminghub.musicplayer.util.LinkHelper

@OptIn(UnstableApi::class)
@Composable
fun FavoritesScreen(
    viewModel: MusicViewModel,
    navController: NavController
) {
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val downloadedUrls by viewModel.downloadedUrls.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Downloaded", "Recent"

    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var selectedTrackForPlaylist by remember { mutableStateOf<TrackModel?>(null) }

    // Dynamic Filtered tracks
    val displayedTracks = remember(favoriteTracks, selectedFilter, searchQuery, downloadedUrls) {
        var list = favoriteTracks

        if (selectedFilter == "Downloaded") {
            list = list.filter { it.audioUrl != null && downloadedUrls.contains(it.audioUrl) }
        } else if (selectedFilter == "Recent") {
            list = list.take(15)
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) || it.artist.lowercase().contains(q)
            }
        }
        list
    }

    val formattedTotalDuration = remember(favoriteTracks) {
        val totalSecs = favoriteTracks.sumOf { it.durationSeconds }
        if (totalSecs > 0) {
            val mins = totalSecs / 60
            if (mins >= 60) {
                "${mins / 60} hr ${mins % 60} mins"
            } else {
                "$mins mins"
            }
        } else {
            "Updated Today"
        }
    }

    // Outer Canvas with Theme Background Gradient (dynamically adjusts to Dark/Light/AMOLED/Presets)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppGradients.current.backgroundBrush)
    ) {
        // Soft ambient radial glow radiating behind the hero heart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MusifyGreen.copy(alpha = 0.18f),
                            Color(0xFF9C27B0).copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        radius = 700f,
                        center = Offset(200f, 320f)
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ── 1. Top Bar: Back Button on Left, Glowing Heart & Search on Right ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circular frosted Back Button
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)),
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Right action buttons: Glowing Heart badge + Search Button (exactly as mockup)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Frosted Glowing Heart pill
                    Surface(
                        shape = CircleShape,
                        color = MusifyGreen.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MusifyGreen.copy(alpha = 0.45f)),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MusifyGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Frosted Search Toggle button
                    Surface(
                        shape = CircleShape,
                        color = if (isSearchActive) MusifyGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        border = BorderStroke(
                            1.dp,
                            if (isSearchActive) MusifyGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
                        ),
                        modifier = Modifier.size(42.dp)
                    ) {
                        IconButton(onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        }) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Clear else Icons.Default.Search,
                                contentDescription = "Search Favorites",
                                tint = if (isSearchActive) MusifyGreen else MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // In-screen Search Field (Expandable animated)
            AnimatedVisibility(visible = isSearchActive) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MusifyGreen.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search in favorites...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = MusifyGreen, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 140.dp)
            ) {
                // ── 2. Hero Section: 3D Glowing Glass Heart + Typography + Badge ──
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 3D Glassmorphism Glowing Double-Heart (matches mockup)
                            Box(
                                modifier = Modifier
                                    .size(116.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Glassmorphic3DHeart(
                                    modifier = Modifier.size(116.dp),
                                    accentColor = MusifyGreen
                                )
                            }

                            Spacer(modifier = Modifier.width(18.dp))

                            // Hero Typography & Offline Status Badge
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Favorites",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${favoriteTracks.size} Songs • $formattedTotalDuration",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                // Status Pill Badge: "Offline Available"
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MusifyGreen.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, MusifyGreen.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MusifyGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (downloadedUrls.isNotEmpty()) "Offline Available" else "Cloud Synced",
                                            color = MusifyGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // ── 3. Row 1 of Actions: Big Play Pill & Frosted Shuffle Button ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Large Glowing "Play" Button
                            Button(
                                onClick = {
                                    if (displayedTracks.isNotEmpty()) {
                                        viewModel.playTrack(displayedTracks.first(), displayedTracks)
                                    }
                                },
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MusifyGreen,
                                    contentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.Black,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Play",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Frosted Glass "Shuffle" Button
                            Surface(
                                shape = RoundedCornerShape(26.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)),
                                modifier = Modifier
                                    .height(48.dp)
                                    .clickable {
                                        if (displayedTracks.isNotEmpty()) {
                                            viewModel.playTrack(displayedTracks.shuffled().first(), displayedTracks)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = MusifyGreen,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Shuffle",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ── 4. Row 2 (Down of Play & Shuffle): Quick Filter Chips ──
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val filterChips = listOf("All", "Downloaded", "Recent")
                            items(filterChips) { chip ->
                                val isSelected = selectedFilter == chip
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) MusifyGreen.copy(alpha = 0.22f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MusifyGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
                                    ),
                                    modifier = Modifier.clickable { selectedFilter = chip }
                                ) {
                                    Text(
                                        text = chip,
                                        color = if (isSelected) MusifyGreen else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // ── 5. Track List ────────────────────────────────────
                if (displayedTracks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = CircleShape,
                                    color = MusifyGreen.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, MusifyGreen.copy(alpha = 0.3f)),
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.FavoriteBorder,
                                            contentDescription = null,
                                            tint = MusifyGreen,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "No matching favorites" else "No Liked Songs Yet",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "Try searching for another song or artist" else "Tap the heart icon on any track to add it to your favorites",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        displayedTracks,
                        key = { index, track -> "${track.audioUrl ?: track.title}_$index" }
                    ) { _, track ->
                        val isCurrent = currentTrack?.audioUrl == track.audioUrl
                        FavoriteSongCard(
                            track = track,
                            isCurrent = isCurrent,
                            isPlaying = isPlaying && isCurrent,
                            viewModel = viewModel,
                            onPlay = { viewModel.playTrack(track, displayedTracks) },
                            onOpenAddToPlaylist = {
                                selectedTrackForPlaylist = track
                                showAddToPlaylistDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Add to Playlist Dialog
    if (showAddToPlaylistDialog && selectedTrackForPlaylist != null) {
        val targetTrack = selectedTrackForPlaylist!!
        AddToPlaylistSimpleDialog(
            playlists = playlists,
            onDismiss = {
                showAddToPlaylistDialog = false
                selectedTrackForPlaylist = null
            },
            onCreatePlaylist = { newName ->
                viewModel.createPlaylist(newName)
            },
            onSelectPlaylist = { pl ->
                viewModel.addTrackToPlaylist(pl.id.toInt(), targetTrack)
                showAddToPlaylistDialog = false
                selectedTrackForPlaylist = null
            }
        )
    }
}

/**
 * 3D Glassmorphism Neon Heart Component matching the generated mockup.
 * Renders layered translucent curves with radiant ambient glow, specular highlights,
 * and a glowing inner neon heart.
 */
@Composable
fun Glassmorphic3DHeart(
    modifier: Modifier = Modifier,
    accentColor: Color = MusifyGreen
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Dynamic Canvas drawing the translucent glass double heart
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Outer Ambient Glow Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.45f),
                        Color(0xFFB388FF).copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.45f, h * 0.45f),
                    radius = w * 0.55f
                )
            )

            // Outer Glass Heart Path
            val outerHeart = createHeartPath(
                left = w * 0.06f,
                top = h * 0.08f,
                width = w * 0.88f,
                height = h * 0.84f
            )

            // 2. Outer Glass Surface Fill (Translucent emerald/violet tint)
            drawPath(
                path = outerHeart,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.35f),
                        Color(0xFF7C4DFF).copy(alpha = 0.25f),
                        Color(0xFF00E5FF).copy(alpha = 0.15f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                ),
                style = Fill
            )

            // 3. Outer Glass Neon Rim Stroke (Glowing rim lighting)
            drawPath(
                path = outerHeart,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.95f),
                        Color(0xFF00E5FF).copy(alpha = 0.70f),
                        Color(0xFFD1C4E9).copy(alpha = 0.85f),
                        Color(0xFF7C4DFF).copy(alpha = 0.50f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                ),
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Inner Floating Glass Heart Path
            val innerHeart = createHeartPath(
                left = w * 0.24f,
                top = h * 0.24f,
                width = w * 0.52f,
                height = h * 0.50f
            )

            // 4. Inner Heart Fill (Luminous neon accent)
            drawPath(
                path = innerHeart,
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.85f),
                        Color(0xFF00E5FF).copy(alpha = 0.65f),
                        Color(0xFF7C4DFF).copy(alpha = 0.40f)
                    ),
                    center = Offset(w * 0.48f, h * 0.46f),
                    radius = w * 0.32f
                ),
                style = Fill
            )

            // 5. Specular highlight curve across top left of inner heart
            drawPath(
                path = innerHeart,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.75f),
                        Color.Transparent
                    ),
                    start = Offset(w * 0.25f, h * 0.25f),
                    end = Offset(w * 0.55f, h * 0.55f)
                ),
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

/**
 * Generates a smooth, organic cubic Bézier heart vector path scaled to bounds
 */
private fun createHeartPath(left: Float, top: Float, width: Float, height: Float): Path {
    val path = Path()
    val right = left + width
    val bottom = top + height
    val midX = left + width / 2f

    path.moveTo(midX, top + height * 0.3f)
    // Left lobe
    path.cubicTo(
        left + width * 0.5f, top,
        left, top,
        left, top + height * 0.34f
    )
    path.cubicTo(
        left, top + height * 0.62f,
        left + width * 0.28f, top + height * 0.78f,
        midX, bottom
    )
    // Right lobe
    path.cubicTo(
        left + width * 0.72f, top + height * 0.78f,
        right, top + height * 0.62f,
        right, top + height * 0.34f
    )
    path.cubicTo(
        right, top,
        left + width * 0.5f, top,
        midX, top + height * 0.3f
    )
    path.close()
    return path
}

/**
 * Animated Equalizer Bars indicator for active playing track
 */
@Composable
fun PlayingEqualizerBars(
    modifier: Modifier = Modifier,
    color: Color = MusifyGreen
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq_bars")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(580, easing = LinearEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(490, easing = LinearEasing), RepeatMode.Reverse),
        label = "h3"
    )

    Row(
        modifier = modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .fillMaxHeight(h1)
                .clip(RoundedCornerShape(1.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .fillMaxHeight(h2)
                .clip(RoundedCornerShape(1.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .fillMaxHeight(h3)
                .clip(RoundedCornerShape(1.dp))
                .background(color)
        )
    }
}

/**
 * Modern Favorites Song Row Item matching the redesigned mockup.
 * Automatically respects dynamic theme colors & accents.
 */
@OptIn(UnstableApi::class)
@Composable
fun FavoriteSongCard(
    track: TrackModel,
    isCurrent: Boolean,
    isPlaying: Boolean,
    viewModel: MusicViewModel,
    onPlay: () -> Unit,
    onOpenAddToPlaylist: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val downloadedUrls by viewModel.downloadedUrls.collectAsState()
    val isDownloaded = track.audioUrl != null && downloadedUrls.contains(track.audioUrl)

    val formattedDuration = remember(track.durationSeconds) {
        if (track.durationSeconds > 0) {
            "${track.durationSeconds / 60}:${(track.durationSeconds % 60).toString().padStart(2, '0')}"
        } else {
            ""
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isCurrent) MusifyGreen.copy(alpha = 0.14f) else Color.Transparent,
        border = if (isCurrent) BorderStroke(1.dp, MusifyGreen.copy(alpha = 0.35f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clickable { onPlay() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art Thumbnail with smooth rounded corners
            SongImage(
                model = track.albumArtUrl,
                contentDescription = track.title,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Track title & artist details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isCurrent) MusifyGreen else MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = track.artist,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Animated Equalizer Wave Bars if currently playing
            if (isCurrent && isPlaying) {
                PlayingEqualizerBars(modifier = Modifier.padding(end = 8.dp))
            }

            // Duration text
            if (formattedDuration.isNotBlank()) {
                Text(
                    text = formattedDuration,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            // Filled Green Heart to toggle Favorite
            IconButton(
                onClick = { viewModel.toggleFavorite(track) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorited",
                    tint = MusifyGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Three-dot Options Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Add to Playlist", color = MaterialTheme.colorScheme.onSurface) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = MusifyGreen) },
                        onClick = {
                            showMenu = false
                            onOpenAddToPlaylist()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Play Next", color = MaterialTheme.colorScheme.onSurface) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            showMenu = false
                            viewModel.playNextTrackInQueue(track)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Queue", color = MaterialTheme.colorScheme.onSurface) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            showMenu = false
                            viewModel.addToQueue(track)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isDownloaded) "Downloaded" else "Download", color = MusifyGreen) },
                        leadingIcon = {
                            Icon(
                                if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.DownloadForOffline,
                                null,
                                tint = MusifyGreen
                            )
                        },
                        onClick = {
                            viewModel.toggleDownload(track)
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share Song", color = MaterialTheme.colorScheme.onSurface) },
                        leadingIcon = { Icon(Icons.Default.Share, null, tint = MusifyGreen) },
                        onClick = {
                            showMenu = false
                            LinkHelper.shareSong(context, track)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Copy Song Link", color = MaterialTheme.colorScheme.onSurface) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = MusifyGreen) },
                        onClick = {
                            showMenu = false
                            LinkHelper.copySongLink(context, track)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("View Artist", color = MaterialTheme.colorScheme.onSurface) },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            showMenu = false
                            viewModel.searchMusic(track.artist)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Play Radio", color = MaterialTheme.colorScheme.onSurface) },
                        leadingIcon = { Icon(Icons.Default.Podcasts, null, tint = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            showMenu = false
                            viewModel.startSongRadio(track)
                        }
                    )
                }
            }
        }
    }
}
