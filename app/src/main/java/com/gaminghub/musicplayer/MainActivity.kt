package com.gaminghub.musicplayer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.gaminghub.musicplayer.ui.AboutSettingsScreen
import com.gaminghub.musicplayer.ui.AppUISettingsScreen
import com.gaminghub.musicplayer.ui.BackupRestoreSettingsScreen
import com.gaminghub.musicplayer.ui.DownloadsScreen
import com.gaminghub.musicplayer.ui.FavoritesScreen
import com.gaminghub.musicplayer.ui.HomeScreen
import com.gaminghub.musicplayer.ui.ImportPlaylistScreen
import com.gaminghub.musicplayer.ui.LibraryScreen
import com.gaminghub.musicplayer.ui.MusicPlaybackSettingsScreen
import com.gaminghub.musicplayer.ui.MyMusicScreen
import com.gaminghub.musicplayer.ui.NowPlayingScreen
import com.gaminghub.musicplayer.ui.OthersSettingsScreen
import com.gaminghub.musicplayer.ui.PlaylistsScreen
import com.gaminghub.musicplayer.ui.SettingsScreen
import com.gaminghub.musicplayer.ui.StatsScreen
import com.gaminghub.musicplayer.ui.ThemeSettingsScreen
import com.gaminghub.musicplayer.ui.TopChartsScreen
import com.gaminghub.musicplayer.ui.YouTubeScreen
import com.gaminghub.musicplayer.ui.theme.MusifyGlassBorder
import com.gaminghub.musicplayer.ui.theme.MusifyGreen
import com.gaminghub.musicplayer.ui.theme.MusifyGreenDark
import com.gaminghub.musicplayer.ui.theme.DefaultSpotifyGreen
import com.gaminghub.musicplayer.ui.theme.LocalAppGradients
import com.gaminghub.musicplayer.ui.theme.AppThemeGradients
import com.gaminghub.musicplayer.ui.theme.ThemeGradientHelper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch

@UnstableApi
class MainActivity : ComponentActivity() {
    private var currentMusicViewModel: MusicViewModel? = null

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.any { it.value }
        if (granted) {
            currentMusicViewModel?.loadLocalTracks()
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (_: Exception) {}

        val perms = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            perms.add(android.Manifest.permission.READ_MEDIA_AUDIO)
            perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            perms.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            perms.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        val needed = perms.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            requestPermissionLauncher.launch(needed.toTypedArray())
        }

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
            val useSystemTheme by settingsViewModel.useSystemTheme.collectAsState()
            val accentColorHex by settingsViewModel.accentColorHex.collectAsState()
            val useAmoled by settingsViewModel.useAmoled.collectAsState()
            val canvasColor by settingsViewModel.canvasColor.collectAsState()
            val cardColor by settingsViewModel.cardColor.collectAsState()

            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val effectiveDark = if (useSystemTheme) isSystemDark else isDarkMode
            val accentColor = androidx.compose.runtime.remember(accentColorHex) {
                try {
                    Color(android.graphics.Color.parseColor(accentColorHex))
                } catch (_: Exception) {
                    DefaultSpotifyGreen
                }
            }

            val bgGradIdx by settingsViewModel.backgroundGradient.collectAsState()
            val cardGradIdx by settingsViewModel.cardGradient.collectAsState()
            val sheetGradIdx by settingsViewModel.bottomSheetGradient.collectAsState()
            val presets = ThemeGradientHelper.getPresets(effectiveDark)
            val bgColors = if (effectiveDark && (useAmoled || canvasColor == "Black")) {
                listOf(Color.Black, Color.Black)
            } else {
                presets.getOrElse(bgGradIdx) { presets[0] }
            }
            val cardColors = presets.getOrElse(cardGradIdx) { presets[0] }
            val sheetColors = presets.getOrElse(sheetGradIdx) { presets[0] }
            val appGradients = AppThemeGradients(
                backgroundBrush = Brush.verticalGradient(bgColors),
                cardBrush = Brush.verticalGradient(cardColors),
                bottomSheetBrush = Brush.verticalGradient(sheetColors),
                backgroundColor = bgColors.first(),
                cardColor = cardColors.first()
            )


            val bgColor = if (!effectiveDark) {
                Color(0xFFF5F5F5)
            } else if (useAmoled || canvasColor == "Black") {
                Color.Black
            } else {
                Color(0xFF121212)
            }

            val surfaceColor = if (!effectiveDark) {
                Color(0xFFFFFFFF)
            } else if (useAmoled) {
                Color(0xFF0A0A0A)
            } else when (cardColor) {
                "Grey800" -> Color(0xFF2E2E2E)
                "Grey850" -> Color(0xFF222222)
                "Black" -> Color.Black
                else -> Color(0xFF181818)
            }

            CompositionLocalProvider(LocalAppGradients provides appGradients) {
                MaterialTheme(
                    colorScheme = if (effectiveDark) {
                        darkColorScheme(
                             primary = accentColor,
                             surface = surfaceColor,
                             background = Color.Transparent,
                             onBackground = Color.White,
                             onSurface = Color.White,
                             surfaceVariant = Color(0xFF242424),
                             onSurfaceVariant = Color(0xFFCAC4D0)
                        )
                    } else {
                        lightColorScheme(
                             primary = accentColor,
                             surface = surfaceColor,
                             background = Color.Transparent,
                             onBackground = Color(0xFF191C1E),
                             onSurface = Color(0xFF191C1E),
                             surfaceVariant = Color(0xFFE8E8E8),
                             onSurfaceVariant = Color(0xFF44474E)
                        )
                    }
                ) {
                    val view = androidx.compose.ui.platform.LocalView.current
                    if (!view.isInEditMode) {
                        androidx.compose.runtime.SideEffect {
                            val window = (view.context as? android.app.Activity)?.window ?: this@MainActivity.window
                            val statusBarColor = bgColors.first().toArgb()
                            window.statusBarColor = statusBarColor
                            window.navigationBarColor = if (!effectiveDark) {
                                android.graphics.Color.parseColor("#FFFFFF")
                            } else if (useAmoled) {
                                android.graphics.Color.BLACK
                            } else {
                                android.graphics.Color.parseColor("#181818")
                            }

                            val insetsController = WindowCompat.getInsetsController(window, view)
                            // When dark mode is OFF (!effectiveDark), notification bar / status bar text and icons become BLACK
                            insetsController.isAppearanceLightStatusBars = !effectiveDark
                            insetsController.isAppearanceLightNavigationBars = !effectiveDark
                        }
                    }

                    val authViewModel: com.gaminghub.musicplayer.auth.AuthViewModel = viewModel()
                    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
                var isSplashDone by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                val updateInfo by com.gaminghub.musicplayer.update.AppUpdateManager.updateInfo.collectAsState()

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    com.gaminghub.musicplayer.update.AppUpdateManager.checkForUpdates(this@MainActivity)
                }

                if (!isSplashDone) {
                    com.gaminghub.musicplayer.ui.SplashScreen(
                        onSplashFinished = { isSplashDone = true }
                    )
                } else if (!isLoggedIn) {
                    com.gaminghub.musicplayer.ui.AuthScreen(
                        authViewModel = authViewModel,
                        onLoginSuccess = { }
                    )
                } else {
                    val musicViewModel: MusicViewModel = viewModel()
                    currentMusicViewModel = musicViewModel
                    
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        handleIncomingIntent(intent)
                    }

                    MusifyMainScreen(
                        viewModel = musicViewModel,
                        settingsViewModel = settingsViewModel,
                        authViewModel = authViewModel
                    )
                }

                // Strictly prompt for updating the app whenever an update is detected from GitHub
                if (isSplashDone && updateInfo != null && updateInfo?.isUpdateAvailable == true) {
                    com.gaminghub.musicplayer.update.StrictUpdateDialog(
                        updateInfo = updateInfo!!,
                        onUpdateClick = { url ->
                            com.gaminghub.musicplayer.update.AppUpdateManager.openDownloadUrl(this@MainActivity, url)
                        },
                        onExitClick = {
                            finishAffinity()
                        },
                        onDismissClick = if (!updateInfo!!.isForceUpdate) {
                            { com.gaminghub.musicplayer.update.AppUpdateManager.dismissOptionalUpdate() }
                        } else null
                    )
                }
            }
        }
    }
}

    override fun onResume() {
        super.onResume()
        com.gaminghub.musicplayer.update.AppUpdateManager.checkForUpdates(this)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: android.content.Intent?) {
        if (intent == null) return
        val action = intent.action
        val data = intent.data

        when (action) {
            android.content.Intent.ACTION_VIEW -> {
                if (data != null) {
                    val uriStr = data.toString()
                    val mimeType = intent.type

                    if (uriStr.endsWith(".json", ignoreCase = true) || mimeType == "application/json") {
                        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val result = com.gaminghub.musicplayer.util.BackupRestoreHelper.restoreBackupFromUri(this@MainActivity, data)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                result.onSuccess {
                                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                                }.onFailure {
                                    Toast.makeText(this@MainActivity, "Failed importing: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        val videoId = data.getQueryParameter("id")
                            ?: data.getQueryParameter("v")
                            ?: com.gaminghub.musicplayer.util.LinkHelper.extractVideoId(uriStr)

                        if (!videoId.isNullOrBlank()) {
                            val title = data.getQueryParameter("title")?.takeIf { it.isNotBlank() } ?: "Shared Track"
                            val artist = data.getQueryParameter("artist")?.takeIf { it.isNotBlank() } ?: "Musify"
                            val audioUrl = "https://www.youtube.com/watch?v=$videoId"
                            val albumArtUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                            currentMusicViewModel?.playTrack(
                                TrackModel(
                                    title = title,
                                    artist = artist,
                                    audioUrl = audioUrl,
                                    albumArtUrl = albumArtUrl
                                )
                            )
                        } else if (data.scheme == "musify" || data.scheme == "youtify" || (data.scheme == "app" && (data.host == "musify" || data.host == "youtify"))) {
                            val paramUrl = data.getQueryParameter("url")
                            val title = data.getQueryParameter("title")?.takeIf { it.isNotBlank() } ?: "Shared Track"
                            val artist = data.getQueryParameter("artist")?.takeIf { it.isNotBlank() } ?: "Musify"
                            if (!paramUrl.isNullOrBlank()) {
                                currentMusicViewModel?.playTrack(
                                    TrackModel(
                                        title = title,
                                        artist = artist,
                                        audioUrl = paramUrl,
                                        albumArtUrl = null
                                    )
                                )
                            } else {
                                currentMusicViewModel?.submitSearch(title)
                            }
                        } else {
                            val title = data.lastPathSegment?.substringBeforeLast(".") ?: "Imported Track"
                            currentMusicViewModel?.playTrack(
                                TrackModel(
                                    title = title,
                                    artist = "External Stream",
                                    audioUrl = uriStr,
                                    albumArtUrl = null
                                )
                            )
                        }
                    }
                }
            }
            android.content.Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrBlank()) {
                    val videoId = com.gaminghub.musicplayer.util.LinkHelper.extractVideoId(sharedText)
                    if (!videoId.isNullOrBlank()) {
                        val audioUrl = "https://www.youtube.com/watch?v=$videoId"
                        val albumArtUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                        currentMusicViewModel?.playTrack(
                            TrackModel(
                                title = "Shared Song",
                                artist = "Musify",
                                audioUrl = audioUrl,
                                albumArtUrl = albumArtUrl
                            )
                        )
                    } else {
                        val cleanQuery = sharedText.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: sharedText.trim()
                        currentMusicViewModel?.submitSearch(cleanQuery)
                    }
                }
            }
            // ── Widget media control intents ──────────────────────────────
            Player.ACTION_PLAY_PAUSE -> currentMusicViewModel?.togglePlayPause()
            Player.ACTION_NEXT       -> currentMusicViewModel?.playNext(fromUser = true)
            Player.ACTION_PREV       -> currentMusicViewModel?.playPrevious()
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusifyMainScreen(
    viewModel: MusicViewModel,
    settingsViewModel: SettingsViewModel,
    authViewModel: com.gaminghub.musicplayer.auth.AuthViewModel = viewModel()
) {
    val realTracks by viewModel.tracks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = androidx.compose.ui.platform.LocalContext.current

    val currentUser by authViewModel.currentUser.collectAsState()
    val googleEmail by authViewModel.googleEmail.collectAsState()
    val googleDisplayName by authViewModel.googleDisplayName.collectAsState()
    val googlePhotoUrl by authViewModel.googlePhotoUrl.collectAsState()

    // ── Widget sync ───────────────────────────────────────────────────────
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying    by viewModel.isPlaying.collectAsState()
    androidx.compose.runtime.LaunchedEffect(currentTrack, isPlaying) {
        MusicWidgetUpdater.update(
            context   = context,
            title     = currentTrack?.title,
            artist    = currentTrack?.artist,
            artUrl    = currentTrack?.albumArtUrl,
            isPlaying = isPlaying
        )
    }

    val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
    val useSystemTheme by settingsViewModel.useSystemTheme.collectAsState()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val effectiveDark = if (useSystemTheme) isSystemDark else isDarkMode

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF121212),
                modifier = Modifier.width(310.dp),
            ){
                Box(
                    modifier = Modifier.fillMaxHeight()
                        .fillMaxWidth()
                        .background(Color(0xFF121212))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.musify_drawer_bg_perfect),
                        contentDescription = "background",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp)
                ) {
                    Spacer(modifier = Modifier.height(52.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 2.dp)
                    ) {
                        Text(
                            text = "Musify",
                            color = Color.White,
                            fontSize = 38.sp,
                            fontFamily = FontFamily(Font(R.font.montserrat_bold, FontWeight.Bold)),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(MusifyGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val drawerVersion = remember(context) {
                            try {
                                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                                "v${pInfo.versionName}"
                            } catch (_: Exception) {
                                "v1.7.0"
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MusifyGreen.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MusifyGreen.copy(alpha = 0.35f)),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = drawerVersion,
                                color = MusifyGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                // ── Google User Profile Header ──
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MusifyGlassBorder.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!googlePhotoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = googlePhotoUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(MusifyGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                val initial = (googleDisplayName ?: googleEmail ?: currentUser?.displayName ?: currentUser?.email ?: "G").take(1).uppercase()
                                Text(initial, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = googleDisplayName ?: currentUser?.displayName ?: googleEmail?.substringBefore("@") ?: "Google User",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = googleEmail ?: currentUser?.email ?: "Google Account",
                                color = MusifyGreen,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = {
                                authViewModel.signOut {
                                    Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out", tint = Color(0xFFEF5350), modifier = Modifier.size(22.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MusifyGlassBorder)
                ) {
                NavigationDrawerItem(
                        label = { Text("Home", color = Color.White) },
                        selected = currentRoute == "home",
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("home") { launchSingleTop = true }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = null, tint = if (currentRoute == "home") MusifyGreen else Color.White.copy(0.7f)) },
                        modifier = Modifier,
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent, 
                                selectedContainerColor = MusifyGreen.copy(0.2f),
                                selectedTextColor = MusifyGreen,
                                unselectedTextColor = Color.White
                            )
                )}
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MusifyGlassBorder)
                ) {
                NavigationDrawerItem(
                    label = { Text("My Music", color = Color.White) },
                    selected = currentRoute == "my_music",
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate("my_music") { launchSingleTop = true }
                    },
                    shape = RoundedCornerShape(12.dp),
                    icon = { Icon(Icons.Default.Folder, contentDescription = null, tint = if (currentRoute == "my_music") MusifyGreen else Color.White.copy(0.7f)) },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent, 
                        selectedContainerColor = MusifyGreen.copy(0.2f),
                        selectedTextColor = MusifyGreen,
                        unselectedTextColor = Color.White
                    )
                )}
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MusifyGlassBorder)
                ) {
                NavigationDrawerItem(
                    label = { Text("Downloads", color = Color.White) },
                    selected = currentRoute == "downloads",
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate("downloads") { launchSingleTop = true }
                    },
                    shape = RoundedCornerShape(12.dp),
                    icon = { Icon(Icons.Default.DownloadForOffline, contentDescription = null, tint = if (currentRoute == "downloads") MusifyGreen else Color.White.copy(0.7f)) },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent, 
                        selectedContainerColor = MusifyGreen.copy(0.2f),
                        selectedTextColor = MusifyGreen,
                        unselectedTextColor = Color.White
                    )
                )}
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MusifyGlassBorder)
                ) {
                NavigationDrawerItem(
                    label = { Text("Playlists", color = Color.White) },
                    selected = currentRoute == "playlists",
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate("playlists") { launchSingleTop = true }
                    },
                    shape = RoundedCornerShape(12.dp),
                    icon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = if (currentRoute == "playlists") MusifyGreen else Color.White.copy(0.7f)) },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent, 
                        selectedContainerColor = MusifyGreen.copy(0.2f),
                        selectedTextColor = MusifyGreen,
                        unselectedTextColor = Color.White
                    )
                )}
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MusifyGlassBorder)
                ) {
                NavigationDrawerItem(
                    label = { Text("Settings", color = Color.White) },
                    selected = currentRoute == "settings",
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate("settings") { launchSingleTop = true }
                    },
                    shape = RoundedCornerShape(12.dp),
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = if (currentRoute == "settings") MusifyGreen else Color.White.copy(0.7f)) },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent, 
                        selectedContainerColor = MusifyGreen.copy(0.2f),
                        selectedTextColor = MusifyGreen,
                        unselectedTextColor = Color.White
                    )
                )}
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MusifyGlassBorder)
                ) {
                NavigationDrawerItem(
                    label = { Text("Help us by rating", color = Color.White) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color.White.copy(0.7f)) },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent, 
                        selectedContainerColor = MusifyGreen.copy(0.2f),
                        selectedTextColor = MusifyGreen,
                        unselectedTextColor = Color.White
                    )
                )}
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp, top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x30000000)),
                    border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Go Premium now", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("👑")
                    }
                }
            }}}
        }
    ) {
        val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }
        val activeDownloads by viewModel.activeDownloads.collectAsState()

        Scaffold(
            bottomBar = {
                if (currentRoute != "now_playing") {
                    Column {
                        if (activeDownloads.isNotEmpty()) {
                            val firstDownload = activeDownloads.values.first()
                            Surface(
                                color = Color(0xFF1E1E28),
                                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                                border = BorderStroke(1.dp, MusifyGreen.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navController.navigate("downloads") }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.DownloadForOffline,
                                                contentDescription = null,
                                                tint = MusifyGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Downloading: ${firstDownload.track.title}",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "${firstDownload.downloadedFormatted}/${firstDownload.totalFormatted} • ${firstDownload.speedFormatted} (${firstDownload.progressPercent}%)",
                                            color = MusifyGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (firstDownload.isIndeterminate) {
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(3.dp)
                                                .clip(RoundedCornerShape(1.5.dp)),
                                            color = MusifyGreen,
                                            trackColor = Color(0xFF2B2B36)
                                        )
                                    } else {
                                        LinearProgressIndicator(
                                            progress = { firstDownload.progressPercent / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(3.dp)
                                                .clip(RoundedCornerShape(1.5.dp)),
                                            color = MusifyGreen,
                                            trackColor = Color(0xFF2B2B36)
                                        )
                                    }
                                }
                            }
                        }
                        MiniPlayer(viewModel = viewModel, settingsViewModel = settingsViewModel, onClick = { navController.navigate("now_playing") })
                        MusifyBottomNavBar(navController = navController)
                    }
                }
            },
            containerColor = Color.Transparent,
            modifier = Modifier.background(LocalAppGradients.current.backgroundBrush)
        ) { paddingValues ->
            NavHost(navController = navController, startDestination = "home", modifier = Modifier.padding(paddingValues)) {
                composable("home") { HomeScreen(tracks = realTracks, viewModel = viewModel, settingsViewModel = settingsViewModel, navController = navController, onMenuClick = openDrawer) }
                composable("top_charts") { TopChartsScreen(viewModel = viewModel, onMenuClick = openDrawer) }
                composable("youtube") { YouTubeScreen(viewModel = viewModel, onMenuClick = openDrawer) }
                composable("library") { LibraryScreen(viewModel = viewModel, navController = navController, onMenuClick = openDrawer) }
                composable("search") { com.gaminghub.musicplayer.ui.SearchScreen(viewModel = viewModel, onBack = { navController.popBackStack() }, navController = navController) }
                composable("my_music") { MyMusicScreen(viewModel = viewModel, navController = navController) }
                composable("my_music/{playlistName}") { backStackEntry ->
                    val playlistName = backStackEntry.arguments?.getString("playlistName")
                    MyMusicScreen(viewModel = viewModel, navController = navController, playlistName = playlistName)
                }
                composable("downloads") { DownloadsScreen(viewModel = viewModel, navController = navController) }
                composable("playlists") { PlaylistsScreen(navController = navController, viewModel = viewModel) }
                composable("playlist_detail/{id}/{name}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                    val name = backStackEntry.arguments?.getString("name") ?: "Playlist"
                    com.gaminghub.musicplayer.ui.PlaylistDetailScreen(
                        playlistId = id,
                        playlistName = name,
                        viewModel = viewModel,
                        navController = navController
                    )
                }
                composable("import_playlist") { ImportPlaylistScreen(navController = navController, viewModel = viewModel) }
                composable("stats") { StatsScreen(navController = navController, viewModel = viewModel) }
                composable("equalizer") { com.gaminghub.musicplayer.ui.EqualizerScreen(navController = navController) }
                composable("settings") { SettingsScreen(navController = navController) }
                composable("music_playback_settings") { MusicPlaybackSettingsScreen(navController = navController, settingsViewModel = settingsViewModel) }
                composable("about_settings") { AboutSettingsScreen(navController = navController) }
                composable("others_settings") { OthersSettingsScreen(navController = navController, settingsViewModel = settingsViewModel) }
                composable("backup_restore_settings") { BackupRestoreSettingsScreen(navController = navController, settingsViewModel = settingsViewModel) }
                composable("theme_settings") { ThemeSettingsScreen(navController = navController, settingsViewModel = settingsViewModel) }
                composable("app_ui_settings") { AppUISettingsScreen(navController = navController, settingsViewModel = settingsViewModel) }
                
                composable("auth") { com.gaminghub.musicplayer.ui.AuthScreen(authViewModel = authViewModel, onLoginSuccess = { navController.popBackStack() }) }
                composable("discover_artists") { com.gaminghub.musicplayer.ui.DiscoverArtistsScreen(viewModel = viewModel, navController = navController, onBack = { navController.popBackStack() }) }
                composable("subscriptions") { com.gaminghub.musicplayer.ui.FollowingScreen(viewModel = viewModel, navController = navController, onBack = { navController.popBackStack() }) }
                composable("following") { com.gaminghub.musicplayer.ui.FollowingScreen(viewModel = viewModel, navController = navController, onBack = { navController.popBackStack() }) }
                composable("now_playing") { NowPlayingScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
                composable("last_session") { com.gaminghub.musicplayer.ui.LastSessionScreen(viewModel = viewModel, navController = navController) }
                composable("favorites") { FavoritesScreen(viewModel = viewModel, navController = navController) }
                composable(
                    route = "curated_playlist/{title}/{subtitle}/{query}?imageUrl={imageUrl}",
                    arguments = listOf(
                        androidx.navigation.navArgument("title") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("subtitle") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
                        androidx.navigation.navArgument("query") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("imageUrl") { type = androidx.navigation.NavType.StringType; defaultValue = "" }
                    )
                ) { backStackEntry ->
                    val title = backStackEntry.arguments?.getString("title") ?: "Playlist"
                    val subtitle = backStackEntry.arguments?.getString("subtitle") ?: ""
                    val query = backStackEntry.arguments?.getString("query") ?: title
                    val imageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""
                    com.gaminghub.musicplayer.ui.CuratedPlaylistScreen(
                        title = title,
                        subtitle = subtitle,
                        query = query,
                        imageUrl = imageUrl,
                        viewModel = viewModel,
                        navController = navController
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun MiniPlayer(viewModel: MusicViewModel, settingsViewModel: SettingsViewModel, onClick: () -> Unit) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()

    val useDenseMiniplayer by settingsViewModel.useDenseMiniplayer.collectAsState()
    val showMiniPlayerDownload by settingsViewModel.showMiniPlayerDownload.collectAsState()
    val showMiniPlayerFavorite by settingsViewModel.showMiniPlayerFavorite.collectAsState()

    currentTrack?.let { track ->
        val progress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = if (useDenseMiniplayer) 2.dp else 4.dp),
            onClick = onClick,
            color = Color.Transparent,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LocalAppGradients.current.cardBrush, shape = RoundedCornerShape(16.dp))
            ) {
                Column {
                // Top Mini Progress Strip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(MusifyGreen, MusifyGreenDark)
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = if (useDenseMiniplayer) 4.dp else 8.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.gaminghub.musicplayer.ui.components.SongImage(
                        model = track.albumArtUrl,
                        contentDescription = "Currently Playing",
                        modifier = Modifier
                            .size(if (useDenseMiniplayer) 38.dp else 46.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(if (useDenseMiniplayer) 8.dp else 12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = if (useDenseMiniplayer) 13.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = track.artist,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            fontSize = if (useDenseMiniplayer) 11.sp else 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Download Button
                    if (showMiniPlayerDownload) {
                        val downloadedUrls by viewModel.downloadedUrls.collectAsState()
                        val activeDownloads by viewModel.activeDownloads.collectAsState()
                        val dlProgress = track.audioUrl?.let { activeDownloads[it] }
                        val isDl = track.audioUrl != null && downloadedUrls.contains(track.audioUrl)

                        IconButton(
                            onClick = { viewModel.toggleDownload(track) },
                            modifier = Modifier.size(if (useDenseMiniplayer) 32.dp else 36.dp)
                        ) {
                            if (dlProgress != null) {
                                CircularProgressIndicator(
                                    progress = { dlProgress.progressPercent / 100f },
                                    modifier = Modifier.size(18.dp),
                                    color = MusifyGreen,
                                    strokeWidth = 2.dp,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                )
                            } else {
                                Icon(
                                    imageVector = if (isDl) Icons.Default.CheckCircle else Icons.Default.DownloadForOffline,
                                    contentDescription = "Download",
                                    tint = if (isDl) MusifyGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(if (useDenseMiniplayer) 20.dp else 22.dp)
                                )
                            }
                        }
                    }

                    // Heart icon
                    if (showMiniPlayerFavorite) {
                        val favoriteUrls by viewModel.favoriteUrls.collectAsState()
                        val isFavorite = track.audioUrl != null && favoriteUrls.contains(track.audioUrl)
                        IconButton(
                            onClick = { viewModel.toggleFavorite(track) },
                            modifier = Modifier.size(if (useDenseMiniplayer) 32.dp else 36.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) MusifyGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(if (useDenseMiniplayer) 20.dp else 22.dp)
                            )
                        }
                    }

                    // Play/Pause Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MusifyGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(
                                onClick = { viewModel.togglePlayPause() },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { viewModel.playNext() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}
}

@Composable
fun MusifyBottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color.Transparent,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalAppGradients.current.bottomSheetBrush)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── 1. Home Tab ───────────────────────────────────
                val isHomeSelected = currentRoute == "home" || currentRoute == null
                BottomNavItem(
                    modifier = Modifier.weight(1f),
                    title = "Home",
                    icon = if (isHomeSelected) Icons.Default.Home else Icons.Outlined.Home,
                    isSelected = isHomeSelected,
                    onClick = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                // ── 2. Top Charts Tab ─────────────────────────────
                val isChartsSelected = currentRoute == "top_charts"
                BottomNavItem(
                    modifier = Modifier.weight(1f),
                    title = "Charts",
                    icon = if (isChartsSelected) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Outlined.TrendingUp,
                    isSelected = isChartsSelected,
                    onClick = {
                        navController.navigate("top_charts") {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                // ── 3. Explore (YouTube) Tab ──────────────────────
                val isExploreSelected = currentRoute == "youtube"
                BottomNavItem(
                    modifier = Modifier.weight(1f),
                    title = "Explore",
                    icon = if (isExploreSelected) Icons.Default.Explore else Icons.Outlined.Explore,
                    isSelected = isExploreSelected,
                    onClick = {
                        navController.navigate("youtube") {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                // ── 4. Library Tab ────────────────────────────────
                val isLibrarySelected = currentRoute == "library" || currentRoute?.startsWith("my_music") == true || currentRoute == "playlists" || currentRoute == "downloads"
                BottomNavItem(
                    modifier = Modifier.weight(1f),
                    title = "Library",
                    icon = if (isLibrarySelected) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                    isSelected = isLibrarySelected,
                    onClick = {
                        navController.navigate("library") {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) MusifyGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            color = if (isSelected) MusifyGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
