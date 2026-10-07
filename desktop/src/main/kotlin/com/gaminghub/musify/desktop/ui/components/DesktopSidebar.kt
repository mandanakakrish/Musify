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
    val googleUser by viewModel.googleUser.collectAsState()
    val isEqualizerOpen by viewModel.isEqualizerOpen.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

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
            isSelected = currentScreen == DesktopScreen.EQUALIZER || isEqualizerOpen,
            onClick = { viewModel.toggleEqualizer() }
        )

        SidebarItem(
            icon = Icons.Default.Settings,
            label = "Settings",
            isSelected = currentScreen == DesktopScreen.SETTINGS,
            onClick = { viewModel.navigateTo(DesktopScreen.SETTINGS) }
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = SurfaceElevated)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.syncWithCloud(showToast = true) },
                    modifier = Modifier.size(24.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = MusifyEmerald, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync with Android",
                            tint = MusifyEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
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

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = SurfaceElevated)
        Spacer(modifier = Modifier.height(12.dp))

        // Google Account Profile or Sign In Button
        if (googleUser != null) {
            val isDummy = googleUser!!.email == "google.user@gmail.com"
            Surface(
                color = if (isDummy) Color(0xFF2A1A1A) else SurfaceElevated,
                shape = RoundedCornerShape(10.dp),
                border = if (isDummy) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f)) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openGoogleSignIn() }
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (isDummy) {
                        Text(
                            text = "⚠ Click to link your real Android email",
                            color = Color(0xFFFF5252),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isDummy) Color(0xFF552222) else MusifyEmerald.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = googleUser!!.name.take(1).uppercase(),
                                color = if (isDummy) Color(0xFFFF8888) else MusifyEmerald,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = googleUser!!.name,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = googleUser!!.email,
                                color = if (isDummy) Color(0xFFFF8888) else TextSecondary,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = { viewModel.syncWithCloud(showToast = true) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), color = MusifyEmerald, strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync",
                                    tint = MusifyEmerald,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { viewModel.signOutGoogle() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Sign Out",
                                tint = TextTertiary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Button(
                onClick = { viewModel.openGoogleSignIn() },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1F1F1F)
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                GoogleLogoIcon(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign in with Google",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
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
