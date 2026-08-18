package com.gaminghub.musicplayer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TrackEntity::class, 
        PlaylistEntity::class,
        com.gaminghub.musify.data.FollowedArtistEntity::class,
        com.gaminghub.musify.data.PlaylistTrackEntity::class,
        com.gaminghub.musify.data.PlaybackStateEntity::class,
        com.gaminghub.musify.data.QueueTrackEntity::class
    ], 
    version = 8, 
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract val dao: MusicDao

    companion object {
        @Volatile
        private var instance: MusicDatabase? = null

        fun getInstance(context: Context): MusicDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music_player.db"
                )
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
            }
        }
    }
}
