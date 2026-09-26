package com.gaminghub.musicplayer.update

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gaminghub.musicplayer.ui.theme.DefaultSpotifyGreen
import com.gaminghub.musicplayer.ui.theme.DefaultSpotifyGreenDark
import com.gaminghub.musicplayer.util.NetworkMonitor

/**
 * Modern modal update dialog with Musify Spotify Green and sleek dark grey/black palette.
 * Provides in-app downloading with live progress tracking and automatic package installation.
 * Also allows users to continue playing offline music from Downloaded and My Music without blocking.
 */
@Composable
fun StrictUpdateDialog(
    updateInfo: AppUpdateInfo,
    onUpdateClick: ((String) -> Unit)? = null,
    onPlayDownloadedClick: (() -> Unit)? = null,
    onPlayMyMusicClick: (() -> Unit)? = null,
    onExitClick: () -> Unit,
    onDismissClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val downloadState by AppUpdateManager.downloadState.collectAsState()
    val isDownloading = downloadState is UpdateDownloadState.Downloading

    val networkMonitor = remember { NetworkMonitor.getInstance(context) }
    val isOnline by networkMonitor.isOnline.collectAsState()

    Dialog(
        onDismissRequest = {
            // Non-dismissible if forceUpdate is true or while downloading
            if (!updateInfo.isForceUpdate && !isDownloading) {
                onDismissClick?.invoke()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.isForceUpdate && !isDownloading,
            dismissOnClickOutside = !updateInfo.isForceUpdate && !isDownloading,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF0E0E13),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(
                    BorderStroke(
                        1.2.dp,
                        Brush.verticalGradient(
                            listOf(
                                DefaultSpotifyGreen.copy(alpha = 0.45f),
                                Color(0xFF262636),
                                Color(0xFF181822)
                            )
                        )
                    ),
                    RoundedCornerShape(26.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Top Icon Badge with Dual Ring & Ambient Glow ───────
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(DefaultSpotifyGreen.copy(alpha = 0.12f), CircleShape)
                        .border(1.dp, DefaultSpotifyGreen.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color(0xFF122017), CircleShape)
                            .border(1.2.dp, DefaultSpotifyGreen.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (!isOnline) Icons.Default.CloudOff else Icons.Default.SystemUpdate,
                            contentDescription = "Update Available",
                            tint = if (!isOnline) Color(0xFFFFB74D) else DefaultSpotifyGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Status Pill Tag ────────────────────────────────────
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            if (!isOnline) Color(0xFF241C12)
                            else if (updateInfo.isForceUpdate) Color(0xFF261214)
                            else Color(0xFF112217)
                        )
                        .border(
                            1.dp,
                            if (!isOnline) Color(0xFFFFB74D).copy(alpha = 0.45f)
                            else if (updateInfo.isForceUpdate) Color(0xFFFF5252).copy(alpha = 0.45f)
                            else DefaultSpotifyGreen.copy(alpha = 0.45f),
                            RoundedCornerShape(50.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (!isOnline) "OFFLINE MODE ACTIVE"
                        else if (updateInfo.isForceUpdate) "MANDATORY UPDATE"
                        else "NEW UPDATE AVAILABLE",
                        color = if (!isOnline) Color(0xFFFFB74D)
                        else if (updateInfo.isForceUpdate) Color(0xFFFF6B6B)
                        else DefaultSpotifyGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Title ──────────────────────────────────────────────
                Text(
                    text = if (!isOnline) "Offline? Listen to Your Music" else updateInfo.releaseTitle.ifBlank { "Musify Update Available" },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Version Comparison Badge ───────────────────────────
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF161622))
                        .border(1.dp, Color(0xFF262636), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current: ${updateInfo.currentVersion}",
                        color = Color(0xFF9898AA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "to",
                        tint = DefaultSpotifyGreen.copy(alpha = 0.8f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = updateInfo.latestVersion,
                        color = DefaultSpotifyGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DefaultSpotifyGreen)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "NEW",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Changelog / What's New ─────────────────────────────
                if (updateInfo.changelog.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 125.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF14141D))
                            .border(1.dp, Color(0xFF242434), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "What's New",
                                tint = DefaultSpotifyGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "What's New in this update",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = updateInfo.changelog,
                            color = Color(0xFFC4C4D4),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── Offline Music Section (Theme: Dark Grey with Green) ─
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (!isOnline) Color(0xFF121B15) else Color(0xFF14141E))
                        .border(
                            1.dp,
                            if (!isOnline) DefaultSpotifyGreen.copy(alpha = 0.5f) else Color(0xFF262636),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (!isOnline) Icons.Default.CloudOff else Icons.Default.MusicNote,
                            contentDescription = "Offline Music",
                            tint = if (!isOnline) Color(0xFFFFB74D) else DefaultSpotifyGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (!isOnline) "Offline Mode • Play Offline Songs" else "Play Offline Music Without Updating",
                            color = if (!isOnline) Color(0xFFFFB74D) else DefaultSpotifyGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (!isOnline) {
                            "You are offline. You can freely play songs from 'Downloaded' and 'My Music'."
                        } else {
                            "Listen right now? Play your downloaded songs or local 'My Music' files."
                        },
                        color = Color(0xFFA8A8B8),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onPlayDownloadedClick?.invoke() ?: AppUpdateManager.enterOfflineModeAndNavigate("downloads")
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = DefaultSpotifyGreen,
                                containerColor = Color(0xFF16221A)
                            ),
                            border = BorderStroke(1.dp, DefaultSpotifyGreen.copy(alpha = 0.55f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DownloadForOffline,
                                contentDescription = "Downloaded",
                                tint = DefaultSpotifyGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Downloaded", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                onPlayMyMusicClick?.invoke() ?: AppUpdateManager.enterOfflineModeAndNavigate("my_music")
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = DefaultSpotifyGreen,
                                containerColor = Color(0xFF16221A)
                            ),
                            border = BorderStroke(1.dp, DefaultSpotifyGreen.copy(alpha = 0.55f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "My Music",
                                tint = DefaultSpotifyGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("My Music", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── In-App Download / Install Actions ────────────────────
                when (val state = downloadState) {
                    is UpdateDownloadState.Idle -> {
                        Button(
                            onClick = {
                                if (!isOnline) {
                                    Toast.makeText(
                                        context,
                                        "Cannot update while offline. Connect to internet or play offline songs above.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else if (onUpdateClick != null) {
                                    onUpdateClick(updateInfo.downloadUrl)
                                } else {
                                    AppUpdateManager.startInAppDownloadAndInstall(context, updateInfo.downloadUrl)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOnline) DefaultSpotifyGreen else Color(0xFF22222E),
                                contentColor = if (isOnline) Color.Black else Color(0xFF888898)
                            ),
                            shape = RoundedCornerShape(26.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = if (isOnline) Icons.Default.Download else Icons.Default.CloudOff,
                                contentDescription = "Download",
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isOnline) "Update Now (Download & Install)" else "Connect to Internet to Download",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    is UpdateDownloadState.Downloading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF161622))
                                .border(1.dp, DefaultSpotifyGreen.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Downloading APK...",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${state.progressPercent}%",
                                    color = DefaultSpotifyGreen,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = {
                                    if (state.totalBytes > 0) state.progressPercent / 100f else 0.5f
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = DefaultSpotifyGreen,
                                trackColor = Color(0xFF262636)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${state.downloadedMb} MB / ${state.totalMb}",
                                    color = Color(0xFF9898AA),
                                    fontSize = 11.sp
                                )
                                TextButton(
                                    onClick = { AppUpdateManager.cancelOrResetDownload() },
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("Cancel", color = Color(0xFFFF5252), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    is UpdateDownloadState.Downloaded -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Downloaded",
                                    tint = DefaultSpotifyGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Download complete! Ready to install.",
                                    color = DefaultSpotifyGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = { AppUpdateManager.promptInstallApk(context, state.apkFile) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DefaultSpotifyGreen,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(26.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = "Install",
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Install Update Now",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    is UpdateDownloadState.Installing -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = DefaultSpotifyGreen,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Launching system installer...",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }

                            Button(
                                onClick = { AppUpdateManager.promptInstallApk(context, state.apkFile) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DefaultSpotifyGreen,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(26.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text(
                                    text = "Tap to Reopen Installer",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    is UpdateDownloadState.PermissionRequired -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF261914))
                                .border(1.dp, Color(0xFFFF7043).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Permission",
                                    tint = Color(0xFFFF7043),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Permission Required",
                                    color = Color(0xFFFF7043),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "To install updates, enable 'Allow from this source' for Musify in Settings.",
                                color = Color(0xFFE2E2EC),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { AppUpdateManager.requestInstallPermission(context) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF7043),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Text("Open Settings to Grant", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(
                                onClick = { AppUpdateManager.promptInstallApk(context, state.apkFile) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("I've Granted It - Install Now", color = DefaultSpotifyGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    is UpdateDownloadState.Failed -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Download Error: ${state.error}",
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            Button(
                                onClick = {
                                    AppUpdateManager.startInAppDownloadAndInstall(context, updateInfo.downloadUrl)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DefaultSpotifyGreen,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(26.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry In-App Download", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { AppUpdateManager.openDownloadUrl(context, updateInfo.downloadUrl) },
                                shape = RoundedCornerShape(26.dp),
                                border = BorderStroke(1.dp, Color(0xFF323246)),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF161622)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = "Browser", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download in Browser", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Footer / Dismiss ───────────────────────────────────
                if (updateInfo.isForceUpdate) {
                    TextButton(
                        onClick = onExitClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Exit App",
                            color = Color(0xFFFF6B6B),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (onDismissClick != null) {
                    TextButton(
                        onClick = onDismissClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Remind Me Later",
                            color = Color(0xFF888899),
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        }
    }
}
