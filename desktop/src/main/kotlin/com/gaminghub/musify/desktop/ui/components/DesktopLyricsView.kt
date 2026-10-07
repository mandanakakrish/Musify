package com.gaminghub.musify.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaminghub.musify.desktop.ui.theme.*
import com.gaminghub.musify.desktop.viewmodel.DesktopMusicViewModel

@Composable
fun DesktopLyricsView(viewModel: DesktopMusicViewModel) {
    val lyricsData by viewModel.lyrics.collectAsState()
    val activeIndex by viewModel.currentLyricIndex.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to active line
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            val targetIndex = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark.copy(alpha = 0.95f))
            .padding(24.dp)
    ) {
        // Close button
        IconButton(
            onClick = { viewModel.toggleLyrics() },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Lyrics",
                tint = TextPrimary
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = currentTrack?.title ?: "Lyrics",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = currentTrack?.artist ?: "",
                color = MusifyEmerald,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            when {
                lyricsData == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MusifyEmerald)
                    }
                }
                lyricsData?.isInstrumental == true -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MusifyEmerald,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "This track is instrumental",
                                color = TextSecondary,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
                lyricsData?.syncedLyrics?.isNotEmpty() == true -> {
                    val syncedList = lyricsData!!.syncedLyrics
                    LazyColumn(
                        state = listState,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        itemsIndexed(syncedList) { index, line ->
                            val isCurrent = index == activeIndex
                            val color = if (isCurrent) TextPrimary else TextTertiary
                            val size = if (isCurrent) 24.sp else 18.sp
                            val weight = if (isCurrent) FontWeight.Bold else FontWeight.Normal

                            Text(
                                text = line.text,
                                color = color,
                                fontSize = size,
                                fontWeight = weight,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.seekTo(line.timeMs) }
                                    .padding(vertical = 10.dp, horizontal = 16.dp)
                            )
                        }
                    }
                }
                !lyricsData?.plainLyrics.isNullOrBlank() -> {
                    LazyColumn(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        item {
                            Text(
                                text = lyricsData!!.plainLyrics!!,
                                color = TextSecondary,
                                fontSize = 16.sp,
                                lineHeight = 28.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No lyrics available for this song",
                            color = TextSecondary,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
