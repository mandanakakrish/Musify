package com.gaminghub.musify.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaminghub.musify.desktop.model.StreamingQuality
import com.gaminghub.musify.desktop.ui.components.GoogleLogoIcon
import com.gaminghub.musify.desktop.ui.theme.*
import com.gaminghub.musify.desktop.viewmodel.DesktopMusicViewModel

@Composable
fun DesktopSettingsScreen(viewModel: DesktopMusicViewModel) {
    val quality by viewModel.streamingQuality.collectAsState()
    val googleUser by viewModel.googleUser.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text("Settings & Preferences", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Configure playback, quality and desktop controls", color = TextSecondary, fontSize = 13.sp)
        }

        // Google Account Section
        item {
            Surface(color = SurfaceElevated, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("GOOGLE ACCOUNT & CLOUD SYNC", color = MusifyEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    if (googleUser != null) {
                        val isDummy = googleUser!!.email.lowercase().contains("google.user")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MusifyEmerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = googleUser!!.name.take(1).uppercase(),
                                    color = MusifyEmerald,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(googleUser!!.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(googleUser!!.email, color = if (isDummy) Color(0xFFFF7777) else TextSecondary, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                if (isDummy) {
                                    Text("⚠️ Placeholder account. Switch to your Android email to sync playlists.", color = Color(0xFFFFB74D), fontSize = 11.sp)
                                } else {
                                    Text("⚡ Cross-Device Sync: Connected to Android (musify-8df01)", color = MusifyEmerald, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.syncWithCloud(showToast = true) },
                                colors = ButtonDefaults.buttonColors(containerColor = MusifyEmerald, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isSyncing
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Syncing...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync with Android", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.openGoogleSignIn() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.SwitchAccount, contentDescription = "Switch Account", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Switch Account", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }

                            Button(
                                onClick = { viewModel.signOutGoogle() },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark, contentColor = Color(0xFFFF5252)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign Out", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                Text("No Google account linked", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    "Sign in with Google to sync your playlists and liked songs across devices.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Button(
                                onClick = { viewModel.openGoogleSignIn() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1F1F1F)),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                GoogleLogoIcon(modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        // Audio Quality Section
        item {
            Surface(color = SurfaceElevated, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("AUDIO STREAMING QUALITY", color = MusifyEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    StreamingQuality.values().forEach { q ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = quality == q,
                                onClick = { viewModel.setStreamingQuality(q) },
                                colors = RadioButtonDefaults.colors(selectedColor = MusifyEmerald)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(q.label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = if (q == StreamingQuality.HIGH) "Lossless AAC/M4A 320 kbps bit rate" else "Optimized for speed and low bandwidth",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Desktop Keyboard Shortcuts Cheat Sheet
        item {
            Surface(color = SurfaceElevated, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("DESKTOP KEYBOARD SHORTCUTS", color = MusifyEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    ShortcutRow("Ctrl + Space", "Play / Pause playback")
                    ShortcutRow("Ctrl + Right Arrow", "Next track")
                    ShortcutRow("Ctrl + Left Arrow", "Previous track")
                    ShortcutRow("Ctrl + Shift + Right", "Fast-Forward 10s")
                    ShortcutRow("Ctrl + Shift + Left", "Rewind 10s")
                    ShortcutRow("Ctrl + Up / Down Arrow", "Volume Up / Down (5%)")
                    ShortcutRow("Ctrl + M", "Mute / Unmute audio")
                    ShortcutRow("Ctrl + L", "Toggle Synced Lyrics overlay")
                    ShortcutRow("Ctrl + Q", "Toggle Play Queue drawer")
                    ShortcutRow("Ctrl + E", "Toggle 10-Band Graphic Equalizer")
                    ShortcutRow("Ctrl + P", "Toggle Picture-in-Picture Mini-Player")
                }
            }
        }

        // Storage & Downloads
        item {
            Surface(color = SurfaceElevated, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("OFFLINE STORAGE", color = MusifyEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Download Location:", color = TextSecondary, fontSize = 12.sp)
                    Text(viewModel.downloadManager.downloadDir.absolutePath, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.downloadManager.openDownloadsFolder() },
                        colors = ButtonDefaults.buttonColors(containerColor = MusifyEmerald, contentColor = androidx.compose.ui.graphics.Color.Black)
                    ) {
                        Text("Open Downloads Folder in File Explorer")
                    }
                }
            }
        }

        // About App
        item {
            Surface(color = SurfaceElevated, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ABOUT MUSIFY", color = MusifyEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Musify for Windows v1.7.5.1", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Built with Compose Multiplatform & Kotlin", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("© 2026 GamingHub Musify. Free & Open Music Engine.", color = TextTertiary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ShortcutRow(key: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(description, color = TextSecondary, fontSize = 13.sp)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceHighlight)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(key, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
