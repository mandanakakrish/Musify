package com.gaminghub.musicplayer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonPin
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
import androidx.compose.ui.window.Dialog
import com.gaminghub.musicplayer.MusicViewModel
import com.gaminghub.musicplayer.TrackModel
import com.gaminghub.musicplayer.ui.components.SongImage
import com.gaminghub.musicplayer.ui.theme.MusifyGreen

@Composable
fun LinkArtistDialog(
    currentTrack: TrackModel,
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    var artistInput by remember { mutableStateOf(currentTrack.artist) }

    // Auto-extract candidate artists from song title (e.g., "Kamariya (From Stree) ft. Aastha Gill")
    val suggestedArtists = remember(currentTrack) {
        val candidates = mutableSetOf<String>()
        val title = currentTrack.title
        val artist = currentTrack.artist

        // Split artist string
        artist.split(",", "&", "feat.", "ft.", "/").forEach {
            val clean = it.trim()
            if (clean.length > 2 && !clean.equals("Unknown Artist", ignoreCase = true)) {
                candidates.add(clean)
            }
        }

        // Check if title mentions known singer tags
        val ftMatch = Regex("(?i)(?:feat\\.?|ft\\.?|by)\\s+([a-zA-Z0-9\\s,]+)").find(title)
        ftMatch?.groupValues?.getOrNull(1)?.split(",", "&")?.forEach {
            val clean = it.trim()
            if (clean.length > 2) candidates.add(clean)
        }

        candidates.toList()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1E24),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PersonPin,
                            contentDescription = null,
                            tint = MusifyGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Link Artist to Song",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Song Info Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF121216), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SongImage(
                        model = currentTrack.albumArtUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTrack.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!currentTrack.uploaderChannel.isNullOrBlank()) {
                            Text(
                                text = "Uploader: ${currentTrack.uploaderChannel}",
                                color = Color(0xFF9E9E9E),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Music Artist Name",
                    color = Color(0xFFB3B3B3),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = artistInput,
                    onValueChange = { artistInput = it },
                    placeholder = { Text("Enter performer or singer name", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MusifyGreen,
                        unfocusedBorderColor = Color(0xFF444450),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF16161C),
                        unfocusedContainerColor = Color(0xFF16161C)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (suggestedArtists.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Suggested Artists",
                        color = Color(0xFFB3B3B3),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(suggestedArtists) { sug ->
                            Surface(
                                color = if (artistInput.equals(sug, ignoreCase = true)) MusifyGreen.copy(alpha = 0.2f) else Color(0xFF2A2A34),
                                shape = RoundedCornerShape(16.dp),
                                border = if (artistInput.equals(sug, ignoreCase = true)) BorderStroke(1.dp, MusifyGreen) else null,
                                modifier = Modifier.clickable { artistInput = sug }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (artistInput.equals(sug, ignoreCase = true)) MusifyGreen else Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = sug,
                                        color = if (artistInput.equals(sug, ignoreCase = true)) MusifyGreen else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val clean = artistInput.trim()
                            if (clean.isNotBlank()) {
                                viewModel.updateTrackArtist(currentTrack, clean)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save & Link", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
