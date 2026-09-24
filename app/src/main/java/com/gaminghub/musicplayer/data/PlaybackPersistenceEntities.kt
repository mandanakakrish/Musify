package com.gaminghub.musicplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey val id: Int = 1, // Single row for current state
    val currentTrackIndex: Int = 0,
    val positionMs: Long = 0,
    val isPlaying: Boolean = false,
    val shuffleMode: Boolean = false,
    val repeatMode: Int = 0
)

@Entity(tableName = "queue_tracks")
data class QueueTrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val queuePosition: Int,
    val audioUrl: String,
    val title: String,
    val artist: String,
    val albumArtUrl: String?
)
