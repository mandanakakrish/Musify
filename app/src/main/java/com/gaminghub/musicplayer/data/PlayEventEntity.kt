package com.gaminghub.musicplayer.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "play_events",
    indices = [
        Index(value = ["playedAt"]),
        Index(value = ["audioUrl"]),
        Index(value = ["syncedToFirestore"])
    ]
)
data class PlayEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val audioUrl: String,
    val playedAt: Long = System.currentTimeMillis(),
    val syncedToFirestore: Boolean = false
)
