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
fun OthersSettingsScreen(navController: NavController, settingsViewModel: SettingsViewModel) {
    val context = LocalContext.current
    val appLanguage by settingsViewModel.appLanguage.collectAsState()
    val minAudioLength by settingsViewModel.minAudioLengthSeconds.collectAsState()
    val liveSearch by settingsViewModel.liveSearch.collectAsState()
    val streamDownloaded by settingsViewModel.streamDownloaded.collectAsState()
    val searchLocalLyrics by settingsViewModel.searchLocalLyrics.collectAsState()
    val supportEqualizer by settingsViewModel.supportEqualizer.collectAsState()
    val stopOnClose by settingsViewModel.stopOnClose.collectAsState()
    val useProxy by settingsViewModel.useProxy.collectAsState()
    val proxyAddress by settingsViewModel.proxyAddress.collectAsState()
    val cacheSizeMb by settingsViewModel.cacheSizeMb.collectAsState()

    var showLanguageDropdown by remember { mutableStateOf(false) }
    var showMinLengthDialog by remember { mutableStateOf(false) }
    var showProxyDialog by remember { mutableStateOf(false) }
    var proxyInput by remember(proxyAddress) { mutableStateOf(proxyAddress) }
    var showFoldersDialog by remember { mutableStateOf(false) }

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
                text = "Others",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 1. Language ─────────────────────────────────────────
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLanguageDropdown = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Language", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Normal)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("App Text Language", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(appLanguage, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("▼", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 10.sp)
                }
            }

            DropdownMenu(
                expanded = showLanguageDropdown,
                onDismissRequest = { showLanguageDropdown = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                listOf("English", "Hindi", "Spanish", "French", "German").forEach { lang ->
                    val isSelected = appLanguage == lang
                    DropdownMenuItem(
                        text = {
                            Text(
                                lang,
                                color = if (isSelected) MusifyGreen else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            settingsViewModel.setAppLanguage(lang)
                            showLanguageDropdown = false
                        }
                    )
                }
            }
        }

        // ── 2. Include/Exclude Folders ──────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showFoldersDialog = true }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text("Include/Exclude Folders", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Normal)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Filter ringtones, recordings or notification sounds from 'My Music'", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 13.sp)
        }

        // ── 3. Min Audio Length to search music ─────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showMinLengthDialog = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Min Audio Length to search music", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Normal)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Audios shorter than this won't show in 'My Music'", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 13.sp)
            }
            Text("$minAudioLength sec", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
        }

        // ── 4. Live Search ──────────────────────────────────────
        ThemeSwitchRow(
            title = "Live Search",
            subtitle = "Search songs automatically as soon as you stop typing",
            checked = liveSearch,
            onCheckedChange = { settingsViewModel.setLiveSearch(it) }
        )

        // ── 5. Stream Downloaded Songs, If available ────────────
        ThemeSwitchRow(
            title = "Stream Downloaded Songs, If available",
            subtitle = "Plays offline local file when downloaded, instead of consuming online data",
            checked = streamDownloaded,
            onCheckedChange = { settingsViewModel.setStreamDownloaded(it) }
        )

        // ── 6. Search lyrics of local songs ─────────────────────
        ThemeSwitchRow(
            title = "Search lyrics of local songs",
            subtitle = "Search online if lyrics aren't embedded in local offline audio files",
            checked = searchLocalLyrics,
            onCheckedChange = { settingsViewModel.setSearchLocalLyrics(it) }
        )

        // ── 7. Support Equalizer ────────────────────────────────
        ThemeSwitchRow(
            title = "Support Equalizer",
            subtitle = "Enable audio DSP effects (Equalizer, Bass Boost, 3D Surround)",
            checked = supportEqualizer,
            onCheckedChange = { settingsViewModel.setSupportEqualizer(it) }
        )

        // ── 8. Stop music on App Close ──────────────────────────
        ThemeSwitchRow(
            title = "Stop music on App Close",
            subtitle = "Stop playback when application is closed from Recents/Task Manager",
            checked = stopOnClose,
            onCheckedChange = { settingsViewModel.setStopOnClose(it) }
        )

        // ── 9. Use Proxy ────────────────────────────────────────
        ThemeSwitchRow(
            title = "Use Proxy",
            subtitle = "Route search and streaming requests through custom proxy server",
            checked = useProxy,
            onCheckedChange = { settingsViewModel.setUseProxy(it) }
        )

        // ── 10. Proxy Settings ──────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showProxyDialog = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Proxy Settings", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Normal)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Change Proxy IP and Port", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 13.sp)
            }
            Text(proxyAddress, color = MusifyGreen, fontSize = 13.sp)
        }

        // ── 11. Clear Cached Details ────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { settingsViewModel.clearCache() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Clear Cached Details", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Normal)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Deletes cached album arts, stream data & temporary files",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(cacheSizeMb, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    // ── Dialog: Min Audio Length ─────────────────────────────────
    if (showMinLengthDialog) {
        val options = listOf(10, 20, 30, 60, 120)
        Dialog(onDismissRequest = { showMinLengthDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1E28),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Min Audio Length", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    options.forEach { sec ->
                        val isSelected = minAudioLength == sec
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsViewModel.setMinAudioLengthSeconds(sec)
                                    showMinLengthDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    settingsViewModel.setMinAudioLengthSeconds(sec)
                                    showMinLengthDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MusifyGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$sec seconds", color = if (isSelected) MusifyGreen else Color.White, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }

    // ── Dialog: Proxy Settings ───────────────────────────────────
    if (showProxyDialog) {
        Dialog(onDismissRequest = { showProxyDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1E28),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Proxy Configuration", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    TextField(
                        value = proxyInput,
                        onValueChange = { proxyInput = it },
                        placeholder = { Text("host:port (e.g. 103.47.67.134:8080)", color = Color.Gray) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2B2B38),
                            unfocusedContainerColor = Color(0xFF2B2B38),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = MusifyGreen
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showProxyDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (proxyInput.isNotBlank()) {
                                    settingsViewModel.setProxyAddress(proxyInput.trim())
                                    Toast.makeText(context, "Proxy updated: ${proxyInput.trim()}", Toast.LENGTH_SHORT).show()
                                }
                                showProxyDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // ── Dialog: Include / Exclude Folders ────────────────────────
    if (showFoldersDialog) {
        Dialog(onDismissRequest = { showFoldersDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1E28),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Folder Filter", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Excluded standard audio directories:\n• /Ringtones/\n• /Notifications/\n• /WhatsApp/Media/WhatsApp Audio/\n• /Recordings/",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showFoldersDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
