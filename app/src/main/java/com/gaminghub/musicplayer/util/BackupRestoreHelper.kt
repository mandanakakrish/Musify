package com.gaminghub.musicplayer.util

import android.content.Context
import android.net.Uri
import com.gaminghub.musicplayer.data.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

data class BackupData(
    val version: Int = 1,
    val app: String = "Musify",
    val timestamp: Long = System.currentTimeMillis(),
    val playlists: List<PlaylistBackupItem> = emptyList(),
    val favorites: List<TrackBackupItem> = emptyList(),
    val followedArtists: List<ArtistBackupItem> = emptyList(),
    val settings: Map<String, Any?> = emptyMap()
)

data class PlaylistBackupItem(
    val name: String,
    val tracks: List<TrackBackupItem> = emptyList()
)

data class TrackBackupItem(
    val title: String,
    val artist: String,
    val audioUrl: String,
    val albumArtUrl: String? = null,
    val album: String? = null,
    val genre: String? = null
)

data class ArtistBackupItem(
    val id: String,
    val name: String,
    val imageUrl: String? = null
)

object BackupRestoreHelper {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun createBackupJson(context: Context): String = withContext(Dispatchers.IO) {
        val db = MusicDatabase.getInstance(context)
        val dao = db.dao
        val prefs = context.getSharedPreferences("Musify_settings", Context.MODE_PRIVATE)

        val playlists = dao.getPlaylistsSync()
        val playlistItems = playlists.map { pl ->
            val tracks = dao.getTracksForPlaylistSync(pl.id).map { t ->
                TrackBackupItem(
                    title = t.title,
                    artist = t.artist,
                    audioUrl = t.audioUrl,
                    albumArtUrl = t.albumArtUrl,
                    album = t.album,
                    genre = t.genre
                )
            }
            PlaylistBackupItem(name = pl.name, tracks = tracks)
        }

        // Favorites from DB
        val favorites = dao.getFavoriteTracksSync().map { t ->
            TrackBackupItem(
                title = t.title,
                artist = t.artist,
                audioUrl = t.audioUrl,
                albumArtUrl = t.albumArtUrl,
                album = t.album,
                genre = t.genre
            )
        }

        // Followed Artists from DB
        val artists = dao.getFollowedArtistsSync()

        val backupData = BackupData(
            playlists = playlistItems,
            favorites = favorites,
            followedArtists = artists.map { ArtistBackupItem(it.id, it.name, it.imageUrl) },
            settings = prefs.all
        )

        gson.toJson(backupData)
    }

    suspend fun writeBackupToUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = createBackupJson(context)
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(json.toByteArray(Charsets.UTF_8))
                output.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreBackupFromUri(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            } ?: return@withContext Result.failure(Exception("Could not open JSON file from storage"))

            restoreFromJsonString(context, jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun restoreFromJsonString(context: Context, jsonString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val db = MusicDatabase.getInstance(context)
            val dao = db.dao
            val prefs = context.getSharedPreferences("Musify_settings", Context.MODE_PRIVATE)

            var restoredPlaylists = 0
            var restoredSongs = 0
            var restoredFavorites = 0

            // 1. Try structured BackupData format
            val backup = try {
                gson.fromJson(jsonString, BackupData::class.java)
            } catch (_: Exception) {
                null
            }

            if (backup != null && (backup.playlists.isNotEmpty() || backup.favorites.isNotEmpty() || backup.followedArtists.isNotEmpty())) {
                // Restore Playlists
                for (pl in backup.playlists) {
                    val plEntity = PlaylistEntity(name = pl.name)
                    dao.insertPlaylist(plEntity)
                    val insertedPl = dao.getPlaylistsSync().find { it.name == pl.name }
                    val plId = insertedPl?.id ?: continue
                    restoredPlaylists++

                    for (tr in pl.tracks) {
                        if (tr.audioUrl.isBlank() || tr.title.isBlank()) continue
                        val trackEntity = TrackEntity(
                            audioUrl = tr.audioUrl,
                            title = tr.title,
                            artist = tr.artist.ifBlank { "Unknown Artist" },
                            albumArtUrl = tr.albumArtUrl,
                            album = tr.album,
                            genre = tr.genre
                        )
                        dao.insertTrack(trackEntity)
                        dao.addTrackToPlaylist(
                            PlaylistTrackEntity(
                                playlistId = plId,
                                audioUrl = tr.audioUrl
                            )
                        )
                        restoredSongs++
                    }
                }

                // Restore Favorites
                for (fav in backup.favorites) {
                    if (fav.audioUrl.isBlank() || fav.title.isBlank()) continue
                    val trackEntity = TrackEntity(
                        audioUrl = fav.audioUrl,
                        title = fav.title,
                        artist = fav.artist.ifBlank { "Unknown Artist" },
                        albumArtUrl = fav.albumArtUrl,
                        album = fav.album,
                        genre = fav.genre,
                        isFavorite = true
                    )
                    dao.insertTrack(trackEntity)
                    restoredFavorites++
                }

                // Restore Followed Artists
                for (art in backup.followedArtists) {
                    if (art.name.isBlank()) continue
                    dao.insertFollowedArtist(
                        FollowedArtistEntity(
                            id = art.id.ifBlank { art.name },
                            name = art.name,
                            imageUrl = art.imageUrl
                        )
                    )
                }

                // Restore Settings
                if (backup.settings.isNotEmpty()) {
                    val editor = prefs.edit()
                    backup.settings.forEach { (key, value) ->
                        when (value) {
                            is Boolean -> editor.putBoolean(key, value)
                            is String -> editor.putString(key, value)
                            is Number -> editor.putLong(key, value.toLong())
                        }
                    }
                    editor.apply()
                }

                return@withContext Result.success("Restored $restoredPlaylists playlists, $restoredFavorites favorites & $restoredSongs songs!")
            }

            // 2. Musify / Youtify / BlackHole Dictionary Playlist Format (with _order key)
            val MusifyTracks = MusifyPlaylistParser.parseJsonToTracks(jsonString)
            if (MusifyTracks.isNotEmpty()) {
                val playlistName = "Imported Playlist"
                val plEntity = PlaylistEntity(name = playlistName)
                dao.insertPlaylist(plEntity)
                val insertedPl = dao.getPlaylistsSync().find { it.name == playlistName }
                    ?: dao.getPlaylistsSync().lastOrNull()
                val plId = insertedPl?.id ?: return@withContext Result.failure(Exception("Failed to create playlist"))
                restoredPlaylists++

                for (track in MusifyTracks) {
                    val url = track.audioUrl ?: continue
                    dao.insertTrack(track.toEntity())
                    dao.addTrackToPlaylist(PlaylistTrackEntity(playlistId = plId, audioUrl = url))
                    restoredSongs++
                }
                return@withContext Result.success("Restored playlist with $restoredSongs songs!")
            }

            // 3. Generic Fallback: If JSON is an array of songs/playlists or third-party format (Youtify/Spotify/ViMusic/Innertune)
            val jsonObject = try { JSONObject(jsonString) } catch (_: Exception) { null }
            val jsonArray = try { JSONArray(jsonString) } catch (_: Exception) { null }

            if (jsonObject != null) {
                // Check for generic playlists object or track list
                val playlistName = jsonObject.optString("name", "Imported Playlist")
                val tracksArray = jsonObject.optJSONArray("tracks") ?: jsonObject.optJSONArray("songs")
                if (tracksArray != null && tracksArray.length() > 0) {
                    val plEntity = PlaylistEntity(name = playlistName)
                    dao.insertPlaylist(plEntity)
                    val insertedPl = dao.getPlaylistsSync().find { it.name == playlistName }
                    val plId = insertedPl?.id ?: return@withContext Result.failure(Exception("Failed to create playlist"))
                    restoredPlaylists++

                    for (i in 0 until tracksArray.length()) {
                        val item = tracksArray.getJSONObject(i)
                        val title = item.optString("title", item.optString("name", ""))
                        val artist = item.optString("artist", item.optString("uploader", "Unknown Artist"))
                        val url = item.optString("audioUrl", item.optString("url", item.optString("videoId", "")))
                        val art = item.optString("albumArtUrl", item.optString("artworkUrl", item.optString("thumbnailUrl", "")))
                        if (url.isNotBlank() && title.isNotBlank()) {
                            val trackEntity = TrackEntity(
                                audioUrl = url,
                                title = title,
                                artist = artist,
                                albumArtUrl = art
                            )
                            dao.insertTrack(trackEntity)
                            dao.addTrackToPlaylist(PlaylistTrackEntity(playlistId = plId, audioUrl = url))
                            restoredSongs++
                        }
                    }
                    return@withContext Result.success("Restored playlist '$playlistName' with $restoredSongs songs!")
                }
            }

            if (jsonArray != null && jsonArray.length() > 0) {
                val plEntity = PlaylistEntity(name = "Imported JSON Playlist")
                dao.insertPlaylist(plEntity)
                val insertedPl = dao.getPlaylistsSync().find { it.name == "Imported JSON Playlist" }
                val plId = insertedPl?.id ?: return@withContext Result.failure(Exception("Failed to create playlist"))
                restoredPlaylists++

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val title = item.optString("title", item.optString("name", ""))
                    val artist = item.optString("artist", item.optString("uploader", "Unknown Artist"))
                    val url = item.optString("audioUrl", item.optString("url", item.optString("videoId", "")))
                    val art = item.optString("albumArtUrl", item.optString("artworkUrl", ""))
                    if (url.isNotBlank() && title.isNotBlank()) {
                        val trackEntity = TrackEntity(
                            audioUrl = url,
                            title = title,
                            artist = artist,
                            albumArtUrl = art
                        )
                        dao.insertTrack(trackEntity)
                        dao.addTrackToPlaylist(PlaylistTrackEntity(playlistId = plId, audioUrl = url))
                        restoredSongs++
                    }
                }
                return@withContext Result.success("Restored playlist with $restoredSongs songs!")
            }

            Result.failure(Exception("Invalid or unrecognized JSON format"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
