package com.gaminghub.musicplayer.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController

@Composable
fun AboutSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentVersion = remember(context) {
        val (name, _) = com.gaminghub.musicplayer.update.AppUpdateManager.getCurrentVersion(context)
        if (name.startsWith("v", ignoreCase = true)) name else "v$name"
    }
    val updateInfo by com.gaminghub.musicplayer.update.AppUpdateManager.updateInfo.collectAsState()
    val isChecking by com.gaminghub.musicplayer.update.AppUpdateManager.isChecking.collectAsState()

    LaunchedEffect(Unit) {
        com.gaminghub.musicplayer.update.AppUpdateManager.checkForUpdates(context)
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
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = "About",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 1. Version (GitHub check) ───────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    com.gaminghub.musicplayer.update.AppUpdateManager.checkForUpdates(context, force = true) { info ->
                        if (info == null || !info.isUpdateAvailable) {
                            Toast.makeText(context, "You are using the latest version ($currentVersion)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Version",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when {
                        isChecking -> "Checking GitHub for updates..."
                        updateInfo?.isUpdateAvailable == true -> "New version ${updateInfo?.latestVersion} available! Tap to prompt update."
                        else -> "Tap to check for updates on GitHub"
                    },
                    color = if (updateInfo?.isUpdateAvailable == true) Color(0xFF00C853)
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF00C853),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = updateInfo?.latestVersion ?: currentVersion,
                    color = if (updateInfo?.isUpdateAvailable == true) Color(0xFF00C853)
                    else MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── 2. Share App ────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Stream unlimited high quality ad-free music with Musify! 🎧\n\nDownload the app:\nhttps://github.com/mandanakakrish/Musify"
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share App"))
                }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                "Share App",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "Let your friends know about us",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }

        // ── 3. Contact Us → Musify GitHub ───────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    try {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            "https://github.com/mandanakakrish/Musify".toUri()
                        )
                        context.startActivity(browserIntent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "github.com/mandanakakrish/Musify", Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                "Contact Us",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "github.com/mandanakakrish/Musify",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }
    }
}
