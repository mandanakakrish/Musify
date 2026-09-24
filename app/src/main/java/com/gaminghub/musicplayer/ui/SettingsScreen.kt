package com.gaminghub.musicplayer.ui

import androidx.compose.foundation.BorderStroke
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
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
        SettingsItemData(if (isAdmin) "Admin Mode (Firebase Verified ★)" else "Firebase Admin Access", if (isAdmin) "Verified via Firebase. Tap to manage privileges" else "Permissions managed in Firebase Firestore", Icons.Outlined.AdminPanelSettings, "admin_dialog"),
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
            .background(MaterialTheme.colorScheme.background)
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
                text = "Settings",
                color = MaterialTheme.colorScheme.onBackground,
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
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
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
                        tint = if (item.route == "admin_dialog" && isAdmin) MusifyGreen else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = if (item.route == "admin_dialog" && isAdmin) MusifyGreen else MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.subtitle,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
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
        val userEmail = googleEmail ?: currentUser?.email ?: ""
        val clipboardManager = LocalClipboardManager.current
        var isCheckingFirebase by remember { mutableStateOf(false) }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showAdminPasscodeDialog = false }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.AdminPanelSettings,
                            contentDescription = null,
                            tint = if (isAdmin) MusifyGreen else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isAdmin) "Firebase Admin Active ★" else "Firebase Admin Access",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAdmin) MusifyGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (isAdmin) "✓ Verified in Firebase Firestore" else "Permission Controlled in Firebase",
                            color = if (isAdmin) MusifyGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isAdmin)
                            "Your account is authorized in Firebase Firestore. You have permission to toggle Dev's Pick and sync cloud tracks."
                            else "Admin access is strictly controlled from Firebase. The owner grants permission by adding your email or UID to the 'admins' collection in Firebase Firestore with active = true.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (userEmail.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Signed in account:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(userEmail, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(userEmail))
                                        Toast.makeText(context, "Email copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Email", tint = MusifyGreen, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    } else {
                        Text("Not signed in. Please sign in via Google first.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }

                    if (!isAdmin) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Optional Dynamic Passkey (from Firebase):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        TextField(
                            value = adminPasscodeInput,
                            onValueChange = { adminPasscodeInput = it },
                            placeholder = { Text("Enter Passkey...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MusifyGreen,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAdminPasscodeDialog = false }) {
                            Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row {
                            if (isAdmin) {
                                Button(
                                    onClick = {
                                        authViewModel.revokeAdminAccess()
                                        showAdminPasscodeDialog = false
                                        Toast.makeText(context, "Admin access revoked", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Revoke", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (adminPasscodeInput.isNotBlank()) {
                                            isCheckingFirebase = true
                                            authViewModel.verifyAdminPasscode(adminPasscodeInput) { ok, msg ->
                                                isCheckingFirebase = false
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                if (ok) {
                                                    showAdminPasscodeDialog = false
                                                    adminPasscodeInput = ""
                                                }
                                            }
                                        } else {
                                            isCheckingFirebase = true
                                            authViewModel.checkAdminStatus { ok ->
                                                isCheckingFirebase = false
                                                val msg = if (ok) "Admin access verified via Firebase!" else "No admin permissions found in Firebase for this account."
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = !isCheckingFirebase
                                ) {
                                    Text(
                                        text = if (isCheckingFirebase) "Checking..." else if (adminPasscodeInput.isNotBlank()) "Verify Key" else "Check Firebase",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
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
