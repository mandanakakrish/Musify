package com.gaminghub.musicplayer.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.util.UnstableApi
import com.gaminghub.musicplayer.MusicViewModel
import com.gaminghub.musicplayer.TrackModel
import com.gaminghub.musicplayer.auth.AuthManager
import com.gaminghub.musicplayer.data.repository.ArtistSyncManager
import com.gaminghub.musicplayer.data.repository.RealtimeArtist
import com.gaminghub.musicplayer.ui.components.ArtistImage
import com.gaminghub.musicplayer.ui.components.SongImage
import com.gaminghub.musicplayer.ui.theme.MusifyGlassBorder
import com.gaminghub.musicplayer.ui.theme.MusifyGlassSurface
import com.gaminghub.musicplayer.ui.theme.MusifyGreen
import kotlinx.coroutines.launch

private fun formatFollowerCount(count: Long): String {
    return when {
        count >= 1_000_000_000 -> String.format(java.util.Locale.US, "%.1fB", count / 1_000_000_000.0)
        count >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", count / 1_000.0)
        count > 0 -> "$count"
        else -> ""
    }
}

@UnstableApi
@Composable
fun ArtistProfileDialog(
    artistName: String,
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val cleanArtist = artistName.trim().ifBlank { "Unknown Artist" }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Admin state check
    val isAdmin by AuthManager.getInstance(context).isAdmin.collectAsState()

    // Synced Realtime Artist from Firestore & Live Open APIs
    val artistKey = remember(cleanArtist) { ArtistSyncManager.sanitizeArtistId(cleanArtist) }
    val syncedMap by ArtistSyncManager.syncedArtists.collectAsState()
    var realtimeArtist by remember(cleanArtist) {
        mutableStateOf(syncedMap[artistKey] ?: RealtimeArtist(name = cleanArtist))
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var artistTracks by remember { mutableStateOf<List<TrackModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val isFollowed by viewModel.isArtistFollowed(cleanArtist).collectAsState(initial = false)
    val localFollowerFormatted by viewModel.getArtistFollowersFormatted(cleanArtist).collectAsState(initial = "")

    // Realtime sync fetch & tracks fetch
    LaunchedEffect(cleanArtist) {
        isLoading = true
        coroutineScope.launch {
            try {
                val fetched = ArtistSyncManager.getArtistDetails(cleanArtist)
                realtimeArtist = fetched
            } catch (_: Exception) {}
        }
        artistTracks = viewModel.fetchArtistTopTracks(cleanArtist)
        isLoading = false
    }

    // Keep realtimeArtist in sync with live Firestore updates
    LaunchedEffect(syncedMap) {
        syncedMap[artistKey]?.let { updated ->
            realtimeArtist = updated
        }
    }

    val displayImage = realtimeArtist.bannerUrl.ifBlank { realtimeArtist.imageUrl }
    val displayAvatar = realtimeArtist.imageUrl.ifBlank { displayImage }
    val displayFollowers = if (realtimeArtist.followerCount > 0L) {
        "${formatFollowerCount(realtimeArtist.followerCount)} Followers"
    } else if (localFollowerFormatted.isNotBlank()) {
        "$localFollowerFormatted Followers"
    } else ""

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF121212),
            border = BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Header with Artist Banner & Realtime Photo ──────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    // Artist Banner Photo
                    if (displayImage.isNotBlank()) {
                        ArtistImage(
                            model = displayImage,
                            contentDescription = cleanArtist,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Scrim gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Black.copy(alpha = 0.65f),
                                        Color(0xFF121212)
                                    )
                                )
                            )
                    )

                    // Top Action Controls (Admin Edit + Close Button)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Admin Edit Profile & Image Link Button
                        if (isAdmin) {
                            Surface(
                                onClick = { showEditDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                color = MusifyGreen,
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Profile",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Edit (Admin)",
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    // Artist Identity & Follower count
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (displayAvatar.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MusifyGreen, CircleShape)
                                    .background(Color(0xFF282834))
                            ) {
                                ArtistImage(
                                    model = displayAvatar,
                                    contentDescription = cleanArtist,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (realtimeArtist.isVerified) {
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = "Verified",
                                        tint = MusifyGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = if (displayFollowers.isNotBlank()) "$displayFollowers • ${realtimeArtist.tagline.ifBlank { "Verified Artist" }}" else "Verified Artist",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = cleanArtist,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (realtimeArtist.genre.isNotBlank() && realtimeArtist.genre != "Various") {
                                Text(
                                    text = realtimeArtist.genre,
                                    color = MusifyGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // ── Action Row (Follow Button + Start Radio) ────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.toggleFollowArtist(cleanArtist, displayAvatar) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowed) Color(0xFF282828) else MusifyGreen,
                            contentColor = if (isFollowed) Color.White else Color.Black
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
                        Text(
                            text = if (isFollowed) "Following" else "Follow",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.startArtistRadio(cleanArtist)
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, MusifyGlassBorder),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Podcasts, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Artist Radio", fontWeight = FontWeight.SemiBold)
                    }
                }

                // ── Artist Bio (if available) ──────────────────────────────────
                if (realtimeArtist.bio.isNotBlank()) {
                    Surface(
                        color = MusifyGlassSurface,
                        border = BorderStroke(1.dp, MusifyGlassBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = realtimeArtist.bio,
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Popular Songs",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (artistTracks.isNotEmpty()) {
                        Text(
                            text = "${artistTracks.size} tracks",
                            color = MusifyGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // ── Tracks List / Loading Indicator ───────────────────────────
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
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
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

    // ── Admin Edit Modal ────────────────────────────────────────────────────────
    if (showEditDialog) {
        EditArtistProfileDialog(
            initialArtist = realtimeArtist,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                ArtistSyncManager.saveArtistToFirestore(updated, updatedBy = "admin")
                realtimeArtist = updated
                showEditDialog = false
                Toast.makeText(context, "Artist details synced to Firebase!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun EditArtistProfileDialog(
    initialArtist: RealtimeArtist,
    onDismiss: () -> Unit,
    onSave: (RealtimeArtist) -> Unit
) {
    var name by remember { mutableStateOf(initialArtist.name) }
    var imageUrl by remember { mutableStateOf(initialArtist.imageUrl) }
    var bannerUrl by remember { mutableStateOf(initialArtist.bannerUrl) }
    var genre by remember { mutableStateOf(initialArtist.genre) }
    var category by remember { mutableStateOf(initialArtist.category) }
    var tagline by remember { mutableStateOf(initialArtist.tagline) }
    var bio by remember { mutableStateOf(initialArtist.bio) }
    var followersStr by remember { mutableStateOf(initialArtist.followerCount.toString()) }
    var isVerified by remember { mutableStateOf(initialArtist.isVerified) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1E1E),
            border = BorderStroke(1.dp, MusifyGreen.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MusifyGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Edit Artist (Admin)",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        // Image Preview Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, MusifyGreen, CircleShape)
                                    .background(Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                ArtistImage(
                                    model = imageUrl,
                                    contentDescription = name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Live Avatar Preview\nPaste image URL below to update instantly",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Artist Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MusifyGreen
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = imageUrl,
                            onValueChange = { imageUrl = it },
                            label = { Text("Avatar / Profile Image URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MusifyGreen
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = bannerUrl,
                            onValueChange = { bannerUrl = it },
                            label = { Text("Banner Image URL (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MusifyGreen
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = genre,
                            onValueChange = { genre = it },
                            label = { Text("Genre (e.g. Bollywood • Romance)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MusifyGreen
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category (Bollywood, Punjabi, Pop, etc.)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MusifyGreen
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = tagline,
                            onValueChange = { tagline = it },
                            label = { Text("Tagline (e.g. Voice of romance)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MusifyGreen
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = followersStr,
                            onValueChange = { followersStr = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Follower Count (e.g. 5000000)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MusifyGreen
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Bio / Description") },
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MusifyGreen
                            )
                        )
                    }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = isVerified,
                                onCheckedChange = { isVerified = it },
                                colors = CheckboxDefaults.colors(checkedColor = MusifyGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verified Artist Badge", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val fol = followersStr.toLongOrNull() ?: 0L
                            val updated = initialArtist.copy(
                                name = name.trim(),
                                imageUrl = imageUrl.trim(),
                                bannerUrl = bannerUrl.trim(),
                                genre = genre.trim().ifBlank { "Music" },
                                category = category.trim().ifBlank { "All" },
                                tagline = tagline.trim(),
                                bio = bio.trim(),
                                followerCount = fol,
                                isVerified = isVerified,
                                updatedAt = System.currentTimeMillis(),
                                updatedBy = "admin"
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save to Cloud", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
