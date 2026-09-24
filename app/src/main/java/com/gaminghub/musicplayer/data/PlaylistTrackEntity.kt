package com.gaminghub.musicplayer.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "audioUrl"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
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
