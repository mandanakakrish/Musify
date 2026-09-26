package com.gaminghub.musicplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    // ── Tracks ────────────────────────────────────────────────────────────────
    @Query("SELECT * FROM tracks WHERE localPath IS NOT NULL AND localPath != '' ORDER BY lastPlayedTimestamp DESC")
    fun getDownloadedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE localPath IS NOT NULL AND localPath != ''")
    fun getDownloadedTracksSync(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1")
    fun getFavoriteTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1")
    fun getFavoriteTracksSync(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE lastPlayedTimestamp IS NOT NULL ORDER BY lastPlayedTimestamp DESC LIMIT 100")
    fun getRecentHistory(): Flow<List<TrackEntity>>

    @Query("UPDATE tracks SET lastPlayedTimestamp = NULL")
    suspend fun clearRecentHistory()

    @Query("UPDATE tracks SET lastPlayedTimestamp = NULL WHERE audioUrl = :url OR (:videoId != '' AND audioUrl LIKE '%' || :videoId || '%')")
    suspend fun removeFromRecentHistory(url: String, videoId: String = "")

    @Query("SELECT * FROM tracks ORDER BY playCount DESC LIMIT 50")
    fun getTop50Tracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE audioUrl = :url LIMIT 1")
    suspend fun getTrackByUrl(url: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE title = :title LIMIT 1")
    suspend fun getTrackByTitle(title: String): TrackEntity?

    @Query("SELECT * FROM tracks")
    suspend fun getAllTracksSync(): List<TrackEntity>

    @Query("UPDATE tracks SET localPath = :localPath WHERE audioUrl = :url")
    suspend fun updateLocalPath(url: String, localPath: String?)

    @Upsert
    suspend fun insertTrack(track: TrackEntity)

    @Upsert
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Transaction
    suspend fun toggleFavorite(url: String, trackRequest: TrackEntity) {
        val existing = getTrackByUrl(url)
        if (existing == null) {
            insertTrack(trackRequest.copy(isFavorite = true))
        } else {
            insertTrack(existing.copy(isFavorite = !existing.isFavorite))
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayEvent(event: PlayEventEntity)

    @Query("SELECT * FROM play_events WHERE syncedToFirestore = 0 ORDER BY playedAt ASC LIMIT :limit")
    suspend fun getUnsyncedPlayEvents(limit: Int = 100): List<PlayEventEntity>

    @Query("UPDATE play_events SET syncedToFirestore = 1 WHERE id IN (:ids)")
    suspend fun markPlayEventsSynced(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM play_events WHERE syncedToFirestore = 0")
    suspend fun getUnsyncedPlayEventsCount(): Int

    @Transaction
    suspend fun recordHistory(url: String, trackRequest: TrackEntity) {
        val existing = getTrackByUrl(url)
        val timestamp = System.currentTimeMillis()
        if (existing == null) {
            insertTrack(trackRequest.copy(lastPlayedTimestamp = timestamp, playCount = 1))
        } else {
            insertTrack(existing.copy(lastPlayedTimestamp = timestamp, playCount = existing.playCount + 1))
        }
        if (url.isNotBlank()) {
            insertPlayEvent(PlayEventEntity(audioUrl = url, playedAt = timestamp))
        }
    }

    @Query("""
        SELECT tracks.*, 
               MAX(COALESCE(pe.cnt, 0), CASE WHEN tracks.lastPlayedTimestamp >= :sinceTimestamp THEN tracks.playCount ELSE 0 END) AS periodPlayCount
        FROM tracks 
        LEFT JOIN (
            SELECT audioUrl, COUNT(*) as cnt 
            FROM play_events 
            WHERE playedAt >= :sinceTimestamp 
            GROUP BY audioUrl
        ) pe ON tracks.audioUrl = pe.audioUrl 
        WHERE (pe.cnt > 0 OR (tracks.lastPlayedTimestamp IS NOT NULL AND tracks.lastPlayedTimestamp >= :sinceTimestamp AND tracks.playCount > 0))
        ORDER BY periodPlayCount DESC, tracks.lastPlayedTimestamp DESC 
        LIMIT 50
    """)
    fun getTopPlayedTracksWithCountSince(sinceTimestamp: Long): Flow<List<TrackPlayCount>>


    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE audioUrl = :url")
    suspend fun updateFavorite(url: String, isFavorite: Boolean)

    @Query("UPDATE tracks SET artist = :newArtist WHERE audioUrl = :url")
    suspend fun updateTrackArtist(url: String, newArtist: String)

    @Query("UPDATE tracks SET isDevpick = :isDevpick WHERE audioUrl = :url")
    suspend fun updateDevPick(url: String, isDevpick: Boolean)

    @Query("SELECT * FROM tracks WHERE isDevpick = 1")
    fun getDevPickTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isDevpick = 1")
    fun getDevPickTracksSync(): List<TrackEntity>

    @Query("SELECT SUM(playCount) FROM tracks")
    fun getTotalSongsPlayedCount(): Flow<Int?>

    @Query("SELECT * FROM tracks WHERE playCount > 0 ORDER BY playCount DESC LIMIT 10")
    fun getMostPlayedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE playCount > 0 ORDER BY playCount DESC LIMIT 1")
    fun getTopMostPlayedTrack(): Flow<TrackEntity?>

    @Query("SELECT isFavorite FROM tracks WHERE audioUrl = :url")
    fun isFavorite(url: String): Flow<Boolean>

    @Query("UPDATE tracks SET skipCount = skipCount + 1 WHERE audioUrl = :url")
    suspend fun incrementSkipCount(url: String)

    @Query("UPDATE tracks SET completionCount = completionCount + 1 WHERE audioUrl = :url")
    suspend fun incrementCompletionCount(url: String)

    @Query("SELECT * FROM tracks WHERE (isFavorite = 1 OR playCount > 1) AND (lastPlayedTimestamp IS NULL OR lastPlayedTimestamp < :thresholdMs) ORDER BY playCount DESC LIMIT 30")
    fun getForgottenFavorites(thresholdMs: Long): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE playCount > 0 ORDER BY playCount DESC, lastPlayedTimestamp DESC LIMIT 30")
    fun getHeavyRotationTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'")
    suspend fun searchTracks(query: String): List<TrackEntity>

    // ── Caching ───────────────────────────────────────────────────────────────
    @Query("SELECT cachedPlayableUrl FROM tracks WHERE audioUrl = :url AND (cachedPlayableUrlExpiry IS NULL OR cachedPlayableUrlExpiry > :currentTime)")
    suspend fun getValidCachedUrl(url: String, currentTime: Long): String?

    @Query("UPDATE tracks SET cachedPlayableUrl = :playableUrl, cachedPlayableUrlExpiry = :expiry WHERE audioUrl = :url")
    suspend fun updateCachedUrl(url: String, playableUrl: String?, expiry: Long?)

    // ── Playlists ─────────────────────────────────────────────────────────────
    @Query("SELECT * FROM playlists ORDER BY createdAt ASC")
    fun getPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists ORDER BY createdAt ASC")
    fun getPlaylistsSync(): List<PlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Int)

    @Query("UPDATE playlists SET name = :newName WHERE id = :id")
    suspend fun renamePlaylist(id: Int, newName: String)

    // ── Playlist Tracks (Playlist Tracks Association) ──────────────────────────
    @Query("SELECT tracks.* FROM tracks INNER JOIN playlist_tracks ON tracks.audioUrl = playlist_tracks.audioUrl WHERE playlist_tracks.playlistId = :playlistId ORDER BY playlist_tracks.addedAt ASC")
    fun getTracksForPlaylist(playlistId: Int): Flow<List<TrackEntity>>

    @Query("SELECT tracks.* FROM tracks INNER JOIN playlist_tracks ON tracks.audioUrl = playlist_tracks.audioUrl WHERE playlist_tracks.playlistId = :playlistId ORDER BY playlist_tracks.addedAt ASC")
    fun getTracksForPlaylistSync(playlistId: Int): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrackToPlaylist(playlistTrack: PlaylistTrackEntity)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND audioUrl = :audioUrl")
    suspend fun removeTrackFromPlaylist(playlistId: Int, audioUrl: String)

    // ── Followed Artists ──────────────────────────────────────────────────────
    @Query("SELECT * FROM followed_artists ORDER BY followedAt DESC")
    fun getFollowedArtists(): Flow<List<FollowedArtistEntity>>

    @Query("SELECT * FROM followed_artists ORDER BY followedAt DESC")
    fun getFollowedArtistsSync(): List<FollowedArtistEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM followed_artists WHERE id = :id OR name = :id COLLATE NOCASE OR TRIM(name) = TRIM(:id) COLLATE NOCASE)")
    fun isArtistFollowed(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowedArtist(artist: FollowedArtistEntity)

    @Query("DELETE FROM followed_artists WHERE id = :id OR name = :id COLLATE NOCASE OR TRIM(name) = TRIM(:id) COLLATE NOCASE")
    suspend fun deleteFollowedArtist(id: String)

    // ── Stats ─────────────────────────────────────────────────────────────────
    @Query("SELECT COUNT(DISTINCT audioUrl) FROM tracks WHERE lastPlayedTimestamp IS NOT NULL")
    fun getUniqueTracksPlayedCount(): Flow<Int>

    @Query("SELECT * FROM tracks WHERE lastPlayedTimestamp IS NOT NULL ORDER BY playCount DESC, lastPlayedTimestamp DESC LIMIT 1")
    fun getMostPlayedTrack(): Flow<TrackEntity?>

    // ── Playback Session Persistence ──────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlaybackState(playbackState: PlaybackStateEntity)

    @Query("SELECT * FROM playback_state WHERE id = 1 LIMIT 1")
    suspend fun getPlaybackState(): PlaybackStateEntity?

    @Query("DELETE FROM queue_tracks")
    suspend fun clearQueueTracks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueTracks(queue: List<QueueTrackEntity>)

    @Transaction
    suspend fun updatePersistentQueue(queue: List<QueueTrackEntity>) {
        clearQueueTracks()
        insertQueueTracks(queue)
    }

    @Query("SELECT * FROM queue_tracks ORDER BY queuePosition ASC")
    suspend fun getQueueTracks(): List<QueueTrackEntity>
}
