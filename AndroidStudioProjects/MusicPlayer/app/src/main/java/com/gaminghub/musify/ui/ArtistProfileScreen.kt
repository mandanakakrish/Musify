package com.gaminghub.musify.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.gaminghub.musify.ArtistModel
import com.gaminghub.musify.TrackModel
import com.gaminghub.musify.data.repository.YouTubeRepository
import com.gaminghub.musify.ui.viewmodels.LibraryViewModel
import com.gaminghub.musify.ui.viewmodels.PlaybackViewModel
import com.gaminghub.musify.util.AuthManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistProfileScreen(
    artistId: String,
    onBack: () -> Unit,
    navController: NavController,
    playbackViewModel: PlaybackViewModel,
    libraryViewModel: LibraryViewModel
) {
    val repository = remember { YouTubeRepository() }
    var artist by remember { mutableStateOf<ArtistModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Artist Follow state
    val isFollowed by libraryViewModel.isArtistFollowed(artistId).collectAsState(initial = false)

    LaunchedEffect(artistId) {
        isLoading = true
        artist = repository.fetchArtistDetails(artistId)
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFE91E63))
            }
        } else if (artist == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Artist not found", color = Color.Gray)
            }
        } else {
            val currentArtist = artist!!
            
            // Hero Image Background (Parallax-ish)
            AsyncImage(
                model = currentArtist.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
                contentScale = ContentScale.Crop
            )

            // Gradient Overlay for Hero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(200.dp))
                
                // Artist Header Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentArtist.name,
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Verified, 
                            contentDescription = "Verified", 
                            tint = Color(0xFF3897F0),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Text(
                        text = "${currentArtist.subscribers ?: "Unknown"} listeners",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { 
                                currentArtist.topTracks.firstOrNull()?.let { 
                                    playbackViewModel.playTrack(it)
                                    playbackViewModel.setQueue(currentArtist.topTracks)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Shuffle Play")
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        OutlinedButton(
                            onClick = { libraryViewModel.toggleFollowArtist(currentArtist) },
                            border = BorderStroke(1.dp, if (isFollowed) Color.Gray else Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                if (isFollowed) "Following" else "Follow", 
                                color = if (isFollowed) Color.Gray else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Top Tracks
                Text(
                    "Popular Releases",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                currentArtist.topTracks.take(5).forEachIndexed { index, track ->
                    ArtistTrackItem(
                        index = index + 1,
                        track = track,
                        onClick = { 
                            playbackViewModel.playTrack(track)
                            playbackViewModel.setQueue(currentArtist.topTracks)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // About section
                Text(
                    "About the Artist",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = currentArtist.bio ?: "No biography available for this artist.",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Custom Top Bar with Back button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, 
                    contentDescription = "Back", 
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ArtistTrackItem(index: Int, track: TrackModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString(),
            modifier = Modifier.width(24.dp),
            color = Color.Gray,
            fontSize = 14.sp
        )
        
        AsyncImage(
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
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Track",
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
    }
}
