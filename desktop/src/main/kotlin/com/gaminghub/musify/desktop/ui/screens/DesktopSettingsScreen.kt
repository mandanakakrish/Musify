package com.gaminghub.musify.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaminghub.musify.desktop.model.StreamingQuality
import com.gaminghub.musify.desktop.ui.theme.*
import com.gaminghub.musify.desktop.viewmodel.DesktopMusicViewModel

@Composable
fun DesktopSettingsScreen(viewModel: DesktopMusicViewModel) {
    val quality by viewModel.streamingQuality.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text("Settings & Preferences", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Configure playback, quality and desktop controls", color = TextSecondary, fontSize = 13.sp)
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

                    ShortcutRow("Space", "Play / Pause playback")
                    ShortcutRow("Right Arrow / Ctrl + Right", "Seek forward 10s / Next track")
                    ShortcutRow("Left Arrow / Ctrl + Left", "Seek backward 10s / Previous track")
                    ShortcutRow("Up Arrow / Down Arrow", "Volume Up / Volume Down (5%)")
                    ShortcutRow("M", "Mute / Unmute audio")
                    ShortcutRow("L", "Toggle Synced Lyrics overlay")
                    ShortcutRow("Q", "Toggle Play Queue drawer")
                    ShortcutRow("E", "Toggle 10-Band Graphic Equalizer")
                    ShortcutRow("P", "Toggle Picture-in-Picture Mini-Player")
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
