package com.gaminghub.musify.desktop.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaminghub.musify.desktop.ui.theme.*
import com.gaminghub.musify.desktop.viewmodel.DesktopMusicViewModel

@Composable
fun DesktopDownloadsScreen(viewModel: DesktopMusicViewModel) {
    val downloadedTracks by viewModel.downloadManager.downloadedTracks.collectAsState()
    val downloadingTracks by viewModel.downloadManager.downloadingTracks.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Header & Actions
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Offline Downloads & Local Music", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("${downloadedTracks.size} songs saved on this computer", color = TextSecondary, fontSize = 13.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { viewModel.downloadManager.scanLocalMusic() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Refresh")
                }

                Button(
                    onClick = { viewModel.downloadManager.openDownloadsFolder() },
                    colors = ButtonDefaults.buttonColors(containerColor = MusifyEmerald, contentColor = androidx.compose.ui.graphics.Color.Black)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Folder in Explorer")
                }
            }
        }

        // Active Downloads if any
        if (downloadingTracks.isNotEmpty()) {
            Text("DOWNLOADING NOW", color = MusifyEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                downloadingTracks.forEach { (url, progress) ->
                    Surface(color = SurfaceElevated, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                            LinearProgressIndicator(
                                progress = { progress },
                                color = MusifyEmerald,
                                trackColor = SurfaceHighlight,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("${(progress * 100).toInt()}%", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        Divider(color = SurfaceElevated)
        Spacer(modifier = Modifier.height(16.dp))

        // Downloaded List
        if (downloadedTracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DownloadDone, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No offline downloads yet.", color = TextSecondary, fontSize = 16.sp)
                    Text("Click the download icon on any song to save it for offline listening.", color = TextTertiary, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(downloadedTracks) { index, track ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.playTrack(track, downloadedTracks) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("${index + 1}", color = TextTertiary, fontSize = 13.sp, modifier = Modifier.width(28.dp))

                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(SurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.OfflinePin, contentDescription = null, tint = MusifyEmerald, modifier = Modifier.size(22.dp))
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(track.artist, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                        }

                        Icon(Icons.Default.CheckCircle, contentDescription = "Downloaded", tint = MusifyEmerald, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
