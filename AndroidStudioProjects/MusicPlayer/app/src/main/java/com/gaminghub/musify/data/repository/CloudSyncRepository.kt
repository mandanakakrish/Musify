package com.gaminghub.musify.data.repository

import android.util.Log
import com.gaminghub.musify.TrackModel
import com.gaminghub.musicplayer.data.MusicDao
import com.gaminghub.musify.data.toEntity
import com.gaminghub.musify.data.toModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CloudSyncRepository(
    private val dao: MusicDao
) {
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Automatically start syncing when user is logged in
        scope.launch {
            auth.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user != null) {
                    startSyncTasks(user.uid)
                }
            }
        }
    }

    private fun startSyncTasks(userId: String) {
        // 1. Sync Favorites
        scope.launch {
            dao.getFavoriteTracks().collectLatest { tracks ->
                val favoriteData = mapOf(
                    "tracks" to tracks.map { it.toModel() },
                    "lastUpdated" to System.currentTimeMillis()
                )
                firestore.collection("users").document(userId)
                    .collection("sync").document("favorites")
                    .set(favoriteData, SetOptions.merge())
            }
        }

        // 2. Sync Followed Artists
        scope.launch {
            dao.getFollowedArtists().collectLatest { artists ->
                val artistData = mapOf(
                    "artists" to artists.map { it.toModel() },
                    "lastUpdated" to System.currentTimeMillis()
                )
                firestore.collection("users").document(userId)
                    .collection("sync").document("artists")
                    .set(artistData, SetOptions.merge())
            }
        }

        // 3. Sync Playlists
        scope.launch {
            dao.getPlaylists().collectLatest { playlists ->
                val playlistTasks = playlists.map { playlist ->
                    val tracks = dao.getTracksForPlaylistSync(playlist.id)
                    mapOf(
                        "id" to playlist.id,
                        "name" to playlist.name,
                        "createdAt" to playlist.createdAt,
                        "tracks" to tracks.map { it.toModel() }
                    )
                }
                
                val syncData = mapOf(
                    "playlists" to playlistTasks,
                    "lastUpdated" to System.currentTimeMillis()
                )
                
                firestore.collection("users").document(userId)
                    .collection("sync").document("playlists")
                    .set(syncData, SetOptions.merge())
            }
        }
    }

    suspend fun downloadFromCloud() {
        val user = auth.currentUser ?: run {
            Log.e("CloudSync", "Restore failed: No user authenticated")
            return
        }
        Log.d("CloudSync", "Starting manual restore for user: ${user.uid}")
        try {
            // Restore Favorites
            val favDoc = firestore.collection("users").document(user.uid)
                .collection("sync").document("favorites").get().await()
            
            if (favDoc.exists()) {
                val cloudTracks = favDoc.get("tracks") as? List<Map<String, Any>>
                Log.d("CloudSync", "Restoring ${cloudTracks?.size ?: 0} favorites")
                cloudTracks?.forEach { map ->
                    val track = mapToTrackModel(map)
                    dao.insertTrack(track.toEntity(isFavorite = true))
                }
            } else {
                Log.d("CloudSync", "No favorites found in cloud")
            }

            // Restore Artists
            val artDoc = firestore.collection("users").document(user.uid)
                .collection("sync").document("artists").get().await()
            
            if (artDoc.exists()) {
                val cloudArtists = artDoc.get("artists") as? List<Map<String, Any>>
                Log.d("CloudSync", "Restoring ${cloudArtists?.size ?: 0} artists")
                cloudArtists?.forEach { map ->
                    val id = map["id"] as? String ?: return@forEach
                    dao.insertFollowedArtist(com.gaminghub.musify.data.FollowedArtistEntity(
                        id = id,
                        name = map["name"] as? String ?: "",
                        imageUrl = map["imageUrl"] as? String
                    ))
                }
            }

            // Restore Playlists
            val playDoc = firestore.collection("users").document(user.uid)
                .collection("sync").document("playlists").get().await()
            
            if (playDoc.exists()) {
                val cloudPlaylists = playDoc.get("playlists") as? List<Map<String, Any>>
                Log.d("CloudSync", "Restoring ${cloudPlaylists?.size ?: 0} playlists")
                cloudPlaylists?.forEach { pMap ->
                    val pName = pMap["name"] as? String ?: "Restored Playlist"
                    val pCreatedAt = pMap["createdAt"] as? Long ?: System.currentTimeMillis()
                    
                    // Create local playlist
                    val playlistEntity = com.gaminghub.musicplayer.data.PlaylistEntity(name = pName, createdAt = pCreatedAt)
                    dao.insertPlaylist(playlistEntity)
                    
                    // Re-fetch to find the generated ID
                    val allPlaylists = dao.getPlaylistsSync()
                    val newPlaylist = allPlaylists.find { it.name == pName } // Simpler match
                    
                    if (newPlaylist != null) {
                        val pTracks = pMap["tracks"] as? List<Map<String, Any>>
                        pTracks?.forEach { tMap ->
                            val track = mapToTrackModel(tMap)
                            dao.insertTrack(track.toEntity())
                            dao.addTrackToPlaylist(com.gaminghub.musify.data.PlaylistTrackEntity(
                                playlistId = newPlaylist.id,
                                audioUrl = track.audioUrl ?: ""
                            ))
                        }
                    }
                }
            }
            Log.d("CloudSync", "Restore completed successfully")
        } catch (e: Exception) {
            Log.e("CloudSync", "Restore failed: ${e.message}", e)
        }
    }

    private fun mapToTrackModel(map: Map<String, Any>): TrackModel {
        return TrackModel(
            title = map["title"] as? String ?: "",
            artist = map["artist"] as? String ?: "",
            audioUrl = map["audioUrl"] as? String,
            albumArtUrl = map["albumArtUrl"] as? String
        )
    }
}
