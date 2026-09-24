package com.gaminghub.musicplayer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.gaminghub.musicplayer.MusicViewModel
import com.gaminghub.musicplayer.ui.theme.MusifyGreen
import com.gaminghub.musicplayer.TrackModel

@OptIn(ExperimentalLayoutApi::class, UnstableApi::class)
@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    navController: NavController? = null
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchSuggestions by viewModel.searchSuggestions.collectAsState()
    val isSearchSubmitted by viewModel.isSearchSubmitted.collectAsState()
    val searchResults by viewModel.searchTracks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val focusManager = LocalFocusManager.current
    var showAllSongs by remember { mutableStateOf(false) }

    val trendingChips = remember {
        listOf(
            "Right Now", "Him & I", "Happy Birthday Songs", "Jamaican",
            "Gym Workout", "The Weeknd", "Believer", "Tokyo Drift", "Bilionera"
        )
    }

    val searchArtists = remember(searchResults) {
        listOf(
            Triple("Todays Top Trending Songs", "Artist • 5 subscribers", "https://i.ytimg.com/vi/qL_PtwfL6-o/hqdefault.jpg"),
            Triple("Zee Nxumalo", "Artist • 5.73M monthly...", "https://i.ytimg.com/vi/X3b8r-6r8_g/hqdefault.jpg"),
            Triple("Arijit Singh", "Artist • 42M subscribers", "https://i.ytimg.com/vi/Yw0S_wT5y_Y/hqdefault.jpg"),
            Triple("Karan Aujla", "Artist • 12M subscribers", "https://i.ytimg.com/vi/8U-5wH7M6U0/hqdefault.jpg")
        )
    }

    val searchPlaylists = remember(searchResults) {
        listOf(
            CuratedCardData("Bollywood Fire", "Playlist • YouTube Music", "https://images.unsplash.com/photo-1546707012-c46675f12716?w=500&q=80", "Bollywood Fire"),
            CuratedCardData("Haryanvi Hits 2025", "Playlist • YouTube Music", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=500&q=80", "Haryanvi Hits"),
            CuratedCardData("Top Hits 2026", "Playlist • YouTube Music", "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80", "Top Hits 2026")
        )
    }

    val searchAlbums = remember(searchResults) {
        listOf(
            CuratedCardData("Latest English Songs", "Album • Pop Hits", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80", "Latest English Songs"),
            CuratedCardData("Top Pop Songs", "Album • Global Hits", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=500&q=80", "Top Pop Songs"),
            CuratedCardData("Toxic Melodies", "Album • Sony Music", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80", "Toxic Melodies")
        )
    }

    val searchVideos = remember(searchResults) {
        listOf(
            Triple("Ve Haaniyaan (feat. Avvy Sra)", "Video • Danny • 380M views • 3:25", "https://i.ytimg.com/vi/9f4T4-P8K5Q/hqdefault.jpg"),
            Triple("Best Romantic Songs", "Video • T-Series • 120M views", "https://i.ytimg.com/vi/a_6416A3g6s/hqdefault.jpg")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Search Header Bar ─────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF242424),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Songs, albums or artists", color = Color(0xFF9E9E9E), fontSize = 15.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = MusifyGreen,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (searchQuery.isNotBlank()) {
                                viewModel.submitSearch(searchQuery)
                            }
                            focusManager.clearFocus()
                        }),
                        modifier = Modifier.weight(1f)
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                        }
                    }
                }
            }
        }

        // ── Content ───────────────────────────────────────────
        if (searchQuery.isEmpty()) {
            val browseScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(browseScrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Trending Search",
                    color = MusifyGreen,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    trendingChips.forEach { chip ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF242424),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    viewModel.submitSearch(chip)
                                    focusManager.clearFocus()
                                }
                        ) {
                            Text(
                                text = chip,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Browse Categories & Moods",
                    color = MusifyGreen,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val browseCategories = listOf(
                    Triple("Pop", Brush.horizontalGradient(listOf(Color(0xFFE91E63), Color(0xFF9C27B0))), Icons.Default.MusicNote),
                    Triple("Hip-Hop", Brush.horizontalGradient(listOf(Color(0xFFFF9800), Color(0xFFFF5722))), Icons.Default.Headphones),
                    Triple("Bollywood", Brush.horizontalGradient(listOf(Color(0xFF9C27B0), Color(0xFF3F51B5))), Icons.Default.Favorite),
                    Triple("Punjabi", Brush.horizontalGradient(listOf(Color(0xFFF44336), Color(0xFFFF9800))), Icons.Default.Whatshot),
                    Triple("Chill & Lo-Fi", Brush.horizontalGradient(listOf(Color(0xFF009688), Color(0xFF3F51B5))), Icons.Default.NightsStay),
                    Triple("Workout", Brush.horizontalGradient(listOf(Color(0xFF4CAF50), Color(0xFF009688))), Icons.Default.FitnessCenter),
                    Triple("Party & Dance", Brush.horizontalGradient(listOf(Color(0xFF673AB7), Color(0xFFE91E63))), Icons.Default.Celebration),
                    Triple("Indie & Acoustic", Brush.horizontalGradient(listOf(Color(0xFF795548), Color(0xFF607D8B))), Icons.Default.GraphicEq),
                    Triple("Devotional", Brush.horizontalGradient(listOf(Color(0xFFFF6F00), Color(0xFFFFB300))), Icons.Default.SelfImprovement),
                    Triple("Rock Classics", Brush.horizontalGradient(listOf(Color(0xFF37474F), Color(0xFF212121))), Icons.Default.Album)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 90.dp)
                ) {
                    browseCategories.chunked(2).forEach { rowItems ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowItems.forEach { (catName, gradient, icon) ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(76.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            viewModel.submitSearch(catName)
                                            focusManager.clearFocus()
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(gradient)
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = catName,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.align(Alignment.TopStart)
                                        )
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .size(28.dp)
                                                .align(Alignment.BottomEnd)
                                        )
                                    }
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        } else if (!isSearchSubmitted) {
            // ── 2. Live Real-Time Autocomplete Recommendations (Google & YouTube style) ──
            val suggestionsToDisplay = if (searchSuggestions.isNotEmpty()) {
                searchSuggestions
            } else {
                listOf(
                    searchQuery,
                    "$searchQuery songs",
                    "$searchQuery song",
                    "$searchQuery music",
                    "$searchQuery official",
                    "$searchQuery remix"
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(suggestionsToDisplay) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.submitSearch(item)
                                focusManager.clearFocus()
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(18.dp))
                            Text(
                                text = item,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { viewModel.onSearchQueryChanged(item) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.NorthWest,
                                contentDescription = "Fill query",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // ── 3. Categorized Search Results ───────────────────
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MusifyGreen)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 140.dp, top = 8.dp)
                ) {
                    // ── Songs ──────────────────────────────────
                    item {
                        Text(
                            text = "Songs",
                            color = MusifyGreen,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }

                    val displayedSongs = if (showAllSongs) searchResults else searchResults.take(5)
                    itemsIndexed(displayedSongs) { index, track ->
                        SearchSongRow(
                            track = track,
                            isCurrent = currentTrack?.audioUrl == track.audioUrl,
                            isPlaying = isPlaying && currentTrack?.audioUrl == track.audioUrl,
                            viewModel = viewModel,
                            onPlay = { viewModel.playTrackFromSearch(track, searchResults) }
                        )
                    }

                    if (!showAllSongs && searchResults.size > 5) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .clickable { showAllSongs = true },
                                    color = Color(0xFF0F2417),
                                    border = BorderStroke(1.dp, MusifyGreen.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("View All", color = MusifyGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }

                    // ── Artists ────────────────────────────────
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Artists",
                            color = MusifyGreen,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(searchArtists) { (name, _, img) ->
                                val followers by viewModel.getArtistFollowersFormatted(name).collectAsState(initial = "1.2M")
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(130.dp)
                                        .clickable { viewModel.searchMusic(name) }
                                ) {
                                    com.gaminghub.musicplayer.ui.components.ArtistImage(
                                        model = img,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(110.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = name,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$followers Followers",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // ── Playlists ──────────────────────────────
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Playlists",
                            color = MusifyGreen,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(searchPlaylists) { card ->
                                CuratedSquareCard(card = card, onClick = {
                                    if (navController != null) {
                                        val encTitle = android.net.Uri.encode(card.title)
                                        val encSub = android.net.Uri.encode(card.subtitle.ifBlank { "Playlist" })
                                        val encQuery = android.net.Uri.encode(card.query.ifBlank { card.title })
                                        val encImage = android.net.Uri.encode(card.imageUrl?.toString() ?: "")
                                        navController.navigate("curated_playlist/$encTitle/$encSub/$encQuery?imageUrl=$encImage")
                                    } else {
                                        viewModel.searchMusic(card.title)
                                    }
                                })
                            }
                        }
                    }

                    // ── Albums ─────────────────────────────────
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Albums",
                            color = MusifyGreen,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(searchAlbums) { card ->
                                CuratedSquareCard(card = card, onClick = {
                                    if (navController != null) {
                                        val encTitle = android.net.Uri.encode(card.title)
                                        val encSub = android.net.Uri.encode(card.subtitle.ifBlank { "Album" })
                                        val encQuery = android.net.Uri.encode(card.query.ifBlank { card.title })
                                        val encImage = android.net.Uri.encode(card.imageUrl?.toString() ?: "")
                                        navController.navigate("curated_playlist/$encTitle/$encSub/$encQuery?imageUrl=$encImage")
                                    } else {
                                        viewModel.searchMusic(card.title)
                                    }
                                })
                            }
                        }
                    }

                    // ── Videos ─────────────────────────────────
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Videos",
                            color = MusifyGreen,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(searchVideos) { (title, subtitle, img) ->
                                Column(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .clickable { viewModel.searchMusic(title) }
                                ) {
                                    com.gaminghub.musicplayer.ui.components.SongImage(
                                        model = img,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(124.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = title,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = subtitle,
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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

@Composable
fun SearchSongRow(
    track: TrackModel,
    isCurrent: Boolean,
    isPlaying: Boolean,
    viewModel: MusicViewModel,
    onPlay: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val favoriteUrls by viewModel.favoriteUrls.collectAsState()
    val isFavorite = track.audioUrl != null && favoriteUrls.contains(track.audioUrl)
    val baseLikes = remember(track.audioUrl) { viewModel.getBaseSongLikes(track) }
    val songLikes = remember(isFavorite, baseLikes) { viewModel.formatCount(if (isFavorite) baseLikes + 1 else baseLikes) }
    val downloadedUrls by viewModel.downloadedUrls.collectAsState()
    val isDownloaded = track.audioUrl != null && downloadedUrls.contains(track.audioUrl)

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
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(14.dp))

        val context = androidx.compose.ui.platform.LocalContext.current
        val authManager = remember { com.gaminghub.musicplayer.auth.AuthManager.getInstance(context) }
        val isAdmin by authManager.isAdmin.collectAsState()

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.title,
                    color = if (isCurrent) MusifyGreen else Color.White,
                    fontSize = 15.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (track.isDevpick) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = "Verified Dev's Pick",
                        tint = MusifyGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (songLikes.isNotBlank()) "Song • ${track.artist} • $songLikes likes" else "Song • ${track.artist}",
                color = Color(0xFFB3B3B3),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color(0xFF242424))
            ) {
                if (isAdmin) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (track.isDevpick) "★ Remove Dev's Pick" else "★ Set as Dev's Pick",
                                color = if (track.isDevpick) Color(0xFFFFB74D) else MusifyGreen,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Verified,
                                null,
                                tint = if (track.isDevpick) Color(0xFFFFB74D) else MusifyGreen
                            )
                        },
                        onClick = {
                            showMenu = false
                            viewModel.toggleDevPick(track, isAdmin = true)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(if (isDownloaded) "Downloaded" else "Download Song", color = MusifyGreen) },
                    leadingIcon = { Icon(if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.DownloadForOffline, null, tint = MusifyGreen) },
                    onClick = { viewModel.toggleDownload(track); showMenu = false }
                )
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
                    onClick = {
                        showMenu = false
                        viewModel.playNextTrackInQueue(track)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Queue", color = Color.White) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = Color.White) },
                    onClick = {
                        showMenu = false
                        viewModel.addToQueue(track)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Playlist", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Add, null, tint = Color.White) },
                    onClick = { showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("View Artist (${track.artist.take(15)})", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = Color.White) },
                    onClick = {
                        showMenu = false
                        viewModel.searchMusic(track.artist)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Play Radio", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Podcasts, null, tint = Color.White) },
                    onClick = {
                        showMenu = false
                        viewModel.playRadioForTrack(track)
                    }
                )
            }
        }
    }
}
