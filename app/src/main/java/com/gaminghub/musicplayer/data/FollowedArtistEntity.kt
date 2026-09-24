package com.gaminghub.musicplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "followed_artists")
data class FollowedArtistEntity(
    @PrimaryKey val id: String, // YouTube Channel ID
    val name: String,
    val imageUrl: String?,
    val followedAt: Long = System.currentTimeMillis()
)
