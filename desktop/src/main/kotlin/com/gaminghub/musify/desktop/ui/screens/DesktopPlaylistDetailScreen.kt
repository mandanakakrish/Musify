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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaminghub.musify.desktop.model.DesktopScreen
import com.gaminghub.musify.desktop.network.rememberAsyncImage
import com.gaminghub.musify.desktop.ui.theme.*
import com.gaminghub.musify.desktop.viewmodel.DesktopMusicViewModel

@Composable
fun DesktopPlaylistDetailScreen(viewModel: DesktopMusicViewModel) {
    val playlist by viewModel.selectedPlaylist.collectAsState()

    if (playlist == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Playlist not found", color = TextSecondary)
        }
        return
    }

    val currentPlaylist = playlist!!

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Back navigation
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { viewModel.navigateTo(DesktopScreen.LIBRARY) }
                .padding(bottom = 16.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Back to Library", color = TextSecondary, fontSize = 14.sp)
        }

        // Header Banner
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            val bitmap = rememberAsyncImage(currentPlaylist.coverUrl)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(160.dp).clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier.size(160.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.QueueMusic, contentDescription = null, tint = MusifyEmerald, modifier = Modifier.size(64.dp))
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column {
                Text("PLAYLIST", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(currentPlaylist.title, color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                if (currentPlaylist.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(currentPlaylist.description, color = TextSecondary, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("${currentPlaylist.tracks.size} tracks", color = TextTertiary, fontSize = 13.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentPlaylist.tracks.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.playTrack(currentPlaylist.tracks.first(), currentPlaylist.tracks) },
                            colors = ButtonDefaults.buttonColors(containerColor = MusifyEmerald, contentColor = Color.Black),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play All", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(onClick = { viewModel.deletePlaylist(currentPlaylist) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Playlist", tint = TextSecondary)
                    }
                }
            }
        }

        Divider(color = SurfaceElevated)
        Spacer(modifier = Modifier.height(16.dp))

        // Tracks List
        if (currentPlaylist.tracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("This playlist has no songs yet. Search and add songs to it!", color = TextSecondary, fontSize = 15.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(currentPlaylist.tracks) { index, track ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.playTrack(track, currentPlaylist.tracks) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("${index + 1}", color = TextTertiary, fontSize = 13.sp, modifier = Modifier.width(28.dp))

                        val bitmap = rememberAsyncImage(track.albumArtUrl)
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(SurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(track.artist, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                        }

                        IconButton(onClick = { viewModel.removeTrackFromPlaylist(currentPlaylist, track) }) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove", tint = TextTertiary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
