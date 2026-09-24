package com.gaminghub.musicplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.gaminghub.musicplayer.MusicViewModel
import com.gaminghub.musicplayer.TrackModel
import com.gaminghub.musicplayer.ui.theme.MusifyGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@UnstableApi
@Composable
fun LastSessionScreen(
    viewModel: MusicViewModel,
    navController: NavController
) {
    val recentTracks by viewModel.recentTracks.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }

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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Last Session & History",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (recentTracks.isNotEmpty()) {
                IconButton(onClick = { showClearConfirm = true }) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Clear History",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }
        }
            if (recentTracks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No Recently Played Songs",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Play songs to build your listening history",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${recentTracks.size} Recently Played Tracks",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Button(
                        onClick = {
                            if (recentTracks.isNotEmpty()) {
                                viewModel.playTrack(recentTracks.first(), recentTracks)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Play All",
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 140.dp)
                ) {
                    itemsIndexed(recentTracks, key = { index, track -> "${track.audioUrl ?: track.title}_$index" }) { index, track ->
                        HistorySongRow(
                            index = index + 1,
                            track = track,
                            isCurrent = currentTrack?.audioUrl == track.audioUrl,
                            isPlaying = isPlaying && currentTrack?.audioUrl == track.audioUrl,
                            viewModel = viewModel,
                            onPlay = { viewModel.playTrack(track, recentTracks) }
                        )
                    }
                }
            }
        }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Listening History?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            text = { Text("This will remove all tracks from your Last Session history.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearRecentHistory()
                        showClearConfirm = false
                    }
                ) {
                    Text("Clear", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@UnstableApi
@Composable
private fun HistorySongRow(
    index: Int,
    track: TrackModel,
    isCurrent: Boolean,
    isPlaying: Boolean,
    viewModel: MusicViewModel,
    onPlay: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showLinkArtistDialog by remember { mutableStateOf(false) }
    var showArtistDialog by remember { mutableStateOf(false) }
    val favoriteUrls by viewModel.favoriteUrls.collectAsState()
    val isFavorite = track.audioUrl != null && favoriteUrls.contains(track.audioUrl)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$index",
            color = if (isCurrent) MusifyGreen else Color.Gray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp)
        )

        com.gaminghub.musicplayer.ui.components.SongImage(
            model = track.albumArtUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isCurrent) MusifyGreen else MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.artist,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!track.uploaderChannel.isNullOrBlank() && !track.uploaderChannel.equals(track.artist, ignoreCase = true)) {
                    Text(
                        text = " • ${track.uploaderChannel}",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        IconButton(onClick = { viewModel.toggleFavorite(track) }, modifier = Modifier.size(36.dp)) {
            Icon(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) MusifyGreen else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(
            onClick = {
                viewModel.removeFromRecentHistory(track)
                android.widget.Toast.makeText(context, "Removed from history", android.widget.Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove from history",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                DropdownMenuItem(
                    text = { Text("Copy Song Link", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = MusifyGreen) },
                    onClick = {
                        showMenu = false
                        com.gaminghub.musicplayer.util.LinkHelper.copySongLink(context, track)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Share Song", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.Default.Share, null, tint = MusifyGreen) },
                    onClick = {
                        showMenu = false
                        com.gaminghub.musicplayer.util.LinkHelper.shareSong(context, track)
                    }
                )
                DropdownMenuItem(
                    text = { Text("View Artist (${track.artist})", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = MusifyGreen) },
                    onClick = {
                        showMenu = false
                        showArtistDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Link / Change Artist", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.Default.PersonPin, null, tint = MusifyGreen) },
                    onClick = {
                        showMenu = false
                        showLinkArtistDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Download", color = MusifyGreen) },
                    leadingIcon = { Icon(Icons.Default.DownloadForOffline, null, tint = MusifyGreen) },
                    onClick = {
                        showMenu = false
                        viewModel.toggleDownload(track)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Remove from History", color = Color(0xFFFF5252)) },
                    leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFFF5252)) },
                    onClick = {
                        showMenu = false
                        viewModel.removeFromRecentHistory(track)
                        android.widget.Toast.makeText(context, "Removed from history", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    if (showArtistDialog) {
        ArtistProfileDialog(
            artistName = track.artist,
            viewModel = viewModel,
            onDismiss = { showArtistDialog = false }
        )
    }

    if (showLinkArtistDialog) {
        LinkArtistDialog(
            currentTrack = track,
            viewModel = viewModel,
            onDismiss = { showLinkArtistDialog = false }
        )
    }
}
