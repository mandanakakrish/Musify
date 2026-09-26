package com.gaminghub.musicplayer.util

import android.content.Context
import android.util.Log
import com.gaminghub.musicplayer.TrackModel
import com.gaminghub.musicplayer.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Parser for Musify / Youtify / BlackHole standard dictionary playlist JSON format.
 * Format sample:
 * {
 *   "-60Bl9h7u5g": {
 *       "id": "-60Bl9h7u5g",
 *       "title": "Kamariya (From \"Stree\")",
 *       "artist": "Aastha Gill, Sachin Sanghvi, Jigar Saraiya & Divya Kumar",
 *       "album": "",
 *       "image": "https://lh3.googleusercontent.com/...",
 *       "url": "https://...",
 *       "perma_url": "https://youtube.com/watch?v=-60Bl9h7u5g",
 *       "duration": "188",
 *       "genre": "YouTube",
 *       "dateAdded": "2025-11-25 20:03:23.373604"
 *   },
 *   "_order": ["-60Bl9h7u5g", "064wPVEu", ...]
 * }
 */
object MusifyPlaylistParser {
    private const val TAG = "MusifyPlaylistParser"

    fun parseJsonToTracks(jsonString: String): List<TrackModel> {
        val tracks = mutableListOf<TrackModel>()
        try {
            val root = JSONObject(jsonString)
            val orderArray = root.optJSONArray("_order")

            val idList = mutableListOf<String>()
            if (orderArray != null && orderArray.length() > 0) {
                for (i in 0 until orderArray.length()) {
                    val id = orderArray.optString(i)
                    if (!id.isNullOrBlank()) {
                        idList.add(id)
                    }
                }
            } else {
                // If _order is missing, collect all root keys that don't start with '_'
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (!key.startsWith("_")) {
                        idList.add(key)
                    }
                }
            }

            for (id in idList) {
                val songObj = root.optJSONObject(id) ?: continue
                val title = songObj.optString("title").trim()
                if (title.isBlank()) continue

                val artist = songObj.optString("artist", "Unknown Artist").trim()
                val album = songObj.optString("album", "").trim()
                val genre = songObj.optString("genre", "").trim()
                val image = songObj.optString("image", "").trim()
                val permaUrl = songObj.optString("perma_url", "").trim()
                val rawUrl = songObj.optString("url", "").trim()

                // Resolve best audio/playback URL
                val audioUrl = when {
                    permaUrl.isNotBlank() -> permaUrl
                    rawUrl.isNotBlank() -> rawUrl
                    id.length in 10..12 -> "https://youtube.com/watch?v=$id"
                    else -> "https://youtube.com/watch?v=$id"
                }

                val durationSec = songObj.optString("duration").toLongOrNull() ?: 0L
                val durationMillis = durationSec * 1000L

                tracks.add(
                    TrackModel(
                        title = title,
                        artist = artist,
                        audioUrl = audioUrl,
                        albumArtUrl = image.ifBlank { null },
                        album = album.ifBlank { null },
                        genre = genre.ifBlank { null }
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Musify playlist JSON: ${e.message}", e)
        }
        return tracks
    }

    suspend fun importPlaylistFromJson(
        context: Context,
        jsonString: String,
        playlistName: String = "Imported Playlist"
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val tracks = parseJsonToTracks(jsonString)
            if (tracks.isEmpty()) {
                return@withContext Result.failure(Exception("No valid tracks found in playlist JSON"))
            }

            val db = MusicDatabase.getInstance(context)
            val dao = db.dao

            val playlistEntity = PlaylistEntity(name = playlistName)
            val plId = dao.insertPlaylist(playlistEntity).toInt()
            if (plId <= 0) {
                return@withContext Result.failure(Exception("Failed to create playlist entity"))
            }

            var count = 0
            for (track in tracks) {
                val url = track.audioUrl ?: continue
                dao.insertTrack(track.toEntity())
                dao.addTrackToPlaylist(
                    PlaylistTrackEntity(
                        playlistId = plId,
                        audioUrl = url
                    )
                )
                count++
            }

            Log.d(TAG, "Successfully imported playlist '$playlistName' with $count tracks")
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Import error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
