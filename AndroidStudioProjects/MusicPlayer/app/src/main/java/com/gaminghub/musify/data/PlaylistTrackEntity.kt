package com.gaminghub.musify.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.gaminghub.musicplayer.data.PlaylistEntity
import com.gaminghub.musicplayer.data.TrackEntity

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "audioUrl"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["audioUrl"],
            childColumns = ["audioUrl"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["audioUrl"])]
)
data class PlaylistTrackEntity(
    val playlistId: Int,
    val audioUrl: String,
    val addedAt: Long = System.currentTimeMillis()
)
