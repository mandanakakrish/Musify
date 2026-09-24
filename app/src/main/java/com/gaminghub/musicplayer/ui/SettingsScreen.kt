package com.gaminghub.musicplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gaminghub.musicplayer.auth.AuthViewModel
import com.gaminghub.musicplayer.ui.theme.MusifyGreen

data class SettingsItemData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun SettingsScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val currentUser by authViewModel.currentUser.collectAsState()
    val googleEmail by authViewModel.googleEmail.collectAsState()
    val googleDisplayName by authViewModel.googleDisplayName.collectAsState()
    val googlePhotoUrl by authViewModel.googlePhotoUrl.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var showAdminPasscodeDialog by remember { mutableStateOf(false) }
    var adminPasscodeInput by remember { mutableStateOf("") }
    val isAdmin by authViewModel.isAdmin.collectAsState()

    val allSettingsItems = listOf(
        SettingsItemData("Equalizer & Sound Effects", "5-Band EQ, Bass Boost, 3D Surround Virtualizer", Icons.Outlined.GraphicEq, "equalizer"),
        SettingsItemData("Theme", "Dark Mode, Accent Color & Hue, Use System Theme", Icons.Outlined.Nightlight, "theme_settings"),
        SettingsItemData("App UI", "Player Screen Background, Buttons to show in Mini Player, Use Dense Minipla...", Icons.Outlined.ColorLens, "app_ui_settings"),
        SettingsItemData("Music & Playback", "Music Language, Streaming Quality, Spotify Local Charts Location", Icons.Outlined.MusicNote, "music_playback_settings"),
        SettingsItemData("Others", "Language, Include/Exclude Folders, Min Audio Length to search music", Icons.Outlined.Settings, "others_settings"),
        SettingsItemData("Backup & Restore", "Create Backup, Restore, Auto Backup", Icons.Outlined.History, "backup_restore_settings"),
        SettingsItemData(if (isAdmin) "Admin Mode (Unlocked ★)" else "Admin & Developer Access", if (isAdmin) "Tap to manage or revoke Admin privileges" else "Enter Admin Passkey to verify Dev's Picks", Icons.Outlined.AdminPanelSettings, "admin_dialog"),
        SettingsItemData("About", "Version, Share App, Contact Us", Icons.Outlined.Info, "about_settings")
    )

    val filteredItems = remember(searchQuery) {
        if (searchQuery.isBlank()) allSettingsItems
        else allSettingsItems.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.subtitle.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        // ── Search Bar ──────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            color = Color(0xFF242424),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search", color = Color.Gray, fontSize = 15.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MusifyGreen
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Settings List ───────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            items(filteredItems, key = { it.title }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            if (item.route == "admin_dialog") {
                                showAdminPasscodeDialog = true
                            } else {
                                navController.navigate(item.route)
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (item.route == "admin_dialog" && isAdmin) MusifyGreen else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = if (item.route == "admin_dialog" && isAdmin) MusifyGreen else Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.subtitle,
                            color = Color.Gray,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (showAdminPasscodeDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAdminPasscodeDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1E28),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (isAdmin) "Admin Mode Active" else "Enter Admin Passkey",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAdmin) 
                            "You have full admin privileges. You can toggle Dev's Pick for any song and cloud-sync verified tracks."
                            else "Enter your master admin key (e.g. ADMIN_JOY_7788) to unlock Dev's Pick management.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )

                    if (!isAdmin) {
                        Spacer(modifier = Modifier.height(16.dp))
                        TextField(
                            value = adminPasscodeInput,
                            onValueChange = { adminPasscodeInput = it },
                            placeholder = { Text("Enter Passkey...", color = Color.Gray) },
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
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAdminPasscodeDialog = false }) {
                            Text("Close", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isAdmin) {
                            Button(
                                onClick = {
                                    authViewModel.revokeAdminAccess()
                                    showAdminPasscodeDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Revoke Admin", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    val ok = authViewModel.verifyAdminPasscode(adminPasscodeInput)
                                    if (ok) {
                                        showAdminPasscodeDialog = false
                                        adminPasscodeInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Unlock", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
