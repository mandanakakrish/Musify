package com.gaminghub.musicplayer.update

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
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
import com.gaminghub.musicplayer.util.NetworkMonitor

/**
 * Aesthetic, modern glassmorphic update dialog styled to perfectly match Musify's
 * Spotify-grade dark UI, dynamic accent theme, and glowing accents.
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

    // Adaptive theme accent — matches the user's active theme or warning palette
    val themeAccent = MaterialTheme.colorScheme.primary
    val primaryAccent by animateColorAsState(
        targetValue = when {
            !isOnline -> Color(0xFFFFB74D) // Amber for offline
            updateInfo.isForceUpdate -> Color(0xFFFF4D4D) // Sleek warning crimson
            themeAccent != Color.Unspecified -> themeAccent
            else -> DefaultSpotifyGreen
        },
        animationSpec = tween(400),
        label = "primaryAccent"
    )

    // Gentle ambient pulse for header halo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Dialog(
        onDismissRequest = {
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
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0F0F14),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .shadow(24.dp, shape = RoundedCornerShape(28.dp), spotColor = primaryAccent.copy(alpha = 0.35f))
                .border(
                    BorderStroke(
                        1.2.dp,
                        Brush.verticalGradient(
                            listOf(
                                primaryAccent.copy(alpha = 0.55f),
                                Color(0x33FFFFFF),
                                Color(0x0EFFFFFF)
                            )
                        )
                    ),
                    RoundedCornerShape(28.dp)
                )
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Ambient Radial Header Glow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    primaryAccent.copy(alpha = 0.16f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Hero Icon Badge with Glowing Halo ───────────────────────
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(primaryAccent.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, primaryAccent.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size((50f * pulseScale).dp)
                                .background(Color(0xFF161622), CircleShape)
                                .border(1.2.dp, primaryAccent.copy(alpha = 0.7f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    !isOnline -> Icons.Default.CloudOff
                                    updateInfo.isForceUpdate -> Icons.Default.Security
                                    else -> Icons.Default.SystemUpdate
                                },
                                contentDescription = "Update Available",
                                tint = primaryAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Status Pill Tag ─────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(primaryAccent.copy(alpha = 0.12f))
                            .border(
                                1.dp,
                                primaryAccent.copy(alpha = 0.35f),
                                RoundedCornerShape(50.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 4.5.dp)
                    ) {
                        Text(
                            text = when {
                                !isOnline -> "OFFLINE MODE ACTIVE"
                                updateInfo.isForceUpdate -> "MANDATORY UPDATE"
                                else -> "NEW VERSION AVAILABLE"
                            },
                            color = primaryAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ── Title ───────────────────────────────────────────────────
                    Text(
                        text = if (!isOnline) {
                            "Offline? Jam to Your Music"
                        } else {
                            updateInfo.releaseTitle.ifBlank { "Musify Update Ready" }
                        },
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // ── Version Comparison Badge ────────────────────────────────
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF161622))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "v${updateInfo.currentVersion}",
                            color = Color(0xFF9898AA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "to",
                            tint = primaryAccent.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "v${updateInfo.latestVersion}",
                            color = primaryAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(primaryAccent)
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "LATEST",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Changelog / What's New ──────────────────────────────────
                    if (updateInfo.changelog.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 125.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF14141E))
                                .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = primaryAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "What's New in this update",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = updateInfo.changelog,
                                color = Color(0xFFC4C4D6),
                                fontSize = 11.5.sp,
                                lineHeight = 16.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // ── Compact Offline Quick-Play Capsule ──────────────────────
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF13131C),
                        border = BorderStroke(1.dp, Color(0x18FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (!isOnline) Icons.Default.CloudOff else Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (!isOnline) Color(0xFFFFB74D) else primaryAccent.copy(alpha = 0.85f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (!isOnline) "Offline Mode • Play Local Library" else "Play Offline Library Without Updating",
                                    color = Color(0xFFDCDCE8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onPlayDownloadedClick?.invoke() ?: AppUpdateManager.enterOfflineModeAndNavigate("downloads")
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White,
                                        containerColor = Color(0xFF1B1B26)
                                    ),
                                    border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DownloadForOffline,
                                        contentDescription = null,
                                        tint = primaryAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Downloaded", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        onPlayMyMusicClick?.invoke() ?: AppUpdateManager.enterOfflineModeAndNavigate("my_music")
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White,
                                        containerColor = Color(0xFF1B1B26)
                                    ),
                                    border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = primaryAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("My Music", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── In-App Download / Install Actions ────────────────────────
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
                                    containerColor = if (isOnline) primaryAccent else Color(0xFF22222E),
                                    contentColor = if (isOnline) Color.Black else Color(0xFF888898)
                                ),
                                shape = RoundedCornerShape(26.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .shadow(
                                        elevation = if (isOnline) 10.dp else 0.dp,
                                        shape = RoundedCornerShape(26.dp),
                                        ambientColor = primaryAccent,
                                        spotColor = primaryAccent
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isOnline) Icons.Default.Download else Icons.Default.CloudOff,
                                    contentDescription = "Download",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isOnline) "Update Now (Download & Install)" else "Connect to Internet to Download",
                                    fontSize = 14.sp,
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
                                    .border(1.dp, primaryAccent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
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
                                        color = primaryAccent,
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
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = primaryAccent,
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
                                        tint = primaryAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Download complete! Ready to install.",
                                        color = primaryAccent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Button(
                                    onClick = { AppUpdateManager.promptInstallApk(context, state.apkFile) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = primaryAccent,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(26.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .shadow(8.dp, RoundedCornerShape(26.dp), spotColor = primaryAccent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SystemUpdate,
                                        contentDescription = "Install",
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Install Update Now",
                                        fontSize = 14.5.sp,
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
                                        color = primaryAccent,
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
                                        containerColor = primaryAccent,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(26.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = "Tap to Reopen Installer",
                                        fontSize = 14.sp,
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
                                        modifier = Modifier.size(18.dp)
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
                                        .height(42.dp)
                                ) {
                                    Text("Open Settings to Grant", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(
                                    onClick = { AppUpdateManager.promptInstallApk(context, state.apkFile) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("I've Granted It - Install Now", color = primaryAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                                        containerColor = primaryAccent,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(26.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
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
                                        .height(42.dp)
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = "Browser", tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Download in Browser", color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Footer / Dismiss ────────────────────────────────────────
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
                                fontSize = 13.sp,
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
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
