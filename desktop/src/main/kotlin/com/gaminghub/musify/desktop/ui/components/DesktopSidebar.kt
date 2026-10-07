package com.gaminghub.musify.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaminghub.musify.desktop.model.DesktopScreen
import com.gaminghub.musify.desktop.ui.theme.*
import com.gaminghub.musify.desktop.viewmodel.DesktopMusicViewModel

@Composable
fun DesktopSidebar(viewModel: DesktopMusicViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    Column(
        modifier = Modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(SurfaceDark)
            .padding(16.dp)
    ) {
        // App Logo & Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp, start = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(MusifyGreen, MusifyEmerald))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Logo",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Musify",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Windows Edition",
                    color = MusifyEmerald,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Navigation Items
        SidebarItem(
            icon = Icons.Default.Home,
            label = "Home",
            isSelected = currentScreen == DesktopScreen.HOME,
            onClick = { viewModel.navigateTo(DesktopScreen.HOME) }
        )

        SidebarItem(
            icon = Icons.Default.Search,
            label = "Search",
            isSelected = currentScreen == DesktopScreen.SEARCH,
            onClick = { viewModel.navigateTo(DesktopScreen.SEARCH) }
        )

        SidebarItem(
            icon = Icons.Default.LibraryMusic,
            label = "Your Library",
            isSelected = currentScreen == DesktopScreen.LIBRARY,
            onClick = { viewModel.navigateTo(DesktopScreen.LIBRARY) }
        )

        SidebarItem(
            icon = Icons.Default.AutoAwesome,
            label = "Dev's Picks",
            isSelected = currentScreen == DesktopScreen.DEV_PICKS,
            onClick = { viewModel.navigateTo(DesktopScreen.DEV_PICKS) }
        )

        SidebarItem(
            icon = Icons.Default.Download,
            label = "Downloads",
            isSelected = currentScreen == DesktopScreen.DOWNLOADS,
            onClick = { viewModel.navigateTo(DesktopScreen.DOWNLOADS) }
        )

        SidebarItem(
            icon = Icons.Default.GraphicEq,
            label = "Equalizer",
            isSelected = currentScreen == DesktopScreen.EQUALIZER,
            onClick = { viewModel.navigateTo(DesktopScreen.EQUALIZER) }
        )

        SidebarItem(
            icon = Icons.Default.Settings,
            label = "Settings",
            isSelected = currentScreen == DesktopScreen.SETTINGS,
            onClick = { viewModel.navigateTo(DesktopScreen.SETTINGS) }
        )

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = SurfaceElevated)
        Spacer(modifier = Modifier.height(16.dp))

        // Playlists Section
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PLAYLISTS",
                color = TextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            IconButton(
                onClick = { viewModel.createPlaylist("New Playlist #${playlists.size + 1}") },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Playlist",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(playlists) { playlist ->
                Text(
                    text = playlist.title,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { viewModel.openPlaylist(playlist) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun SidebarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) SurfaceElevated else Color.Transparent
    val tint = if (isSelected) MusifyEmerald else TextSecondary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
