package com.gaminghub.musicplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.gaminghub.musicplayer.SettingsViewModel
import com.gaminghub.musicplayer.ui.theme.MusifyGreen

@Composable
fun MusicPlaybackSettingsScreen(navController: NavController, settingsViewModel: SettingsViewModel) {
    val musicLanguage by settingsViewModel.musicLanguage.collectAsState()
    val spotifyLocation by settingsViewModel.spotifyLocalChartsLocation.collectAsState()
    val streamingQuality by settingsViewModel.streamingQuality.collectAsState()
    val wifiStreamingQuality by settingsViewModel.wifiStreamingQuality.collectAsState()
    val youtubeQuality by settingsViewModel.youtubeQuality.collectAsState()
    val loadLastSession by settingsViewModel.loadLastSession.collectAsState()
    val replayOnSkipPrevious by settingsViewModel.replayOnSkipPrevious.collectAsState()
    val enforceRepeating by settingsViewModel.enforceRepeating.collectAsState()
    val autoplay by settingsViewModel.autoplay.collectAsState()
    val cacheSongs by settingsViewModel.cacheSongs.collectAsState()

    var showMusicLangDropdown by remember { mutableStateOf(false) }
    var showCountryDropdown by remember { mutableStateOf(false) }
    var showQualityDropdown by remember { mutableStateOf(false) }
    var showWifiQualityDropdown by remember { mutableStateOf(false) }
    var showYoutubeQualityDropdown by remember { mutableStateOf(false) }

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
                text = "Music & Playback",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 1. Music Language ───────────────────────────────────
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showMusicLangDropdown = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Music Language", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Normal)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("To display songs and recommendations on Home Screen", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(musicLanguage, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("▼", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 10.sp)
                }
            }

            DropdownMenu(
                expanded = showMusicLangDropdown,
                onDismissRequest = { showMusicLangDropdown = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                listOf("All / Global", "English", "Hindi", "Punjabi", "Tamil", "Telugu", "Spanish").forEach { lang ->
                    val isSelected = musicLanguage == lang
                    DropdownMenuItem(
                        text = {
                            Text(
                                lang,
                                color = if (isSelected) MusifyGreen else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            settingsViewModel.setMusicLanguage(lang)
                            showMusicLangDropdown = false
                        }
                    )
                }
            }
        }

        // ── 2. Spotify Local Charts Location ────────────────────
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCountryDropdown = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Spotify Local Charts Location", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Normal)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Country for Top Spotify Local Charts", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(spotifyLocation, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("▼", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 10.sp)
                }
            }

            DropdownMenu(
                expanded = showCountryDropdown,
                onDismissRequest = { showCountryDropdown = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                listOf("India", "United States", "United Kingdom", "Canada", "Australia", "Global").forEach { country ->
                    val isSelected = spotifyLocation == country
                    DropdownMenuItem(
                        text = {
                            Text(
                                country,
                                color = if (isSelected) MusifyGreen else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            settingsViewModel.setSpotifyLocalChartsLocation(country)
                            showCountryDropdown = false
                        }
                    )
                }
            }
        }

        // ── 3. Streaming Quality ────────────────────────────────
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showQualityDropdown = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Streaming Quality (Mobile)", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Normal)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Higher quality uses more cellular data", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(streamingQuality, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("▼", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 10.sp)
                }
            }

            DropdownMenu(
                expanded = showQualityDropdown,
                onDismissRequest = { showQualityDropdown = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                listOf("96 kbps (Low)", "160 kbps (High)", "320 kbps (Ultra)").forEach { q ->
                    val cleanQ = q.substringBefore(" ")
                    val isSelected = streamingQuality.startsWith(cleanQ)
                    DropdownMenuItem(
                        text = {
                            Text(
                                q,
                                color = if (isSelected) MusifyGreen else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            settingsViewModel.setStreamingQuality(cleanQ)
                            showQualityDropdown = false
                        }
                    )
                }
            }
        }

        // ── 4. Streaming Quality (Wifi) ─────────────────────────
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showWifiQualityDropdown = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Streaming Quality (Wifi)", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Normal)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Used automatically whenever connected to Wi-Fi", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(wifiStreamingQuality, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("▼", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 10.sp)
                }
            }

            DropdownMenu(
                expanded = showWifiQualityDropdown,
                onDismissRequest = { showWifiQualityDropdown = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                listOf("96 kbps (Low)", "160 kbps (High)", "320 kbps (Ultra)").forEach { q ->
                    val cleanQ = q.substringBefore(" ")
                    val isSelected = wifiStreamingQuality.startsWith(cleanQ)
                    DropdownMenuItem(
                        text = {
                            Text(
                                q,
                                color = if (isSelected) MusifyGreen else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            settingsViewModel.setWifiStreamingQuality(cleanQ)
                            showWifiQualityDropdown = false
                        }
                    )
                }
            }
        }

        // ── 5. YouTube Streaming Quality ────────────────────────
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showYoutubeQualityDropdown = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("YouTube Streaming Quality", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Normal)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Stream resolution and bitrate selection", color = Color.Gray, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(youtubeQuality, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("▼", color = Color.Gray, fontSize = 10.sp)
                }
            }

            DropdownMenu(
                expanded = showYoutubeQualityDropdown,
                onDismissRequest = { showYoutubeQualityDropdown = false },
                modifier = Modifier.background(Color(0xFF242424))
            ) {
                listOf("Auto (High)", "High", "Low").forEach { q ->
                    val isSelected = youtubeQuality == q
                    DropdownMenuItem(
                        text = {
                            Text(
                                q,
                                color = if (isSelected) MusifyGreen else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            settingsViewModel.setYoutubeQuality(q)
                            showYoutubeQualityDropdown = false
                        }
                    )
                }
            }
        }

        // ── 6. Load Last Session on App Start ───────────────────
        ThemeSwitchRow(
            title = "Load Last Session on App Start",
            subtitle = "Automatically load last queue when app starts",
            checked = loadLastSession,
            onCheckedChange = { settingsViewModel.setLoadLastSession(it) }
        )

        // ── 7. Replay on Skip Previous ──────────────────────────
        ThemeSwitchRow(
            title = "Replay on Skip Previous",
            subtitle = "Replay from start instead of skipping to previous song",
            checked = replayOnSkipPrevious,
            onCheckedChange = { settingsViewModel.setReplayOnSkipPrevious(it) }
        )

        // ── 8. Enforce Repeating ────────────────────────────────
        ThemeSwitchRow(
            title = "Enforce Repeating",
            subtitle = "Keep the same repeat option for every session",
            checked = enforceRepeating,
            onCheckedChange = { settingsViewModel.setEnforceRepeating(it) }
        )

        // ── 9. Autoplay ─────────────────────────────────────────
        ThemeSwitchRow(
            title = "Autoplay",
            subtitle = "Automatically add related songs to the queue when current queue finishes",
            checked = autoplay,
            onCheckedChange = { settingsViewModel.setAutoplay(it) }
        )

        // ── 10. Cache Songs ─────────────────────────────────────
        ThemeSwitchRow(
            title = "Cache Songs",
            subtitle = "Songs will be cached for instant future playback (Uses device storage)",
            checked = cacheSongs,
            onCheckedChange = { settingsViewModel.setCacheSongs(it) }
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
}
