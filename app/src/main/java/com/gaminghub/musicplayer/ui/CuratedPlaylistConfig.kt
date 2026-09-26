package com.gaminghub.musicplayer.ui

/**
 * Compile-time constant curated playlist definitions.
 * Defined here (not inside HomeScreen) so the composable function's stack frame
 * stays small and these lists are not re-allocated on each recomposition.
 */
object CuratedPlaylistConfig {

    val featuredPlaylists = listOf(
        CuratedCardData("Top Weekly Videos Tamil", "Tamil Hits", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80", "Top Tamil Hit Songs"),
        CuratedCardData("Top Weekly Videos Telugu", "Telugu Hits", "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=500&q=80", "Top Telugu Hit Songs"),
        CuratedCardData("Top Weekly Videos Hindi", "Hindi Hits", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=500&q=80", "Top Hindi Bollywood Hit Songs"),
        CuratedCardData("Top Weekly Videos Punjabi", "Punjabi Fire", "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80", "Top Punjabi Hit Songs"),
        CuratedCardData("Daily Top Videos Global", "Global Chart", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=500&q=80", "Global Top Billboard Hits Songs")
    )

    val communityPlaylists = listOf(
        CuratedCardData("90s Super Hit Songs", "Kumar Sanu, Udit Narayan", "https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=500&q=80", "90s Bollywood Evergreen Superhit Songs"),
        CuratedCardData("Heartbreak Melodies", "Sad & Soulful Melodies", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80", "Bollywood Sad Heartbreak Songs Melodies"),
        CuratedCardData("Tamil 90s Melodies", "Ilaiyaraaja, AR Rahman", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80", "90s Tamil Evergreen Melodies Songs"),
        CuratedCardData("Bollywood Romance", "Bollywood Favorites", "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&q=80", "Bollywood Romantic Love Songs Hits"),
        CuratedCardData("Old is Gold", "Evergreen Classics", "https://images.unsplash.com/photo-1485579149621-3123dd979885?w=500&q=80", "Old Hindi Evergreen Classics Kishore Kumar")
    )

    val dancingOnYourOwn = listOf(
        CuratedCardData("90s Bollywood Dance", "High Energy", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=500&q=80", "90s Bollywood Dance Songs Hits"),
        CuratedCardData("Bollywood Fire", "Party Hits", "https://images.unsplash.com/photo-1546707012-c46675f12716?w=500&q=80", "Bollywood Party Dance Songs Club Hits"),
        CuratedCardData("Punjabi Party", "Bhangra Beats", "https://images.unsplash.com/photo-1571266028243-3716f02d2d2e?w=500&q=80", "Punjabi Party Songs Bhangra Hits"),
        CuratedCardData("Haryanvi Party", "Desi Beats", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=500&q=80", "Haryanvi Dance Songs Party Hits"),
        CuratedCardData("00s Bollywood Dance", "Throwback Hits", "https://images.unsplash.com/photo-1429962714451-bb934ecdc4ec?w=500&q=80", "2000s Bollywood Dance Songs Hits"),
        CuratedCardData("Bhangra Bangers", "Dhol Vibes", "https://images.unsplash.com/photo-1511192336575-5a79af67a629?w=500&q=80", "Punjabi Bhangra Bangers Songs")
    )

    val indiaBiggestHits = listOf(
        CuratedCardData("Bollywood Hitlist", "Chartbusters", "https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?w=500&q=80", "Bollywood Hitlist Top Songs"),
        CuratedCardData("Punjab Fire", "Trending North", "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80", "Punjabi Hits Karan Aujla AP Dhillon Shubh"),
        CuratedCardData("Hits of 2026", "Fresh Releases", "https://images.unsplash.com/photo-1520523839898-507127040409?w=500&q=80", "Latest Bollywood Hindi Songs"),
        CuratedCardData("Kollywood Hitlist", "Anirudh Vibes", "https://images.unsplash.com/photo-1518972559570-7cc1309f3229?w=500&q=80", "Kollywood Tamil Hits Anirudh Ravichander"),
        CuratedCardData("Sub Condiment", "Indie South Asia", "https://images.unsplash.com/photo-1511735111819-9a3f7709049c?w=500&q=80", "Indian Indie Pop Songs Prateek Kuhad"),
        CuratedCardData("I-Pop Hits!", "Pop Sensations", "https://images.unsplash.com/photo-1526478806334-5fd488fcaabc?w=500&q=80", "Indian Pop Hits Darshan Raval Armaan Malik")
    )

    val nostalgicHits = listOf(
        CuratedCardData("00s Bollywood Romance", "KK, Shreya Ghoshal", "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&q=80", "2000s Bollywood Romantic Songs KK Shreya Ghoshal"),
        CuratedCardData("90s Bollywood Romance", "Alka Yagnik, Kumar Sanu", "https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=500&q=80", "90s Bollywood Romantic Songs Alka Yagnik Kumar Sanu"),
        CuratedCardData("Chai, Baarish aur 90s", "Rainy Nostalgia", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500&q=80", "Bollywood Monsoon Rainy Songs 90s"),
        CuratedCardData("90s Bollywood Sad Songs", "Heartfelt Classics", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80", "90s Bollywood Sad Songs Heartfelt"),
        CuratedCardData("10s Tollywood Dance Hits", "Allu Arjun, DSP", "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=500&q=80", "Telugu 2010s Dance Songs Allu Arjun DSP")
    )

    val newReleases = listOf(
        CuratedCardData("Om Namah Shivay", "Devotional & Chants", "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=500&q=80", "Shiv Bhajans Mahadev Devotional Songs"),
        CuratedCardData("Shiv Stotras", "Sacred Chants", "https://images.unsplash.com/photo-1519744346361-7a46d19cd819?w=500&q=80", "Lord Shiva Stotras Sacred Chants"),
        CuratedCardData("Trending Fresh", "Top New Tracks", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=500&q=80", "Latest Hindi Bollywood Songs 2025"),
        CuratedCardData("New Release Mix", "Fresh Mix", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=500&q=80", "New Bollywood Hit Songs Mix")
    )

    val albumsAndSingles = listOf(
        CuratedCardData("Acoustic & Unplugged", "Soft & Acoustic", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80", "Bollywood Acoustic Unplugged Songs"),
        CuratedCardData("Late Night Lo-Fi", "Calm & Relaxing", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=500&q=80", "Hindi Lo-Fi Chill Beats Songs"),
        CuratedCardData("Sufi Soul", "Mystical & Soulful", "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80", "Bollywood Sufi Songs Kailash Kher"),
        CuratedCardData("Retro Ghazals", "Poetic & Melodic", "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&q=80", "Best Hindi Ghazals Jagjit Singh")
    )

    val chaiAndChill = listOf(
        CuratedCardData("90s Chill: Bollywood", "Kumar Sanu, Alka Yagnik...", "https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=500&q=80", "90s Chill Soft Bollywood Songs"),
        CuratedCardData("00s Chill: Telugu", "K.S. Chithra, Sumangali, ...", "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=500&q=80", "2000s Telugu Soft Melodies Chill Songs")
    )

    val musicVideos = listOf(
        CuratedCardData("Top Bollywood Music Videos", "Visual Spectacle", "https://images.unsplash.com/photo-1514320291840-2e0a9bf2a9ae?w=500&q=80", "Top Bollywood Music Videos Songs"),
        CuratedCardData("Party Music Videos", "Club Beats", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80", "Bollywood Party Music Video Songs"),
        CuratedCardData("Punjabi Visuals", "Dhol & Glamour", "https://images.unsplash.com/photo-1504609773096-104ff2c73ba4?w=500&q=80", "Top Punjabi Music Videos Songs")
    )
}
