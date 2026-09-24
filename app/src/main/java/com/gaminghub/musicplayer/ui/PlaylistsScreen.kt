package com.gaminghub.musicplayer.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.gaminghub.musicplayer.MusicViewModel
import com.gaminghub.musicplayer.data.PlaylistEntity
import com.gaminghub.musicplayer.ui.theme.MusifyGreen

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlaylistsScreen(navController: NavController, viewModel: MusicViewModel) {
    val playlists by viewModel.playlists.collectAsState()
    val context = LocalContext.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }

    // Rename/Delete state
    var playlistToRename by remember { mutableStateOf<PlaylistEntity?>(null) }
    var playlistToDelete by remember { mutableStateOf<PlaylistEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top Bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                "Playlists",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ── Action Buttons ───────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            PlaylistActionItem("Create Playlist", Icons.Default.Add) {
                showCreateDialog = true
            }
            PlaylistActionItem("Import Playlist", Icons.AutoMirrored.Filled.ExitToApp) {
                navController.navigate("import_playlist")
            }
            PlaylistActionItem("Merge Playlists", Icons.AutoMirrored.Filled.MergeType) {
                showMergeDialog = true
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color.White.copy(alpha = 0.08f)
        )

        // ── Playlist List ────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {

            // Permanent "Favorite Songs" entry — always first, no 3-dot menu
            item {
                FavoriteSongsRow(onClick = { navController.navigate("favorites") })
            }

            // User-created playlists from Room
            items(playlists, key = { it.id }) { playlist ->
                UserPlaylistRow(
                    playlist = playlist,
                    onClick = { navController.navigate("playlist_detail/${playlist.id}/${playlist.name}") },
                    onRename = { playlistToRename = playlist },
                    onDelete = { playlistToDelete = playlist },
                    onExport = {
                        Toast.makeText(context, "Export coming soon", Toast.LENGTH_SHORT).show()
                    },
                    onShare = {
                        com.gaminghub.musicplayer.util.LinkHelper.sharePlaylist(context, playlist.name)
                    }
                )
            }
        }
    }

    // ── Create Playlist Dialog ───────────────────────────────────────────────
    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                if (name.isNotBlank()) {
                    viewModel.createPlaylist(name)
                }
            }
        )
    }

    // ── Merge Dialog ─────────────────────────────────────────────────────────
    if (showMergeDialog) {
        MergePlaylistsDialog(
            playlists = playlists,
            onDismiss = { showMergeDialog = false },
            onConfirm = { selectedPlaylists, newName ->
                showMergeDialog = false
                if (selectedPlaylists.size >= 2 && newName.isNotBlank()) {
                    viewModel.mergePlaylists(selectedPlaylists.map { it.id }, newName)
                } else {
                    Toast.makeText(context, "Select at least 2 playlists to merge", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // ── Rename Dialog ────────────────────────────────────────────────────────
    playlistToRename?.let { pl ->
        RenamePlaylistDialog(
            currentName = pl.name,
            onDismiss = { playlistToRename = null },
            onConfirm = { newName ->
                if (newName.isNotBlank()) {
                    viewModel.renamePlaylist(pl.id, newName)
                }
                playlistToRename = null
            }
        )
    }

    // ── Delete Confirm Dialog ────────────────────────────────────────────────
    playlistToDelete?.let { pl ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            containerColor = Color(0xFF2B2B2B),
            shape = RoundedCornerShape(12.dp),
            title = {
                Text("Delete Playlist", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${pl.name}\"?",
                    color = Color.Gray
                )
            },
            confirmButton = {
                Surface(
                    onClick = {
                        viewModel.deletePlaylist(pl.id)
                        playlistToDelete = null
                    },
                    color = Color(0xFFB71C1C),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "Delete",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

// ── Permanent Favorite Songs Row ─────────────────────────────────────────────
@Composable
fun FavoriteSongsRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail with heart gradient background
        Surface(
            modifier = Modifier.size(56.dp),
            color = Color(0xFF3D0A1E),
            shape = RoundedCornerShape(6.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MusifyGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Favorite Songs",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Your liked tracks",
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
        // No 3-dot menu — this row is permanent and non-editable
    }
}

// ── User Playlist Row with 3-dot Menu ────────────────────────────────────────
@Composable
fun UserPlaylistRow(
    playlist: PlaylistEntity,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        com.gaminghub.musicplayer.ui.components.PlaylistImage(
            model = null,
            contentDescription = playlist.name,
            modifier = Modifier
                .size(56.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                playlist.name,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Playlist",
                color = Color.Gray,
                fontSize = 13.sp
            )
        }

        // 3-dot menu
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(Color(0xFF2B2B2B))
            ) {
                PlaylistMenuItem(
                    icon = Icons.Outlined.DriveFileRenameOutline,
                    label = "Rename",
                    tint = Color.White
                ) {
                    menuExpanded = false
                    onRename()
                }
                PlaylistMenuItem(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    tint = Color(0xFFFF5252)
                ) {
                    menuExpanded = false
                    onDelete()
                }
                PlaylistMenuItem(
                    icon = Icons.Default.Upload,
                    label = "Export",
                    tint = Color.White
                ) {
                    menuExpanded = false
                    onExport()
                }
                PlaylistMenuItem(
                    icon = Icons.Default.Share,
                    label = "Share",
                    tint = Color.White
                ) {
                    menuExpanded = false
                    onShare()
                }
            }
        }
    }
}

// ── Dropdown Menu Item ────────────────────────────────────────────────────────
@Composable
fun PlaylistMenuItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(label, color = tint, fontSize = 15.sp)
            }
        },
        onClick = onClick,
        modifier = Modifier.background(Color(0xFF2B2B2B))
    )
}

@Composable
fun CreatePlaylistDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2B2B2B),
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(
                "Create New Playlist",
                color = Color(0xFF1DB954),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Playlist name", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color(0xFF1DB954),
                    unfocusedIndicatorColor = Color(0xFF1DB954)
                ),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(text)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    "Ok",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

@Composable
fun MergePlaylistsDialog(
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onConfirm: (List<PlaylistEntity>, String) -> Unit
) {
    val selectedIds = remember { mutableStateListOf<Int>() }
    var mergedName by remember { mutableStateOf("Merged Playlist") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2B2B2B),
        shape = RoundedCornerShape(12.dp),
        title = {
            Text("Merge Playlists", color = Color(0xFF1DB954), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = mergedName,
                    onValueChange = { mergedName = it },
                    label = { Text("New Playlist Name", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color(0xFF1DB954),
                        unfocusedIndicatorColor = Color.Gray
                    ),
                    singleLine = true
                )
                Text(
                    "Select at least 2 playlists to merge:",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                    items(playlists) { pl ->
                        val isChecked = selectedIds.contains(pl.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedIds.removeAll { it == pl.id } else selectedIds.add(pl.id)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E1E1E)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.MusicNote, null, tint = Color.Gray)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(pl.name, color = Color.White, modifier = Modifier.weight(1f), fontSize = 14.sp)
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedIds.add(pl.id) else selectedIds.removeAll { it == pl.id }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1DB954))
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selected = playlists.filter { selectedIds.contains(it.id) }
                    onConfirm(selected, mergedName)
                },
                enabled = selectedIds.size >= 2 && mergedName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Merge", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

// ── Rename Playlist Dialog ────────────────────────────────────────────────────
@Composable
fun RenamePlaylistDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2B2B2B),
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(
                "Rename Playlist",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color(0xFF1DB954),
                    unfocusedIndicatorColor = Color.Gray
                ),
                singleLine = true
            )
        },
        confirmButton = {
            Surface(
                onClick = { onConfirm(text) },
                color = if (text.isNotBlank()) Color(0xFF1DB954) else Color.Gray,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    "Save",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

// ── Playlist Action Item (for Create / Import / Merge) ───────────────────────
@Composable
fun PlaylistActionItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(24.dp))
        Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
    }
}
