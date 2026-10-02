package com.gaminghub.musicplayer.data.firebase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.gaminghub.musicplayer.TrackModel
import com.gaminghub.musicplayer.data.*
import com.gaminghub.musicplayer.util.CrashReporter
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class FirestorePlaylist(
    val id: String = "",
    val name: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val tracks: List<FirestoreTrack> = emptyList()
)

data class FirestoreTrack(
    val videoId: String = "",
    val songId: String = videoId,
    val title: String = "",
    val artist: String = "",
    val audioUrl: String = "",
    val albumArtUrl: String = "",
    val album: String = "",
    val genre: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

data class FirestoreFavorite(
    val videoId: String = "",
    val songId: String = videoId,
    val title: String = "",
    val artist: String = "",
    val audioUrl: String = "",
    val albumArtUrl: String = "",
    val favoritedAt: Long = System.currentTimeMillis()
)

data class FirestorePlayEvent(
    val eventId: String = "",
    val videoId: String = "",
    val songId: String = videoId,
    val audioUrl: String = "",
    val title: String = "",
    val artist: String = "",
    val playedAt: Long = System.currentTimeMillis()
)

data class FirestorePlayedTrack(
    val videoId: String = "",
    val songId: String = videoId,
    val title: String = "",
    val artist: String = "",
    val audioUrl: String = "",
    val albumArtUrl: String = "",
    val playCount: Int = 0,
    val lastPlayedTimestamp: Long = 0L
)

data class FirestoreDevPick(
    val videoId: String = "",
    val songId: String = videoId,
    val title: String = "",
    val artist: String = "",
    val audioUrl: String = "",
    val albumArtUrl: String = "",
    val album: String = "",
    val genre: String = "",
    val durationSeconds: Long = 0L,
    val uploaderChannel: String = "",
    val isDevpick: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

data class FirestoreFollowedArtist(
    val id: String = "",
    val name: String = "",
    val imageUrl: String? = null,
    val followedAt: Long = System.currentTimeMillis()
)

object FirestoreSyncManager {
    private const val TAG = "FirestoreSyncManager"

    private val isSyncing = java.util.concurrent.atomic.AtomicBoolean(false)

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore not initialized: ${e.message}")
            null
        }
    }

    /**
     * Extracts YouTube video ID from standard YouTube URL or stream URL
     */
    fun extractVideoId(url: String?): String {
        if (url.isNullOrBlank()) return ""
        try {
            val uri = Uri.parse(url)
            val vParam = uri.getQueryParameter("v")
            if (!vParam.isNullOrBlank()) return vParam

            val lastSegment = uri.lastPathSegment ?: ""
            if (lastSegment.length == 11 && !lastSegment.contains(".")) return lastSegment
            if (url.contains("youtu.be/")) {
                val sub = url.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
                if (sub.length == 11) return sub
            }
        } catch (_: Exception) {}
        // Fallback deterministic safe key based on hash
        return "track_${url.hashCode().toString().replace("-", "n")}"
    }

    /**
     * Uploads all local Room playlists, favorites, and history to Cloud Firestore
     */
    suspend fun syncLocalToCloud(context: Context, userId: String): Result<String> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(Exception("Firestore not available"))
        try {
            val roomDb = MusicDatabase.getInstance(context)
            val dao = roomDb.dao
            val userDoc = db.collection("users").document(userId)

            // 1. Sync Playlists
            val playlists = dao.getPlaylistsSync()
            var uploadedPlaylists = 0
            var uploadedTracks = 0

            for (pl in playlists) {
                val tracks = dao.getTracksForPlaylistSync(pl.id).map { t ->
                    FirestoreTrack(
                        videoId = extractVideoId(t.audioUrl),
                        title = t.title,
                        artist = t.artist,
                        audioUrl = t.audioUrl,
                        albumArtUrl = t.albumArtUrl ?: "",
                        album = t.album ?: "",
                        genre = t.genre ?: ""
                    )
                }
                val plData = hashMapOf(
                    "id" to pl.id.toString(),
                    "name" to pl.name,
                    "updatedAt" to System.currentTimeMillis(),
                    "tracks" to tracks.map { mapOf(
                        "videoId" to it.videoId,
                        "songId" to it.videoId,
                        "title" to it.title,
                        "artist" to it.artist,
                        "audioUrl" to it.audioUrl,
                        "albumArtUrl" to it.albumArtUrl,
                        "album" to it.album,
                        "genre" to it.genre
                    )}
                )
                userDoc.collection("playlists").document(pl.id.toString())
                    .set(plData, SetOptions.merge())
                    .await()

                uploadedPlaylists++
                uploadedTracks += tracks.size
            }

            // 2. Sync Favorites
            val favorites = dao.getFavoriteTracksSync()
            var uploadedFavorites = 0
            for (fav in favorites) {
                try {
                    val rawUrl = fav.audioUrl
                    val videoId = extractVideoId(rawUrl)
                    val docId = if (videoId.isNotBlank()) {
                        videoId
                    } else {
                        sanitizeDocumentId(
                            (if (rawUrl.isNotBlank()) rawUrl else "${fav.title}_${fav.artist}").ifBlank { "track_${fav.title.hashCode()}" }
                        )
                    }
                    if (docId.isBlank()) continue
                    val favData = hashMapOf(
                        "videoId" to (if (videoId.isNotBlank()) videoId else docId),
                        "songId" to (if (videoId.isNotBlank()) videoId else docId),
                        "title" to fav.title,
                        "artist" to fav.artist,
                        "audioUrl" to fav.audioUrl,
                        "albumArtUrl" to (fav.albumArtUrl ?: ""),
                        "album" to (fav.album ?: ""),
                        "genre" to (fav.genre ?: ""),
                        "isDevpick" to fav.isDevpick,
                        "favoritedAt" to System.currentTimeMillis()
                    )
                    userDoc.collection("favorites").document(docId)
                        .set(favData, SetOptions.merge())
                        .await()
                    uploadedFavorites++
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync favorite track ${fav.title} to Firestore: ${e.message}")
                }
            }

            // 3. Sync Play History & Pending Events
            try {
                syncPendingPlayEvents(context, userId)
            } catch (e: Exception) {
                Log.w(TAG, "Error flushing pending play events: ${e.message}")
            }
            var uploadedPlayedTracks = 0
            val playedTracks = dao.getAllTracksSync().filter { it.playCount > 0 || it.lastPlayedTimestamp != null }
            for (track in playedTracks) {
                try {
                    val rawUrl = track.audioUrl
                    val videoId = extractVideoId(rawUrl)
                    val docId = if (videoId.isNotBlank()) {
                        videoId
                    } else {
                        sanitizeDocumentId(
                            (if (rawUrl.isNotBlank()) rawUrl else "${track.title}_${track.artist}").ifBlank { "track_${track.title.hashCode()}" }
                        )
                    }
                    if (docId.isBlank()) continue
                    val playedData = hashMapOf<String, Any?>(
                        "videoId" to (if (videoId.isNotBlank()) videoId else docId),
                        "songId" to (if (videoId.isNotBlank()) videoId else docId),
                        "title" to track.title,
                        "artist" to track.artist,
                        "audioUrl" to track.audioUrl,
                        "albumArtUrl" to (track.albumArtUrl ?: ""),
                        "album" to (track.album ?: ""),
                        "genre" to (track.genre ?: ""),
                        "isDevpick" to track.isDevpick,
                        "playCount" to track.playCount,
                        "lastPlayedTimestamp" to track.lastPlayedTimestamp
                    )
                    userDoc.collection("played_tracks").document(docId).set(
                        playedData,
                        SetOptions.merge()
                    ).await()
                    uploadedPlayedTracks++
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync played track ${track.title} to Firestore: ${e.message}")
                }
            }
            val totalPlays = playedTracks.sumOf { it.playCount }

            // 4. Sync Followed Artists
            val followedArtists = dao.getFollowedArtistsSync()
            var uploadedArtists = 0
            for (art in followedArtists) {
                try {
                    val cleanName = art.name.trim()
                    if (cleanName.isBlank()) continue
                    val cleanDocId = sanitizeDocumentId(cleanName.lowercase())
                    if (cleanDocId.isNotBlank()) {
                        userDoc.collection("followed_artists").document(cleanDocId).set(
                            hashMapOf(
                                "id" to art.id.ifBlank { cleanName },
                                "name" to cleanName,
                                "imageUrl" to (art.imageUrl ?: ""),
                                "followedAt" to art.followedAt
                            ),
                            SetOptions.merge()
                        ).await()
                        uploadedArtists++
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync followed artist ${art.name} to Firestore: ${e.message}")
                }
            }

            // 5. Sync Local DevPicks to Global devpicks Collection
            var uploadedDevPicks = 0
            try {
                val devPicks = dao.getDevPickTracksSync()
                for (dp in devPicks) {
                    val rawUrl = dp.audioUrl
                    val videoId = extractVideoId(rawUrl)
                    val docId = if (videoId.isNotBlank()) videoId else sanitizeDocumentId(rawUrl.ifBlank { dp.title })
                    if (docId.isBlank()) continue
                    val devPickData = hashMapOf(
                        "videoId" to (if (videoId.isNotBlank()) videoId else docId),
                        "songId" to (if (videoId.isNotBlank()) videoId else docId),
                        "audioUrl" to rawUrl,
                        "title" to dp.title,
                        "artist" to dp.artist,
                        "albumArtUrl" to (dp.albumArtUrl ?: ""),
                        "album" to (dp.album ?: ""),
                        "genre" to (dp.genre ?: ""),
                        "isDevpick" to true,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    db.collection("devpicks").document(docId).set(devPickData, SetOptions.merge()).await()
                    uploadedDevPicks++
                }
                Log.d(TAG, "Uploaded $uploadedDevPicks devpicks to Firestore")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to upload local devpicks to Firestore: ${e.message}")
            }

            // 6. Update Complete User Sync Metadata
            try {
                val authManager = com.gaminghub.musicplayer.auth.AuthManager.getInstance(context)
                userDoc.set(
                    hashMapOf(
                        "userId" to userId,
                        "email" to (authManager.googleEmail.value ?: ""),
                        "displayName" to (authManager.googleDisplayName.value ?: ""),
                        "photoUrl" to (authManager.googlePhotoUrl.value ?: ""),
                        "deviceModel" to android.os.Build.MODEL,
                        "deviceManufacturer" to android.os.Build.MANUFACTURER,
                        "lastSyncTimestamp" to System.currentTimeMillis(),
                        "lastActiveTimestamp" to System.currentTimeMillis(),
                        "playlistCount" to uploadedPlaylists,
                        "favoriteCount" to uploadedFavorites,
                        "playedTrackCount" to uploadedPlayedTracks,
                        "followedArtistCount" to uploadedArtists,
                        "totalPlays" to totalPlays
                    ),
                    SetOptions.merge()
                ).await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update sync metadata: ${e.message}")
            }

            CrashReporter.log("Synced $uploadedPlaylists playlists, $uploadedFavorites favorites, $uploadedPlayedTracks played tracks, $uploadedArtists followed artists to Firestore for user $userId")
            Result.success("Cloud Sync complete: $uploadedPlaylists playlists, $uploadedFavorites favorites, $uploadedPlayedTracks played songs & $uploadedArtists followed artists backed up to Firestore!")
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            Result.failure(e)
        }
    }

    /**
     * Downloads playlists and favorites from Cloud Firestore into local Room DB
     */
    suspend fun syncCloudToLocal(context: Context, userId: String): Result<String> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(Exception("Firestore not available"))
        try {
            val roomDb = MusicDatabase.getInstance(context)
            val dao = roomDb.dao
            val userDoc = db.collection("users").document(userId)

            var restoredPlaylists = 0
            var restoredSongs = 0
            var restoredFavorites = 0

            // 1. Download Playlists
            val playlistDocs = userDoc.collection("playlists").get().await()
            val existingPlaylists = dao.getPlaylistsSync().toMutableList()
            for (doc in playlistDocs.documents) {
                val name = doc.getString("name") ?: continue
                var plEntity = existingPlaylists.find { it.name.equals(name, ignoreCase = true) }
                if (plEntity == null) {
                    dao.insertPlaylist(PlaylistEntity(name = name))
                    plEntity = dao.getPlaylistsSync().find { it.name.equals(name, ignoreCase = true) }
                    if (plEntity != null) existingPlaylists.add(plEntity)
                }
                val plId = plEntity?.id ?: continue
                restoredPlaylists++

                val tracksList = doc.get("tracks") as? List<Map<String, Any?>> ?: emptyList()
                for (item in tracksList) {
                    val title = item["title"] as? String ?: ""
                    val artist = item["artist"] as? String ?: "Unknown Artist"
                    val audioUrl = item["audioUrl"] as? String ?: ""
                    val albumArtUrl = item["albumArtUrl"] as? String
                    val album = item["album"] as? String
                    val genre = item["genre"] as? String

                    if (title.isNotBlank() && audioUrl.isNotBlank()) {
                        val existingTrack = dao.getTrackByUrl(audioUrl)
                        if (existingTrack == null) {
                            dao.insertTrack(
                                TrackEntity(
                                    audioUrl = audioUrl,
                                    title = title,
                                    artist = artist,
                                    albumArtUrl = albumArtUrl,
                                    album = album,
                                    genre = genre
                                )
                            )
                        }
                        dao.addTrackToPlaylist(PlaylistTrackEntity(playlistId = plId, audioUrl = audioUrl))
                        restoredSongs++
                    }
                }
            }

            // 2. Download Favorites
            val favoriteDocs = userDoc.collection("favorites").get().await()
            for (doc in favoriteDocs.documents) {
                try {
                    val title = doc.getString("title") ?: continue
                    val artist = doc.getString("artist") ?: "Unknown Artist"
                    val rawAudioUrl = doc.getString("audioUrl")
                    val videoId = doc.getString("videoId") ?: doc.id
                    val audioUrl = if (!rawAudioUrl.isNullOrBlank()) {
                        rawAudioUrl
                    } else if (videoId.isNotBlank() && !videoId.startsWith("track_")) {
                        "https://www.youtube.com/watch?v=$videoId"
                    } else {
                        continue
                    }
                    val albumArtUrl = doc.getString("albumArtUrl")

                    val existingTrack = dao.getTrackByUrl(audioUrl)
                    if (existingTrack != null) {
                        if (!existingTrack.isFavorite) {
                            dao.insertTrack(existingTrack.copy(isFavorite = true))
                        }
                    } else {
                        dao.insertTrack(
                            TrackEntity(
                                audioUrl = audioUrl,
                                title = title,
                                artist = artist,
                                albumArtUrl = albumArtUrl,
                                isFavorite = true
                            )
                        )
                    }
                    restoredFavorites++
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to restore favorite track: ${e.message}")
                }
            }

            // 3. Restore Play History & Played Tracks
            val restoredPlayedTracks = try {
                syncPlayHistoryFromCloud(context, userId).getOrDefault(0)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to restore play history: ${e.message}")
                0
            }

            // 4. Restore Followed Artists
            var restoredArtists = 0
            val artistDocs = userDoc.collection("followed_artists").get().await()
            for (doc in artistDocs.documents) {
                try {
                    val name = (doc.getString("name") ?: doc.id).trim()
                    val id = (doc.getString("id") ?: name).trim()
                    val imageUrl = doc.getString("imageUrl")
                    val followedAt = doc.getLong("followedAt") ?: System.currentTimeMillis()
                    if (name.isNotBlank()) {
                        dao.insertFollowedArtist(
                            FollowedArtistEntity(
                                id = id.ifBlank { name },
                                name = name,
                                imageUrl = if (!imageUrl.isNullOrBlank()) imageUrl else null,
                                followedAt = followedAt
                            )
                        )
                        restoredArtists++
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to restore followed artist: ${e.message}")
                }
            }

            // 5. Restore Global Developer's Picks (DevPicks)
            val restoredDevPicks = try {
                syncDevPicksFromCloud(context).getOrDefault(0)
            } catch (e: Exception) {
                Log.w(TAG, "DevPicks sync note: ${e.message}")
                0
            }

            Result.success("Restored $restoredPlaylists playlists, $restoredFavorites favorites, $restoredPlayedTracks played songs, $restoredArtists followed artists & $restoredDevPicks dev picks from Cloud Firestore!")
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            Result.failure(e)
        }
    }

    /**
     * Real-time Cloud Firestore updates for individual playlist changes
     */
    suspend fun savePlaylist(userId: String, playlistId: Long, name: String, tracks: List<TrackModel>) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        try {
            val firestoreTracks = tracks.map { t ->
                FirestoreTrack(
                    videoId = extractVideoId(t.audioUrl),
                    title = t.title,
                    artist = t.artist,
                    audioUrl = t.audioUrl ?: "",
                    albumArtUrl = t.albumArtUrl ?: "",
                    album = t.album ?: "",
                    genre = t.genre ?: ""
                )
            }
            db.collection("users").document(userId)
                .collection("playlists").document(playlistId.toString())
                .set(
                    hashMapOf(
                        "id" to playlistId.toString(),
                        "name" to name,
                        "updatedAt" to System.currentTimeMillis(),
                        "tracks" to firestoreTracks
                    ),
                    SetOptions.merge()
                ).await()
            Log.d(TAG, "Saved playlist to Firestore for $userId: $name (id=$playlistId, tracks=${firestoreTracks.size})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save playlist to Firestore: ${e.message}")
            CrashReporter.recordException(e)
        }
    }

    /**
     * Real-time Cloud Firestore updates for playlist deletion
     */
    suspend fun deletePlaylist(userId: String, playlistId: Long) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        try {
            db.collection("users").document(userId)
                .collection("playlists").document(playlistId.toString())
                .delete()
                .await()
            Log.d(TAG, "Deleted playlist from Firestore for $userId: id=$playlistId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete playlist from Firestore: ${e.message}")
            CrashReporter.recordException(e)
        }
    }

    /**
     * Real-time Cloud Firestore updates for favorite toggle
     */
    suspend fun updateFavorite(userId: String, track: TrackModel, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        if (userId.isBlank()) return@withContext
        try {
            val rawUrl = track.audioUrl ?: ""
            val videoId = extractVideoId(rawUrl)
            val docId = if (videoId.isNotBlank()) {
                videoId
            } else {
                sanitizeDocumentId(
                    (if (rawUrl.isNotBlank()) rawUrl else "${track.title}_${track.artist}").ifBlank { "track_${track.title.hashCode()}" }
                )
            }
            if (docId.isBlank()) return@withContext
            val favDoc = db.collection("users").document(userId).collection("favorites").document(docId)
            if (isFavorite) {
                favDoc.set(
                    hashMapOf(
                        "videoId" to (if (videoId.isNotBlank()) videoId else docId),
                        "title" to track.title,
                        "artist" to track.artist,
                        "audioUrl" to rawUrl,
                        "albumArtUrl" to (track.albumArtUrl ?: ""),
                        "favoritedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()
                Log.d(TAG, "Added favorite to Firestore for $userId: ${track.title} ($docId)")
            } else {
                favDoc.delete().await()
                if (videoId.isNotBlank() && videoId != docId) {
                    try {
                        db.collection("users").document(userId).collection("favorites").document(videoId).delete().await()
                    } catch (_: Exception) {}
                }
                Log.d(TAG, "Removed favorite from Firestore for $userId: ${track.title} ($docId)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update favorite in Firestore: ${e.message}")
            CrashReporter.recordException(e)
        }
    }

    /**
     * Real-time Cloud Firestore updates for followed artist toggle
     */
    suspend fun updateFollowedArtist(userId: String, artistName: String, imageUrl: String? = null, isFollowed: Boolean) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        if (userId.isBlank()) return@withContext
        val cleanName = artistName.trim()
        if (cleanName.isBlank()) return@withContext
        try {
            val cleanDocId = sanitizeDocumentId(cleanName.lowercase())
            if (cleanDocId.isBlank()) return@withContext
            val docRef = db.collection("users").document(userId).collection("followed_artists").document(cleanDocId)
            if (isFollowed) {
                docRef.set(
                    hashMapOf(
                        "id" to cleanName,
                        "name" to cleanName,
                        "imageUrl" to (imageUrl ?: ""),
                        "followedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()
                Log.d(TAG, "Followed artist in Firestore for $userId: $cleanName ($cleanDocId)")
            } else {
                docRef.delete().await()
                val rawDocId = sanitizeDocumentId(cleanName)
                if (rawDocId != cleanDocId && rawDocId.isNotBlank()) {
                    try {
                        db.collection("users").document(userId).collection("followed_artists").document(rawDocId).delete().await()
                    } catch (_: Exception) {}
                }
                Log.d(TAG, "Unfollowed artist in Firestore for $userId: $cleanName ($cleanDocId)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update followed artist in Firestore: ${e.message}")
            CrashReporter.recordException(e)
        }
    }

    /**
     * Real-time Cloud Firestore updates for removing a single track from listening history
     */
    suspend fun removeFromHistory(userId: String, track: TrackModel) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        if (userId.isBlank()) return@withContext
        try {
            val rawUrl = track.audioUrl ?: ""
            val videoId = extractVideoId(rawUrl)
            val docId = if (videoId.isNotBlank()) videoId else sanitizeDocumentId(rawUrl)
            if (docId.isBlank()) return@withContext
            db.collection("users").document(userId)
                .collection("played_tracks").document(docId)
                .set(hashMapOf<String, Any?>("lastPlayedTimestamp" to null), SetOptions.merge())
                .await()
            Log.d(TAG, "Removed track from Firestore listening history for $userId: $docId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove track from history: ${e.message}")
            CrashReporter.recordException(e)
        }
    }

    /**
     * Real-time Cloud Firestore updates for clearing all listening history
     */
    suspend fun clearHistory(userId: String) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        try {
            val playedDocs = db.collection("users").document(userId).collection("played_tracks").get().await()
            val batch = db.batch()
            for (doc in playedDocs.documents) {
                batch.update(doc.reference, "lastPlayedTimestamp", null)
            }
            batch.commit().await()
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
    }

    /**
     * Real-time Cloud Firestore updates for Developer's Picks (DevPicks).
     * Saves using the track's YouTube videoId / songId as the Firestore document ID.
     */
    suspend fun updateDevPick(track: TrackModel, isDevpick: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(Exception("Firestore not initialized"))
        try {
            val rawUrl = track.audioUrl ?: ""
            val videoId = extractVideoId(rawUrl)
            val docId = if (videoId.isNotBlank()) videoId else sanitizeDocumentId(rawUrl.ifBlank { track.title })
            if (docId.isBlank()) return@withContext Result.failure(Exception("Invalid document ID"))

            val devPickDoc = db.collection("devpicks").document(docId)
            if (isDevpick) {
                val map = hashMapOf(
                    "videoId" to (if (videoId.isNotBlank()) videoId else docId),
                    "songId" to (if (videoId.isNotBlank()) videoId else docId),
                    "audioUrl" to rawUrl,
                    "title" to track.title,
                    "artist" to track.artist,
                    "albumArtUrl" to (track.albumArtUrl ?: ""),
                    "album" to (track.album ?: ""),
                    "genre" to (track.genre ?: ""),
                    "durationSeconds" to track.durationSeconds,
                    "uploaderChannel" to (track.uploaderChannel ?: ""),
                    "isDevpick" to true,
                    "updatedAt" to System.currentTimeMillis()
                )
                devPickDoc.set(map, SetOptions.merge()).await()
                Log.d(TAG, "Added devpick to Firestore with videoId/songId: ${track.title} ($docId)")
            } else {
                devPickDoc.delete().await()
                // Backward-compatible cleanup for any previous url-escaped doc IDs
                val legacyDocId = rawUrl.replace("/", "_").replace(":", "_").replace("?", "_").replace("=", "_")
                if (legacyDocId != docId && legacyDocId.isNotBlank()) {
                    try {
                        db.collection("devpicks").document(legacyDocId).delete().await()
                    } catch (_: Exception) {}
                }
                Log.d(TAG, "Removed devpick from Firestore: ${track.title} ($docId)")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update devpick in Firestore: ${e.message}", e)
            CrashReporter.recordException(e)
            Result.failure(e)
        }
    }

    /**
     * Syncs all Developer's Picks from Cloud Firestore into Room DB
     */
    suspend fun syncDevPicksFromCloud(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(Exception("Firestore not available"))
        try {
            val roomDb = MusicDatabase.getInstance(context)
            val dao = roomDb.dao
            val snap = db.collection("devpicks").get().await()
            var count = 0
            for (doc in snap.documents) {
                val videoId = doc.getString("videoId") ?: doc.getString("songId") ?: doc.id
                val rawAudioUrl = doc.getString("audioUrl")
                val audioUrl = if (!rawAudioUrl.isNullOrBlank()) {
                    rawAudioUrl
                } else if (videoId.isNotBlank() && !videoId.startsWith("track_")) {
                    "https://www.youtube.com/watch?v=$videoId"
                } else {
                    continue
                }
                val title = doc.getString("title") ?: "Track"
                val artist = doc.getString("artist") ?: "Unknown"
                val albumArtUrl = doc.getString("albumArtUrl")
                val album = doc.getString("album")
                val genre = doc.getString("genre")

                val existing = dao.getTrackByUrl(audioUrl)
                if (existing != null) {
                    if (!existing.isDevpick) {
                        dao.insertTrack(existing.copy(isDevpick = true))
                        count++
                    }
                } else {
                    dao.insertTrack(
                        TrackEntity(
                            audioUrl = audioUrl,
                            title = title,
                            artist = artist,
                            albumArtUrl = albumArtUrl,
                            album = album,
                            genre = genre,
                            isDevpick = true
                        )
                    )
                    count++
                }
            }
            Log.d(TAG, "Synced $count devpicks from Firestore into local DB")
            Result.success(count)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync devpicks from Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Sanitizes strings for safe Firestore document ID usage
     */
    fun sanitizeDocumentId(raw: String): String {
        return raw.trim()
            .replace("/", "_")
            .replace("\\", "_")
            .replace(".", "_")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")
    }

    /**
     * Comprehensive bidirectional sync (downloads cloud updates to local DB, then pushes local data)
     */
    suspend fun syncAll(context: Context, userId: String): Result<String> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.failure(Exception("Invalid user ID"))
        val db = firestore ?: return@withContext Result.failure(Exception("Firestore not available"))
        if (!isSyncing.compareAndSet(false, true)) {
            Log.d(TAG, "Sync already in progress, skipping concurrent syncAll call")
            return@withContext Result.success("Sync already in progress")
        }
        try {
            Log.d(TAG, "Starting full bidirectional sync for user $userId")
            val pullResult = syncCloudToLocal(context, userId)
            val pushResult = syncLocalToCloud(context, userId)
            val summary = "Sync complete: Pull [${pullResult.isSuccess}] | Push [${pushResult.isSuccess}]"
            Log.d(TAG, summary)
            Result.success(summary)
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncAll: ${e.message}")
            Result.failure(e)
        } finally {
            isSyncing.set(false)
        }
    }

    /**
     * Records a play event in real-time if connected.
     * When online, writes to users/{userId}/play_events and updates users/{userId}/played_tracks.
     * If offline, it fails gracefully and remains queued in Room (syncedToFirestore = false)
     * until NetworkMonitor flushes it upon internet reconnection.
     */
    suspend fun recordPlayEventRealtime(
        context: Context,
        userId: String,
        track: TrackModel
    ) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        val url = track.audioUrl ?: return@withContext
        if (url.isBlank()) return@withContext

        try {
            val videoId = extractVideoId(url)
            val now = System.currentTimeMillis()
            val userDoc = db.collection("users").document(userId)

            val eventRef = userDoc.collection("play_events").document()
            val eventData = hashMapOf(
                "eventId" to eventRef.id,
                "videoId" to videoId,
                "audioUrl" to url,
                "title" to track.title,
                "artist" to track.artist,
                "playedAt" to now
            )

            val trackDocId = if (videoId.isNotBlank()) videoId else sanitizeDocumentId(url)
            val trackRef = userDoc.collection("played_tracks").document(trackDocId)
            val trackData = hashMapOf(
                "videoId" to videoId,
                "title" to track.title,
                "artist" to track.artist,
                "audioUrl" to url,
                "albumArtUrl" to (track.albumArtUrl ?: ""),
                "lastPlayedTimestamp" to now,
                "playCount" to FieldValue.increment(1)
            )

            val batch = db.batch()
            batch.set(eventRef, eventData)
            batch.set(trackRef, trackData, SetOptions.merge())

            // ── Global Weekly & All-Time Top Songs (Cloud Firestore) ─────────
            val weekKey = getWeeklyChartKey(now)
            if (trackDocId.isNotBlank()) {
                val globalTrackData = hashMapOf(
                    "weekKey" to weekKey,
                    "videoId" to videoId,
                    "title" to track.title,
                    "artist" to track.artist,
                    "audioUrl" to url,
                    "albumArtUrl" to (track.albumArtUrl ?: ""),
                    "lastPlayedTimestamp" to now,
                    "playCount" to FieldValue.increment(1)
                )
                // 1. Weekly Top Songs (Cloud Firestore under users/global_charts)
                val globalWeeklyRef = db.collection("users")
                    .document("global_charts")
                    .collection("weekly_top_songs")
                    .document(weekKey)
                    .collection("tracks")
                    .document(trackDocId)
                batch.set(globalWeeklyRef, globalTrackData, SetOptions.merge())

                // 2. All-Time Top Songs (Cloud Firestore under users/global_charts)
                val globalAllTimeRef = db.collection("users")
                    .document("global_charts")
                    .collection("all_time_top_songs")
                    .document(trackDocId)
                batch.set(globalAllTimeRef, globalTrackData, SetOptions.merge())
            }

            batch.set(
                userDoc,
                hashMapOf<String, Any>(
                    "totalPlays" to FieldValue.increment(1),
                    "lastActiveTimestamp" to now
                ),
                SetOptions.merge()
            )
            val committed = withTimeoutOrNull(5000L) {
                batch.commit().await()
                true
            } ?: false

            if (committed) {
                // Mark local event as synced in Room
                val roomDb = MusicDatabase.getInstance(context)
                val unsynced = roomDb.dao.getUnsyncedPlayEvents(10)
                val matched = unsynced.filter { it.audioUrl == url && Math.abs(it.playedAt - now) < 5000L }
                if (matched.isNotEmpty()) {
                    roomDb.dao.markPlayEventsSynced(matched.map { it.id })
                }
                Log.d(TAG, "Recorded real-time play event in Firestore for user $userId ($videoId)")
            } else {
                Log.d(TAG, "Firestore write buffered / backend offline for user $userId ($videoId)")
            }
        } catch (e: Exception) {
            // Offline or network error: queued in Room with syncedToFirestore = false
            Log.d(TAG, "Pending offline play event queued in Room: ${e.message}")
        }
    }

    /**
     * Flushes all offline-recorded playback events from Room to Cloud Firestore.
     * Called automatically by NetworkMonitor whenever internet connectivity is restored.
     */
    suspend fun syncPendingPlayEvents(context: Context, userId: String): Result<Int> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(Exception("Firestore not available"))
        try {
            val roomDb = MusicDatabase.getInstance(context)
            val dao = roomDb.dao
            val unsyncedEvents = dao.getUnsyncedPlayEvents(100)
            if (unsyncedEvents.isEmpty()) {
                return@withContext Result.success(0)
            }

            Log.d(TAG, "Syncing ${unsyncedEvents.size} pending offline play events for user $userId")
            val userDoc = db.collection("users").document(userId)
            val batch = db.batch()
            val syncedIds = mutableListOf<Long>()

            val countsPerUrl = mutableMapOf<String, Int>()
            val latestTimePerUrl = mutableMapOf<String, Long>()

            for (event in unsyncedEvents) {
                val videoId = extractVideoId(event.audioUrl)
                val eventRef = userDoc.collection("play_events").document("pe_${event.id}_${event.playedAt}")
                val eventData = hashMapOf(
                    "eventId" to eventRef.id,
                    "videoId" to videoId,
                    "audioUrl" to event.audioUrl,
                    "playedAt" to event.playedAt
                )
                batch.set(eventRef, eventData, SetOptions.merge())
                syncedIds.add(event.id)

                countsPerUrl[event.audioUrl] = (countsPerUrl[event.audioUrl] ?: 0) + 1
                val prevLatest = latestTimePerUrl[event.audioUrl] ?: 0L
                if (event.playedAt > prevLatest) {
                    latestTimePerUrl[event.audioUrl] = event.playedAt
                }
            }

            // ── Per-user played_tracks + Global weekly_top_songs ──────────
            val weekKey = getWeeklyChartKey()
            for ((url, count) in countsPerUrl) {
                val videoId = extractVideoId(url)
                val trackEntity = dao.getTrackByUrl(url)
                val trackDocId = if (videoId.isNotBlank()) videoId else sanitizeDocumentId(url)

                // 1. Per-user played_tracks (unchanged)
                val trackRef = userDoc.collection("played_tracks").document(trackDocId)
                val trackData = hashMapOf(
                    "videoId" to videoId,
                    "title" to (trackEntity?.title ?: "Track"),
                    "artist" to (trackEntity?.artist ?: "Unknown"),
                    "audioUrl" to url,
                    "albumArtUrl" to (trackEntity?.albumArtUrl ?: ""),
                    "lastPlayedTimestamp" to (latestTimePerUrl[url] ?: System.currentTimeMillis()),
                    "playCount" to FieldValue.increment(count.toLong())
                )
                batch.set(trackRef, trackData, SetOptions.merge())

                // 2. Global weekly leaderboard across all users
                if (trackDocId.isNotBlank()) {
                    val globalData = hashMapOf(
                        "weekKey"   to weekKey,
                        "videoId"   to videoId,
                        "title"     to (trackEntity?.title ?: "Track"),
                        "artist"    to (trackEntity?.artist ?: "Unknown"),
                        "audioUrl"  to url,
                        "albumArtUrl" to (trackEntity?.albumArtUrl ?: ""),
                        "playCount" to FieldValue.increment(count.toLong()),
                        "lastPlayedTimestamp" to (latestTimePerUrl[url] ?: System.currentTimeMillis())
                    )
                    // 1. Weekly Global
                    val userWeeklyRef = db.collection("users")
                        .document("global_charts")
                        .collection("weekly_top_songs")
                        .document(weekKey)
                        .collection("tracks")
                        .document(trackDocId)
                    batch.set(userWeeklyRef, globalData, SetOptions.merge())

                    // 2. All-Time Global
                    val userAllTimeRef = db.collection("users")
                        .document("global_charts")
                        .collection("all_time_top_songs")
                        .document(trackDocId)
                    batch.set(userAllTimeRef, globalData, SetOptions.merge())

                    try {
                        db.collection("weekly_top_songs").document(weekKey).collection("tracks").document(trackDocId).set(globalData, SetOptions.merge())
                        db.collection("all_time_top_songs").document(trackDocId).set(globalData, SetOptions.merge())
                    } catch (_: Exception) {}
                }
            }

            batch.set(
                userDoc,
                hashMapOf(
                    "totalPlays" to FieldValue.increment(syncedIds.size.toLong()),
                    "lastActiveTimestamp" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )

            val committed = withTimeoutOrNull(5000L) {
                batch.commit().await()
                true
            } ?: false

            if (committed) {
                dao.markPlayEventsSynced(syncedIds)
                Log.d(TAG, "Successfully synced ${syncedIds.size} offline play events to Firestore for user $userId")
                Result.success(syncedIds.size)
            } else {
                Log.d(TAG, "Firestore write timed out / offline: ${syncedIds.size} play events queued in local cache")
                Result.success(0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed syncing pending play events: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Downloads played tracks and recent play events from Cloud Firestore into Room
     * for multi-device sync and complete cloud restore.
     */
    suspend fun syncPlayHistoryFromCloud(context: Context, userId: String): Result<Int> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(Exception("Firestore not available"))
        try {
            val roomDb = MusicDatabase.getInstance(context)
            val dao = roomDb.dao
            val userDoc = db.collection("users").document(userId)

            val playedTracksSnap = userDoc.collection("played_tracks").get().await()
            var restoredCount = 0

            for (doc in playedTracksSnap.documents) {
                val audioUrl = doc.getString("audioUrl") ?: ""
                if (audioUrl.isBlank()) continue
                val title = doc.getString("title") ?: "Track"
                val artist = doc.getString("artist") ?: "Unknown"
                val albumArtUrl = doc.getString("albumArtUrl")
                val cloudPlayCount = (doc.getLong("playCount") ?: 0L).toInt()
                val cloudLastPlayed = doc.getLong("lastPlayedTimestamp")

                val localTrack = dao.getTrackByUrl(audioUrl)
                if (localTrack == null) {
                    dao.insertTrack(
                        TrackEntity(
                            audioUrl = audioUrl,
                            title = title,
                            artist = artist,
                            albumArtUrl = albumArtUrl,
                            playCount = cloudPlayCount,
                            lastPlayedTimestamp = cloudLastPlayed
                        )
                    )
                    restoredCount++
                } else {
                    val mergedPlayCount = maxOf(localTrack.playCount, cloudPlayCount)
                    val mergedTimestamp = maxOf(localTrack.lastPlayedTimestamp ?: 0L, cloudLastPlayed ?: 0L)
                    dao.insertTrack(
                        localTrack.copy(
                            playCount = mergedPlayCount,
                            lastPlayedTimestamp = if (mergedTimestamp > 0L) mergedTimestamp else null
                        )
                    )
                    restoredCount++
                }
            }

            // Restore recent play events (up to 100 for rolling 7-day charts)
            try {
                val eventsSnap = userDoc.collection("play_events")
                    .orderBy("playedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(100)
                    .get()
                    .await()

                for (doc in eventsSnap.documents) {
                    val url = doc.getString("audioUrl") ?: ""
                    val playedAt = doc.getLong("playedAt") ?: 0L
                    if (url.isNotBlank() && playedAt > 0L) {
                        dao.insertPlayEvent(
                            PlayEventEntity(
                                audioUrl = url,
                                playedAt = playedAt,
                                syncedToFirestore = true
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Play events query note: ${e.message}")
            }

            Log.d(TAG, "Restored $restoredCount played tracks from Firestore for user $userId")
            Result.success(restoredCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring play history from Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    fun getWeeklyChartKey(timestamp: Long = System.currentTimeMillis()): String {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = timestamp
            firstDayOfWeek = java.util.Calendar.MONDAY
        }
        val year = cal.get(java.util.Calendar.YEAR)
        val week = cal.get(java.util.Calendar.WEEK_OF_YEAR)
        return String.format(java.util.Locale.US, "%d_w%02d", year, week)
    }

    fun getPreviousWeeklyChartKey(): String {
        return getWeeklyChartKey(System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000))
    }

    /**
     * Fetches the top songs played by ALL users in the past 7 days from Cloud Firestore.
     * Prioritizes `users/global_charts/weekly_top_songs/{weekKey}/tracks` to align with Firestore security rules.
     */
    suspend fun fetchWeeklyTopSongsAllUsers(limit: Int = 30): Result<List<TrackModel>> =
        withContext(Dispatchers.IO) {
            val db = firestore ?: return@withContext Result.failure(Exception("Firestore not available"))
            try {
                val currentWeekKey = getWeeklyChartKey()
                val prevWeekKey = getPreviousWeeklyChartKey()

                // 1. Fetch current week's top tracks across all users
                val currentSnap = try {
                    db.collection("users")
                        .document("global_charts")
                        .collection("weekly_top_songs")
                        .document(currentWeekKey)
                        .collection("tracks")
                        .orderBy("playCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(limit.toLong())
                        .get()
                        .await()
                } catch (e: Exception) {
                    Log.d(TAG, "users/global_charts query error, trying root weekly_top_songs: ${e.message}")
                    try {
                        db.collection("weekly_top_songs")
                            .document(currentWeekKey)
                            .collection("tracks")
                            .orderBy("playCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .limit(limit.toLong())
                            .get()
                            .await()
                    } catch (_: Exception) { null }
                }

                val currentTracks = currentSnap?.documents?.mapNotNull { doc ->
                    val title     = doc.getString("title")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val artist    = doc.getString("artist") ?: "Unknown"
                    val audioUrl  = doc.getString("audioUrl") ?: ""
                    val artUrl    = doc.getString("albumArtUrl")
                    val playCount = (doc.getLong("playCount") ?: 0L).toInt()
                    TrackModel(
                        title       = title,
                        artist      = artist,
                        audioUrl    = audioUrl.ifBlank { null },
                        albumArtUrl = artUrl,
                        playcount   = playCount
                    )
                } ?: emptyList()

                if (currentTracks.size >= 10) {
                    Log.d(TAG, "Fetched ${currentTracks.size} weekly top tracks for $currentWeekKey from Cloud")
                    return@withContext Result.success(currentTracks)
                }

                // 2. If current week is early / has fewer tracks, also fetch previous week's tracks
                val prevSnap = try {
                    db.collection("users")
                        .document("global_charts")
                        .collection("weekly_top_songs")
                        .document(prevWeekKey)
                        .collection("tracks")
                        .orderBy("playCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(limit.toLong())
                        .get()
                        .await()
                } catch (_: Exception) {
                    try {
                        db.collection("weekly_top_songs")
                            .document(prevWeekKey)
                            .collection("tracks")
                            .orderBy("playCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .limit(limit.toLong())
                            .get()
                            .await()
                    } catch (_: Exception) { null }
                }

                val prevTracks = prevSnap?.documents?.mapNotNull { doc ->
                    val title     = doc.getString("title")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val artist    = doc.getString("artist") ?: "Unknown"
                    val audioUrl  = doc.getString("audioUrl") ?: ""
                    val artUrl    = doc.getString("albumArtUrl")
                    val playCount = (doc.getLong("playCount") ?: 0L).toInt()
                    TrackModel(
                        title       = title,
                        artist      = artist,
                        audioUrl    = audioUrl.ifBlank { null },
                        albumArtUrl = artUrl,
                        playcount   = playCount
                    )
                } ?: emptyList()

                val combined = (currentTracks + prevTracks)
                    .distinctBy { it.audioUrl ?: it.title }
                    .sortedByDescending { it.playcount }
                    .take(limit)

                Log.d(TAG, "Fetched ${combined.size} combined weekly top tracks ($currentWeekKey + $prevWeekKey) from Cloud")
                Result.success(combined)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch weekly top songs from Cloud: ${e.message}")
                Result.failure(e)
            }
        }

    /**
     * Fetches the All-Time top songs played across ALL users from Cloud Firestore.
     * Queries `users/global_charts/all_time_top_songs` ordered by `playCount DESCENDING`.
     */
    suspend fun fetchGlobalAllTimeTopSongsAllUsers(limit: Int = 50): Result<List<TrackModel>> =
        withContext(Dispatchers.IO) {
            val db = firestore ?: return@withContext Result.failure(Exception("Firestore not available"))
            try {
                val snap = try {
                    db.collection("users")
                        .document("global_charts")
                        .collection("all_time_top_songs")
                        .orderBy("playCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(limit.toLong())
                        .get()
                        .await()
                } catch (e: Exception) {
                    Log.d(TAG, "users/global_charts all_time query error, trying root all_time_top_songs: ${e.message}")
                    try {
                        db.collection("all_time_top_songs")
                            .orderBy("playCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .limit(limit.toLong())
                            .get()
                            .await()
                    } catch (_: Exception) { null }
                }

                val tracks = snap?.documents?.mapNotNull { doc ->
                    val title     = doc.getString("title")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val artist    = doc.getString("artist") ?: "Unknown"
                    val audioUrl  = doc.getString("audioUrl") ?: ""
                    val artUrl    = doc.getString("albumArtUrl")
                    val playCount = (doc.getLong("playCount") ?: 0L).toInt()
                    TrackModel(
                        title       = title,
                        artist      = artist,
                        audioUrl    = audioUrl.ifBlank { null },
                        albumArtUrl = artUrl,
                        playcount   = playCount
                    )
                } ?: emptyList()

                Log.d(TAG, "Fetched ${tracks.size} global all-time top tracks from Cloud")
                Result.success(tracks)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch global all-time top songs from Cloud: ${e.message}")
                Result.failure(e)
            }
        }

    /**
     * Seeds local plays (both weekly and all-time) into Cloud Firestore global charts
     * if the cloud collections are currently empty.
     */
    suspend fun seedGlobalChartsFromLocal(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(Exception("Firestore not available"))
        try {
            val roomDb = MusicDatabase.getInstance(context)
            val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
            val localWeekly = roomDb.dao.getTopPlayedTracksWithCountSince(sevenDaysAgo).first()
            val localAllTime = roomDb.dao.getTop50Tracks().first()

            val weekKey = getWeeklyChartKey()
            val batch = db.batch()
            var count = 0

            // 1. Seed Weekly
            for (item in localWeekly.take(30)) {
                val url = item.track.audioUrl
                val videoId = extractVideoId(url)
                val trackDocId = if (videoId.isNotBlank()) videoId else sanitizeDocumentId(url)
                if (trackDocId.isBlank()) continue

                val weeklyData = hashMapOf(
                    "weekKey" to weekKey,
                    "videoId" to videoId,
                    "title" to item.track.title,
                    "artist" to item.track.artist,
                    "audioUrl" to url,
                    "albumArtUrl" to (item.track.albumArtUrl ?: ""),
                    "playCount" to FieldValue.increment(item.periodPlayCount.toLong().coerceAtLeast(1L)),
                    "lastPlayedTimestamp" to (item.track.lastPlayedTimestamp ?: System.currentTimeMillis())
                )

                val userSubRef = db.collection("users")
                    .document("global_charts")
                    .collection("weekly_top_songs")
                    .document(weekKey)
                    .collection("tracks")
                    .document(trackDocId)
                batch.set(userSubRef, weeklyData, SetOptions.merge())
                count++
            }

            // 2. Seed All-Time
            for (track in localAllTime.take(50)) {
                val url = track.audioUrl
                val videoId = extractVideoId(url)
                val trackDocId = if (videoId.isNotBlank()) videoId else sanitizeDocumentId(url)
                if (trackDocId.isBlank()) continue

                val allTimeData = hashMapOf(
                    "videoId" to videoId,
                    "title" to track.title,
                    "artist" to track.artist,
                    "audioUrl" to url,
                    "albumArtUrl" to (track.albumArtUrl ?: ""),
                    "playCount" to FieldValue.increment(track.playCount.toLong().coerceAtLeast(1L)),
                    "lastPlayedTimestamp" to (track.lastPlayedTimestamp ?: System.currentTimeMillis())
                )

                val userAllTimeRef = db.collection("users")
                    .document("global_charts")
                    .collection("all_time_top_songs")
                    .document(trackDocId)
                batch.set(userAllTimeRef, allTimeData, SetOptions.merge())
                count++
            }

            batch.commit().await()
            Log.d(TAG, "Seeded $count total entries to Cloud global charts (weekly $weekKey + all-time)")
            Result.success(count)
        } catch (e: Exception) {
            Log.w(TAG, "Seeding global charts to Cloud failed: ${e.message}")
            Result.failure(e)
        }
    }
}
