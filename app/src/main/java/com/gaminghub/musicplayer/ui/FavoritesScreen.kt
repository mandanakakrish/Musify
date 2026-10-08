package com.gaminghub.musicplayer.ui

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.gaminghub.musicplayer.MusicViewModel
import com.gaminghub.musicplayer.TrackModel
import com.gaminghub.musicplayer.ui.components.SongImage
import com.gaminghub.musicplayer.ui.theme.MusifyGlassBorder
import com.gaminghub.musicplayer.ui.theme.MusifyGlassSurface
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

    // Filtered track list based on chip & search query
    val displayedTracks = remember(favoriteTracks, selectedFilter, searchQuery, downloadedUrls) {
        var list = favoriteTracks

        // Filter chip
        if (selectedFilter == "Downloaded") {
            list = list.filter { it.audioUrl != null && downloadedUrls.contains(it.audioUrl) }
        } else if (selectedFilter == "Recent") {
            list = list.take(15)
        }

        // Search query
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) || it.artist.lowercase().contains(q)
            }
        }
        list
    }

    // Dynamic total duration calculation
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E15)) // Deep Obsidian Canvas matching redesign mockup
    ) {
        // Ambient soft emerald & violet background glow at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MusifyGreen.copy(alpha = 0.14f),
                            Color(0xFF7C4DFF).copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        radius = 650f
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Navigation Bar ──────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back button in frosted circular surface
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Glowing heart badge in center
                Surface(
                    shape = CircleShape,
                    color = MusifyGreen.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, MusifyGreen.copy(alpha = 0.35f)),
                    modifier = Modifier.size(38.dp)
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

                // Search Toggle Icon Button
                Surface(
                    shape = CircleShape,
                    color = if (isSearchActive) MusifyGreen.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(
                        1.dp,
                        if (isSearchActive) MusifyGreen.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f)
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
                            tint = if (isSearchActive) MusifyGreen else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // In-screen Search Field (Expandable)
            AnimatedVisibility(visible = isSearchActive) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1F2A),
                    border = BorderStroke(1.dp, MusifyGreen.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search in favorites...", color = Color.Gray, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = MusifyGreen, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
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
                // ── Hero Section (Glass Heart + Typography + Badge) ───
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 3D Glassmorphism Glowing Heart Badge
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF1A3828),
                                                Color(0xFF261D38)
                                            )
                                        )
                                    )
                                    .border(
                                        BorderStroke(
                                            1.5.dp,
                                            Brush.linearGradient(
                                                listOf(
                                                    MusifyGreen.copy(alpha = 0.75f),
                                                    Color(0xFFB388FF).copy(alpha = 0.6f)
                                                )
                                            )
                                        ),
                                        RoundedCornerShape(26.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // Outer Ambient Halo Glow
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(MusifyGreen.copy(alpha = 0.25f))
                                )
                                // Main 3D Heart Symbol
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = MusifyGreen,
                                    modifier = Modifier.size(54.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(18.dp))

                            // Hero Typography
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Favorites",
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${favoriteTracks.size} Songs • $formattedTotalDuration",
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Status Pill Badge: "Offline Available"
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MusifyGreen.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, MusifyGreen.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MusifyGreen,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (downloadedUrls.isNotEmpty()) "Offline Available" else "Cloud Synced",
                                            color = MusifyGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // ── Action Controls Bar (Play + Shuffle + Filter Chips) ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Large Emerald "Play" Button
                            Button(
                                onClick = {
                                    if (displayedTracks.isNotEmpty()) {
                                        viewModel.playTrack(displayedTracks.first(), displayedTracks)
                                    }
                                },
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MusifyGreen,
                                    contentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Play",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Frosted Glass "Shuffle" Button
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                                modifier = Modifier
                                    .height(44.dp)
                                    .clickable {
                                        if (displayedTracks.isNotEmpty()) {
                                            viewModel.playTrack(displayedTracks.shuffled().first(), displayedTracks)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = MusifyGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Shuffle",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Quick Filter Chips ("All", "Downloaded", "Recent")
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val filterChips = listOf("All", "Downloaded", "Recent")
                                items(filterChips) { chip ->
                                    val isSelected = selectedFilter == chip
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = if (isSelected) MusifyGreen.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) MusifyGreen else Color.White.copy(alpha = 0.12f)
                                        ),
                                        modifier = Modifier.clickable { selectedFilter = chip }
                                    ) {
                                        Text(
                                            text = chip,
                                            color = if (isSelected) MusifyGreen else Color.White.copy(alpha = 0.75f),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // ── Song List Items ──────────────────────────────────
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
                                    color = MusifyGreen.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, MusifyGreen.copy(alpha = 0.25f)),
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
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "Try searching for another song or artist" else "Tap the heart icon on any track to add it to your favorites",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
 * Modern Favorites Song Row Item matching the redesigned mockup
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
        shape = RoundedCornerShape(14.dp),
        color = if (isCurrent) MusifyGreen.copy(alpha = 0.12f) else Color.Transparent,
        border = if (isCurrent) BorderStroke(1.dp, MusifyGreen.copy(alpha = 0.32f)) else null,
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
            // Album Art Thumbnail
            SongImage(
                model = track.albumArtUrl,
                contentDescription = track.title,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Track title & artist details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isCurrent) MusifyGreen else Color.White,
                    fontSize = 15.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = track.artist,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Wave equalizer indicator if currently playing
            if (isCurrent && isPlaying) {
                PlayingEqualizerBars(modifier = Modifier.padding(end = 8.dp))
            }

            // Duration text
            if (formattedDuration.isNotBlank()) {
                Text(
                    text = formattedDuration,
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 8.dp)
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

            // 3-Dot Options Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = Color(0xFF1E1F2A),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Add to Playlist", color = Color.White) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = MusifyGreen) },
                        onClick = {
                            showMenu = false
                            onOpenAddToPlaylist()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Play Next", color = Color.White) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = Color.White) },
                        onClick = {
                            showMenu = false
                            viewModel.playNextTrackInQueue(track)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Queue", color = Color.White) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = Color.White) },
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
                        text = { Text("Share Song", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Share, null, tint = MusifyGreen) },
                        onClick = {
                            showMenu = false
                            LinkHelper.shareSong(context, track)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Copy Song Link", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = MusifyGreen) },
                        onClick = {
                            showMenu = false
                            LinkHelper.copySongLink(context, track)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("View Artist", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = Color.White) },
                        onClick = {
                            showMenu = false
                            viewModel.searchMusic(track.artist)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Play Radio", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Podcasts, null, tint = Color.White) },
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
