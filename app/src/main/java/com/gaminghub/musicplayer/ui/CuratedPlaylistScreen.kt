package com.gaminghub.musicplayer.ui

import android.widget.Toast
import androidx.annotation.OptIn
import kotlinx.coroutines.launch
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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.gaminghub.musicplayer.ui.components.PlaylistImage
import com.gaminghub.musicplayer.ui.components.SongImage
import com.gaminghub.musicplayer.ui.theme.MusifyGreen

@OptIn(UnstableApi::class)
@Composable
fun CuratedPlaylistScreen(
    title: String,
    subtitle: String,
    query: String,
    imageUrl: String,
    viewModel: MusicViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    var playlistTracks by remember { mutableStateOf<List<TrackModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    fun loadTracks() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val tracks = viewModel.fetchTracksForQuery(query.ifBlank { title })
                if (tracks.isNotEmpty()) {
                    playlistTracks = tracks
                } else {
                    errorMessage = "No tracks found for this playlist"
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load playlist"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(query, title) {
        isLoading = true
        errorMessage = null
        try {
            val tracks = viewModel.fetchTracksForQuery(query.ifBlank { title })
            if (tracks.isNotEmpty()) {
                playlistTracks = tracks
            } else {
                errorMessage = "No tracks found for this playlist"
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load playlist"
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top Bar ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            IconButton(onClick = {
                val shareText = "Listen to $title on Musify!"
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(android.content.Intent.createChooser(intent, "Share Playlist"))
            }) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = MusifyGreen)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ── Hero Banner ──────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PlaylistImage(
                        model = imageUrl.ifBlank { null },
                        contentDescription = title,
                        modifier = Modifier
                            .size(170.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = MusifyGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isLoading) "Loading tracks..." else "${playlistTracks.size} Songs",
                            color = MusifyGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (playlistTracks.isNotEmpty()) {
                                    viewModel.playTrack(playlistTracks.first(), playlistTracks)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            enabled = playlistTracks.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Play All",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (playlistTracks.isNotEmpty()) {
                                    val shuffled = playlistTracks.shuffled()
                                    viewModel.playTrack(shuffled.first(), shuffled)
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MusifyGreen),
                            border = BorderStroke(1.dp, MusifyGreen),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            enabled = playlistTracks.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = null,
                                tint = MusifyGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Shuffle",
                                color = MusifyGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // ── Loading / Empty State ────────────────────────
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MusifyGreen, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading songs...", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            } else if (errorMessage != null && playlistTracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(errorMessage ?: "Error", color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { loadTracks() },
                                colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // ── Tracks List ──────────────────────────────
                itemsIndexed(
                    playlistTracks,
                    key = { index, track -> "${track.audioUrl ?: track.title}_$index" }
                ) { index, track ->
                    CuratedTrackItem(
                        index = index + 1,
                        track = track,
                        isCurrent = currentTrack?.audioUrl == track.audioUrl,
                        isPlaying = isPlaying && currentTrack?.audioUrl == track.audioUrl,
                        viewModel = viewModel,
                        onPlay = { viewModel.playTrack(track, playlistTracks) }
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun CuratedTrackItem(
    index: Int,
    track: TrackModel,
    isCurrent: Boolean,
    isPlaying: Boolean,
    viewModel: MusicViewModel,
    onPlay: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val favoriteUrls by viewModel.favoriteUrls.collectAsState()
    val isFavorite = track.audioUrl != null && favoriteUrls.contains(track.audioUrl)
    val downloadedUrls by viewModel.downloadedUrls.collectAsState()
    val isDownloaded = track.audioUrl != null && downloadedUrls.contains(track.audioUrl)
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track number or playing indicator
        Box(
            modifier = Modifier.width(28.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isCurrent) {
                Icon(
                    Icons.Default.GraphicEq,
                    contentDescription = "Playing",
                    tint = MusifyGreen,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = "$index",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        SongImage(
            model = track.albumArtUrl,
            contentDescription = null,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isCurrent) MusifyGreen else MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = { viewModel.toggleFavorite(track) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) MusifyGreen else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }

        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                DropdownMenuItem(
                    text = { Text(if (isDownloaded) "Downloaded" else "Download Song", color = MusifyGreen) },
                    leadingIcon = { Icon(if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.DownloadForOffline, null, tint = MusifyGreen) },
                    onClick = {
                        viewModel.toggleDownload(track)
                        showMenu = false
                    }
                )
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
                    text = { Text("Play Next", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        showMenu = false
                        Toast.makeText(context, "Added to play next", Toast.LENGTH_SHORT).show()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Queue", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        showMenu = false
                        Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Playlist", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        showMenu = false
                        Toast.makeText(context, "Add to playlist", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
