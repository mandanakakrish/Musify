package com.gaminghub.musify

import androidx.compose.ui.graphics.Color

val MusifyPink = Color(0xFFE91E63)

data class TrackModel(
    val title: String,
    val artist: String,
    val audioUrl: String?,
    val albumArtUrl: String?,
    val artistId: String? = null,
    val uploaderName: String? = null,
    val album: String? = null,
    val genre: String? = null
)

data class ArtistModel(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val bio: String? = null,
    val subscribers: String? = null,
    val topTracks: List<TrackModel> = emptyList()
)

data class AlbumModel(
    val id: String,
    val title: String,
    val artist: String,
    val imageUrl: String?,
    val tracks: List<TrackModel> = emptyList()
)

data class LyricLine(
    val timeMs: Long,
    val text: String
)
