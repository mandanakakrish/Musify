package com.gaminghub.musicplayer.ui

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.gaminghub.musicplayer.MusicViewModel
import com.gaminghub.musicplayer.SettingsViewModel
import com.gaminghub.musicplayer.TrackModel
import com.gaminghub.musicplayer.data.PlaylistEntity
import com.gaminghub.musicplayer.ui.theme.MusifyDarkBg
import com.gaminghub.musicplayer.ui.theme.MusifyGlassBorder
import com.gaminghub.musicplayer.ui.theme.MusifyGlassSurface
import com.gaminghub.musicplayer.ui.theme.MusifyGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.flowOf
import kotlin.math.roundToInt

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: MusicViewModel,
    settingsViewModel: SettingsViewModel? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val upNextQueue by viewModel.upNextQueue.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val sleepTimerMillis by viewModel.sleepTimerMillis.collectAsState()
    // Note: currentPosition and lyrics states are isolated in NowPlayingProgressSection
    // and NowPlayingLyricsPanel below to avoid recomposing this entire 1750-line screen 4 times/sec.

    val favoriteUrls by viewModel.favoriteUrls.collectAsState()
    val isFavorite = currentTrack?.audioUrl != null && favoriteUrls.contains(currentTrack?.audioUrl)

    // UI state
    var showMenu by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showVisualizer by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showArtistDialog by remember { mutableStateOf(false) }
    var selectedProfileArtist by remember { mutableStateOf<String?>(null) }
    var showArtistChooserDialog by remember { mutableStateOf(false) }
    var collaboratingArtists by remember { mutableStateOf<List<String>>(emptyList()) }

    fun openArtistProfileOrChooser(artistString: String) {
        val split = com.gaminghub.musicplayer.util.ArtistMatcher.splitArtists(artistString)
        if (split.size > 1) {
            collaboratingArtists = split
            showArtistChooserDialog = true
        } else if (split.size == 1) {
            selectedProfileArtist = split[0]
        } else if (artistString.isNotBlank()) {
            selectedProfileArtist = artistString
        }
    }

    var showLinkArtistDialog by remember { mutableStateOf(false) }
    var isShuffleOn by remember { mutableStateOf(false) }
    var isRepeatOn by remember { mutableStateOf(false) }
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()

    val authManager = remember { com.gaminghub.musicplayer.auth.AuthManager.getInstance(context) }
    val isAdmin by authManager.isAdmin.collectAsState()

    val settingsPrefs = remember { context.getSharedPreferences("Musify_settings", android.content.Context.MODE_PRIVATE) }
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDarkModeSetting by (settingsViewModel?.isDarkMode ?: flowOf(settingsPrefs.getBoolean("is_dark_mode", false))).collectAsState(initial = settingsPrefs.getBoolean("is_dark_mode", false))
    val useSystemThemeSetting by (settingsViewModel?.useSystemTheme ?: flowOf(settingsPrefs.getBoolean("use_system_theme", false))).collectAsState(initial = settingsPrefs.getBoolean("use_system_theme", false))
    val isDark = if (useSystemThemeSetting) isSystemDark else isDarkModeSetting

    val playerBackground = remember { settingsPrefs.getString("player_background", "Default (Blurred Artwork)") ?: "Default (Blurred Artwork)" }
    val enableArtworkGestures = remember { settingsPrefs.getBoolean("artwork_gestures", true) }
    val enableVolumeGestures = remember { settingsPrefs.getBoolean("volume_gestures", false) }
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager }

    var dominantColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(currentTrack?.albumArtUrl) {
        val artUrl = currentTrack?.albumArtUrl
        if (artUrl.isNullOrBlank()) {
            dominantColor = null
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            try {
                val loader = context.imageLoader
                val req = ImageRequest.Builder(context)
                    .data(artUrl)
                    .allowHardware(false)
                    .build()
                val res = (loader.execute(req) as? SuccessResult)?.drawable
                val bmp = (res as? BitmapDrawable)?.bitmap
                if (bmp != null) {
                    val p = Palette.from(bmp).generate()
                    val s = p.vibrantSwatch ?: p.dominantSwatch ?: p.mutedSwatch
                    if (s != null) {
                        withContext(Dispatchers.Main) {
                            dominantColor = Color(s.rgb)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val dynamicAccentColor by animateColorAsState(
        targetValue = dominantColor ?: MusifyGreen,
        animationSpec = tween(800),
        label = "dynamicAccentColor"
    )

    val bgModifier = if (!isDark) {
        Modifier.background(
            androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    dynamicAccentColor.copy(alpha = 0.25f),
                    Color(0xFFF3F7F5),
                    Color(0xFFE2EBE5),
                    Color(0xFFD3E2D8)
                )
            )
        )
    } else {
        when (playerBackground) {
            "Solid Black" -> Modifier.background(Color.Black)
            "Subtle Gradient" -> Modifier.background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        dynamicAccentColor.copy(alpha = 0.28f),
                        Color(0xFF12101D),
                        Color.Black
                    )
                )
            )
            else -> Modifier.background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        dynamicAccentColor.copy(alpha = 0.35f),
                        Color(0xFF101216),
                        MusifyDarkBg
                    )
                )
            )
        }
    }

    val textColor = if (isDark) Color.White else Color(0xFF191C1E)
    val textSecondaryColor = if (isDark) Color.Gray else Color(0xFF5A6065)
    val iconTint = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF222628)
    val glassSurface = if (isDark) MusifyGlassSurface else Color.White.copy(alpha = 0.75f)
    val glassBorder = if (isDark) MusifyGlassBorder else Color(0x1F000000)
    val sheetBg = if (isDark) Color(0xFF161622) else Color(0xFFF6FAF7)

    val downloadedUrls by viewModel.downloadedUrls.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val currentDownloadProgress = currentTrack?.audioUrl?.let { activeDownloads[it] }
    val isDownloaded = currentTrack != null && viewModel.isTrackDownloaded(currentTrack)
    val isDownloading = currentDownloadProgress != null

    val baseLikes = remember(currentTrack?.audioUrl) { currentTrack?.let { viewModel.getBaseSongLikes(it) } ?: 0L }
    val songLikes = remember(isFavorite, baseLikes, currentTrack?.audioUrl) {
        if (currentTrack != null) viewModel.formatCount(if (isFavorite) baseLikes + 1 else baseLikes)
        else ""
    }

    // Fetch lyrics when entering screen or track changes
    LaunchedEffect(currentTrack?.audioUrl) {
        currentTrack?.let { viewModel.fetchLyrics(it) }
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 52.dp,
        sheetContainerColor = sheetBg,
        sheetContentColor = textColor,
        sheetDragHandle = null,
        containerColor = if (isDark) MusifyGlassSurface else Color.Transparent,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetContent = {
            UpNextPanel(
                currentTrack = currentTrack,
                upNextQueue = upNextQueue,
                viewModel = viewModel,
                isDark = isDark,
                onTrackClick = { index -> viewModel.playFromQueue(index) },
                onReorder = { from, to -> viewModel.reorderQueue(from, to) },
                onRemoveTrack = { index -> viewModel.removeFromQueue(index) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(bgModifier)
                .padding(horizontal = 18.dp)
                .padding(bottom = innerPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Bar ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowDown, "Back", tint = textColor, modifier = Modifier.size(32.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (sleepTimerMillis > 0) {
                        val minutes = (sleepTimerMillis / 1000) / 60
                        val seconds = (sleepTimerMillis / 1000) % 60
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MusifyGreen.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MusifyGreen.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showSleepTimerDialog = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "🌙 %02d:%02d".format(minutes, seconds),
                                    color = MusifyGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    IconButton(onClick = { currentTrack?.let { viewModel.toggleDownload(it) } }) {
                        if (isDownloading) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { (currentDownloadProgress?.progressPercent ?: 0) / 100f },
                                    modifier = Modifier.size(22.dp),
                                    color = MusifyGreen,
                                    strokeWidth = 2.dp,
                                    trackColor = if (isDark) Color(0xFF2B2B36) else Color(0xFFD6D6D6)
                                )
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel Download",
                                    tint = textColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        } else {
                            Icon(
                                if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.DownloadForOffline,
                                contentDescription = if (isDownloaded) "Downloaded" else "Download Song",
                                tint = if (isDownloaded) Color(0xFF00E676) else iconTint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    IconButton(onClick = { 
                        showLyrics = !showLyrics 
                        if (showLyrics) showVisualizer = false
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.QueueMusic,
                            "Lyrics",
                            tint = if (showLyrics) MusifyGreen else iconTint
                        )
                    }
                    IconButton(onClick = { 
                        showVisualizer = !showVisualizer 
                        if (showVisualizer) showLyrics = false
                    }) {
                        Icon(
                            Icons.Default.GraphicEq,
                            "Visualizer",
                            tint = if (showVisualizer) MusifyGreen else iconTint
                        )
                    }
                    IconButton(onClick = { showEqualizerDialog = true }) {
                        Icon(
                            Icons.Default.Tune,
                            "Equalizer",
                            tint = iconTint
                        )
                    }
                    IconButton(onClick = {
                        currentTrack?.let { com.gaminghub.musicplayer.util.LinkHelper.shareSong(context, it) }
                    }) {
                        Icon(Icons.Default.Share, "Share", tint = iconTint)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "More", tint = iconTint)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(if (isDark) Color(0xFF222230) else Color(0xFFF7FAF8))
                        ) {
                            if (isAdmin) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (currentTrack?.isDevpick == true) "★ Remove Dev's Pick" else "★ Set as Dev's Pick",
                                            color = if (currentTrack?.isDevpick == true) Color(0xFFFFB74D) else MusifyGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        currentTrack?.let { viewModel.toggleDevPick(it, isAdmin = true) }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Verified,
                                            null,
                                            tint = if (currentTrack?.isDevpick == true) Color(0xFFFFB74D) else MusifyGreen
                                        )
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Copy Song Link", color = textColor) },
                                onClick = {
                                    showMenu = false
                                    currentTrack?.let { com.gaminghub.musicplayer.util.LinkHelper.copySongLink(context, it) }
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = MusifyGreen) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isDownloaded) "Downloaded (Tap to delete)" else if (isDownloading) "Downloading..." else "Download Song Offline", color = textColor) },
                                onClick = {
                                    showMenu = false
                                    currentTrack?.let { viewModel.toggleDownload(it) }
                                },
                                leadingIcon = {
                                    Icon(
                                        if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.DownloadForOffline,
                                        null,
                                        tint = if (isDownloaded) Color(0xFF00E676) else MusifyGreen
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Start Song Radio", color = textColor) },
                                onClick = {
                                    showMenu = false
                                    currentTrack?.let { viewModel.startSongRadio(it) }
                                },
                                leadingIcon = { Icon(Icons.Default.Podcasts, null, tint = MusifyGreen) }
                            )
                            if (isAdmin) {
                                DropdownMenuItem(
                                    text = { Text("Link / Edit Artist", color = textColor) },
                                    onClick = {
                                        showMenu = false
                                        showLinkArtistDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.PersonPin, null, tint = MusifyGreen) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("View Artist Profile", color = textColor) },
                                onClick = {
                                    showMenu = false
                                    currentTrack?.artist?.let { openArtistProfileOrChooser(it) }
                                },
                                leadingIcon = { Icon(Icons.Default.Person, null, tint = MusifyGreen) }
                            )
                            DropdownMenuItem(
                                text = { Text("Add to Playlist", color = textColor) },
                                onClick = { showMenu = false; showAddToPlaylistDialog = true },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = MusifyGreen) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (sleepTimerMillis > 0) "Sleep Timer (${sleepTimerMillis / 60000}m)" else "Sleep Timer", color = textColor) },
                                onClick = { showMenu = false; showSleepTimerDialog = true },
                                leadingIcon = { Icon(Icons.Default.Timer, null, tint = MusifyGreen) }
                            )
                            DropdownMenuItem(
                                text = { Text("Playback Speed", color = textColor) },
                                onClick = { showMenu = false; showSpeedDialog = true },
                                leadingIcon = { Icon(Icons.Default.Speed, null, tint = MusifyGreen) }
                            )
                            DropdownMenuItem(
                                text = { Text("Watch on YouTube", color = textColor) },
                                onClick = {
                                    showMenu = false
                                    currentTrack?.audioUrl?.let { url ->
                                        try {
                                             context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                                        } catch (_: Exception) {}
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.SmartDisplay, null, tint = MusifyGreen) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Album Art or Lyrics Panel ─────────────────────────
            if (showLyrics) {
                NowPlayingLyricsPanel(
                    viewModel = viewModel,
                    glassSurface = glassSurface,
                    glassBorder = glassBorder,
                    textColor = textColor
                )
            } else if (showVisualizer) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    color = if (isDark) Color(0xFF141420) else Color(0xFFEEF3F0),
                    border = BorderStroke(1.dp, glassBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        com.gaminghub.musicplayer.ui.components.DynamicAudioVisualizer(
                            isPlaying = isPlaying,
                            modifier = Modifier.fillMaxWidth().height(180.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (isPlaying) "AUDIO SPECTRUM ACTIVE" else "PAUSED",
                            color = if (isPlaying) MusifyGreen else textSecondaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            } else {
                var totalDragX by remember { mutableFloatStateOf(0f) }
                var totalDragY by remember { mutableFloatStateOf(0f) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(glassSurface)
                        .pointerInput(enableArtworkGestures, enableVolumeGestures) {
                            if (enableArtworkGestures) {
                                detectTapGestures(
                                    onTap = { showLyrics = !showLyrics },
                                    onDoubleTap = { viewModel.togglePlayPause() },
                                    onLongPress = { showVisualizer = !showVisualizer }
                                )
                            }
                        }
                        .pointerInput(enableArtworkGestures, enableVolumeGestures) {
                            detectDragGestures(
                                onDragStart = {
                                    totalDragX = 0f
                                    totalDragY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragX += dragAmount.x
                                    totalDragY += dragAmount.y

                                    if (enableVolumeGestures && kotlin.math.abs(totalDragY) > 35f) {
                                        if (totalDragY < -35f) {
                                            audioManager?.adjustStreamVolume(
                                                android.media.AudioManager.STREAM_MUSIC,
                                                android.media.AudioManager.ADJUST_RAISE,
                                                android.media.AudioManager.FLAG_SHOW_UI
                                            )
                                            totalDragY = 0f
                                        } else if (totalDragY > 35f) {
                                            audioManager?.adjustStreamVolume(
                                                android.media.AudioManager.STREAM_MUSIC,
                                                android.media.AudioManager.ADJUST_LOWER,
                                                android.media.AudioManager.FLAG_SHOW_UI
                                            )
                                            totalDragY = 0f
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (enableArtworkGestures && !enableVolumeGestures && kotlin.math.abs(totalDragX) > 60f) {
                                        if (totalDragX < -60f) {
                                            viewModel.playNext(fromUser = true)
                                        } else if (totalDragX > 60f) {
                                            viewModel.playPrevious()
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    com.gaminghub.musicplayer.ui.components.SongImage(
                        model = currentTrack?.albumArtUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Song Title, Artist & Quick Controls ──────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    IconButton(onClick = { currentTrack?.let { viewModel.toggleFavorite(it) } }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Favorite",
                            tint = if (isFavorite) MusifyGreen else textSecondaryColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    if (songLikes.isNotBlank()) {
                        Text(
                            text = songLikes,
                            color = if (isFavorite) MusifyGreen else textSecondaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            com.gaminghub.musicplayer.util.MusifyFileMetadataHelper.cleanDisplayTitle(currentTrack?.title ?: "Unknown Title"),
                            color = textColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentTrack?.isDevpick == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Verified Dev's Pick",
                                tint = MusifyGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    val artistDisplay = currentTrack?.artist?.let {
                        if (it.isBlank() || it == "<unknown>") "Offline Audio" else it
                    } ?: "Unknown Artist"
                    Text(
                        artistDisplay,
                        color = MusifyGreen.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable {
                            currentTrack?.artist?.let { openArtistProfileOrChooser(it) }
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF1B261F) else Color(0xFFE8F5E9),
                        border = BorderStroke(0.5.dp, MusifyGreen.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showEqualizerDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = MusifyGreen,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val isOff = isDownloaded
                            val qualityText = remember {
                                settingsPrefs.getString("streaming_quality", "320 kbps") ?: "320 kbps"
                            }
                            Text(
                                text = if (isOff) "OFFLINE • 320 KBPS" else "HQ • $qualityText".uppercase(),
                                color = MusifyGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(onClick = { currentTrack?.let { viewModel.toggleDownload(it) } }) {
                    if (isDownloading) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { (currentDownloadProgress?.progressPercent ?: 0) / 100f },
                                modifier = Modifier.size(26.dp),
                                color = MusifyGreen,
                                strokeWidth = 2.5.dp,
                                trackColor = if (isDark) Color(0xFF2B2B36) else Color(0xFFD6D6D6)
                            )
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel Download",
                                tint = textColor,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    } else {
                        Icon(
                            if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.DownloadForOffline,
                            "Download Song",
                            tint = if (isDownloaded) Color(0xFF00E676) else iconTint,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── Progress Slider & Timestamps ──────────────────────
            NowPlayingProgressSection(
                viewModel = viewModel,
                duration = duration,
                isDark = isDark,
                textSecondaryColor = textSecondaryColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Controls Row ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    isShuffleOn = !isShuffleOn
                    viewModel.setShuffleModeEnabled(isShuffleOn)
                }) {
                    Icon(
                        Icons.Default.Shuffle,
                        "Shuffle",
                        tint = if (isShuffleOn) MusifyGreen else textSecondaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = { viewModel.playPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = textColor, modifier = Modifier.size(38.dp))
                }
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MusifyGreen)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(35.dp)
                    )
                }
                IconButton(onClick = { viewModel.playNext() }) {
                    Icon(Icons.Default.SkipNext, "Next", tint = textColor, modifier = Modifier.size(38.dp))
                }
                IconButton(onClick = {
                    isRepeatOn = !isRepeatOn
                    viewModel.setRepeatMode(if (isRepeatOn) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF)
                }) {
                    Icon(
                        if (isRepeatOn) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        "Repeat",
                        tint = if (isRepeatOn) MusifyGreen else textSecondaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Quick Actions Row ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showAddToPlaylistDialog = true }) {
                    Icon(
                        Icons.AutoMirrored.Filled.PlaylistAdd,
                        "Add to Playlist",
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDownloaded) (if (isDark) Color(0x2200E676) else Color(0x2200C853)) else glassSurface,
                    border = BorderStroke(1.dp, if (isDownloaded) (if (isDark) Color(0x5500E676) else Color(0x6600C853)) else MusifyGreen.copy(alpha = 0.35f)),
                    modifier = Modifier.clickable { currentTrack?.let { viewModel.toggleDownload(it) } }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                progress = { (currentDownloadProgress?.progressPercent ?: 0) / 100f },
                                modifier = Modifier.size(16.dp),
                                color = MusifyGreen,
                                strokeWidth = 2.dp,
                                trackColor = if (isDark) Color(0xFF2B2B36) else Color(0xFFD6D6D6)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "${currentDownloadProgress?.progressPercent ?: 0}%",
                                color = MusifyGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.DownloadForOffline,
                                contentDescription = null,
                                tint = if (isDownloaded) (if (isDark) Color(0xFF00E676) else Color(0xFF008938)) else MusifyGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                if (isDownloaded) "Downloaded" else "Download",
                                color = if (isDownloaded) (if (isDark) Color(0xFF00E676) else Color(0xFF008938)) else textColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                IconButton(onClick = { showSleepTimerDialog = true }) {
                    Icon(
                        Icons.Default.Timer,
                        "Sleep Timer",
                        tint = if (sleepTimerMillis > 0) MusifyGreen else iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = glassSurface,
                    border = BorderStroke(1.dp, glassBorder),
                    modifier = Modifier.clickable { showSpeedDialog = true }
                ) {
                    Text(
                        text = "${String.format("%.2f", playbackSpeed).trimEnd('0').trimEnd('.')}x",
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (currentDownloadProgress != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = glassSurface,
                    border = BorderStroke(1.dp, MusifyGreen.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DownloadForOffline,
                                    contentDescription = null,
                                    tint = MusifyGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "${currentDownloadProgress.downloadedFormatted} / ${currentDownloadProgress.totalFormatted}",
                                    color = textColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                "${currentDownloadProgress.speedFormatted} (${currentDownloadProgress.progressPercent}%)",
                                color = MusifyGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (currentDownloadProgress.isIndeterminate) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MusifyGreen,
                                trackColor = if (isDark) Color(0xFF2B2B36) else Color(0xFFD6D6D6)
                            )
                        } else {
                            LinearProgressIndicator(
                                progress = { currentDownloadProgress.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MusifyGreen,
                                trackColor = if (isDark) Color(0xFF2B2B36) else Color(0xFFD6D6D6)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // ── Speed Dialog ──────────────────────────────────────────
    if (showSpeedDialog) {
        val isSpeedLocked by viewModel.isSpeedLocked.collectAsState()
        SpeedControlDialog(
            currentSpeed = playbackSpeed,
            isSpeedLocked = isSpeedLocked,
            onDismiss = { showSpeedDialog = false },
            onSpeedSet = { speed -> viewModel.setPlaybackSpeed(speed); showSpeedDialog = false },
            onSpeedLockToggle = { locked -> viewModel.setSpeedLock(locked) }
        )
    }

    // ── Add To Playlist Dialog ────────────────────────────────
    if (showAddToPlaylistDialog) {
        AddToPlaylistSimpleDialog(
            playlists = playlists,
            onDismiss = { showAddToPlaylistDialog = false },
            onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
            onSelectPlaylist = { playlist ->
                // Playlist added
                showAddToPlaylistDialog = false
            }
        )
    }

    // ── Sleep Timer Dialog ────────────────────────────────────
    if (showSleepTimerDialog) {
        SleepTimerSimpleDialog(
            currentRemainingMillis = sleepTimerMillis,
            onDismiss = { showSleepTimerDialog = false },
            onSetTimer = { minutes ->
                viewModel.setSleepTimer(minutes)
                showSleepTimerDialog = false
            },
            onCancelTimer = {
                viewModel.cancelSleepTimer()
                showSleepTimerDialog = false
            }
        )
    }

    // ── Artist Chooser Dialog (Multi-artist collaborations) ─────
    if (showArtistChooserDialog && collaboratingArtists.isNotEmpty()) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showArtistChooserDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E1E26),
                border = BorderStroke(1.dp, MusifyGlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Collaborating Artists",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select an artist to view profile & songs",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    collaboratingArtists.forEach { artistName ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF282834),
                            border = BorderStroke(0.5.dp, Color(0x33FFFFFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    showArtistChooserDialog = false
                                    selectedProfileArtist = artistName
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MusifyGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MusifyGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = artistName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MusifyGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.TextButton(
                        onClick = { showArtistChooserDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // ── Artist Profile Dialog ─────────────────────────────────
    if (selectedProfileArtist != null) {
        ArtistProfileDialog(
            artistName = selectedProfileArtist!!,
            viewModel = viewModel,
            onDismiss = { selectedProfileArtist = null }
        )
    } else if (showArtistDialog && currentTrack != null) {
        ArtistProfileDialog(
            artistName = currentTrack!!.artist,
            viewModel = viewModel,
            onDismiss = { showArtistDialog = false }
        )
    }

    // ── Link / Edit Artist Dialog ─────────────────────────────
    if (showLinkArtistDialog && currentTrack != null) {
        LinkArtistDialog(
            currentTrack = currentTrack!!,
            viewModel = viewModel,
            onDismiss = { showLinkArtistDialog = false }
        )
    }

    // ── Equalizer Dialog ──────────────────────────────────────
    if (showEqualizerDialog) {
        EqualizerSimpleDialog(
            onDismiss = { showEqualizerDialog = false }
        )
    }
}

private data class UpNextQueueItem(
    val id: String,
    val track: TrackModel
)

// ─────────────────────────────────────────────────────────────
// Up Next Panel — shown inside BottomSheetScaffold
// ─────────────────────────────────────────────────────────────
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun UpNextPanel(
    currentTrack: TrackModel?,
    upNextQueue: List<TrackModel>,
    viewModel: MusicViewModel,
    isDark: Boolean = true,
    onTrackClick: (Int) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onRemoveTrack: (Int) -> Unit
) {
    var localItems by remember {
        mutableStateOf(
            upNextQueue.mapIndexed { idx, track ->
                val stableKey = track.audioUrl ?: "${track.title}_${track.artist}"
                UpNextQueueItem(id = "${stableKey}_${idx}_${System.identityHashCode(track)}", track = track)
            }
        )
    }
    var isDragging by remember { mutableStateOf(false) }
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var initialDragIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val favoriteUrls by viewModel.favoriteUrls.collectAsState()

    val itemTextColor = if (isDark) Color.White else Color(0xFF191C1E)
    val itemSecondaryColor = if (isDark) Color(0xFFB3B3B3) else Color(0xFF5A6065)
    val itemIconTint = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF222628)

    LaunchedEffect(upNextQueue) {
        if (!isDragging) {
            localItems = upNextQueue.mapIndexed { idx, track ->
                val stableKey = track.audioUrl ?: "${track.title}_${track.artist}"
                UpNextQueueItem(id = "${stableKey}_${idx}_${System.identityHashCode(track)}", track = track)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.65f)
    ) {
        // ── Drag pill handle ──────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isDark) MusifyGlassSurface else Color(0xFFCCD3CF))
            )
        }

        // ── "Up Next" title ──────────────────────────────
        Text(
            "Up Next",
            color = itemTextColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (localItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No songs in queue", color = itemSecondaryColor, fontSize = 14.sp)
            }
        } else {
            // ── Queue list with drag-to-reorder ──────────────
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                itemsIndexed(localItems, key = { _, item -> item.id }) { index, item ->
                    val track = item.track
                    val isDraggingThis = isDragging && draggedItemIndex == index
                    val elevation by animateDpAsState(if (isDraggingThis) 8.dp else 0.dp, label = "elevation")
                    val itemFav = track.audioUrl != null && favoriteUrls.contains(track.audioUrl)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isDraggingThis) Modifier
                                    .zIndex(2f)
                                    .graphicsLayer { translationY = dragOffsetY }
                                    .shadow(elevation, RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color(0xFF282836) else Color(0xFFE2EBE5), RoundedCornerShape(8.dp))
                                else Modifier.background(Color.Transparent)
                            )
                            .clickable(enabled = !isDragging) { onTrackClick(index) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.gaminghub.musicplayer.ui.components.SongImage(
                            model = track.albumArtUrl,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                track.title,
                                color = itemTextColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                track.artist,
                                color = itemSecondaryColor,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleFavorite(track) },
                            modifier = Modifier.size(36.dp),
                            enabled = !isDragging
                        ) {
                            Icon(
                                if (itemFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                "Favorite",
                                tint = if (itemFav) Color(0xFF1DB954) else itemIconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Long-press remove button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .pointerInput(item.id) {
                                    detectTapGestures(
                                        onTap = {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Long press to remove from queue",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        onLongPress = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onRemoveTrack(index)
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Long press to remove",
                                tint = Color(0xFFFF6B6B).copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Drag Handle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .pointerInput(item.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            isDragging = true
                                            initialDragIndex = index
                                            draggedItemIndex = index
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y
                                            val rowHeight = with(density) { 68.dp.toPx() }
                                            val currentIdx = draggedItemIndex ?: return@detectDragGesturesAfterLongPress
                                            val deltaSteps = (dragOffsetY / rowHeight).roundToInt()
                                            if (deltaSteps != 0) {
                                                val targetIdx = (currentIdx + deltaSteps).coerceIn(0, localItems.size - 1)
                                                if (targetIdx != currentIdx) {
                                                    val mutable = localItems.toMutableList()
                                                    val movedItem = mutable.removeAt(currentIdx)
                                                    mutable.add(targetIdx, movedItem)
                                                    localItems = mutable
                                                    dragOffsetY -= (targetIdx - currentIdx) * rowHeight
                                                    draggedItemIndex = targetIdx
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            val from = initialDragIndex
                                            val to = draggedItemIndex
                                            if (from != null && to != null && from != to) {
                                                onReorder(from, to)
                                            }
                                            isDragging = false
                                            draggedItemIndex = null
                                            initialDragIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            isDragging = false
                                            draggedItemIndex = null
                                            initialDragIndex = null
                                            dragOffsetY = 0f
                                            localItems = upNextQueue.mapIndexed { idx, track ->
                                                val stableKey = track.audioUrl ?: "${track.title}_${track.artist}"
                                                UpNextQueueItem(id = "${stableKey}_${idx}_${System.identityHashCode(track)}", track = track)
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DragHandle,
                                "Reorder",
                                tint = if (isDraggingThis) MusifyGreen else itemIconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Add To Playlist Dialog
// ─────────────────────────────────────────────────────────────
@Composable
fun AddToPlaylistSimpleDialog(
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onSelectPlaylist: (PlaylistEntity) -> Unit
) {
    var newPlaylistName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF1E1E28)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Add to Playlist", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                if (isCreating) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist Name", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MusifyGreen,
                            cursorColor = MusifyGreen
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { isCreating = false }) { Text("Cancel", color = Color.Gray) }
                        Button(
                            onClick = {
                                if (newPlaylistName.isNotBlank()) {
                                    onCreatePlaylist(newPlaylistName)
                                    isCreating = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen)
                        ) {
                            Text("Create")
                        }
                    }
                } else {
                    Button(
                        onClick = { isCreating = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Playlist")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (playlists.isEmpty()) {
                        Text("No playlists created yet", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp))
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(playlists) { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onSelectPlaylist(playlist) }
                                        .padding(vertical = 10.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = MusifyGreen)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(playlist.name, color = Color.White, fontSize = 15.sp)
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("Close", color = Color.Gray) }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Sleep Timer Dialog
// ─────────────────────────────────────────────────────────────
@Composable
fun SleepTimerSimpleDialog(
    currentRemainingMillis: Long,
    onDismiss: () -> Unit,
    onSetTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit
) {
    val options = listOf(5, 10, 15, 30, 45, 60)

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF1E1E28)) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Sleep Timer", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                if (currentRemainingMillis > 0) {
                    val remainingMins = (currentRemainingMillis / 1000) / 60
                    val remainingSecs = (currentRemainingMillis / 1000) % 60
                    Text(
                        text = "Active: ${String.format("%02d:%02d", remainingMins, remainingSecs)} remaining",
                        color = MusifyGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onCancelTimer,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Turn Off Timer")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text("Stop audio in:", color = Color.LightGray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))

                options.chunked(3).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowOptions.forEach { mins ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MusifyGlassSurface,
                                border = BorderStroke(1.dp, MusifyGlassBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSetTimer(mins) }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                    Text("$mins min", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close", color = Color.Gray)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Speed Control Dialog
// ─────────────────────────────────────────────────────────────
@Composable
fun SpeedControlDialog(
    currentSpeed: Float,
    isSpeedLocked: Boolean,
    onDismiss: () -> Unit,
    onSpeedSet: (Float) -> Unit,
    onSpeedLockToggle: (Boolean) -> Unit
) {
    var sliderSpeed by remember { mutableFloatStateOf(currentSpeed) }
    var localSpeedLocked by remember { mutableStateOf(isSpeedLocked) }
    val row1 = listOf(0.50f, 0.75f, 1.0f, 1.25f)
    val row2 = listOf(1.50f, 1.75f, 2.0f)

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF1E1E28)) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Playback Speed", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                Text("${String.format("%.2f", sliderSpeed)}x", color = MusifyGreen, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                Slider(
                    value = sliderSpeed,
                    onValueChange = { v ->
                        sliderSpeed = (Math.round(v / 0.05f) * 0.05f).coerceIn(0.50f, 2.0f)
                    },
                    valueRange = 0.50f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = MusifyGreen,
                        activeTrackColor = MusifyGreen,
                        inactiveTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0.50x", color = Color.Gray, fontSize = 11.sp)
                    Text("2.00x", color = Color.Gray, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Preset Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row1.forEach { preset ->
                        val active = Math.abs(sliderSpeed - preset) < 0.01f
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (active) MusifyGreen else MusifyGlassSurface,
                            border = BorderStroke(1.dp, if (active) MusifyGreen else MusifyGlassBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { sliderSpeed = preset }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    "${String.format("%.2f", preset).trimEnd('0').trimEnd('.')}x",
                                    color = if (active) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Preset Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row2.forEach { preset ->
                        val active = Math.abs(sliderSpeed - preset) < 0.01f
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (active) MusifyGreen else MusifyGlassSurface,
                            border = BorderStroke(1.dp, if (active) MusifyGreen else MusifyGlassBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { sliderSpeed = preset }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    "${String.format("%.2f", preset).trimEnd('0').trimEnd('.')}x",
                                    color = if (active) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Speed Lock Row
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF262636),
                    border = BorderStroke(1.dp, if (localSpeedLocked) MusifyGreen.copy(alpha = 0.5f) else MusifyGlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Speed Lock",
                                tint = if (localSpeedLocked) MusifyGreen else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Speed Lock",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Remember speed across songs & app restarts",
                                    color = Color.LightGray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Switch(
                            checked = localSpeedLocked,
                            onCheckedChange = { localSpeedLocked = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = MusifyGreen,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0xFF3B3B4D)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSpeedLockToggle(localSpeedLocked)
                            onSpeedSet(sliderSpeed)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen)
                    ) {
                        Text("Apply", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NowPlayingProgressSection(
    viewModel: MusicViewModel,
    duration: Long,
    isDark: Boolean,
    textSecondaryColor: Color,
    modifier: Modifier = Modifier
) {
    val currentPosition by viewModel.currentPosition.collectAsState()

    Slider(
        value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
        onValueChange = { if (duration > 0) viewModel.seekTo((it * duration).toLong()) },
        thumb = {
            Box(
                modifier = Modifier.size(width = 12.dp, height = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MusifyGreen)
                )
            }
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(4.dp),
                colors = SliderDefaults.colors(
                    activeTrackColor = MusifyGreen,
                    inactiveTrackColor = if (isDark) Color(0x33FFFFFF) else Color(0x22000000)
                ),
                thumbTrackGapSize = 0.dp,
                drawStopIndicator = null
            )
        },
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(formatTime(currentPosition), color = textSecondaryColor, fontSize = 12.sp)
        Text(formatTime(duration), color = textSecondaryColor, fontSize = 12.sp)
    }
}

@Composable
private fun NowPlayingLyricsPanel(
    viewModel: MusicViewModel,
    glassSurface: Color,
    glassBorder: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val syncedLyrics by viewModel.syncedLyrics.collectAsState()
    val plainLyrics by viewModel.plainLyrics.collectAsState()
    val currentLyricIndex by viewModel.currentLyricIndex.collectAsState()

    Surface(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp)),
        color = glassSurface,
        border = BorderStroke(1.dp, glassBorder)
    ) {
        if (syncedLyrics.isNotEmpty()) {
            val lyricListState = rememberLazyListState()

            LaunchedEffect(currentLyricIndex) {
                if (currentLyricIndex >= 0 && currentLyricIndex < syncedLyrics.size) {
                    lyricListState.animateScrollToItem(
                        index = (currentLyricIndex - 1).coerceAtLeast(0)
                    )
                }
            }

            LazyColumn(
                state = lyricListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 40.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(syncedLyrics) { index, line ->
                    val isActive = index == currentLyricIndex
                    val alpha by animateFloatAsState(if (isActive) 1f else 0.4f, label = "alpha")
                    val scale by animateFloatAsState(if (isActive) 1.08f else 1.0f, label = "scale")

                    Surface(
                        color = if (isActive) MusifyGreen.copy(alpha = 0.12f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.seekTo(line.timeMs) }
                    ) {
                        Text(
                            text = line.text,
                            color = if (isActive) MusifyGreen else textColor,
                            fontSize = if (isActive) 19.sp else 17.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                }
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = plainLyrics ?: "Searching for lyrics...",
                    color = textColor.copy(alpha = 0.9f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}


