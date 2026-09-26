package com.gaminghub.musicplayer.ui

import android.R.attr.id
import androidx.annotation.OptIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.gaminghub.musicplayer.R
import com.gaminghub.musicplayer.R.*
import com.gaminghub.musicplayer.R.drawable.song
import com.gaminghub.musicplayer.SettingsViewModel
import com.gaminghub.musicplayer.TrackModel
import com.gaminghub.musicplayer.ui.theme.MusifyGreen
import com.gaminghub.musicplayer.ui.theme.LocalAppGradients
import kotlinx.coroutines.flow.flowOf

data class CuratedCardData(
    val title: String,
    val subtitle: String = "",
    val imageUrl: Any? = null,
    val query: String = ""
)

@OptIn(UnstableApi::class)
@Composable
fun HomeScreen(
    tracks: List<TrackModel>,
    viewModel: MusicViewModel,
    settingsViewModel: SettingsViewModel,
    navController: NavController,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ── Stable content state — recompose only when data changes ───────────────
    val recentTracks by viewModel.recentTracks.collectAsState()
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val userName by settingsViewModel.userName.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val supermix by viewModel.supermix.collectAsState()
    val dailyMixes by viewModel.dailyMixes.collectAsState()
    val listenAgain by viewModel.listenAgain.collectAsState()
    val becauseYouLikeArtist by viewModel.becauseYouLikeArtist.collectAsState()
    val discoverFresh by viewModel.discoverFresh.collectAsState()
    val showPlaylistsOnHome by settingsViewModel.showPlaylistsOnHome.collectAsState()
    val showLastSession by settingsViewModel.showLastSession.collectAsState()
    val tasteSummary by viewModel.tasteSummary.collectAsState()

    // ── Playback-reactive state — only used in components that need it ─────────
    // Separated so that isPlaying/currentTrack toggling every frame doesn't
    // recompose the entire LazyColumn with all curated sections and image rows.
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()

    val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()
    var showEditNameDialog by remember { mutableStateOf(false) }

    // remember(navController) prevents lambda recreation on every recomposition,
    // which would invalidate all child composables that receive this callback.
    val openCuratedPlaylist: (CuratedCardData) -> Unit = remember(navController) {
        { card ->
            val encTitle = android.net.Uri.encode(card.title)
            val encSub = android.net.Uri.encode(card.subtitle.ifBlank { "Curated Playlist" })
            val encQuery = android.net.Uri.encode(card.query.ifBlank { card.title })
            val encImage = android.net.Uri.encode(card.imageUrl?.toString() ?: "")
            navController.navigate("curated_playlist/$encTitle/$encSub/$encQuery?imageUrl=$encImage")
        }
    }

    val featuredPlaylists = CuratedPlaylistConfig.featuredPlaylists
    val communityPlaylists = CuratedPlaylistConfig.communityPlaylists
    val dancingOnYourOwn = CuratedPlaylistConfig.dancingOnYourOwn
    val indiaBiggestHits = CuratedPlaylistConfig.indiaBiggestHits
    val nostalgicHits = CuratedPlaylistConfig.nostalgicHits
    val newReleases = CuratedPlaylistConfig.newReleases
    val albumsAndSingles = CuratedPlaylistConfig.albumsAndSingles
    val chaiAndChill = CuratedPlaylistConfig.chaiAndChill
    val musicVideos = CuratedPlaylistConfig.musicVideos

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            // ── 1. Top Bar Greeting & Search (Natural Scroll) ───────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                                .clickable { showEditNameDialog = true }
                        ) {
                            Text(
                                text = "Hi There,",
                                color = MusifyGreen,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = userName.ifBlank { "Krish" },//TODO:Krish->User Name
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Clean Musify Search Pill
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("search") }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MusifyGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Songs, albums or artists",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Working Category Filter Chips
                    CategoryFilterChipsRow(
                        categories = viewModel.categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { category ->
                            viewModel.selectCategory(category)
                        }
                    )
                }
            }

            // Loading indicator for category switch
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MusifyGreen, modifier = Modifier.size(36.dp))
                    }
                }
            }

            // ── 2. Your Playlists ─────────────────────────────────────
            item {
                Column {
                    SectionHeader(title = "Your Playlists", showPlayAll = false)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (favoriteTracks.isNotEmpty()) {
                            item {
                                val favArtUrls = remember(favoriteTracks) {
                                    favoriteTracks.mapNotNull { it.albumArtUrl }.filter { it.isNotBlank() }
                                }
                                Column(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .clickable { navController.navigate("favorites") }
                                ) {
                                    com.gaminghub.musicplayer.ui.components.PlaylistCollageThumbnail(
                                        artUrls = favArtUrls,
                                        modifier = Modifier.size(140.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Favorite Songs",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        lineHeight = 16.sp,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${favoriteTracks.size} Songs",
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (playlists.isNotEmpty()) {
                            items(playlists) { playlist ->
                                UserPlaylistSquareCard(
                                    playlist = playlist,
                                    viewModel = viewModel,
                                    onClick = { navController.navigate("playlist_detail/${playlist.id}/${playlist.name}") }
                                )
                            }
                        }
                        if (playlists.isEmpty() && favoriteTracks.isEmpty()) {
                            item {
                                CuratedSquareCard(
                                    card = CuratedCardData("Create Playlist", "Tap to add songs", ""),
                                    onClick = { navController.navigate("playlists") }
                                )
                            }
                        }
                    }
                }
            }

            // ── 2.1 Made For You / My Supermix Hero Card ───────────────────
            if (supermix.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Made for you",
                        badge = "AI Personalized",
                        showPlayAll = true,
                        onPlayAllClick = {
                            viewModel.playTrack(supermix.first(), supermix)
                        }
                    )
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .background(LocalAppGradients.current.cardBrush, RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                viewModel.playTrack(supermix.first(), supermix)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "My Supermix",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tasteSummary,
                                    color = MusifyGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${supermix.size} personalized songs tuned to your taste",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(
                                onClick = { viewModel.playTrack(supermix.first(), supermix) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MusifyGreen, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play Supermix",
                                    tint = Color.Black
                                )
                            }
                        }
                    }
                }
            }

            // ── 2.2 Your Daily Mixes ──────────────────────────────────────
            if (dailyMixes.isNotEmpty()) {
                item {
                    Column {
                        SectionHeader(
                            title = "Your Daily Mixes",
                            badge = "Vibe Clusters",
                            showPlayAll = false
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(dailyMixes) { mix ->
                                CuratedSquareCard(
                                    card = CuratedCardData(
                                        title = mix.title,
                                        subtitle = mix.subtitle,
                                        imageUrl = mix.coverUrl,
                                        query = ""
                                    ),
                                    onClick = {
                                        if (mix.tracks.isNotEmpty()) {
                                            viewModel.playTrack(mix.tracks.first(), mix.tracks)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ── 2.3 Listen Again (Heavy Rotation) ─────────────────────────
            if (listenAgain.isNotEmpty()) {
                item {
                    Column {
                        SectionHeader(
                            title = "Listen again",
                            showPlayAll = true,
                            onPlayAllClick = {
                                viewModel.playTrack(listenAgain.first(), listenAgain)
                            }
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(listenAgain) { track ->
                                CuratedSquareCard(
                                    card = CuratedCardData(
                                        title = track.title,
                                        subtitle = track.artist,
                                        imageUrl = track.albumArtUrl,
                                        query = ""
                                    ),
                                    onClick = {
                                        viewModel.playTrack(track, listenAgain)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ── 2.4 Because You Like [Top Artist] ─────────────────────────
            becauseYouLikeArtist?.let { (artistName, artistTracks) ->
                if (artistTracks.isNotEmpty()) {
                    item {
                        Column {
                            SectionHeader(
                                title = "Because you like $artistName",
                                showPlayAll = true,
                                onPlayAllClick = {
                                    viewModel.playTrack(artistTracks.first(), artistTracks)
                                }
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(artistTracks) { track ->
                                    CuratedSquareCard(
                                        card = CuratedCardData(
                                            title = track.title,
                                            subtitle = track.artist,
                                            imageUrl = track.albumArtUrl,
                                            query = ""
                                        ),
                                        onClick = {
                                            viewModel.playTrack(track, artistTracks)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 2.5 Discover Fresh Music ──────────────────────────────────
            if (discoverFresh.isNotEmpty()) {
                item {
                    Column {
                        SectionHeader(
                            title = "Discover fresh music",
                            badge = "New to You",
                            showPlayAll = true,
                            onPlayAllClick = {
                                viewModel.playTrack(discoverFresh.first(), discoverFresh)
                            }
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(discoverFresh) { track ->
                                CuratedSquareCard(
                                    card = CuratedCardData(
                                        title = track.title,
                                        subtitle = track.artist,
                                        imageUrl = track.albumArtUrl,
                                        query = ""
                                    ),
                                    onClick = {
                                        viewModel.playTrack(track, discoverFresh)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ── 2.6 Last Session ──────────────────────────────────────
            if (showLastSession && recentTracks.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Last Session",
                        actionText = "See all",
                        onActionClick = { navController.navigate("last_session") }
                    )
                }
                items(recentTracks.take(4)) { track ->
                    ModernTrackListItem(
                        track = track,
                        isCurrent = currentTrack?.audioUrl == track.audioUrl,
                        isPlaying = isPlaying && currentTrack?.audioUrl == track.audioUrl,
                        viewModel = viewModel,
                        queue = recentTracks
                    )
                }
            }

            // ── 3. Featured Playlists For You ─────────────────────────
            if (showPlaylistsOnHome) {
                item {
                    Column {
                        SectionHeader(title = "Featured playlists for you", showPlayAll = false)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(featuredPlaylists) { card ->
                                CuratedSquareCard(card = card, onClick = { openCuratedPlaylist(card) })
                            }
                        }
                    }
                }

                // ── 4. Trending Community Playlists ───────────────────────
                item {
                    Column {
                        SectionHeader(title = "Trending community playlists", showPlayAll = false)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(communityPlaylists) { card ->
                                CuratedSquareCard(card = card, onClick = { openCuratedPlaylist(card) })
                            }
                        }
                    }
                }
            }

            // ── 5. Dancing on your own ────────────────────────────────
            item {
                Column {
                    SectionHeader(title = "Dancing on your own", showPlayAll = false)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(dancingOnYourOwn) { card ->
                            CuratedSquareCard(card = card, onClick = { openCuratedPlaylist(card) })
                        }
                    }
                }
            }

            // ── 6. India's Biggest Hits ───────────────────────────────
            item {
                Column {
                    SectionHeader(title = "India's biggest hits", showPlayAll = false)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(indiaBiggestHits) { card ->
                            CuratedSquareCard(card = card, onClick = { openCuratedPlaylist(card) })
                        }
                    }
                }
            }

            // ── 7. Brb, Being Nostalgic! ──────────────────────────────
            item {
                Column {
                    SectionHeader(title = "Brb, Being Nostalgic!", showPlayAll = false)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(nostalgicHits) { card ->
                            CuratedSquareCard(card = card, onClick = { openCuratedPlaylist(card) })
                        }
                    }
                }
            }

            // ── 8. Quick Picks ────────────────────────────────────────
            if (tracks.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Quick picks",
                        showPlayAll = true,
                        onPlayAllClick = {
                            tracks.firstOrNull()?.let { viewModel.playTrack(it, tracks) }
                        }
                    )
                }
                items(tracks.take(5)) { track ->
                    ModernTrackListItem(
                        track = track,
                        isCurrent = currentTrack?.audioUrl == track.audioUrl,
                        isPlaying = isPlaying && currentTrack?.audioUrl == track.audioUrl,
                        viewModel = viewModel,
                        queue = tracks
                    )
                }
            }

            // ── 9. New Releases ───────────────────────────────────────
            item {
                Column {
                    SectionHeader(title = "New releases", showPlayAll = false)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(newReleases) { card ->
                            CuratedSquareCard(card = card, onClick = { openCuratedPlaylist(card) })
                        }
                    }
                }
            }

            // ── 10. Chai & Chill ──────────────────────────────────────
            item {
                Column {
                    SectionHeader(title = "Chai & Chill", showPlayAll = false)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(chaiAndChill) { card ->
                            CuratedSquareCard(card = card, onClick = { openCuratedPlaylist(card) })
                        }
                    }
                }
            }

            // ── 11. Acoustic & Soulful ──────────────────────────────────
            item {
                Column {
                    SectionHeader(title = "Acoustic & Soulful", showPlayAll = false)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(albumsAndSingles) { card ->
                            CuratedSquareCard(card = card, onClick = { openCuratedPlaylist(card) })
                        }
                    }
                }
            }

            // ── 12. Music Videos ──────────────────────────────────────
            item {
                Column {
                    SectionHeader(title = "Music videos", showPlayAll = false)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(musicVideos) { card ->
                            CuratedSquareCard(card = card, onClick = { openCuratedPlaylist(card) })
                        }
                    }
                }
            }
        }
    }

    if (showEditNameDialog) {
        var tempName by remember { mutableStateOf(userName.ifBlank { "Krish" }) }//TODO:Krish->User
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = Color(0xFF383838),
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Name",
                    color = MusifyGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                TextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = MusifyGreen,
                        focusedIndicatorColor = MusifyGreen,
                        unfocusedIndicatorColor = MusifyGreen
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            settingsViewModel.setUserName(tempName.trim())
                        }
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Ok", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun CuratedSquareCard(
    card: CuratedCardData,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        com.gaminghub.musicplayer.ui.components.PlaylistImage(
            model = card.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = card.title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            lineHeight = 16.sp,
            overflow = TextOverflow.Ellipsis
        )
        if (card.subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = card.subtitle,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun UserPlaylistSquareCard(
    playlist: com.gaminghub.musicplayer.data.PlaylistEntity,
    viewModel: MusicViewModel,
    onClick: () -> Unit
) {
    val tracks by viewModel.getTracksForPlaylist(playlist.id).collectAsState(initial = emptyList())
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        com.gaminghub.musicplayer.ui.components.PlaylistThumbnail(
            playlistId = playlist.id,
            viewModel = viewModel,
            modifier = Modifier.size(140.dp),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = playlist.name,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            lineHeight = 16.sp,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${tracks.size} Songs",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CategoryFilterChipsRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(categories) { category ->
            val isSelected = category.equals(selectedCategory, ignoreCase = true)
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) MusifyGreen else MaterialTheme.colorScheme.surfaceVariant,
                label = "chipBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "chipText"
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = backgroundColor,
                border = BorderStroke(1.dp, if (isSelected) MusifyGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onCategorySelected(category) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected && category == "Trending") {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = category,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun ModernTrackListItem(
    track: TrackModel,
    isCurrent: Boolean,
    isPlaying: Boolean,
    viewModel: MusicViewModel,
    queue: List<TrackModel> = emptyList()
) {
    var showMenu by remember { mutableStateOf(false) }
    val favoriteUrls by viewModel.favoriteUrls.collectAsState()
    val isFavorite = track.audioUrl != null && favoriteUrls.contains(track.audioUrl)
    val baseLikes = remember(track.audioUrl) { viewModel.getBaseSongLikes(track) }
    val songLikes = remember(isFavorite, baseLikes) { viewModel.formatCount(if (isFavorite) baseLikes + 1 else baseLikes) }
    val downloadedUrls by viewModel.downloadedUrls.collectAsState()
    val isDownloaded = track.audioUrl != null && downloadedUrls.contains(track.audioUrl)
    val context = androidx.compose.ui.platform.LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.playTrack(track, queue) }
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
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (songLikes.isNotBlank()) "${track.artist} • $songLikes likes" else track.artist,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = { viewModel.toggleFavorite(track) }, modifier = Modifier.size(36.dp)) {
            Icon(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) MusifyGreen else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                DropdownMenuItem(
                    text = { Text(if (isDownloaded) "Downloaded" else "Download Song", color = MusifyGreen) },
                    leadingIcon = { Icon(if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.DownloadForOffline, null, tint = MusifyGreen) },
                    onClick = { viewModel.toggleDownload(track); showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Copy Song Link", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = MusifyGreen) },
                    onClick = {
                        showMenu = false
                        com.gaminghub.musicplayer.util.LinkHelper.copySongLink(context, track)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Share Song", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.Default.Share, null, tint = MusifyGreen) },
                    onClick = {
                        showMenu = false
                        com.gaminghub.musicplayer.util.LinkHelper.shareSong(context, track)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Play Next", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.onSurface) },
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
                    text = { Text("Remove from History", color = Color(0xFFFF5252)) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252)) },
                    onClick = {
                        showMenu = false
                        viewModel.removeFromRecentHistory(track)
                        android.widget.Toast.makeText(context, "Removed from history", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    badge: String? = null,
    showPlayAll: Boolean = false,
    onPlayAllClick: () -> Unit = {},
    actionText: String? = null,
    onActionClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = MusifyGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (badge != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MusifyGreen.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badge,
                        color = MusifyGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (showPlayAll) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Play all",
                color = MusifyGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onPlayAllClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        } else if (actionText != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = actionText,
                color = MusifyGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onActionClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}
