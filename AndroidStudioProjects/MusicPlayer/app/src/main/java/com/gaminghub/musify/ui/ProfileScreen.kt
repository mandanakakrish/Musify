package com.gaminghub.musify.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gaminghub.musify.MusifyPink
import com.gaminghub.musify.ui.viewmodels.AuthState
import com.gaminghub.musify.ui.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    libraryViewModel: com.gaminghub.musify.ui.viewmodels.LibraryViewModel,
    onBack: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val isRestoring by libraryViewModel.isRestoring.collectAsState()
    val favorites by libraryViewModel.favoriteTracks.collectAsState()
    val playlists by libraryViewModel.playlists.collectAsState()
    
    val user = (authState as? AuthState.Authenticated)?.user
    val isGuest = authState is AuthState.Guest
    
    val displayName = user?.displayName ?: if (isGuest) "Guest User" else "Disconnected"
    val email = user?.email ?: if (isGuest) "guest@musify.plus" else "No email"
    val photoUrl = user?.photoUrl?.toString()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MusifyPink.copy(alpha = 0.12f), Color.Black),
                        startY = 0f,
                        endY = 600f
                    )
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Avatar Background Glow
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(130.dp),
                    shape = CircleShape,
                    color = MusifyPink.copy(alpha = 0.2f)
                ) {}
                
                Surface(
                    modifier = Modifier.size(110.dp),
                    shape = CircleShape,
                    color = Color(0xFF1A1A1A),
                    border = androidx.compose.foundation.BorderStroke(2.dp, MusifyPink)
                ) {
                    if (photoUrl != null) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            tint = Color.Gray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = displayName,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            
            Surface(
                modifier = Modifier.padding(top = 8.dp),
                color = if (isGuest) Color.Gray.copy(alpha = 0.2f) else MusifyPink.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isGuest) "Guest Mode" else "Premium Member",
                    color = if (isGuest) Color.LightGray else MusifyPink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Account Info Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                ProfileInfoItem(icon = Icons.Default.Person, label = "Username", value = displayName)
                ProfileInfoItem(icon = Icons.Default.Email, label = "Email", value = email)
                ProfileInfoItem(
                    icon = Icons.Default.Badge, 
                    label = "Status", 
                    value = if (isGuest) "Limited Access" else "Full Access (Sync Enabled)"
                )
            }
            
            if (!isGuest) {
                Spacer(modifier = Modifier.height(32.dp))
                
                // Sync Dashboard
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        "Cloud Storage",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF1A1A1A),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Synced Items", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                    Text("${favorites.size}", color = MusifyPink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                    Text(" Favorites", color = Color.White, fontSize = 14.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${playlists.size}", color = MusifyPink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                    Text(" Playlists", color = Color.White, fontSize = 14.sp)
                                }
                            }
                            
                            Button(
                                onClick = { libraryViewModel.restoreFromCloud() },
                                enabled = !isRestoring,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MusifyPink,
                                    contentColor = Color.White,
                                    disabledContainerColor = Color.DarkGray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                if (isRestoring) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Restore", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Sign Out Button
            Button(
                onClick = { authViewModel.signOut() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f))
            ) {
                Text("Sign Out", color = Color.Red, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfileInfoItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.05f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(10.dp),
                tint = Color.Gray
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(label, color = Color.Gray, fontSize = 12.sp)
            Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}
