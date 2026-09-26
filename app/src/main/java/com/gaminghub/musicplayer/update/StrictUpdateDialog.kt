package com.gaminghub.musicplayer.update

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gaminghub.musicplayer.ui.theme.MusifyGreen

/**
 * A strict, non-dismissible modal update dialog that prompts the user to update the app.
 * Provides in-app downloading with live progress tracking and automatic package installation.
 */
@Composable
fun StrictUpdateDialog(
    updateInfo: AppUpdateInfo,
    onUpdateClick: ((String) -> Unit)? = null,
    onExitClick: () -> Unit,
    onDismissClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val downloadState by AppUpdateManager.downloadState.collectAsState()
    val isDownloading = downloadState is UpdateDownloadState.Downloading

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
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF16161F),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(1.dp, Color(0xFF282838), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Top Icon Badge ─────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(MusifyGreen.copy(alpha = 0.15f), CircleShape)
                        .border(1.5.dp, MusifyGreen.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update Available",
                        tint = MusifyGreen,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Strict Pill Tag ────────────────────────────────────
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            if (updateInfo.isForceUpdate) Color(0xFFE53935).copy(alpha = 0.2f)
                            else MusifyGreen.copy(alpha = 0.2f)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (updateInfo.isForceUpdate) "MANDATORY UPDATE REQUIRED" else "NEW VERSION AVAILABLE",
                        color = if (updateInfo.isForceUpdate) Color(0xFFFF5252) else MusifyGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Title ──────────────────────────────────────────────
                Text(
                    text = updateInfo.releaseTitle.ifBlank { "Update Musify to Continue" },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // ── Version Comparison Row ─────────────────────────────
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF20202E))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current: ${updateInfo.currentVersion}",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "to",
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New: ${updateInfo.latestVersion}",
                        color = MusifyGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Changelog / What's New ─────────────────────────────
                if (updateInfo.changelog.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E1E2C))
                            .border(1.dp, Color(0xFF2C2C3E), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "What's New in ${updateInfo.latestVersion}:",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = updateInfo.changelog,
                            color = Color(0xFFB0B0C0),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // ── Notice message ─────────────────────────────────────
                Text(
                    text = if (updateInfo.isForceUpdate) {
                        "This version contains critical updates and fixes. You must install the update to continue using the app."
                    } else {
                        "A new version is available on GitHub. Update now for the best experience."
                    },
                    color = Color(0xFF9090A0),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── In-App Download / Install Actions ────────────────────
                when (val state = downloadState) {
                    is UpdateDownloadState.Idle -> {
                        Button(
                            onClick = {
                                if (onUpdateClick != null) {
                                    onUpdateClick(updateInfo.downloadUrl)
                                } else {
                                    AppUpdateManager.startInAppDownloadAndInstall(context, updateInfo.downloadUrl)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MusifyGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Update Now (Download & Install)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    is UpdateDownloadState.Downloading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1E1E2C))
                                .border(1.dp, Color(0xFF2C2C3E), RoundedCornerShape(14.dp))
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
                                    color = MusifyGreen,
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
                                color = MusifyGreen,
                                trackColor = Color(0xFF2C2C3E)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${state.downloadedMb} MB / ${state.totalMb}",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                                TextButton(
                                    onClick = { AppUpdateManager.cancelOrResetDownload() },
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("Cancel", color = Color(0xFFFF5252), fontSize = 12.sp)
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
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Downloaded",
                                    tint = MusifyGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Download complete! Ready to install.",
                                    color = MusifyGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = { AppUpdateManager.promptInstallApk(context, state.apkFile) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MusifyGreen,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = "Install",
                                    modifier = Modifier.size(18.dp)
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
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = MusifyGreen,
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
                                    containerColor = MusifyGreen,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
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
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF2C1E1E))
                                .border(1.dp, Color(0xFFE53935).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
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
                                color = Color(0xFFE0E0E0),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { AppUpdateManager.requestInstallPermission(context) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF7043),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Text("Open Settings to Grant", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = { AppUpdateManager.promptInstallApk(context, state.apkFile) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("I've Granted It - Install Now", color = MusifyGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Button(
                                onClick = {
                                    AppUpdateManager.startInAppDownloadAndInstall(context, updateInfo.downloadUrl)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MusifyGreen,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(14.dp),
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
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = "Browser", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download in Browser", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Strict exit / Close button
                if (updateInfo.isForceUpdate) {
                    TextButton(
                        onClick = onExitClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Exit App",
                            color = Color(0xFFFF5252),
                            fontSize = 14.sp,
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
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
