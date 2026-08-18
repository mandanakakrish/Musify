package com.gaminghub.musify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.gaminghub.musify.TrackModel
import com.gaminghub.musify.ui.viewmodels.PlaybackViewModel
import com.gaminghub.musify.ui.viewmodels.LibraryViewModel
import kotlinx.coroutines.flow.flowOf

@Composable
fun TrackListItem(
    track: TrackModel,
    playbackViewModel: PlaybackViewModel,
    libraryViewModel: LibraryViewModel? = null,
    queue: List<TrackModel>? = null,
    navController: NavController? = null,
    subtitle: String? = null
) {
    val displaySubtitle = subtitle ?: track.artist
    val isFavorite by (track.audioUrl?.let { libraryViewModel?.isFavorite(it) } ?: flowOf(false)).collectAsState(initial = false)
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                playbackViewModel.playTrack(track, queue ?: listOf(track))
            }
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album Art with Play Overlay
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = track.albumArtUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = rememberVectorPainter(Icons.Default.Album),
                error = rememberVectorPainter(Icons.Default.Album)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title and Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = displaySubtitle,
                color = if (subtitle == null && track.artistId != null) MaterialTheme.colorScheme.primary else Color.Gray,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(enabled = subtitle == null && track.artistId != null) {
                    if (subtitle == null) {
                        track.artistId?.let { id ->
                            navController?.navigate("artist_profile/$id")
                        }
                    }
                }
            )
        }

        // Action Buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (libraryViewModel != null) {
                IconButton(onClick = { libraryViewModel.toggleFavorite(track) }) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFE91E63) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF2B2B2B))
                ) {
                    DropdownMenuItem(
                        text = { Text("Play Next", color = Color.White) },
                        onClick = {
                            playbackViewModel.addToQueueNext(track)
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.SkipNext, null, tint = Color.White) }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Queue", color = Color.White) },
                        onClick = {
                            playbackViewModel.addToQueueEnd(track)
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, tint = Color.White) }
                    )
                    if (libraryViewModel != null) {
                        DropdownMenuItem(
                            text = { Text("Download", color = Color.White) },
                            onClick = {
                                libraryViewModel.downloadTrack(track)
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Download, null, tint = Color.White) }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Playlist", color = Color.White) },
                            onClick = {
                                showMenu = false
                                // TODO: open playlist selection sheet
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = Color.White) }
                        )
                    }
                }
            }
        }
    }
}
