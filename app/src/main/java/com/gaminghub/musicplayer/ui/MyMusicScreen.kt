package com.gaminghub.musicplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.gaminghub.musicplayer.ui.theme.MusifyGreen
import com.gaminghub.musicplayer.MusicViewModel
import com.gaminghub.musicplayer.TrackModel

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyMusicScreen(viewModel: MusicViewModel, navController: NavController, playlistName: String? = null) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Songs", "Artists", "Genres", "Folders")

    // Use LOCAL device tracks for all data
    val allTracks by viewModel.localTracks.collectAsState()
    val artists by viewModel.localArtists.collectAsState()
    val genres by viewModel.localGenres.collectAsState()
    val folders by viewModel.localFolders.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf("Date Added") }
    var sortOrder by remember { mutableStateOf("Decreasing") }

    // Selected tag filters
    var selectedArtistTag by remember { mutableStateOf<String?>(null) }
    var selectedGenreTag by remember { mutableStateOf<String?>(null) }

    // Sorted songs list
    val sortedTracks = remember(allTracks, sortOption, sortOrder) {
        val base = when (sortOption) {
            "Display Name" -> allTracks.sortedBy { it.title }
            "Album" -> allTracks.sortedBy { it.album ?: "" }
            "Artist" -> allTracks.sortedBy { it.artist }
            else -> allTracks
        }
        if (sortOrder == "Decreasing") base.reversed() else base
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ── Top Bar ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                playlistName ?: "My Music",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Row {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground)
                }
                if (selectedTab == 0) {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(Color(0xFF2B2B2B))
                        ) {
                            listOf("Display Name", "Date Added", "Album", "Artist").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, color = Color.White) },
                                    onClick = { sortOption = option; showSortMenu = false },
                                    trailingIcon = {
                                        if (sortOption == option) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                    }
                                )
                            }
                            HorizontalDivider(color = Color.Gray, thickness = 0.5.dp)
                            listOf("Increasing", "Decreasing").forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order, color = Color.White) },
                                    onClick = { sortOrder = order; showSortMenu = false },
                                    trailingIcon = {
                                        if (sortOrder == order) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Tabs ─────────────────────────────────────────────
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MusifyGreen,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MusifyGreen
                    )
                }
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        selectedTab = index
                        selectedArtistTag = null
                        selectedGenreTag = null
                    },
                    text = {
                        Text(
                            title,
                            color = if (selectedTab == index) Color(0xFF1DB954) else Color.Gray,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        // ── Tab Content ───────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {

                // ── Songs Tab ────────────────────────────────
                0 -> {
                    Column {
                        if (sortedTracks.isEmpty()) {
                            SpotifyEmptyState("Download Something")
                        } else {
                            MyMusicSubHeader(
                                count = sortedTracks.size,
                                onShuffle = { viewModel.playTrack(sortedTracks.shuffled().first(), sortedTracks) },
                                onPlayAll = { viewModel.playTrack(sortedTracks.first(), sortedTracks) }
                            )
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 140.dp)
                            ) {
                                itemsIndexed(sortedTracks, key = { index, track -> "${track.audioUrl ?: track.title}_$index" }) { _, track ->
                                    val isCurrent = currentTrack?.audioUrl == track.audioUrl || 
                                        (currentTrack != null && currentTrack?.title == track.title && currentTrack?.artist == track.artist)
                                    PlaylistSongRow(
                                        track = track,
                                        isCurrent = isCurrent,
                                        isPlaying = isPlaying && isCurrent,
                                        viewModel = viewModel,
                                        onPlay = { viewModel.playTrack(track, sortedTracks) }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Artists Tab ───────────────────────────────
                1 -> {
                    Column {
                        if (artists.isEmpty()) {
                            SpotifyEmptyState("Download Something")
                        } else {
                            // Artist tag chips
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                artists.keys.sorted().forEach { artistName ->
                                    val isSelected = selectedArtistTag == artistName
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isSelected) Color.White else Color(0xFF2B2B2B),
                                        modifier = Modifier.clickable {
                                            selectedArtistTag = if (isSelected) null else artistName
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.Black else Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                artistName,
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = Color(0xFF2B2B2B), thickness = 0.5.dp)
                            // List: filtered or all
                            val filteredArtists = if (selectedArtistTag != null)
                                artists.filter { it.key == selectedArtistTag }
                            else artists
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                                contentPadding = PaddingValues(bottom = 140.dp)
                            ) {
                                items(filteredArtists.toList()) { (name, tracks) ->
                                    CategoryListItem(
                                        title = name,
                                        subtitle = "${tracks.size} Songs",
                                        artUrl = tracks.firstOrNull()?.albumArtUrl
                                    ) { viewModel.playTrack(tracks.first(), tracks) }
                                }
                            }
                        }
                    }
                }

                // ── Genres Tab ────────────────────────────────
                2 -> {
                    Column {
                        if (genres.isEmpty()) {
                            SpotifyEmptyState("Download Something")
                        } else {
                            // Genre tag chips
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                genres.keys.sorted().forEach { genreName ->
                                    val isSelected = selectedGenreTag == genreName
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isSelected) Color(0xFF1DB954) else Color(0xFF2B2B2B),
                                        modifier = Modifier.clickable {
                                            selectedGenreTag = if (isSelected) null else genreName
                                        }
                                    ) {
                                        Text(
                                            genreName,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = Color(0xFF2B2B2B), thickness = 0.5.dp)
                            // List: filtered or all
                            val filteredGenres = if (selectedGenreTag != null)
                                genres.filter { it.key == selectedGenreTag }
                            else genres
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                                contentPadding = PaddingValues(bottom = 140.dp)
                            ) {
                                items(filteredGenres.toList()) { (name, tracks) ->
                                    CategoryListItem(
                                        title = name,
                                        subtitle = "${tracks.size} Songs",
                                        artUrl = tracks.firstOrNull()?.albumArtUrl
                                    ) { viewModel.playTrack(tracks.first(), tracks) }
                                }
                            }
                        }
                    }
                }

                // ── Folders Tab ───────────────────────────────
                3 -> {
                    if (folders.isEmpty()) {
                        SpotifyEmptyState("Download Something")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                            contentPadding = PaddingValues(bottom = 140.dp)
                        ) {
                            items(folders.toList()) { (name, tracks) ->
                                val folderName = name.substringAfterLast("/")
                                CategoryListItem(
                                    title = folderName,
                                    subtitle = "${tracks.size} Songs",
                                    artUrl = null
                                ) { viewModel.playTrack(tracks.first(), tracks) }
                            }
                        }
                    }
                }
            }

            // Shuffle FAB (non-Songs tabs)
            if (selectedTab != 0) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomEnd) {
                    Surface(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .clickable {
                                val list = when (selectedTab) {
                                    1 -> (if (selectedArtistTag != null) artists[selectedArtistTag] else artists.values.flatten()) ?: emptyList()
                                    2 -> (if (selectedGenreTag != null) genres[selectedGenreTag] else genres.values.flatten()) ?: emptyList()
                                    3 -> folders.values.flatten()
                                    else -> emptyList()
                                }
                                if (list.isNotEmpty()) viewModel.playTrack(list.shuffled().first(), list)
                            },
                        color = Color(0xFF1A1A1A),
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpotifyEmptyState(actionText: String = "Download Something") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color(0xFF1DB954).copy(alpha = 0.12f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Nothing to Show Here",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = actionText,
                color = Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun MyMusicSubHeader(count: Int, onShuffle: () -> Unit, onPlayAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$count Songs", color = Color.Gray, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = onShuffle,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = onPlayAll) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play All", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun CategoryListItem(title: String, subtitle: String, artUrl: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.gaminghub.musicplayer.ui.components.SongImage(
            model = artUrl,
            contentDescription = null,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Color.Gray, fontSize = 13.sp)
        }
    }
}

// Unused but kept for tag display reference
@Composable
private fun TagChip(label: String, isSelected: Boolean, color: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) color else Color(0xFF2B2B2B),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            label,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
