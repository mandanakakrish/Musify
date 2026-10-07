package com.gaminghub.musify.desktop.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaminghub.musify.desktop.model.TrackModel
import com.gaminghub.musify.desktop.network.rememberAsyncImage
import com.gaminghub.musify.desktop.ui.theme.*
import com.gaminghub.musify.desktop.viewmodel.DesktopMusicViewModel

@Composable
fun DesktopSearchScreen(viewModel: DesktopMusicViewModel) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    var trackForPlaylist by remember { mutableStateOf<TrackModel?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Search Input Bar
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.search(it) },
            placeholder = { Text("Search songs, artists, albums...", color = TextSecondary) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.search("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MusifyEmerald,
                unfocusedBorderColor = SurfaceElevated,
                focusedContainerColor = SurfaceElevated,
                unfocusedContainerColor = SurfaceElevated,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MusifyEmerald)
            }
        } else if (results.isEmpty() && query.isNotBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No results found for '$query'",
                    color = TextSecondary,
                    fontSize = 16.sp
                )
            }
        } else if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Type in the search bar above to find any song",
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            // Track Results List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(results) { index, track ->
                    SearchResultRow(
                        index = index + 1,
                        track = track,
                        onPlay = { viewModel.playTrack(track, results) },
                        onPlayNext = { viewModel.playNextInQueue(track) },
                        onAddToQueue = { viewModel.addToQueue(track) },
                        onAddToPlaylist = { trackForPlaylist = track },
                        onDownload = { viewModel.downloadTrack(track) }
                    )
                }
            }
        }
    }

    // Add to Playlist Dialog
    if (trackForPlaylist != null) {
        val track = trackForPlaylist!!
        AlertDialog(
            onDismissRequest = { trackForPlaylist = null },
            title = { Text("Add to Playlist", color = TextPrimary) },
            text = {
                Column {
                    if (playlists.isEmpty()) {
                        Text("No playlists found. Create one first from the sidebar!", color = TextSecondary)
                    } else {
                        playlists.forEach { pl ->
                            Text(
                                text = pl.title,
                                color = TextPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        viewModel.addTrackToPlaylist(pl, track)
                                        trackForPlaylist = null
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { trackForPlaylist = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
private fun SearchResultRow(
    index: Int,
    track: TrackModel,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownload: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onPlay)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Track index
        Text(
            text = "$index",
            color = TextTertiary,
            fontSize = 13.sp,
            modifier = Modifier.width(28.dp)
        )

        // Album Art
        val bitmap = rememberAsyncImage(track.albumArtUrl)
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp))
            )
        } else {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)).background(SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = track.artist,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        // Quick Actions
        IconButton(onClick = onPlayNext) {
            Icon(Icons.Default.PlaylistPlay, contentDescription = "Play Next", tint = TextSecondary, modifier = Modifier.size(20.dp))
        }

        IconButton(onClick = onDownload) {
            Icon(Icons.Default.Download, contentDescription = "Download", tint = TextSecondary, modifier = Modifier.size(20.dp))
        }

        // More options dropdown
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(SurfaceElevated)
            ) {
                DropdownMenuItem(
                    text = { Text("Add to Queue", color = TextPrimary) },
                    onClick = {
                        onAddToQueue()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Playlist", color = TextPrimary) },
                    onClick = {
                        onAddToPlaylist()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Download Offline", color = TextPrimary) },
                    onClick = {
                        onDownload()
                        showMenu = false
                    }
                )
            }
        }
    }
}
