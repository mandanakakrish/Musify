package com.gaminghub.musicplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import com.gaminghub.musify.data.FollowedArtistEntity
import com.gaminghub.musify.data.PlaylistTrackEntity
import com.gaminghub.musify.data.PlaybackStateEntity
import com.gaminghub.musify.data.QueueTrackEntity

@Dao
interface MusicDao {
    // ── Tracks ────────────────────────────────────────────────────────────────
    @Query("SELECT * FROM tracks WHERE isFavorite = 1")
    fun getFavoriteTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE lastPlayedTimestamp IS NOT NULL ORDER BY lastPlayedTimestamp DESC LIMIT 20")
    fun getRecentHistory(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY playCount DESC LIMIT 50")
    fun getTop50Tracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE audioUrl = :url LIMIT 1")
    suspend fun getTrackByUrl(url: String): TrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    @Transaction
    suspend fun toggleFavorite(url: String, trackRequest: TrackEntity) {
        val existing = getTrackByUrl(url)
        if (existing == null) {
            insertTrack(trackRequest.copy(isFavorite = true))
        } else {
            insertTrack(existing.copy(isFavorite = !existing.isFavorite))
        }
    }

    @Transaction
    suspend fun recordHistory(url: String, trackRequest: TrackEntity) {
        val existing = getTrackByUrl(url)
        val timestamp = System.currentTimeMillis()
        if (existing == null) {
            insertTrack(trackRequest.copy(lastPlayedTimestamp = timestamp, playCount = 1))
        } else {
            insertTrack(existing.copy(lastPlayedTimestamp = timestamp, playCount = existing.playCount + 1))
        }
    }

    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE audioUrl = :url")
    suspend fun updateFavorite(url: String, isFavorite: Boolean)

    @Query("SELECT isFavorite FROM tracks WHERE audioUrl = :url")
    fun isFavorite(url: String): Flow<Boolean>

    @Query("SELECT * FROM tracks WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'")
    suspend fun searchTracks(query: String): List<TrackEntity>

    // ── Caching ───────────────────────────────────────────────────────────────
    @Query("SELECT cachedPlayableUrl FROM tracks WHERE audioUrl = :url AND (cachedPlayableUrlExpiry IS NULL OR cachedPlayableUrlExpiry > :currentTime)")
    fun getValidCachedUrl(url: String, currentTime: Long): String?

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

    // ── Followed Artists ──────────────────────────────────────────────────────
    @Query("SELECT * FROM followed_artists ORDER BY followedAt DESC")
    fun getFollowedArtists(): Flow<List<FollowedArtistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM followed_artists WHERE id = :id)")
    fun isArtistFollowed(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowedArtist(artist: FollowedArtistEntity)

    @Query("DELETE FROM followed_artists WHERE id = :id")
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
