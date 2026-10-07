package com.gaminghub.musify.desktop.model

data class TrackModel(
    val title: String,
    val artist: String,
    val audioUrl: String,
    val albumArtUrl: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val durationSeconds: Long = 0L,
    val uploaderChannel: String? = null,
    val isDevpick: Boolean = false,
    val playcount: Int = 0,
    val isLocal: Boolean = false,
    val localFilePath: String? = null
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

data class LyricsData(
    val plainLyrics: String? = null,
    val syncedLyrics: List<LyricLine> = emptyList(),
    val isInstrumental: Boolean = false
)

data class PlaylistModel(
    val id: String,
    val title: String,
    val description: String = "",
    val coverUrl: String? = null,
    val trackCount: Int = 0,
    val tracks: List<TrackModel> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class CuratedCardData(
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val searchQuery: String
)

enum class RepeatMode {
    OFF, ALL, ONE
}

enum class DesktopScreen {
    HOME, SEARCH, LIBRARY, PLAYLIST_DETAIL, DOWNLOADS, DEV_PICKS, EQUALIZER, SETTINGS
}

enum class StreamingQuality(val label: String, val targetBitrate: Int) {
    HIGH("320 kbps (High Quality)", 320_000),
    STANDARD("160 kbps (Standard)", 160_000),
    LOW("96 kbps (Data Saver)", 96_000)
}
