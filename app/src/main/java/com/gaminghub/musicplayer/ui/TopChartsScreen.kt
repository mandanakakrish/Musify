package com.gaminghub.musicplayer.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.gaminghub.musicplayer.MusicViewModel
import com.gaminghub.musicplayer.TrackModel
import com.gaminghub.musicplayer.ui.components.SongImage
import com.gaminghub.musicplayer.ui.theme.MusifyGlassBorder
import com.gaminghub.musicplayer.ui.theme.MusifyGlassSurface
import com.gaminghub.musicplayer.ui.theme.MusifyGreen

@OptIn(UnstableApi::class)
@Composable
fun TopChartsScreen(viewModel: MusicViewModel, onMenuClick: (() -> Unit)? = null) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val globalWeeklyTopCharts by viewModel.globalWeeklyTopCharts.collectAsState()
    val globalAllTimeTopCharts by viewModel.globalAllTimeTopCharts.collectAsState()
    val isGlobalChartsLoading by viewModel.isGlobalChartsLoading.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val tabs = listOf("Weekly", "All Time")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top Bar ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onMenuClick?.invoke() }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Top Charts",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (selectedTab == 0) "Top Played by All Users" else "All-Time Top Played by All Users",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.loadGlobalTopCharts(forceRefresh = true) }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Charts",
                        tint = if (isGlobalChartsLoading) MusifyGreen else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = { showInfoDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Chart Info",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ── Tabs ─────────────────────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MusifyGreen,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MusifyGreen,
                        height = 3.dp
                    )
                }
            },
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            color = if (selectedTab == index) MusifyGreen else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        // ── Tab Contents ─────────────────────────────────────
        when (selectedTab) {
            0 -> {
                // ── Weekly (Past 7 Days - All Users on Cloud Firestore) ───
                if (isGlobalChartsLoading && globalWeeklyTopCharts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MusifyGreen,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                "Loading Weekly Top Songs from All Users...",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    TopChartsList(
                        title = "Weekly Top Songs (All Users)",
                        tracks = globalWeeklyTopCharts,
                        emptyMessage = "No weekly plays recorded across users yet.\nPlay songs in Musify and your streams will automatically rank on the Weekly Top Charts!",
                        exploreButtonText = "View All-Time Chart",
                        onExploreClick = { selectedTab = 1 },
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        viewModel = viewModel,
                        isWeekly = true
                    )
                }
            }
            else -> {
                // ── All Time (All Users on Cloud Firestore) ───────────────
                if (isGlobalChartsLoading && globalAllTimeTopCharts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MusifyGreen,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                "Loading All-Time Top Songs from All Users...",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    TopChartsList(
                        title = "All-Time Top Songs (All Users)",
                        tracks = globalAllTimeTopCharts,
                        emptyMessage = "No all-time plays recorded across users yet.\nPlay songs in Musify and your streams will automatically rank on the All-Time Top Charts!",
                        exploreButtonText = "View Weekly Chart",
                        onExploreClick = { selectedTab = 0 },
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        viewModel = viewModel,
                        isWeekly = false
                    )
                }
            }
        }
    }

    // In-App Charts Information Dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            containerColor = Color(0xFF1E1E24),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = MusifyGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("About Top Charts", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Musify Top Charts\n\n" +
                    "Both charts are calculated globally from ALL users directly on Cloud Firestore:\n\n" +
                    "• Weekly: Live cloud leaderboard of the top songs played across ALL Musify users in the rolling past 7 days.\n\n" +
                    "• All Time: Live cloud leaderboard of the all-time most played songs across ALL Musify users.",
                    color = Color(0xFFCCCCCC),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Got It", color = MusifyGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun TopChartsList(
    title: String,
    tracks: List<TrackModel>,
    emptyMessage: String,
    exploreButtonText: String = "Switch Chart",
    onExploreClick: () -> Unit,
    currentTrack: TrackModel?,
    isPlaying: Boolean,
    viewModel: MusicViewModel,
    isWeekly: Boolean
) {
    if (tracks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = MusifyGlassSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MusifyGlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MusifyGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MusifyGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = emptyMessage,
                        color = Color(0xFFAAAAAA),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onExploreClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = exploreButtonText,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 140.dp)
        ) {
            // ── Hero Header Card (Exact Half Size: 60dp) ─────────────
            item {
                Surface(
                    color = MusifyGlassSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MusifyGlassBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(60.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MusifyGreen.copy(alpha = 0.18f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(horizontal = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = title,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )

                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.playTrack(tracks.first(), tracks) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Play All", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.playTrack(tracks.shuffled().first(), tracks.shuffled()) },
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Shuffle, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // ── Track Rows with Medal Rankings & Plays ───────────
            itemsIndexed(
                tracks,
                key = { index, track -> "${track.audioUrl ?: track.title}_$index" }
            ) { index, track ->
                TopChartTrackRow(
                    index = index + 1,
                    track = track,
                    isCurrent = currentTrack?.audioUrl == track.audioUrl,
                    isPlaying = isPlaying && currentTrack?.audioUrl == track.audioUrl,
                    isWeekly = isWeekly,
                    viewModel = viewModel,
                    onPlay = { viewModel.playTrack(track, tracks) }
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun TopChartTrackRow(
    index: Int,
    track: TrackModel,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isWeekly: Boolean,
    viewModel: MusicViewModel,
    onPlay: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val downloadedUrls by viewModel.downloadedUrls.collectAsState()
    val isDownloaded = track.audioUrl != null && downloadedUrls.contains(track.audioUrl)

    // Medal colors for top 3
    val rankColor = when (index) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFE0E0E0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color(0xFF888888)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank Badge
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "$index",
                color = rankColor,
                fontSize = if (index <= 3) 16.sp else 14.sp,
                fontWeight = if (index <= 3) FontWeight.Bold else FontWeight.Medium
            )
        }

        // Album Art
        SongImage(
            model = track.albumArtUrl,
            contentDescription = null,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Title, Artist, & Weekly Play Count Pill
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isCurrent) MusifyGreen else MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = track.artist,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (track.playcount > 0) {
                    Surface(
                        color = if (isWeekly) MusifyGreen.copy(alpha = 0.15f) else Color(0x33FFFFFF),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isWeekly) {
                                if (track.playcount == 1) "🔥 1 play" else "🔥 ${track.playcount} plays"
                            } else {
                                if (track.playcount == 1) "1 play" else "${track.playcount} plays"
                            },
                            color = if (isWeekly) MusifyGreen else Color(0xFFCCCCCC),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }

        // Options Menu
        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color(0xFF242424))
            ) {
                DropdownMenuItem(
                    text = { Text(if (isDownloaded) "Downloaded" else "Download Song", color = MusifyGreen) },
                    leadingIcon = { Icon(if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.DownloadForOffline, null, tint = MusifyGreen) },
                    onClick = { viewModel.toggleDownload(track); showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Favorite", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Favorite, null, tint = MusifyGreen) },
                    onClick = { viewModel.toggleFavorite(track); showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Play Next", color = Color.White) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = Color.White) },
                    onClick = { showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Add to Queue", color = Color.White) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = Color.White) },
                    onClick = { showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Add to Playlist", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Add, null, tint = Color.White) },
                    onClick = { showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("View Artist (${track.artist.take(15)})", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = Color.White) },
                    onClick = { showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Play Radio", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Podcasts, null, tint = Color.White) },
                    onClick = { showMenu = false }
                )
            }
        }
    }
}
