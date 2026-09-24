package com.gaminghub.musicplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gaminghub.musicplayer.TrackModel

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val audioUrl: String, // YouTube URL as identifier
    val title: String,
    val artist: String,
    val albumArtUrl: String? = null,
    val isFavorite: Boolean = false,
    val lastPlayedTimestamp: Long? = null,
    val playCount: Int = 0,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
    val cachedPlayableUrl: String? = null,
    val cachedPlayableUrlExpiry: Long? = null,
    val localPath: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val isDevpick: Boolean = false,
    val skipCount: Int = 0,
    val completionCount: Int = 0
)

fun TrackEntity.toModel() = TrackModel(
    title = title,
    artist = artist,
    audioUrl = audioUrl,
    albumArtUrl = albumArtUrl,
    album = album,
    genre = genre,
    isDevpick = isDevpick,
    playcount = playCount,
    skipCount = skipCount,
    completionCount = completionCount
)

fun TrackModel.toEntity(isFavorite: Boolean = false, lastPlayed: Long? = null) = TrackEntity(
    audioUrl = audioUrl ?: "",
    title = title,
    artist = artist,
    albumArtUrl = albumArtUrl,
    isFavorite = isFavorite,
    lastPlayedTimestamp = lastPlayed,
    playCount = playcount,
    album = album,
    genre = genre,
    isDevpick = isDevpick,
    skipCount = skipCount,
    completionCount = completionCount
)

data class TrackPlayCount(
    @androidx.room.Embedded val track: TrackEntity,
    val periodPlayCount: Int
)

fun TrackPlayCount.toModel() = track.toModel().copy(playcount = periodPlayCount)

