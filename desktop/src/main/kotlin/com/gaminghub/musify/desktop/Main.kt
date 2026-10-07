package com.gaminghub.musify.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.gaminghub.musify.desktop.ui.DesktopApp
import com.gaminghub.musify.desktop.viewmodel.DesktopMusicViewModel
import java.awt.Dimension

fun main() = application {
    val viewModel = remember { DesktopMusicViewModel() }
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isMiniPlayer by viewModel.isMiniPlayer.collectAsState()

    var isWindowVisible by remember { mutableStateOf(true) }

    // Window size adapts between full desktop (1240x820) and mini player (320x420)
    val windowState = rememberWindowState(
        size = if (isMiniPlayer) DpSize(320.dp, 420.dp) else DpSize(1240.dp, 820.dp)
    )

    LaunchedEffect(isMiniPlayer) {
        windowState.size = if (isMiniPlayer) DpSize(320.dp, 420.dp) else DpSize(1240.dp, 820.dp)
    }

    // Taskbar System Tray
    Tray(
        icon = painterResource("icon.png"),
        tooltip = if (currentTrack != null) "Musify: ${currentTrack?.title} - ${currentTrack?.artist}" else "Musify Windows Player",
        menu = {
            Item(
                text = if (isPlaying) "Pause" else "Play",
                onClick = { viewModel.togglePlayPause() }
            )
            Item(
                text = "Next Track",
                onClick = { viewModel.playNext() }
            )
            Item(
                text = "Previous Track",
                onClick = { viewModel.playPrevious() }
            )
            Separator()
            Item(
                text = if (isWindowVisible) "Hide to Tray" else "Restore Window",
                onClick = { isWindowVisible = !isWindowVisible }
            )
            Separator()
            Item(
                text = "Exit Musify",
                onClick = {
                    viewModel.audioPlayer.release()
                    exitApplication()
                }
            )
        }
    )

    if (isWindowVisible) {
        val windowTitle = if (currentTrack != null) {
            "${currentTrack?.title} • ${currentTrack?.artist} - Musify"
        } else {
            "Musify - Windows Music Player"
        }

        Window(
            onCloseRequest = {
                viewModel.audioPlayer.release()
                exitApplication()
            },
            state = windowState,
            title = windowTitle,
            icon = painterResource("icon.png"),
            alwaysOnTop = isMiniPlayer
        ) {
            window.minimumSize = if (isMiniPlayer) Dimension(300, 380) else Dimension(900, 600)
            DesktopApp(viewModel)
        }
    }
}
