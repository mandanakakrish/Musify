package com.gaminghub.musify.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import java.util.UUID

@Composable
fun DesktopGoogleSignInDialog(viewModel: DesktopMusicViewModel) {
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var isLoadingBrowser by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = { viewModel.closeGoogleSignIn() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            modifier = Modifier.width(460.dp).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GoogleLogoIcon(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Google Sign-In",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                    text = "Sign in with your Google account to sync your liked songs, custom playlists, and music recommendations.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Browser Sign-In Button
                Button(
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
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1F1F1F)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    if (isLoadingBrowser) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.DarkGray,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Connecting to Google...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        GoogleLogoIcon(modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Sign in with Google (Browser)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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

                Spacer(modifier = Modifier.height(20.dp))

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SurfaceElevated)
                    Text(
                        text = "  OR SIGN IN DIRECTLY  ",
                        color = TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SurfaceElevated)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name Field
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Full Name or Nickname", fontSize = 12.sp) },
                    placeholder = { Text("e.g. John Doe", color = TextTertiary, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MusifyEmerald,
                        unfocusedBorderColor = SurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Email Field
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Google Account Email", fontSize = 12.sp) },
                    placeholder = { Text("e.g. user@gmail.com", color = TextTertiary, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MusifyEmerald,
                        unfocusedBorderColor = SurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Confirm Sign In Button
                Button(
                    onClick = {
                        val finalEmail = if (emailInput.isNotBlank()) {
                            if (emailInput.contains("@")) emailInput.trim() else "${emailInput.trim()}@gmail.com"
                        } else "user@gmail.com"

                        val finalName = if (nameInput.isNotBlank()) nameInput.trim() else "Google User"

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
                        containerColor = MusifyEmerald,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

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
    // Elegant multi-colored Google 'G' representation
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "G",
            color = Color(0xFF4285F4), // Google Blue
            fontSize = 13.sp,
            fontWeight = FontWeight.Black
        )
    }
}
