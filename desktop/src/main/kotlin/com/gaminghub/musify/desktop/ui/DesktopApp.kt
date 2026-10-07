package com.gaminghub.musify.desktop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaminghub.musify.desktop.model.DesktopScreen
import com.gaminghub.musify.desktop.ui.components.*
import com.gaminghub.musify.desktop.ui.screens.*
import com.gaminghub.musify.desktop.ui.theme.*
import com.gaminghub.musify.desktop.viewmodel.DesktopMusicViewModel
import kotlinx.coroutines.delay

@Composable
fun DesktopApp(viewModel: DesktopMusicViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isLyricsOpen by viewModel.isLyricsOpen.collectAsState()
    val isQueueOpen by viewModel.isQueueOpen.collectAsState()
    val isEqualizerOpen by viewModel.isEqualizerOpen.collectAsState()
    val isMiniPlayer by viewModel.isMiniPlayer.collectAsState()

    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.onShowToast = { msg ->
            toastMessage = msg
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3000)
            toastMessage = null
        }
    }

    MusifyDesktopTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when {
                            event.key == Key.Spacebar -> {
                                viewModel.togglePlayPause()
                                true
                            }
                            event.key == Key.DirectionRight && event.isCtrlPressed -> {
                                viewModel.playNext()
                                true
                            }
                            event.key == Key.DirectionLeft && event.isCtrlPressed -> {
                                viewModel.playPrevious()
                                true
                            }
                            event.key == Key.DirectionRight -> {
                                viewModel.seekTo(viewModel.positionMs.value + 10_000L)
                                true
                            }
                            event.key == Key.DirectionLeft -> {
                                viewModel.seekTo((viewModel.positionMs.value - 10_000L).coerceAtLeast(0L))
                                true
                            }
                            event.key == Key.DirectionUp -> {
                                viewModel.setVolume((viewModel.volume.value + 0.05f).coerceAtMost(1f))
                                true
                            }
                            event.key == Key.DirectionDown -> {
                                viewModel.setVolume((viewModel.volume.value - 0.05f).coerceAtLeast(0f))
                                true
                            }
                            event.key == Key.M -> {
                                viewModel.toggleMute()
                                true
                            }
                            event.key == Key.L -> {
                                viewModel.toggleLyrics()
                                true
                            }
                            event.key == Key.Q -> {
                                viewModel.toggleQueue()
                                true
                            }
                            event.key == Key.E -> {
                                viewModel.toggleEqualizer()
                                true
                            }
                            event.key == Key.P -> {
                                viewModel.toggleMiniPlayer()
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            if (isMiniPlayer) {
                DesktopMiniPlayer(viewModel)
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Main Content Area (Sidebar + Active Screen + Side Panels)
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        DesktopSidebar(viewModel)

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            // Active screen
                            when (currentScreen) {
                                DesktopScreen.HOME -> DesktopHomeScreen(viewModel)
                                DesktopScreen.SEARCH -> DesktopSearchScreen(viewModel)
                                DesktopScreen.LIBRARY -> DesktopLibraryScreen(viewModel)
                                DesktopScreen.PLAYLIST_DETAIL -> DesktopPlaylistDetailScreen(viewModel)
                                DesktopScreen.DOWNLOADS -> DesktopDownloadsScreen(viewModel)
                                DesktopScreen.DEV_PICKS -> DesktopDevPicksScreen(viewModel)
                                DesktopScreen.EQUALIZER -> DesktopEqualizerDialog(viewModel)
                                DesktopScreen.SETTINGS -> DesktopSettingsScreen(viewModel)
                            }

                            // Lyrics Overlay
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isLyricsOpen,
                                enter = fadeIn() + slideInVertically { it / 2 },
                                exit = fadeOut() + slideOutVertically { it / 2 }
                            ) {
                                DesktopLyricsView(viewModel)
                            }
                        }

                        // Queue Drawer
                        this@Row.AnimatedVisibility(
                            visible = isQueueOpen,
                            enter = slideInHorizontally { it },
                            exit = slideOutHorizontally { it }
                        ) {
                            DesktopQueueView(viewModel)
                        }
                    }

                    // Bottom Persistent Player Bar
                    DesktopPlayerBar(viewModel)
                }

                // Equalizer Modal Dialog
                if (isEqualizerOpen) {
                    DesktopEqualizerDialog(viewModel)
                }
            }

            // Desktop Toast Notification
            if (toastMessage != null) {
                Surface(
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 20.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Text(
                        text = toastMessage!!,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
