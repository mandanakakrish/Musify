package com.gaminghub.musify.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gaminghub.musify.ArtistModel

@Entity(tableName = "followed_artists")
data class FollowedArtistEntity(
    @PrimaryKey val id: String, // YouTube Channel ID
    val name: String,
    val imageUrl: String?,
    val followedAt: Long = System.currentTimeMillis()
)

fun FollowedArtistEntity.toModel() = ArtistModel(
    id = id,
    name = name,
    imageUrl = imageUrl
)

fun ArtistModel.toEntity() = FollowedArtistEntity(
    id = id,
    name = name,
    imageUrl = imageUrl
)
