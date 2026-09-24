package com.gaminghub.musicplayer.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.gaminghub.musicplayer.MusicViewModel
import com.gaminghub.musicplayer.ui.theme.MusifyGreen

@OptIn(UnstableApi::class)
@Composable
fun LibraryScreen(viewModel: MusicViewModel, navController: NavController, onMenuClick: () -> Unit) {
    val group1 = listOf(
        LibraryItemData("Discover Artists", Icons.Default.Explore, "discover_artists"),
        LibraryItemData("Following", Icons.Default.Group, "subscriptions"),
        LibraryItemData("Now Playing", Icons.AutoMirrored.Filled.QueueMusic, "now_playing"),
        LibraryItemData("Last Session", Icons.Default.History, "last_session"),
        LibraryItemData("Listening Stats", Icons.Default.AutoGraph, "stats")
    )

    val group2 = listOf(
        LibraryItemData("Favorites", Icons.Default.Favorite, "favorites"),
        LibraryItemData("My Music", Icons.Default.Folder, "my_music"),
        LibraryItemData("Playlists", Icons.AutoMirrored.Filled.PlaylistPlay, "playlists"),
        LibraryItemData("Downloads", Icons.Default.DownloadForOffline, "downloads")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
            }
            Text(
                text = "Library",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { navController.navigate("search") }) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LibraryGroupCard(items = group1, onNavigate = { route -> navController.navigate(route) })
            }
            item {
                LibraryGroupCard(items = group2, onNavigate = { route -> navController.navigate(route) })
            }
        }
    }
}

data class LibraryItemData(val title: String, val icon: ImageVector, val route: String)

@Composable
fun LibraryGroupCard(items: List<LibraryItemData>, onNavigate: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF181818)
    ) {
        Column {
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(item.route) }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF282828)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = Color(0xFF757575),
                        modifier = Modifier.size(13.dp)
                    )
                }

                if (index < items.size - 1) {
                    HorizontalDivider(
                        color = Color(0x1AFFFFFF),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 68.dp)
                    )
                }
            }
        }
    }
}
