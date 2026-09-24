package com.gaminghub.musicplayer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.gaminghub.musicplayer.ui.theme.MusifyGreen
import com.gaminghub.musicplayer.MusicViewModel
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ImportPlaylistScreen(navController: NavController, viewModel: MusicViewModel? = null) {
    var showYoutubeDialog by remember { mutableStateOf(false) }
    var showSpotifyDialog by remember { mutableStateOf(false) }
    var showRessoDialog by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    // File Picker for JSON backup/playlist files
    val jsonFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isImporting = true
                val result = com.gaminghub.musicplayer.util.BackupRestoreHelper.restoreBackupFromUri(context, uri)
                isImporting = false
                result.onSuccess { msg ->
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                    navController.popBackStack()
                }.onFailure { err ->
                    android.widget.Toast.makeText(context, "Import failed: ${err.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // File Picker for local imports
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        // Handle imported music files
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Import Playlist", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            ImportActionItem("Import from JSON Backup / Playlist File", Icons.Default.Restore) {
                jsonFilePicker.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
            }
            ImportActionItem("Import from Spotify (Playlist, Album, Track)", Icons.Default.Podcasts) {
                showSpotifyDialog = true
            }
            ImportActionItem("Import from YouTube", Icons.Filled.PlayCircleFilled) {
                showYoutubeDialog = true
            }
            ImportActionItem("Import from Resso", Icons.Default.MusicNote) {
                showRessoDialog = true
            }
            ImportActionItem("Import from Local Audio Files", Icons.AutoMirrored.Filled.ExitToApp) {
                filePicker.launch(arrayOf("audio/*"))
            }

            if (isImporting) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = MusifyGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Importing & Resolving official songs...", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }

    if (showSpotifyDialog) {
        ImportLinkDialog(
            title = "Enter Spotify Link",
            description = "Enter any public Spotify playlist, album, or track link (e.g. https://open.spotify.com/playlist/...).\n\nMusify will automatically resolve each song to official studio audio and save it to your playlists.",
            onDismiss = { showSpotifyDialog = false },
            onConfirm = { link ->
                isImporting = true
                viewModel?.importSpotifyPlaylist(link) { name, count ->
                    isImporting = false
                    navController.popBackStack()
                }
            }
        )
    }

    if (showYoutubeDialog) {
        ImportLinkDialog(
            title = "Enter YouTube Playlist Link",
            description = "Enter the full YouTube playlist link (e.g. https://www.youtube.com/playlist?list=...). \n\nTo obtain these links, go to the playlist page inside the YouTube app and tap the \"Share\" or \"Copy Link\" button. \n\nPlease make sure the playlist is public.",
            onDismiss = { showYoutubeDialog = false },
            onConfirm = { link ->
                val name = link.substringAfterLast("list=").take(20).ifBlank { "YouTube Playlist" }
                viewModel?.createPlaylist(name)
                navController.popBackStack()
            }
        )
    }

    if (showRessoDialog) {
        ImportLinkDialog(
            title = "Enter Resso Playlist Link",
            onDismiss = { showRessoDialog = false },
            onConfirm = { link ->
                val name = link.substringAfterLast("/").ifBlank { "Resso Playlist" }
                viewModel?.createPlaylist(name)
                navController.popBackStack()
            }
        )
    }
}

@Composable
fun ImportActionItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(24.dp))
        Text(title, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontSize = 16.sp)
    }
}

@Composable
fun ImportLinkDialog(title: String, description: String? = null, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2B2B2B),
        title = { Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium) },
        text = {
            Column {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.Gray
                    ),
                    singleLine = true
                )
                if (description != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(description, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text); onDismiss() }) {
                Text("Ok", color = MusifyGreen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        },
        shape = RoundedCornerShape(8.dp)
    )
}
