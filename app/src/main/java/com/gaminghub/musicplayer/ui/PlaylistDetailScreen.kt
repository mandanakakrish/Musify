package com.gaminghub.musicplayer.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.gaminghub.musicplayer.MusicViewModel
import com.gaminghub.musicplayer.TrackModel
import com.gaminghub.musicplayer.ui.theme.MusifyGreen

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playlistName: String,
    viewModel: MusicViewModel,
    navController: NavController
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Songs", "Albums", "Artists", "Genres")

    val allTracks by viewModel.tracks.collectAsState()
    val playlistTracks by viewModel.getTracksForPlaylist(playlistId.toInt()).collectAsState(initial = emptyList())

    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    // Grouping for tabs
    val albumsMap = remember(playlistTracks) {
        playlistTracks.groupBy { it.album?.ifBlank { it.title } ?: it.title }
    }
    val artistsMap = remember(playlistTracks) {
        playlistTracks.groupBy { it.artist.ifBlank { "Unknown Artist" } }
    }
    val genresMap = remember(playlistTracks) {
        playlistTracks.groupBy { it.genre?.ifBlank { "YouTube" } ?: "YouTube" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = playlistName,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.navigate("search") }) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground)
                }
                IconButton(onClick = { }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
            // ── Tabs ─────────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MusifyGreen,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MusifyGreen,
                            height = 3.dp
                        )
                    }
                },
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTab == index) MusifyGreen else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                // ── 1. SONGS TAB ─────────────────────────────────────────────
                0 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Subheader: Count + Shuffle + Play
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 0.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically

                        ) {
                            Text(
                                "${playlistTracks.size} Songs",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (playlistTracks.isNotEmpty()) {
                                                val shuffled = playlistTracks.shuffled()
                                                viewModel.playTrack(shuffled.first(), shuffled)
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Shuffle, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Shuffle", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                IconButton(
                                    onClick = {
                                        if (playlistTracks.isNotEmpty()) {
                                            viewModel.playTrack(playlistTracks.first(), playlistTracks)
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(28.dp))
                                }
                            }
                        }

                        if (playlistTracks.isEmpty()) {
                            SpotifyEmptyState(actionText = "Go and Add Something")
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 140.dp)
                            ) {
                                itemsIndexed(playlistTracks, key = { index, track -> "${track.audioUrl ?: track.title}_$index" }) { index, track ->
                                    PlaylistSongRow(
                                        track = track,
                                        playlistId = playlistId,
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

                // ── 2. ALBUMS TAB ────────────────────────────────────────────
                1 -> {
                    if (albumsMap.isEmpty()) {
                        SpotifyEmptyState(actionText = "No Albums Found")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp)
                        ) {
                            items(albumsMap.toList(), key = { it.first }) { (albumName, tracks) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.playTrack(tracks.first(), tracks) }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    com.gaminghub.musicplayer.ui.components.SongImage(
                                        model = tracks.firstOrNull()?.albumArtUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = albumName,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (tracks.size == 1) "1 Song" else "${tracks.size} Songs",
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 3. ARTISTS TAB ───────────────────────────────────────────
                2 -> {
                    if (artistsMap.isEmpty()) {
                        SpotifyEmptyState(actionText = "No Artists Found")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp)
                        ) {
                            items(artistsMap.toList(), key = { it.first }) { (artistName, tracks) ->
                                val followers by viewModel.getArtistFollowersFormatted(artistName).collectAsState(initial = "")
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.playTrack(tracks.first(), tracks) }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    com.gaminghub.musicplayer.ui.components.ArtistImage(
                                        model = tracks.firstOrNull()?.albumArtUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = artistName,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (followers.isNotBlank()) "$followers Followers • ${if (tracks.size == 1) "1 Song" else "${tracks.size} Songs"}" else if (tracks.size == 1) "1 Song" else "${tracks.size} Songs",
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 4. GENRES TAB ────────────────────────────────────────────
                3 -> {
                    if (genresMap.isEmpty()) {
                        SpotifyEmptyState(actionText = "No Genres Found")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp)
                        ) {
                            items(genresMap.toList(), key = { it.first }) { (genreName, tracks) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.playTrack(tracks.first(), tracks) }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 4-Grid Collage Artwork
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        val arts = tracks.take(4).mapNotNull { it.albumArtUrl }
                                        if (arts.size >= 4) {
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                Row(modifier = Modifier.weight(1f)) {
                                                    com.gaminghub.musicplayer.ui.components.SongImage(model = arts[0], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
                                                    com.gaminghub.musicplayer.ui.components.SongImage(model = arts[1], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
                                                }
                                                Row(modifier = Modifier.weight(1f)) {
                                                    com.gaminghub.musicplayer.ui.components.SongImage(model = arts[2], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
                                                    com.gaminghub.musicplayer.ui.components.SongImage(model = arts[3], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
                                                }
                                            }
                                        } else if (arts.isNotEmpty()) {
                                            com.gaminghub.musicplayer.ui.components.SongImage(model = arts.first(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        } else {
                                            com.gaminghub.musicplayer.ui.components.PlaylistImage(model = null, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = genreName,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (tracks.size == 1) "1 Song" else "${tracks.size} Songs",
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

@OptIn(UnstableApi::class)
@Composable
fun PlaylistSongRow(
    track: TrackModel,
    playlistId: Long = 0L,
    isCurrent: Boolean,
    isPlaying: Boolean,
    viewModel: MusicViewModel,
    onPlay: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showLinkArtistDialog by remember { mutableStateOf(false) }
    val favoriteUrls by viewModel.favoriteUrls.collectAsState()
    val isFavorite = track.audioUrl != null && favoriteUrls.contains(track.audioUrl)
    val baseLikes = remember(track.audioUrl) { viewModel.getBaseSongLikes(track) }
    val songLikes = remember(isFavorite, baseLikes) { viewModel.formatCount(if (isFavorite) baseLikes + 1 else baseLikes) }
    val downloadedUrls by viewModel.downloadedUrls.collectAsState()
    val isDownloaded = track.audioUrl != null && downloadedUrls.contains(track.audioUrl)
    val context = androidx.compose.ui.platform.LocalContext.current
    val authManager = remember { com.gaminghub.musicplayer.auth.AuthManager.getInstance(context) }
    val isAdmin by authManager.isAdmin.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.gaminghub.musicplayer.ui.components.SongImage(
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
                color = if (isCurrent) MusifyGreen else MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (songLikes.isNotBlank()) "${track.artist} • $songLikes likes" else track.artist,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = { viewModel.toggleFavorite(track) }, modifier = Modifier.size(36.dp)) {
            Icon(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) MusifyGreen else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = Color(0xFF22222E),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.background(Color(0xFF22222E), RoundedCornerShape(12.dp))
            ) {
                DropdownMenuItem(
                    text = { Text("Remove from Playlist", color = Color(0xFFFF5252)) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252)) },
                    onClick = {
                        showMenu = false
                        viewModel.removeTrackFromPlaylist(playlistId.toInt(), track.audioUrl ?: "")
                    }
                )
                if (isAdmin) {
                    DropdownMenuItem(
                        text = { Text("Link / Edit Artist", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.PersonPin, null, tint = MusifyGreen) },
                        onClick = {
                            showMenu = false
                            showLinkArtistDialog = true
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Copy Song Link", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = MusifyGreen) },
                    onClick = {
                        showMenu = false
                        com.gaminghub.musicplayer.util.LinkHelper.copySongLink(context, track)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Share Song", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Share, null, tint = MusifyGreen) },
                    onClick = {
                        showMenu = false
                        com.gaminghub.musicplayer.util.LinkHelper.shareSong(context, track)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Play Next", color = Color.White) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = Color.White) },
                    onClick = { showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Add to Queue", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.onSurface) },
                    onClick = { showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Add to Playlist", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurface) },
                    onClick = { showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("View Album", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.Default.Album, null, tint = MaterialTheme.colorScheme.onSurface) },
                    onClick = { showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("View Artist (${track.artist.take(15)})", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurface) },
                    onClick = { showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Play Radio", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.Default.Podcasts, null, tint = MaterialTheme.colorScheme.onSurface) },
                    onClick = { showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text(if (isDownloaded) "Downloaded" else "Download Song", color = MusifyGreen) },
                    leadingIcon = { Icon(if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.DownloadForOffline, null, tint = MusifyGreen) },
                    onClick = { viewModel.toggleDownload(track); showMenu = false }
                )
            }
        }
    }

    if (showLinkArtistDialog) {
        LinkArtistDialog(
            currentTrack = track,
            viewModel = viewModel,
            onDismiss = { showLinkArtistDialog = false }
        )
    }
}
