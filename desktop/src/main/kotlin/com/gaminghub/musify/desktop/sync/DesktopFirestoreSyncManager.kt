package com.gaminghub.musify.desktop.sync

import com.gaminghub.musify.desktop.data.DesktopDatabase
import com.gaminghub.musify.desktop.model.PlaylistModel
import com.gaminghub.musify.desktop.model.TrackModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SyncResult(
    val playlistsCount: Int,
    val favoritesCount: Int,
    val isSuccess: Boolean,
    val message: String
)

object DesktopFirestoreSyncManager {

    private const val PROJECT_ID = "musify-8df01"
    private const val API_KEY = "AIzaSyCxf6HkoECeubQgc4lrhx9q-HIlmj3YUxY"
    private const val BASE_URL = "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    fun getSyncUserId(email: String): String {
        return email.trim().lowercase().replace(".", "_").replace("@", "_")
    }

    private fun extractVideoId(url: String?): String {
        if (url.isNullOrBlank()) return ""
        try {
            if (url.contains("v=")) {
                val sub = url.substringAfter("v=").substringBefore("&").substringBefore("#")
                if (sub.length == 11) return sub
            }
            if (url.contains("youtu.be/")) {
                val sub = url.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
                if (sub.length == 11) return sub
            }
        } catch (_: Exception) {}
        return "track_${Math.abs(url.hashCode())}"
    }

    private fun sanitizeDocId(key: String): String {
        return key.replace(Regex("[^a-zA-Z0-9_]"), "_").take(100).ifBlank { "item_${System.currentTimeMillis()}" }
    }

    /**
     * Downloads playlists, favorites, and history from Cloud Firestore for the given Google email
     * and merges them into the local SQLite database.
     */
    suspend fun pullFromCloud(email: String, database: DesktopDatabase): SyncResult = withContext(Dispatchers.IO) {
        val syncUserId = getSyncUserId(email)
        var restoredPlaylists = 0
        var restoredFavorites = 0

        try {
            // 1. Pull Playlists from users/{syncUserId}/playlists
            val playlistsUrl = "$BASE_URL/users/$syncUserId/playlists?pageSize=100&key=$API_KEY"
            val playlistsReq = Request.Builder().url(playlistsUrl).get().build()
            client.newCall(playlistsReq).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val root = JSONObject(bodyStr)
                    val docs = root.optJSONArray("documents")
                    if (docs != null) {
                        for (i in 0 until docs.length()) {
                            val doc = docs.optJSONObject(i) ?: continue
                            val fields = doc.optJSONObject("fields") ?: continue
                            val plId = fields.optJSONObject("id")?.optString("stringValue")
                                ?: doc.optString("name").substringAfterLast("/")
                            val plName = fields.optJSONObject("name")?.optString("stringValue") ?: "Imported Playlist"
                            val updatedAt = fields.optJSONObject("updatedAt")?.optString("integerValue")?.toLongOrNull()
                                ?: System.currentTimeMillis()

                            val tracksArray = fields.optJSONObject("tracks")
                                ?.optJSONObject("arrayValue")
                                ?.optJSONArray("values")

                            val tracks = mutableListOf<TrackModel>()
                            if (tracksArray != null) {
                                for (j in 0 until tracksArray.length()) {
                                    val tObj = tracksArray.optJSONObject(j)?.optJSONObject("mapValue")?.optJSONObject("fields") ?: continue
                                    val title = tObj.optJSONObject("title")?.optString("stringValue") ?: ""
                                    val artist = tObj.optJSONObject("artist")?.optString("stringValue") ?: "Unknown Artist"
                                    val audioUrl = tObj.optJSONObject("audioUrl")?.optString("stringValue") ?: ""
                                    val albumArtUrl = tObj.optJSONObject("albumArtUrl")?.optString("stringValue")
                                    val album = tObj.optJSONObject("album")?.optString("stringValue")
                                    val genre = tObj.optJSONObject("genre")?.optString("stringValue")

                                    if (title.isNotBlank() && audioUrl.isNotBlank()) {
                                        tracks.add(
                                            TrackModel(
                                                title = title,
                                                artist = artist,
                                                audioUrl = audioUrl,
                                                albumArtUrl = albumArtUrl,
                                                album = album,
                                                genre = genre
                                            )
                                        )
                                    }
                                }
                            }

                            database.upsertCloudPlaylist(plId, plName, tracks, updatedAt)
                            restoredPlaylists++
                        }
                    }
                }
            }

            // 2. Pull Favorites from users/{syncUserId}/favorites
            val favsUrl = "$BASE_URL/users/$syncUserId/favorites?pageSize=300&key=$API_KEY"
            val favsReq = Request.Builder().url(favsUrl).get().build()
            client.newCall(favsReq).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val root = JSONObject(bodyStr)
                    val docs = root.optJSONArray("documents")
                    if (docs != null) {
                        for (i in 0 until docs.length()) {
                            val doc = docs.optJSONObject(i) ?: continue
                            val fields = doc.optJSONObject("fields") ?: continue
                            val title = fields.optJSONObject("title")?.optString("stringValue") ?: continue
                            val artist = fields.optJSONObject("artist")?.optString("stringValue") ?: "Unknown Artist"
                            val rawAudioUrl = fields.optJSONObject("audioUrl")?.optString("stringValue")
                            val videoId = fields.optJSONObject("videoId")?.optString("stringValue")
                            val audioUrl = if (!rawAudioUrl.isNullOrBlank()) {
                                rawAudioUrl
                            } else if (!videoId.isNullOrBlank() && !videoId.startsWith("track_")) {
                                "https://youtube.com/watch?v=$videoId"
                            } else continue
                            val albumArtUrl = fields.optJSONObject("albumArtUrl")?.optString("stringValue")
                            val album = fields.optJSONObject("album")?.optString("stringValue")
                            val genre = fields.optJSONObject("genre")?.optString("stringValue")

                            database.upsertCloudFavorite(
                                TrackModel(
                                    title = title,
                                    artist = artist,
                                    audioUrl = audioUrl,
                                    albumArtUrl = albumArtUrl,
                                    album = album,
                                    genre = genre
                                )
                            )
                            restoredFavorites++
                        }
                    }
                }
            }

            // 3. Pull Played Tracks from users/{syncUserId}/played_tracks
            val historyUrl = "$BASE_URL/users/$syncUserId/played_tracks?pageSize=100&key=$API_KEY"
            val historyReq = Request.Builder().url(historyUrl).get().build()
            client.newCall(historyReq).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val root = JSONObject(bodyStr)
                    val docs = root.optJSONArray("documents")
                    if (docs != null) {
                        for (i in 0 until docs.length()) {
                            val doc = docs.optJSONObject(i) ?: continue
                            val fields = doc.optJSONObject("fields") ?: continue
                            val title = fields.optJSONObject("title")?.optString("stringValue") ?: continue
                            val artist = fields.optJSONObject("artist")?.optString("stringValue") ?: "Unknown"
                            val audioUrl = fields.optJSONObject("audioUrl")?.optString("stringValue") ?: continue
                            val albumArtUrl = fields.optJSONObject("albumArtUrl")?.optString("stringValue")
                            val playCount = fields.optJSONObject("playCount")?.optString("integerValue")?.toIntOrNull() ?: 1
                            val lastPlayed = fields.optJSONObject("lastPlayedTimestamp")?.optString("integerValue")?.toLongOrNull() ?: 0L

                            database.upsertCloudPlayedTrack(
                                TrackModel(
                                    title = title,
                                    artist = artist,
                                    audioUrl = audioUrl,
                                    albumArtUrl = albumArtUrl
                                ),
                                playCount = playCount,
                                lastPlayedTimestamp = lastPlayed
                            )
                        }
                    }
                }
            }

            SyncResult(
                playlistsCount = restoredPlaylists,
                favoritesCount = restoredFavorites,
                isSuccess = true,
                message = "Synced $restoredPlaylists playlists and $restoredFavorites favorites with Android cloud."
            )
        } catch (e: Exception) {
            SyncResult(
                playlistsCount = restoredPlaylists,
                favoritesCount = restoredFavorites,
                isSuccess = false,
                message = "Sync failed: ${e.message}"
            )
        }
    }

    /**
     * Uploads a local playlist to Cloud Firestore so the Android app receives it immediately.
     */
    suspend fun syncPlaylistToCloud(email: String, playlist: PlaylistModel) = withContext(Dispatchers.IO) {
        try {
            val syncUserId = getSyncUserId(email)
            val cleanId = sanitizeDocId(playlist.id)

            val tracksArray = JSONArray()
            playlist.tracks.forEach { track ->
                val vId = extractVideoId(track.audioUrl)
                val trackFields = JSONObject().apply {
                    put("videoId", JSONObject().put("stringValue", vId))
                    put("songId", JSONObject().put("stringValue", vId))
                    put("title", JSONObject().put("stringValue", track.title))
                    put("artist", JSONObject().put("stringValue", track.artist))
                    put("audioUrl", JSONObject().put("stringValue", track.audioUrl))
                    put("albumArtUrl", JSONObject().put("stringValue", track.albumArtUrl ?: ""))
                    put("album", JSONObject().put("stringValue", track.album ?: ""))
                    put("genre", JSONObject().put("stringValue", track.genre ?: ""))
                }
                tracksArray.put(JSONObject().put("mapValue", JSONObject().put("fields", trackFields)))
            }

            val plFields = JSONObject().apply {
                put("id", JSONObject().put("stringValue", playlist.id))
                put("name", JSONObject().put("stringValue", playlist.title))
                put("updatedAt", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
                put("tracks", JSONObject().put("arrayValue", JSONObject().put("values", tracksArray)))
            }

            val payload = JSONObject().put("fields", plFields)
            val url = "$BASE_URL/users/$syncUserId/playlists/$cleanId?key=$API_KEY"

            val body = payload.toString().toRequestBody(jsonMediaType)
            val req = Request.Builder().url(url).patch(body).build()
            client.newCall(req).execute().close()
        } catch (e: Exception) {
            System.err.println("Failed to sync playlist to cloud: ${e.message}")
        }
    }

    /**
     * Deletes a playlist from Cloud Firestore.
     */
    suspend fun deletePlaylistFromCloud(email: String, playlistId: String) = withContext(Dispatchers.IO) {
        try {
            val syncUserId = getSyncUserId(email)
            val cleanId = sanitizeDocId(playlistId)
            val url = "$BASE_URL/users/$syncUserId/playlists/$cleanId?key=$API_KEY"
            val req = Request.Builder().url(url).delete().build()
            client.newCall(req).execute().close()
        } catch (e: Exception) {
            System.err.println("Failed to delete playlist from cloud: ${e.message}")
        }
    }

    /**
     * Updates favorite status in Cloud Firestore.
     */
    suspend fun syncFavoriteToCloud(email: String, track: TrackModel, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        try {
            val syncUserId = getSyncUserId(email)
            val vId = extractVideoId(track.audioUrl)
            val docId = if (vId.isNotBlank()) vId else sanitizeDocId(track.title)

            val url = "$BASE_URL/users/$syncUserId/favorites/$docId?key=$API_KEY"
            if (isFavorite) {
                val favFields = JSONObject().apply {
                    put("videoId", JSONObject().put("stringValue", vId))
                    put("songId", JSONObject().put("stringValue", vId))
                    put("title", JSONObject().put("stringValue", track.title))
                    put("artist", JSONObject().put("stringValue", track.artist))
                    put("audioUrl", JSONObject().put("stringValue", track.audioUrl))
                    put("albumArtUrl", JSONObject().put("stringValue", track.albumArtUrl ?: ""))
                    put("album", JSONObject().put("stringValue", track.album ?: ""))
                    put("genre", JSONObject().put("stringValue", track.genre ?: ""))
                    put("favoritedAt", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
                }
                val payload = JSONObject().put("fields", favFields)
                val body = payload.toString().toRequestBody(jsonMediaType)
                val req = Request.Builder().url(url).patch(body).build()
                client.newCall(req).execute().close()
            } else {
                val req = Request.Builder().url(url).delete().build()
                client.newCall(req).execute().close()
            }
        } catch (e: Exception) {
            System.err.println("Failed to sync favorite to cloud: ${e.message}")
        }
    }
}
