package com.gaminghub.musify.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.gaminghub.musify.desktop.ui.DesktopApp
import com.gaminghub.musify.desktop.ui.components.DesktopCustomTitleBar
import com.gaminghub.musify.desktop.ui.components.attachWindowResizer
import com.gaminghub.musify.desktop.viewmodel.DesktopMusicViewModel
import java.awt.Dimension
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun setupFileLogging() {
    try {
        val userHome = System.getProperty("user.home")
        val musifyDir = File(userHome, ".musify")
        if (!musifyDir.exists()) musifyDir.mkdirs()
        val logFile = File(musifyDir, "musify.log")

        val fileOut = FileOutputStream(logFile, true)
        val origOut = System.out
        val origErr = System.err
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

        class DualOutputStream(private val primary: OutputStream, private val secondary: OutputStream) : OutputStream() {
            override fun write(b: Int) {
                primary.write(b)
                secondary.write(b)
            }
            override fun write(b: ByteArray, off: Int, len: Int) {
                primary.write(b, off, len)
                secondary.write(b, off, len)
            }
            override fun flush() {
                primary.flush()
                secondary.flush()
            }
        }

        val dualOut = PrintStream(DualOutputStream(origOut, fileOut), true)
        val dualErr = PrintStream(DualOutputStream(origErr, fileOut), true)

        System.setOut(dualOut)
        System.setErr(dualErr)

        println("\n=== Musify Windows Player Started at ${dateFormat.format(Date())} ===")
    } catch (e: Exception) {
        System.err.println("Failed to setup file logging: ${e.message}")
    }
}

fun main() {
    System.setProperty("java.net.preferIPv4Stack", "true")
    System.setProperty("java.net.preferIPv4Addresses", "true")
    setupFileLogging()

    application {
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
            icon = painterResource("icon.webp"),
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
                icon = painterResource("icon.webp"),
                undecorated = true, // Custom black title bar with Musify Emerald green text
                alwaysOnTop = isMiniPlayer
            ) {
                window.minimumSize = if (isMiniPlayer) Dimension(300, 380) else Dimension(900, 600)

                LaunchedEffect(window) {
                    attachWindowResizer(window) { windowState.placement == WindowPlacement.Maximized }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Sleek Black Title Bar with Musify Emerald Branding & Controls
                    DesktopCustomTitleBar(
                        windowState = windowState,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        onClose = {
                            viewModel.audioPlayer.release()
                            exitApplication()
                        }
                    )

                    // Musify Desktop App Content
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        DesktopApp(viewModel)
                    }
                }
            }
        }
    }
}
