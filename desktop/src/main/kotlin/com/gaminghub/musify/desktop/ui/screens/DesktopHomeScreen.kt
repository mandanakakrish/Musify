package com.gaminghub.musify.desktop.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaminghub.musify.desktop.model.CuratedCardData
import com.gaminghub.musify.desktop.model.CuratedPlaylistConfig
import com.gaminghub.musify.desktop.model.DesktopScreen
import com.gaminghub.musify.desktop.network.rememberAsyncImage
import com.gaminghub.musify.desktop.ui.theme.*
import com.gaminghub.musify.desktop.viewmodel.DesktopMusicViewModel
import java.util.Calendar

@Composable
fun DesktopHomeScreen(viewModel: DesktopMusicViewModel) {
    val greeting = rememberGreeting()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // Hero Greeting
        item {
            Column {
                Text(
                    text = greeting,
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Discover millions of songs with crystal clear audio",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }

        // Featured Playlists
        item {
            CuratedSection(
                title = "Featured Hits",
                items = CuratedPlaylistConfig.featuredPlaylists,
                onCardClick = { card ->
                    viewModel.navigateTo(DesktopScreen.SEARCH)
                    viewModel.search(card.searchQuery)
                }
            )
        }

        // India's Biggest Hits
        item {
            CuratedSection(
                title = "India's Biggest Hits",
                items = CuratedPlaylistConfig.indiaBiggestHits,
                onCardClick = { card ->
                    viewModel.navigateTo(DesktopScreen.SEARCH)
                    viewModel.search(card.searchQuery)
                }
            )
        }

        // Community Favorites
        item {
            CuratedSection(
                title = "Community Favorites",
                items = CuratedPlaylistConfig.communityPlaylists,
                onCardClick = { card ->
                    viewModel.navigateTo(DesktopScreen.SEARCH)
                    viewModel.search(card.searchQuery)
                }
            )
        }

        // Dancing On Your Own
        item {
            CuratedSection(
                title = "Dancing On Your Own",
                items = CuratedPlaylistConfig.dancingOnYourOwn,
                onCardClick = { card ->
                    viewModel.navigateTo(DesktopScreen.SEARCH)
                    viewModel.search(card.searchQuery)
                }
            )
        }

        // Nostalgic Hits
        item {
            CuratedSection(
                title = "Nostalgic Hits & Classics",
                items = CuratedPlaylistConfig.nostalgicHits,
                onCardClick = { card ->
                    viewModel.navigateTo(DesktopScreen.SEARCH)
                    viewModel.search(card.searchQuery)
                }
            )
        }

        // New Releases
        item {
            CuratedSection(
                title = "Fresh Releases",
                items = CuratedPlaylistConfig.newReleases,
                onCardClick = { card ->
                    viewModel.navigateTo(DesktopScreen.SEARCH)
                    viewModel.search(card.searchQuery)
                }
            )
        }

        // Albums & Singles
        item {
            CuratedSection(
                title = "Chill, Lo-Fi & Soul",
                items = CuratedPlaylistConfig.albumsAndSingles,
                onCardClick = { card ->
                    viewModel.navigateTo(DesktopScreen.SEARCH)
                    viewModel.search(card.searchQuery)
                }
            )
        }
    }
}

@Composable
private fun CuratedSection(
    title: String,
    items: List<CuratedCardData>,
    onCardClick: (CuratedCardData) -> Unit
) {
    Column {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items) { card ->
                CuratedCard(card = card, onClick = { onCardClick(card) })
            }
        }
    }
}

@Composable
private fun CuratedCard(
    card: CuratedCardData,
    onClick: () -> Unit
) {
    Surface(
        color = SurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(170.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(146.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                val bitmap = rememberAsyncImage(card.imageUrl)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = card.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(SurfaceHighlight),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MusifyEmerald, modifier = Modifier.size(24.dp))
                    }
                }

                // Play icon badge overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MusifyEmerald),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = card.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            Text(
                text = card.subtitle,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

private fun rememberGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
}
