package com.gaminghub.musify.data

import com.gaminghub.musify.TrackModel
import com.gaminghub.musicplayer.data.TrackEntity

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
