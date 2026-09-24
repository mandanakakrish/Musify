package com.gaminghub.musicplayer.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.gaminghub.musicplayer.SettingsViewModel
import com.gaminghub.musicplayer.ui.theme.MusifyGreen

@Composable
fun AppUISettingsScreen(navController: NavController, settingsViewModel: SettingsViewModel) {
    val context = LocalContext.current
    val playerBackground by settingsViewModel.playerBackground.collectAsState()
    val useDenseMiniplayer by settingsViewModel.useDenseMiniplayer.collectAsState()
    val showMiniPlayerDownload by settingsViewModel.showMiniPlayerDownload.collectAsState()
    val showMiniPlayerFavorite by settingsViewModel.showMiniPlayerFavorite.collectAsState()
    val showPlaylistsOnHome by settingsViewModel.showPlaylistsOnHome.collectAsState()
    val showLastSession by settingsViewModel.showLastSession.collectAsState()
    val enableArtworkGestures by settingsViewModel.enableArtworkGestures.collectAsState()
    val enableVolumeGestures by settingsViewModel.enableVolumeGestures.collectAsState()
    val useLessData by settingsViewModel.useLessData.collectAsState()

    var showPlayerBgDialog by remember { mutableStateOf(false) }
    var showMiniPlayerButtonsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Top Header ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }

            Text(
                text = "App UI",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 1. Player Screen Background ─────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPlayerBgDialog = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Player Screen Background", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Normal)
                Spacer(modifier = Modifier.height(2.dp))
                Text(playerBackground, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 13.sp)
            }
            Text("▼", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 10.sp)
        }

        // ── 2. Use Dense Miniplayer ─────────────────────────────
        ThemeSwitchRow(
            title = "Use Dense Miniplayer",
            subtitle = "Miniplayer height and padding will be compact",
            checked = useDenseMiniplayer,
            onCheckedChange = { settingsViewModel.setUseDenseMiniplayer(it) }
        )

        // ── 3. Buttons to show in Mini Player ───────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showMiniPlayerButtonsDialog = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Buttons to show in Mini Player", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Normal)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    listOfNotNull(
                        if (showMiniPlayerDownload) "Download" else null,
                        if (showMiniPlayerFavorite) "Favorite" else null,
                        "Play/Pause",
                        "Next"
                    ).joinToString(", "),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    fontSize = 13.sp
                )
            }
            Text("Edit", color = MusifyGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        // ── 4. Compact Notification Buttons ─────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { Toast.makeText(context, "Notification buttons: Prev, Play/Pause, Next active", Toast.LENGTH_SHORT).show() }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text("Compact Notification Buttons", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Normal)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Previous, Play/Pause, Next enabled for notification controls", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 13.sp)
        }

        // ── 5. Show Playlists on Home Screen ────────────────────
        ThemeSwitchRow(
            title = "Show Playlists on Home Screen",
            subtitle = "Displays curated & community playlists on Home Screen",
            checked = showPlaylistsOnHome,
            onCheckedChange = { settingsViewModel.setShowPlaylistsOnHome(it) }
        )

        // ── 6. Show Last Session ────────────────────────────────
        ThemeSwitchRow(
            title = "Show Last Session",
            subtitle = "Show recently played tracks on Home Screen",
            checked = showLastSession,
            onCheckedChange = { settingsViewModel.setShowLastSession(it) }
        )

        // ── 7. Enable Artwork Gestures ──────────────────────────
        ThemeSwitchRow(
            title = "Enable Artwork Gestures",
            subtitle = "Enables tap to toggle lyrics, double-tap to play/pause, and swipe to skip tracks on the artwork",
            checked = enableArtworkGestures,
            onCheckedChange = { settingsViewModel.setEnableArtworkGestures(it) }
        )

        // ── 8. Enable Volume Gesture Controls ──────────────────
        ThemeSwitchRow(
            title = "Enable Volume Gesture Controls",
            subtitle = "Vertical swipe on the player artwork adjusts playback volume",
            checked = enableVolumeGestures,
            onCheckedChange = { settingsViewModel.setEnableVolumeGestures(it) }
        )

        // ── 9. Use Less Data for Images ────────────────────────
        ThemeSwitchRow(
            title = "Use Less Data for Images",
            subtitle = "Loads compressed artwork thumbnails to save mobile data",
            checked = useLessData,
            onCheckedChange = { settingsViewModel.setUseLessData(it) }
        )

        Spacer(modifier = Modifier.height(100.dp))
    }

    // ── Dialog: Player Screen Background ─────────────────────────
    if (showPlayerBgDialog) {
        val options = listOf(
            "Default (Blurred Artwork)",
            "Subtle Gradient",
            "Solid Black"
        )
        Dialog(onDismissRequest = { showPlayerBgDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1E28),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Player Screen Background", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    options.forEach { opt ->
                        val isSelected = playerBackground == opt
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsViewModel.setPlayerBackground(opt)
                                    showPlayerBgDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    settingsViewModel.setPlayerBackground(opt)
                                    showPlayerBgDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MusifyGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(opt, color = if (isSelected) MusifyGreen else Color.White, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }

    // ── Dialog: Mini Player Buttons ──────────────────────────────
    if (showMiniPlayerButtonsDialog) {
        Dialog(onDismissRequest = { showMiniPlayerButtonsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1E28),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Buttons in Mini Player", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settingsViewModel.setShowMiniPlayerDownload(!showMiniPlayerDownload) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Download Button", color = Color.White, fontSize = 15.sp)
                        Checkbox(
                            checked = showMiniPlayerDownload,
                            onCheckedChange = { settingsViewModel.setShowMiniPlayerDownload(it) },
                            colors = CheckboxDefaults.colors(checkedColor = MusifyGreen)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settingsViewModel.setShowMiniPlayerFavorite(!showMiniPlayerFavorite) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Favorite (Heart) Button", color = Color.White, fontSize = 15.sp)
                        Checkbox(
                            checked = showMiniPlayerFavorite,
                            onCheckedChange = { settingsViewModel.setShowMiniPlayerFavorite(it) },
                            colors = CheckboxDefaults.colors(checkedColor = MusifyGreen)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showMiniPlayerButtonsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
