package com.gaminghub.musicplayer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TrackEntity::class, 
        PlaylistEntity::class,
        FollowedArtistEntity::class,
        PlaylistTrackEntity::class,
        PlaybackStateEntity::class,
        QueueTrackEntity::class,
        PlayEventEntity::class
    ], 
    version = 14, 
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract val dao: MusicDao

    companion object {
        @Volatile
        private var instance: MusicDatabase? = null

        private val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tracks ADD COLUMN skipCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tracks ADD COLUMN completionCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS play_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        audioUrl TEXT NOT NULL,
                        playedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_play_events_playedAt ON play_events(playedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_play_events_audioUrl ON play_events(audioUrl)")
                db.execSQL("""
                    INSERT INTO play_events (audioUrl, playedAt)
                    SELECT audioUrl, lastPlayedTimestamp 
                    FROM tracks 
                    WHERE lastPlayedTimestamp IS NOT NULL AND audioUrl IS NOT NULL AND audioUrl != ''
                """.trimIndent())
            }
        }

        private val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE play_events ADD COLUMN syncedToFirestore INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_play_events_syncedToFirestore ON play_events(syncedToFirestore)")
            }
        }

        private val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add performance indexes on hot query columns
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_lastPlayedTimestamp ON tracks(lastPlayedTimestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_isFavorite ON tracks(isFavorite)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_playCount ON tracks(playCount)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_localPath ON tracks(localPath)")
            }
        }

        fun getInstance(context: Context): MusicDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music_player.db"
                )
                .addMigrations(MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                .fallbackToDestructiveMigration(true)
                .build().also { instance = it }
            }
        }
    }
}
