package com.gaminghub.musicplayer.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.gaminghub.musicplayer.MusicViewModel
import com.gaminghub.musicplayer.ui.components.ArtistImage
import com.gaminghub.musicplayer.ui.theme.MusifyGlassBorder
import com.gaminghub.musicplayer.ui.theme.MusifyGlassSurface
import com.gaminghub.musicplayer.ui.theme.MusifyGreen

data class DiscoverArtist(
    val name: String,
    val genre: String,
    val category: String, // "Bollywood", "Punjabi", "Pop", "Hip-Hop", "Indie", "South Indian"
    val imageUrl: String,
    val isFeatured: Boolean = false,
    val tagline: String = ""
)

private val CURATED_ARTISTS = listOf(
    // ── Bollywood & Melodic ──────────────────────────
    DiscoverArtist(
        name = "Arijit Singh",
        genre = "Bollywood • Romance",
        category = "Bollywood",
        imageUrl = "https://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_500x500.jpg",
        isFeatured = true,
        tagline = "The voice of romance & soul"
    ),
    DiscoverArtist(
        name = "Shreya Ghoshal",
        genre = "Bollywood • Classical",
        category = "Bollywood",
        imageUrl = "https://c.saavncdn.com/artists/Shreya_Ghoshal_007_20241101074144_500x500.jpg",
        tagline = "Melody queen of Indian cinema"
    ),
    DiscoverArtist(
        name = "Atif Aslam",
        genre = "Bollywood • Sufi",
        category = "Bollywood",
        imageUrl = "https://c.saavncdn.com/artists/Atif_Aslam_500x500.jpg",
        tagline = "Soul-stirring romantic ballads"
    ),
    DiscoverArtist(
        name = "Jubin Nautiyal",
        genre = "Bollywood • Devotional & Pop",
        category = "Bollywood",
        imageUrl = "https://c.saavncdn.com/artists/Jubin_Nautiyal_003_20231130204020_500x500.jpg",
        tagline = "Pure melodies & trending chart-toppers"
    ),
    DiscoverArtist(
        name = "Sonu Nigam",
        genre = "Bollywood • Evergreen",
        category = "Bollywood",
        imageUrl = "https://c.saavncdn.com/artists/Sonu_Nigam_003_20260813182013_500x500.jpg",
        tagline = "Timeless playback maestro"
    ),
    DiscoverArtist(
        name = "Sunidhi Chauhan",
        genre = "Bollywood • Energetic Pop",
        category = "Bollywood",
        imageUrl = "https://c.saavncdn.com/artists/Sunidhi_Chauhan_005_20250515061617_500x500.jpg",
        tagline = "Powerhouse female vocals"
    ),
    DiscoverArtist(
        name = "Mohit Chauhan",
        genre = "Bollywood • Mountain Folk",
        category = "Bollywood",
        imageUrl = "https://c.saavncdn.com/artists/Mohit_Chauhan_500x500.jpg",
        tagline = "Acoustic tranquility & nostalgia"
    ),

    // ── Punjabi & Desi Beats ─────────────────────────
    DiscoverArtist(
        name = "Diljit Dosanjh",
        genre = "Punjabi • Global Pop",
        category = "Punjabi",
        imageUrl = "https://c.saavncdn.com/artists/Diljit_Dosanjh_005_20231025073054_500x500.jpg",
        isFeatured = true,
        tagline = "Taking Punjabi music worldwide"
    ),
    DiscoverArtist(
        name = "Karan Aujla",
        genre = "Punjabi • Hip-Hop",
        category = "Punjabi",
        imageUrl = "https://c.saavncdn.com/artists/Karan_Aujla_004_20260810121947_500x500.jpg",
        tagline = "Geetan di machine"
    ),
    DiscoverArtist(
        name = "AP Dhillon",
        genre = "Punjabi • Synthwave Pop",
        category = "Punjabi",
        imageUrl = "https://c.saavncdn.com/artists/AP_Dhillon_004_20251023102150_500x500.jpg",
        tagline = "Smooth neo-Punjabi soundscapes"
    ),
    DiscoverArtist(
        name = "Sidhu Moose Wala",
        genre = "Punjabi • Folk Rap",
        category = "Punjabi",
        imageUrl = "https://c.saavncdn.com/artists/Sidhu_Moose_Wala_004_20250617183705_500x500.jpg",
        tagline = "The immortal legend"
    ),
    DiscoverArtist(
        name = "Shubh",
        genre = "Punjabi • Trap & R&B",
        category = "Punjabi",
        imageUrl = "https://c.saavncdn.com/artists/Shubh_000_20220921112507_500x500.jpg",
        tagline = "Fresh viral bangers"
    ),
    DiscoverArtist(
        name = "Guru Randhawa",
        genre = "Punjabi • Party Pop",
        category = "Punjabi",
        imageUrl = "https://c.saavncdn.com/artists/Guru_Randhawa_004_20250701125845_500x500.jpg",
        tagline = "High-energy dance floor anthems"
    ),
    DiscoverArtist(
        name = "B Praak",
        genre = "Punjabi • Emotional Ballads",
        category = "Punjabi",
        imageUrl = "https://c.saavncdn.com/artists/B_Praak_001_20191118112005_500x500.jpg",
        tagline = "Heart-wrenching soul anthems"
    ),

    // ── Pop & Global Hits ────────────────────────────
    DiscoverArtist(
        name = "The Weeknd",
        genre = "Pop • R&B & Synthwave",
        category = "Pop",
        imageUrl = "https://c.saavncdn.com/artists/The_Weeknd_002_20241003071400_500x500.jpg",
        isFeatured = true,
        tagline = "Dark pop & cinematic anthems"
    ),
    DiscoverArtist(
        name = "Taylor Swift",
        genre = "Pop • Country & Folk",
        category = "Pop",
        imageUrl = "https://c.saavncdn.com/artists/Taylor_Swift_003_20200226074119_500x500.jpg",
        tagline = "Record-breaking storytelling icon"
    ),
    DiscoverArtist(
        name = "Drake",
        genre = "Hip-Hop • Global R&B",
        category = "Pop",
        imageUrl = "https://c.saavncdn.com/artists/Drake_006_20260520062317_500x500.jpg",
        tagline = "Certified chart titan"
    ),
    DiscoverArtist(
        name = "Ed Sheeran",
        genre = "Pop • Acoustic Singer-Songwriter",
        category = "Pop",
        imageUrl = "https://c.saavncdn.com/artists/Ed_Sheeran_002_20250625073038_500x500.jpg",
        tagline = "Acoustic loops & worldwide stadium hits"
    ),
    DiscoverArtist(
        name = "Justin Bieber",
        genre = "Pop • Contemporary R&B",
        category = "Pop",
        imageUrl = "https://c.saavncdn.com/artists/Justin_Bieber_005_20201127112218_500x500.jpg",
        tagline = "Unstoppable global hitmaker"
    ),
    DiscoverArtist(
        name = "Dua Lipa",
        genre = "Pop • Disco & Dance",
        category = "Pop",
        imageUrl = "https://c.saavncdn.com/artists/Dua_Lipa_004_20231120090922_500x500.jpg",
        tagline = "Futuristic disco nostalgia"
    ),
    DiscoverArtist(
        name = "Billie Eilish",
        genre = "Pop • Alternative & Indie",
        category = "Pop",
        imageUrl = "https://c.saavncdn.com/artists/Billie_Eilish_20190211151539_500x500.jpg",
        tagline = "Intimate vocals & boundary-pushing bass"
    ),

    // ── Hip-Hop & Rap ────────────────────────────────
    DiscoverArtist(
        name = "Badshah",
        genre = "Hip-Hop • Commercial Desi Rap",
        category = "Hip-Hop",
        imageUrl = "https://c.saavncdn.com/artists/Badshah_006_20241118064015_500x500.jpg",
        isFeatured = true,
        tagline = "Pioneer of Indian club anthems"
    ),
    DiscoverArtist(
        name = "Divine",
        genre = "Hip-Hop • Mumbai Gully Rap",
        category = "Hip-Hop",
        imageUrl = "https://c.saavncdn.com/artists/DIVINE_006_20250911071442_500x500.jpg",
        tagline = "From the streets to the world stage"
    ),
    DiscoverArtist(
        name = "Seedhe Maut",
        genre = "Hip-Hop • DHH Underground",
        category = "Hip-Hop",
        imageUrl = "https://c.saavncdn.com/artists/Seedhe_Maut_004_20250527085239_500x500.jpg",
        tagline = "Raw energy & intricate rhymes"
    ),
    DiscoverArtist(
        name = "King",
        genre = "Hip-Hop • Melodic Pop Rap",
        category = "Hip-Hop",
        imageUrl = "https://c.saavncdn.com/734/Champagne-Talk-Hindi-2022-20221008011951-500x500.jpg",
        tagline = "Smooth romantic rap ballads"
    ),
    DiscoverArtist(
        name = "KR\$NA",
        genre = "Hip-Hop • Technical Lyricism",
        category = "Hip-Hop",
        imageUrl = "https://c.saavncdn.com/artists/Krsna_Solo_003_20230223094118_500x500.jpg",
        tagline = "Unrivaled complex bars & punchlines"
    ),
    DiscoverArtist(
        name = "MC Stan",
        genre = "Hip-Hop • New School Trap",
        category = "Hip-Hop",
        imageUrl = "https://c.saavncdn.com/artists/MC_STAN_001_20240315074305_500x500.jpg",
        tagline = "P-Town hip-hop phenomenon"
    ),

    // ── Indie & Acoustic ─────────────────────────────
    DiscoverArtist(
        name = "Anuv Jain",
        genre = "Indie • Acoustic Storytelling",
        category = "Indie",
        imageUrl = "https://c.saavncdn.com/artists/Anuv_Jain_001_20231206073013_500x500.jpg",
        isFeatured = true,
        tagline = "Heartfelt acoustic confessions"
    ),
    DiscoverArtist(
        name = "Prateek Kuhad",
        genre = "Indie • Folk Pop",
        category = "Indie",
        imageUrl = "https://c.saavncdn.com/artists/Prateek_Kuhad_006_20260515064251_500x500.jpg",
        tagline = "Warm acoustic serenades"
    ),
    DiscoverArtist(
        name = "Zaeden",
        genre = "Indie • Acoustic & Electronic",
        category = "Indie",
        imageUrl = "https://c.saavncdn.com/artists/Zaeden_007_20240819092729_500x500.jpg",
        tagline = "Catchy indie pop melodies"
    ),
    DiscoverArtist(
        name = "The Local Train",
        genre = "Indie • Hindi Rock",
        category = "Indie",
        imageUrl = "https://c.saavncdn.com/artists/The_Local_Train_004_20251031175921_500x500.jpg",
        tagline = "Iconic Hindi indie rock anthems"
    ),
    DiscoverArtist(
        name = "Ritviz",
        genre = "Indie • Classical Electronic Fusion",
        category = "Indie",
        imageUrl = "https://c.saavncdn.com/artists/Ritviz_500x500.jpg",
        tagline = "Unique bass music with Indian folk"
    ),

    // ── South Indian & Regional ──────────────────────
    DiscoverArtist(
        name = "Anirudh Ravichander",
        genre = "South • High Energy Fusion",
        category = "South Indian",
        imageUrl = "https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg",
        isFeatured = true,
        tagline = "Rockstar viral musical sensation"
    ),
    DiscoverArtist(
        name = "Sid Sriram",
        genre = "South • Carnatic & Contemporary",
        category = "South Indian",
        imageUrl = "https://c.saavncdn.com/artists/Sid_Sriram_005_20240425180600_500x500.jpg",
        tagline = "Mesmerizing soulful vocal mastery"
    ),
    DiscoverArtist(
        name = "A.R. Rahman",
        genre = "South • Orchestral World Music",
        category = "South Indian",
        imageUrl = "https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg",
        tagline = "Two-time Oscar winning musical genius"
    ),
    DiscoverArtist(
        name = "Devi Sri Prasad",
        genre = "South • High Octane Beats",
        category = "South Indian",
        imageUrl = "https://c.saavncdn.com/artists/Devi_Sri_Prasad_008_20250619062824_500x500.jpg",
        tagline = "Infectious mass beats & dance hits"
    )
)

private val CATEGORIES = listOf(
    "All",
    "Trending",
    "Bollywood",
    "Punjabi",
    "Pop",
    "Hip-Hop",
    "Indie",
    "South Indian"
)

@OptIn(UnstableApi::class)
@Composable
fun DiscoverArtistsScreen(
    viewModel: MusicViewModel,
    navController: NavController,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedArtistForProfile by remember { mutableStateOf<String?>(null) }
    val followedArtists by viewModel.followedArtists.collectAsState()

    // Filter artists based on category & search query
    val filteredArtists = remember(searchQuery, selectedCategory) {
        val query = searchQuery.trim().lowercase()
        CURATED_ARTISTS.filter { artist ->
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Trending" -> artist.isFeatured
                else -> artist.category.equals(selectedCategory, ignoreCase = true)
            }
            val matchesSearch = query.isEmpty() ||
                    artist.name.lowercase().contains(query) ||
                    artist.genre.lowercase().contains(query) ||
                    artist.category.lowercase().contains(query)

            matchesCategory && matchesSearch
        }
    }

    // Determine current spotlight hero artist
    val spotlightArtist = remember(selectedCategory, searchQuery) {
        if (searchQuery.isNotBlank()) null
        else {
            CURATED_ARTISTS.firstOrNull { artist ->
                when (selectedCategory) {
                    "All" -> artist.name == "Arijit Singh"
                    "Trending" -> artist.isFeatured
                    else -> artist.category.equals(selectedCategory, ignoreCase = true) && artist.isFeatured
                }
            } ?: CURATED_ARTISTS.firstOrNull { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── 1. Top App Bar ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Discover Artists",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Explore trending creators & styles",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                // Following Quick Link Chip
                Surface(
                    onClick = { navController.navigate("subscriptions") },
                    color = MusifyGlassSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MusifyGlassBorder),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = "Following",
                            tint = MusifyGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${followedArtists.size} Following",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ── 2. Live Search Bar ───────────────────────────────────────
            Surface(
                color = MusifyGlassSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MusifyGlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (searchQuery.isNotEmpty()) MusifyGreen else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search artists, genres...",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ── 3. Category Filter Chips ────────────────────────────────
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CATEGORIES) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = category,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF1C1C24),
                            labelColor = Color.LightGray,
                            selectedContainerColor = MusifyGreen,
                            selectedLabelColor = Color.Black
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) MusifyGreen else Color(0x33FFFFFF),
                            enabled = true,
                            selected = isSelected,
                            borderWidth = 1.dp
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // ── 4. Main Grid of Artists ─────────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Spotlight Hero Banner Item (spans full width)
                spotlightArtist?.let { hero ->
                    item(span = { GridItemSpan(2) }) {
                        SpotlightHeroCard(
                            artist = hero,
                            viewModel = viewModel,
                            onCardClick = { selectedArtistForProfile = hero.name },
                            onStartRadio = { viewModel.startArtistRadio(hero.name) }
                        )
                    }

                    item(span = { GridItemSpan(2) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedCategory == "All") "Popular & Trending Artists" else "$selectedCategory Artists",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${filteredArtists.size} artists",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Grid items for filtered artists
                items(filteredArtists, key = { it.name }) { artist ->
                    DiscoverArtistCard(
                        artist = artist,
                        viewModel = viewModel,
                        onClick = { selectedArtistForProfile = artist.name },
                        onStartRadio = { viewModel.startArtistRadio(artist.name) }
                    )
                }

                // Online Search Fallback if no local matches
                if (filteredArtists.isEmpty() && searchQuery.isNotBlank()) {
                    item(span = { GridItemSpan(2) }) {
                        OnlineSearchFallbackCard(
                            query = searchQuery.trim(),
                            onExplore = { selectedArtistForProfile = searchQuery.trim() },
                            onStartRadio = { viewModel.startArtistRadio(searchQuery.trim()) }
                        )
                    }
                }
            }
        }
    }

    // Artist Profile Detail Dialog
    selectedArtistForProfile?.let { artistName ->
        ArtistProfileDialog(
            artistName = artistName,
            viewModel = viewModel,
            onDismiss = { selectedArtistForProfile = null }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun SpotlightHeroCard(
    artist: DiscoverArtist,
    viewModel: MusicViewModel,
    onCardClick: () -> Unit,
    onStartRadio: () -> Unit
) {
    val isFollowed by viewModel.isArtistFollowed(artist.name).collectAsState(initial = false)
    val followersFormatted by viewModel.getArtistFollowersFormatted(artist.name).collectAsState(initial = "")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1A1A24),
        border = BorderStroke(1.dp, MusifyGlassBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MusifyGreen.copy(alpha = 0.25f),
                            Color(0xFF14141E)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Artist Avatar with glow
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF282834)),
                        contentAlignment = Alignment.Center
                    ) {
                        ArtistImage(
                            model = artist.imageUrl,
                            contentDescription = artist.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MusifyGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "FEATURED ARTIST",
                                    color = MusifyGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = MusifyGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = artist.name,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = if (followersFormatted.isNotBlank()) "$followersFormatted followers • ${artist.genre}" else artist.genre,
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (artist.tagline.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "“${artist.tagline}”",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: Start Radio + Follow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onStartRadio,
                        colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Podcasts,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Start Radio",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.toggleFollowArtist(artist.name, artist.imageUrl) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isFollowed) Color(0xFF282834) else Color.Transparent,
                            contentColor = if (isFollowed) MusifyGreen else Color.White
                        ),
                        border = BorderStroke(1.dp, if (isFollowed) MusifyGreen else Color(0x66FFFFFF)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isFollowed) Icons.Default.Check else Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFollowed) "Following" else "Follow",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun DiscoverArtistCard(
    artist: DiscoverArtist,
    viewModel: MusicViewModel,
    onClick: () -> Unit,
    onStartRadio: () -> Unit
) {
    val isFollowed by viewModel.isArtistFollowed(artist.name).collectAsState(initial = false)
    val followersFormatted by viewModel.getArtistFollowersFormatted(artist.name).collectAsState(initial = "")

    Surface(
        color = MusifyGlassSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MusifyGlassBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Artist Avatar with Radio play overlay button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF282834)),
                contentAlignment = Alignment.Center
            ) {
                ArtistImage(
                    model = artist.imageUrl,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Artist Name
            Text(
                text = artist.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Genre / Category
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = artist.genre,
                color = Color.Gray,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Followers
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (followersFormatted.isNotBlank()) "$followersFormatted followers" else "Artist",
                color = Color(0xFFAAAAAA),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row: Quick Radio Play + Follow Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Radio play icon
                Surface(
                    onClick = onStartRadio,
                    shape = CircleShape,
                    color = Color(0xFF282834),
                    border = BorderStroke(1.dp, MusifyGlassBorder),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Podcasts,
                            contentDescription = "Radio",
                            tint = MusifyGreen,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Follow / Following button
                Button(
                    onClick = { viewModel.toggleFollowArtist(artist.name, artist.imageUrl) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowed) Color(0xFF22222E) else MusifyGreen,
                        contentColor = if (isFollowed) MusifyGreen else Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = if (isFollowed) BorderStroke(1.dp, MusifyGreen) else null,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (isFollowed) "Following" else "Follow",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OnlineSearchFallbackCard(
    query: String,
    onExplore: () -> Unit,
    onStartRadio: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MusifyGlassSurface,
        border = BorderStroke(1.dp, MusifyGlassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MusifyGreen,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Search “$query” on YouTube Music",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Explore this artist's top tracks, albums, and discography.",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onExplore,
                    colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Artist", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onStartRadio,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, MusifyGlassBorder),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Podcasts, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Radio")
                }
            }
        }
    }
}
