package com.gaminghub.musify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gaminghub.musify.MusifyPink
import com.gaminghub.musify.ui.viewmodels.LibraryViewModel
import com.gaminghub.musify.TrackModel

@Composable
fun AddToPlaylistDialog(
    track: TrackModel,
    libraryViewModel: LibraryViewModel,
    onDismiss: () -> Unit
) {
    val playlists by libraryViewModel.playlists.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    
    // Track which playlists are selected
    val selectedPlaylists = remember { mutableStateListOf<Int>() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A1A1A),
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Add to Playlists",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCreateDialog = true }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF2B2B2B), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MusifyPink)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Create New Playlist", color = MusifyPink, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    
                    items(playlists) { playlist ->
                        val isSelected = selectedPlaylists.contains(playlist.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    if (isSelected) selectedPlaylists.remove(playlist.id)
                                    else selectedPlaylists.add(playlist.id)
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedPlaylists.add(playlist.id)
                                    else selectedPlaylists.remove(playlist.id)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MusifyPink,
                                    uncheckedColor = Color.Gray,
                                    checkmarkColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            @Suppress("DEPRECATION")
                            Text(playlist.name, color = Color.White, modifier = Modifier.weight(1f))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            selectedPlaylists.forEach { id ->
                                libraryViewModel.addTrackToPlaylist(id, track)
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MusifyPink),
                        shape = RoundedCornerShape(8.dp),
                        enabled = selectedPlaylists.isNotEmpty()
                    ) {
                        Text("Add (${selectedPlaylists.size})", color = Color.White)
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var playlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Color(0xFF2B2B2B),
            title = { Text("New Playlist", color = Color.White) },
            text = {
                TextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    placeholder = { Text("Name", color = Color.Gray) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (playlistName.isNotBlank()) {
                        libraryViewModel.createPlaylist(playlistName)
                        showCreateDialog = false
                    }
                }) {
                    Text("Create", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}
