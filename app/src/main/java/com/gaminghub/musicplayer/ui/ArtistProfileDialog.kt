package com.gaminghub.musicplayer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.window.Dialog
import androidx.media3.common.util.UnstableApi
import com.gaminghub.musicplayer.ui.theme.MusifyGreen
import com.gaminghub.musicplayer.ui.components.SongImage
import com.gaminghub.musicplayer.MusicViewModel
import com.gaminghub.musicplayer.TrackModel
import com.gaminghub.musicplayer.ui.theme.MusifyDarkBg
import com.gaminghub.musicplayer.ui.theme.MusifyGlassBorder
import com.gaminghub.musicplayer.ui.theme.MusifyGlassSurface
import kotlinx.coroutines.flow.flowOf

@UnstableApi
@Composable
fun ArtistProfileDialog(
    artistName: String,
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val cleanArtist = artistName.trim().ifBlank { "Unknown Artist" }
    var artistTracks by remember { mutableStateOf<List<TrackModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val isFollowed by viewModel.isArtistFollowed(cleanArtist).collectAsState(initial = false)
    val followerCount by viewModel.getArtistFollowersFormatted(cleanArtist).collectAsState(initial = "")

    LaunchedEffect(cleanArtist) {
        isLoading = true
        artistTracks = viewModel.fetchArtistTopTracks(cleanArtist)
        isLoading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF121212),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with Artist Banner & Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF1DB954).copy(alpha = 0.5f), Color(0xFF121212))
                            )
                        )
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = Color(0xFF1DB954),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (followerCount.isNotBlank()) "$followerCount Followers • Verified" else "Verified Artist",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Text(
                            text = cleanArtist,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Action Row (Follow Button + Start Radio)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.toggleFollowArtist(cleanArtist) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowed) Color(0xFF282828) else Color(0xFF1DB954),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isFollowed) Icons.Default.Check else Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isFollowed) "Following ($followerCount)" else "Follow ($followerCount)")
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.startArtistRadio(cleanArtist)
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MusifyGlassBorder),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Podcasts, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Artist Radio")
                    }
                }

                Text(
                    text = "Popular Songs",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MusifyGreen)
                    }
                } else if (artistTracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No songs found for this artist", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        itemsIndexed(artistTracks) { index, track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.playTrack(track, artistTracks)
                                        onDismiss()
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(28.dp)
                                )

                                SongImage(
                                    model = track.albumArtUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artist,
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.playTrack(track, artistTracks)
                                        onDismiss()
                                    }
                                ) {
                                    Icon(Icons.Default.PlayCircleFilled, contentDescription = "Play", tint = MusifyGreen)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
