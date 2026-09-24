package com.gaminghub.musicplayer.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gaminghub.musicplayer.SettingsViewModel
import com.gaminghub.musicplayer.auth.AuthViewModel
import com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager
import com.gaminghub.musicplayer.ui.theme.MusifyGreen
import com.gaminghub.musicplayer.util.BackupRestoreHelper
import kotlinx.coroutines.launch

@Composable
fun BackupRestoreSettingsScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val autoBackup by settingsViewModel.autoBackup.collectAsState()
    val autoBackupLocation by settingsViewModel.autoBackupLocation.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val googleEmail by authViewModel.googleEmail.collectAsState()
    val googleDisplayName by authViewModel.googleDisplayName.collectAsState()

    val userSyncId = com.gaminghub.musicplayer.auth.AuthManager.getInstance(context).getSyncUserId()

    var isProcessing by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf("Processing...") }

    // Launcher to save JSON backup file to local storage
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                processingMessage = "Saving backup JSON file..."
                val success = BackupRestoreHelper.writeBackupToUri(context, uri)
                isProcessing = false
                if (success) {
                    Toast.makeText(context, "Backup JSON file saved to local storage!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to save backup file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Launcher to pick and restore JSON backup file from local storage
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                processingMessage = "Restoring from JSON file..."
                val result = BackupRestoreHelper.restoreBackupFromUri(context, uri)
                isProcessing = false
                result.onSuccess { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    Toast.makeText(context, "Restore failed: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
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
                text = "Backup & Restore",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 1. Cloud Firestore Section ──────────────────────────
        Text(
            text = "Cloud Firestore Database",
            color = MusifyGreen,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF141414),
            border = BorderStroke(1.dp, Color(0xFF242424))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = MusifyGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cloud Sync: ${googleEmail ?: currentUser?.email ?: "Google Account"}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sync playlists, favorites, play counts & history to Firestore",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                processingMessage = "Uploading to Cloud Firestore..."
                                val res = FirestoreSyncManager.syncLocalToCloud(context, userSyncId)
                                isProcessing = false
                                res.onSuccess { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                    .onFailure { Toast.makeText(context, "Upload failed: ${it.message}", Toast.LENGTH_LONG).show() }
                            }
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Backup to Cloud", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                processingMessage = "Downloading from Cloud Firestore..."
                                val res = FirestoreSyncManager.syncCloudToLocal(context, userSyncId)
                                isProcessing = false
                                res.onSuccess { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                    .onFailure { Toast.makeText(context, "Restore failed: ${it.message}", Toast.LENGTH_LONG).show() }
                            }
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MusifyGreen)
                    ) {
                        Text("Restore Cloud", color = MusifyGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 2. Local JSON Backup Section ────────────────────────
        Text(
            text = "Local Storage JSON Backup",
            color = MusifyGreen,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    createBackupLauncher.launch("musify_backup_${System.currentTimeMillis()}.json")
                }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text("Create JSON Backup", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Normal)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Save your playlists, favorites and settings as a JSON file", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 13.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // Accepts all JSON, text, and binary document formats so file manager never filters out .json
                    restoreBackupLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
                }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text("Restore from JSON File", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Normal)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Select and restore from a JSON backup file in local storage", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 13.sp)
        }

        // ── 3. Auto Backup ──────────────────────────────────────
        ThemeSwitchRow(
            title = "Auto Backup",
            subtitle = "Automatically backup data",
            checked = autoBackup,
            onCheckedChange = { settingsViewModel.setAutoBackup(it) }
        )

        // ── 4. Auto Backup Location ─────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto Backup Location", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Normal)
                Spacer(modifier = Modifier.height(2.dp))
                Text(autoBackupLocation, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 13.sp)
            }
            Text(
                "Reset",
                color = MusifyGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable {
                        settingsViewModel.setAutoBackupLocation("/storage/emulated/0/Musify/Backups")
                        Toast.makeText(context, "Location reset to default", Toast.LENGTH_SHORT).show()
                    }
                    .padding(4.dp)
            )
        }

        if (isProcessing) {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(color = MusifyGreen, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(processingMessage, color = Color.White, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}
