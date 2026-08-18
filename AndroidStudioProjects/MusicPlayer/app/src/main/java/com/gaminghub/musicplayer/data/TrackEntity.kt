package com.gaminghub.musicplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gaminghub.musicplayer.TrackModel

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val audioUrl: String, // YouTube URL as identifier
    val title: String,
    val artist: String,
    val albumArtUrl: String?,
    val isFavorite: Boolean = false,
    val lastPlayedTimestamp: Long? = null,
    val playCount: Int = 0,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
    val cachedPlayableUrl: String? = null,
    val cachedPlayableUrlExpiry: Long? = null,
    val localPath: String? = null,
    val album: String? = null,
    val genre: String? = null
)

fun TrackEntity.toModel() = TrackModel(
    title = title,
    artist = artist,
    audioUrl = audioUrl,
    albumArtUrl = albumArtUrl,
    album = album,
    genre = genre
)

fun TrackModel.toEntity(isFavorite: Boolean = false, lastPlayed: Long? = null) = TrackEntity(
    audioUrl = audioUrl ?: "",
    title = title,
    artist = artist,
    albumArtUrl = albumArtUrl,
    isFavorite = isFavorite,
    lastPlayedTimestamp = lastPlayed,
    album = album,
    genre = genre
)
