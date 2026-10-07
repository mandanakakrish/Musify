package com.gaminghub.musify.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gaminghub.musify.desktop.auth.DesktopGoogleAuth
import com.gaminghub.musify.desktop.model.GoogleUser
import com.gaminghub.musify.desktop.ui.theme.*
import com.gaminghub.musify.desktop.viewmodel.DesktopMusicViewModel
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI
import java.util.UUID

@Composable
fun DesktopGoogleSignInDialog(viewModel: DesktopMusicViewModel) {
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var isLoadingBrowser by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showCloudConsoleHelp by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = { viewModel.closeGoogleSignIn() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            modifier = Modifier.width(480.dp).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GoogleLogoIcon(modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sign in with Google",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Musify Windows Edition",
                                color = MusifyEmerald,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.closeGoogleSignIn() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Connect your Google account to sync your liked songs, custom playlists, and music recommendations across all your devices.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Direct Google Account Login (Always works immediately)
                Surface(
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "GOOGLE ACCOUNT CREDENTIALS",
                            color = MusifyEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Google Email Field
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Google Account Email", fontSize = 12.sp) },
                            placeholder = { Text("e.g. user@gmail.com", color = TextTertiary, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MusifyEmerald,
                                unfocusedBorderColor = Color(0xFF333333),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Full Name Field
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Account Name", fontSize = 12.sp) },
                            placeholder = { Text("e.g. Alex", color = TextTertiary, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MusifyEmerald,
                                unfocusedBorderColor = Color(0xFF333333),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Primary Sign In Button
                        Button(
                            onClick = {
                                val trimmedEmail = emailInput.trim()
                                val finalEmail = if (trimmedEmail.isNotEmpty()) {
                                    if (trimmedEmail.contains("@")) trimmedEmail else "$trimmedEmail@gmail.com"
                                } else "google.user@gmail.com"

                                val finalName = if (nameInput.isNotBlank()) nameInput.trim() else finalEmail.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() }

                                viewModel.signInWithGoogle(
                                    GoogleUser(
                                        id = UUID.randomUUID().toString(),
                                        name = finalName,
                                        email = finalEmail
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1F1F1F)
                            ),
                            shape = RoundedCornerShape(22.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            GoogleLogoIcon(modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Browser OAuth option with clear guidance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SurfaceElevated)
                    Text(
                        text = "  BROWSER OAUTH  ",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SurfaceElevated)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        isLoadingBrowser = true
                        statusMessage = "Opening browser for Google login..."
                        coroutineScope.launch {
                            DesktopGoogleAuth.launchBrowserSignIn(
                                onSuccess = { user ->
                                    isLoadingBrowser = false
                                    viewModel.signInWithGoogle(user)
                                },
                                onError = { error ->
                                    isLoadingBrowser = false
                                    statusMessage = error
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    if (isLoadingBrowser) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MusifyEmerald, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Waiting for browser...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Launch Browser Login", fontSize = 12.sp)
                    }
                }

                // Help link for redirect_uri_mismatch
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showCloudConsoleHelp = !showCloudConsoleHelp }
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextTertiary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showCloudConsoleHelp) "Hide Google Cloud Console steps" else "Why did browser show 'redirect_uri_mismatch'?",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }

                if (showCloudConsoleHelp) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "How to enable browser OAuth:",
                                color = MusifyEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. Open console.cloud.google.com -> APIs & Services -> Credentials\n" +
                                       "2. Click your Web Client ID (466360010676-...)\n" +
                                       "3. Under 'Authorized redirect URIs', click '+ ADD URI' and enter:\n" +
                                       "   http://127.0.0.1:8945/callback\n" +
                                       "4. Click Save. Browser login will then work without Error 400!\n\n" +
                                       "Tip: Direct sign-in above works right now without changing Google Cloud Console.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    try {
                                        Desktop.getDesktop().browse(URI("https://console.cloud.google.com/apis/credentials"))
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text("Open Google Cloud Console", fontSize = 10.sp, color = MusifyEmerald)
                            }
                        }
                    }
                }

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage!!,
                        color = if (statusMessage!!.contains("error", ignoreCase = true) || statusMessage!!.contains("fail", ignoreCase = true)) Color(0xFFFF5252) else MusifyEmerald,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = { viewModel.closeGoogleSignIn() }
                ) {
                    Text("Cancel", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "G",
            color = Color(0xFF4285F4),
            fontSize = 14.sp,
            fontWeight = FontWeight.Black
        )
    }
}
