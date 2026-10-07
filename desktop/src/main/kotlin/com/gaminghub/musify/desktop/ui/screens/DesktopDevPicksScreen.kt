package com.gaminghub.musify.desktop.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaminghub.musify.desktop.model.TrackModel
import com.gaminghub.musify.desktop.network.rememberAsyncImage
import com.gaminghub.musify.desktop.ui.theme.*
import com.gaminghub.musify.desktop.viewmodel.DesktopMusicViewModel

private val DevPicksList = listOf(
    TrackModel("Illuminati", "Sushin Shyam, Dabzee", "https://www.youtube.com/watch?v=tOM-nWPcRQ4", "https://i.ytimg.com/vi/tOM-nWPcRQ4/hqdefault.jpg"),
    TrackModel("Aasa Kooda", "Sai Abhyankkar, Sai Smriti", "https://www.youtube.com/watch?v=a3Ue-LN5B9U", "https://i.ytimg.com/vi/a3Ue-LN5B9U/hqdefault.jpg"),
    TrackModel("Katchi Sera", "Sai Abhyankkar", "https://www.youtube.com/watch?v=CN_y_jA4e4A", "https://i.ytimg.com/vi/CN_y_jA4e4A/hqdefault.jpg"),
    TrackModel("Tauba Tauba", "Karan Aujla", "https://www.youtube.com/watch?v=LK7-_dgAVQE", "https://i.ytimg.com/vi/LK7-_dgAVQE/hqdefault.jpg"),
    TrackModel("Winning Speech", "Karan Aujla", "https://www.youtube.com/watch?v=0pWsCDJvj78", "https://i.ytimg.com/vi/0pWsCDJvj78/hqdefault.jpg"),
    TrackModel("Millionaire", "Yo Yo Honey Singh", "https://www.youtube.com/watch?v=XO8wew38VM8", "https://i.ytimg.com/vi/XO8wew38VM8/hqdefault.jpg"),
    TrackModel("Hass Hass", "Diljit Dosanjh, Sia", "https://www.youtube.com/watch?v=4dUC4r5a6mQ", "https://i.ytimg.com/vi/4dUC4r5a6mQ/hqdefault.jpg"),
    TrackModel("Nadaaniyan", "Akshath", "https://www.youtube.com/watch?v=gT8hBqA_v84", "https://i.ytimg.com/vi/gT8hBqA_v84/hqdefault.jpg"),
    TrackModel("Jo Tum Mere Ho", "Anuv Jain", "https://www.youtube.com/watch?v=ilNt2bikxZ8", "https://i.ytimg.com/vi/ilNt2bikxZ8/hqdefault.jpg"),
    TrackModel("Husn", "Anuv Jain", "https://www.youtube.com/watch?v=0rE2wU91y4M", "https://i.ytimg.com/vi/0rE2wU91y4M/hqdefault.jpg")
)

@Composable
fun DesktopDevPicksScreen(viewModel: DesktopMusicViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 20.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(MusifyEmerald),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text("Developer's Picks", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Hand-picked special tracks selected by the Musify creator team", color = TextSecondary, fontSize = 13.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(DevPicksList) { index, track ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.playTrack(track, DevPicksList) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("${index + 1}", color = TextTertiary, fontSize = 13.sp, modifier = Modifier.width(28.dp))

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

                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(track.artist, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                    }

                    IconButton(onClick = { viewModel.playTrack(track, DevPicksList) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MusifyEmerald)
                    }
                }
            }
        }
    }
}
