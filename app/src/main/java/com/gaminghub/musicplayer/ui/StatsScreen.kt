package com.gaminghub.musicplayer.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.gaminghub.musicplayer.ui.components.SongImage
import com.gaminghub.musicplayer.MusicViewModel
import com.gaminghub.musicplayer.TrackModel
import com.gaminghub.musicplayer.ui.theme.MusifyDarkBg
import com.gaminghub.musicplayer.ui.theme.MusifyGlassBorder
import com.gaminghub.musicplayer.ui.theme.MusifyGlassSurface
import com.gaminghub.musicplayer.ui.theme.MusifyGreen

@OptIn(UnstableApi::class)
@Composable
fun StatsScreen(navController: NavController, viewModel: MusicViewModel) {
    val totalSongsPlayed by viewModel.totalSongsPlayed.collectAsState()
    val mostPlayedTracks by viewModel.mostPlayedTracks.collectAsState()
    val topMostPlayedTrack by viewModel.topMostPlayedTrack.collectAsState()
    val globalWeeklyTopCharts by viewModel.globalWeeklyTopCharts.collectAsState()
    val topArtistsStats by viewModel.topArtistsStats.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val estimatedMinutes = (totalSongsPlayed * 3.5).toInt()
    val hours = estimatedMinutes / 60
    val minutes = estimatedMinutes % 60
    val listeningTimeText = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Listening Stats & Wrapped",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 0. Musify Wrapped Hero Banner ──────────────────────────────
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MusifyGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF8E2DE2).copy(alpha = 0.35f),
                                        Color(0xFF4A00E0).copy(alpha = 0.25f),
                                        MusifyGreen.copy(alpha = 0.20f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("✨", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Musify Wrapped & Insights",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    color = MusifyGreen,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        color = Color.Black,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your personal listening habits, top favorite artists, and music replay statistics.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // ── 1. Dual Key Metrics (Total Songs + Listening Time) ────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Songs Card
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Total Plays",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalSongsPlayed",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Songs completed",
                                color = MusifyGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Total Listening Time Card
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Time Listened",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = listeningTimeText,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Estimated audio time",
                                color = MusifyGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ── 2. All Users Weekly Top Songs Cloud Banner ───────────────
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MusifyGreen.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("top_charts") }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF00796B).copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🌍", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "All Users Weekly Top Chart",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val topCommunity = globalWeeklyTopCharts.firstOrNull()
                                Text(
                                    text = if (topCommunity != null) {
                                        "#1 Song: ${topCommunity.title} • ${topCommunity.artist} (${topCommunity.playcount} plays)"
                                    } else {
                                        "Top played songs across all Musify users over the past 7 days"
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Surface(
                                color = MusifyGreen,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "View",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── 3. Top Artists Leaderboard ────────────────────────────────
            if (topArtistsStats.isNotEmpty()) {
                item {
                    Text(
                        text = "Top Artists",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            topArtistsStats.take(5).forEachIndexed { index, stat ->
                                val rankBadge = when (index) {
                                    0 -> "🥇"
                                    1 -> "🥈"
                                    2 -> "🥉"
                                    else -> "#${index + 1}"
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = rankBadge,
                                        fontSize = 16.sp,
                                        modifier = Modifier.width(36.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stat.artist,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "${stat.totalPlays} plays",
                                        color = MusifyGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (index < topArtistsStats.take(5).size - 1) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 4. Most Played Song Hero Card ─────────────────────────────
            item {
                Text(
                    text = "Most Played Song",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                if (topMostPlayedTrack != null) {
                    val track = topMostPlayedTrack!!
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MusifyGreen.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.playTrack(track, mostPlayedTracks) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(72.dp)) {
                                SongImage(
                                    model = track.albumArtUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(bottomEnd = 6.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text("👑 #1", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = track.title,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (track.isDevpick) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = "Dev's Pick",
                                            tint = MusifyGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = track.artist,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "🔥 Played ${track.playcount} times",
                                        color = Color(0xFFFFB74D),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.playTrack(track, mostPlayedTracks) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MusifyGreen)
                            ) {
                                Icon(
                                    if (isPlaying && currentTrack?.audioUrl == track.audioUrl) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Play some songs to see your #1 most played track!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ── 3. Top Most Played Songs Leaderboard ───────────────────────
            if (mostPlayedTracks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Top Played Songs",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                itemsIndexed(mostPlayedTracks, key = { index, track -> "${track.audioUrl ?: track.title}_$index" }) { index, track ->
                    LeaderboardTrackRow(
                        rank = index + 1,
                        track = track,
                        isCurrent = currentTrack?.audioUrl == track.audioUrl,
                        isPlaying = isPlaying && currentTrack?.audioUrl == track.audioUrl,
                        onClick = { viewModel.playTrack(track, mostPlayedTracks) }
                    )
                }
            }
        }
    }
}

@Composable
fun LeaderboardTrackRow(
    rank: Int,
    track: TrackModel,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isCurrent) MusifyGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isCurrent) MusifyGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$rank",
                color = when (rank) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> Color.Gray
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(24.dp)
            )

            SongImage(
                model = track.albumArtUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.title,
                        color = if (isCurrent) MusifyGreen else MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (track.isDevpick) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Dev's Pick",
                            tint = MusifyGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = Color(0xFF282834),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${track.playcount} plays",
                    color = MusifyGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
