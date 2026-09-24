package com.gaminghub.musicplayer

// The specific data for each song (Generic model used across the app)
data class TrackModel(
    val title: String,
    val artist: String,
    val audioUrl: String?,
    val albumArtUrl: String?,
    val album: String? = null,
    val genre: String? = null,
    val durationSeconds: Long = 0L,
    val uploaderChannel: String? = null,
    val isDevpick: Boolean = false,
    val playcount: Int = 0,
    val isLocal: Boolean = false,
    val skipCount: Int = 0,
    val completionCount: Int = 0
)

data class ArtistModel(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val followersCount: Long = 0L,
    val tracks: List<TrackModel> = emptyList(),
    val isFollowed: Boolean = false
)

data class LyricLine(
    val timeMs: Long,
    val text: String
)
