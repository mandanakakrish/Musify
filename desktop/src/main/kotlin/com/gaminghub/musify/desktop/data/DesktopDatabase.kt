package com.gaminghub.musify.desktop.data

import com.gaminghub.musify.desktop.model.GoogleUser
import com.gaminghub.musify.desktop.model.PlaylistModel
import com.gaminghub.musify.desktop.model.TrackModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

class DesktopDatabase {

    private val dbFile: File by lazy {
        val dir = File(System.getProperty("user.home"), ".musify")
        if (!dir.exists()) dir.mkdirs()
        File(dir, "musify.db")
    }

    private val connectionUrl: String
        get() = "jdbc:sqlite:${dbFile.absolutePath}"

    init {
        initTables()
    }

    private fun getConnection(): Connection {
        return DriverManager.getConnection(connectionUrl)
    }

    private fun initTables() {
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                // Tracks & Favorites
                stmt.execute(
                    """
                    CREATE TABLE IF NOT EXISTS tracks (
                        audioUrl TEXT PRIMARY KEY,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        albumArtUrl TEXT,
                        album TEXT,
                        durationSeconds INTEGER DEFAULT 0,
                        isFavorite INTEGER DEFAULT 0,
                        playCount INTEGER DEFAULT 0,
                        lastPlayedTimestamp INTEGER
                    );
                    """.trimIndent()
                )

                // Playlists
                stmt.execute(
                    """
                    CREATE TABLE IF NOT EXISTS playlists (
                        id TEXT PRIMARY KEY,
                        title TEXT NOT NULL,
                        description TEXT,
                        coverUrl TEXT,
                        createdAt INTEGER NOT NULL
                    );
                    """.trimIndent()
                )

                // Playlist Tracks
                stmt.execute(
                    """
                    CREATE TABLE IF NOT EXISTS playlist_tracks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        playlistId TEXT NOT NULL,
                        audioUrl TEXT NOT NULL,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        albumArtUrl TEXT,
                        durationSeconds INTEGER DEFAULT 0,
                        trackOrder INTEGER DEFAULT 0,
                        FOREIGN KEY (playlistId) REFERENCES playlists(id) ON DELETE CASCADE
                    );
                    """.trimIndent()
                )

                // Playback History
                stmt.execute(
                    """
                    CREATE TABLE IF NOT EXISTS history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        audioUrl TEXT NOT NULL,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        albumArtUrl TEXT,
                        playedAt INTEGER NOT NULL
                    );
                    """.trimIndent()
                )

                // Settings
                stmt.execute(
                    """
                    CREATE TABLE IF NOT EXISTS settings (
                        key TEXT PRIMARY KEY,
                        value TEXT NOT NULL
                    );
                    """.trimIndent()
                )

                // Google Account
                stmt.execute(
                    """
                    CREATE TABLE IF NOT EXISTS google_account (
                        id TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        email TEXT NOT NULL,
                        photoUrl TEXT
                    );
                    """.trimIndent()
                )
            }
        }
    }

    suspend fun getFavorites(): List<TrackModel> = withContext(Dispatchers.IO) {
        val list = mutableListOf<TrackModel>()
        getConnection().use { conn ->
            val sql = "SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY lastPlayedTimestamp DESC"
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    list.add(
                        TrackModel(
                            audioUrl = rs.getString("audioUrl"),
                            title = rs.getString("title"),
                            artist = rs.getString("artist"),
                            albumArtUrl = rs.getString("albumArtUrl"),
                            album = rs.getString("album"),
                            durationSeconds = rs.getLong("durationSeconds"),
                            playcount = rs.getInt("playCount")
                        )
                    )
                }
            }
        }
        list
    }

    suspend fun isFavorite(audioUrl: String): Boolean = withContext(Dispatchers.IO) {
        getConnection().use { conn ->
            val sql = "SELECT isFavorite FROM tracks WHERE audioUrl = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, audioUrl)
                val rs = stmt.executeQuery()
                if (rs.next()) rs.getInt("isFavorite") == 1 else false
            }
        }
    }

    suspend fun toggleFavorite(track: TrackModel): Boolean = withContext(Dispatchers.IO) {
        getConnection().use { conn ->
            val currentFav = isFavorite(track.audioUrl)
            val newFav = if (currentFav) 0 else 1

            val sql = """
                INSERT INTO tracks (audioUrl, title, artist, albumArtUrl, album, durationSeconds, isFavorite, lastPlayedTimestamp)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(audioUrl) DO UPDATE SET isFavorite = excluded.isFavorite;
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, track.audioUrl)
                stmt.setString(2, track.title)
                stmt.setString(3, track.artist)
                stmt.setString(4, track.albumArtUrl)
                stmt.setString(5, track.album)
                stmt.setLong(6, track.durationSeconds)
                stmt.setInt(7, newFav)
                stmt.setLong(8, System.currentTimeMillis())
                stmt.executeUpdate()
            }
            newFav == 1
        }
    }

    suspend fun recordPlay(track: TrackModel) = withContext(Dispatchers.IO) {
        getConnection().use { conn ->
            // Insert history
            val histSql = "INSERT INTO history (audioUrl, title, artist, albumArtUrl, playedAt) VALUES (?, ?, ?, ?, ?)"
            conn.prepareStatement(histSql).use { stmt ->
                stmt.setString(1, track.audioUrl)
                stmt.setString(2, track.title)
                stmt.setString(3, track.artist)
                stmt.setString(4, track.albumArtUrl)
                stmt.setLong(5, System.currentTimeMillis())
                stmt.executeUpdate()
            }

            // Update track play count
            val trackSql = """
                INSERT INTO tracks (audioUrl, title, artist, albumArtUrl, album, durationSeconds, playCount, lastPlayedTimestamp)
                VALUES (?, ?, ?, ?, ?, ?, 1, ?)
                ON CONFLICT(audioUrl) DO UPDATE SET 
                    playCount = tracks.playCount + 1,
                    lastPlayedTimestamp = excluded.lastPlayedTimestamp;
            """.trimIndent()
            conn.prepareStatement(trackSql).use { stmt ->
                stmt.setString(1, track.audioUrl)
                stmt.setString(2, track.title)
                stmt.setString(3, track.artist)
                stmt.setString(4, track.albumArtUrl)
                stmt.setString(5, track.album)
                stmt.setLong(6, track.durationSeconds)
                stmt.setLong(7, System.currentTimeMillis())
                stmt.executeUpdate()
            }
        }
    }

    suspend fun getHistory(limit: Int = 50): List<TrackModel> = withContext(Dispatchers.IO) {
        val list = mutableListOf<TrackModel>()
        getConnection().use { conn ->
            val sql = "SELECT * FROM history ORDER BY playedAt DESC LIMIT ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, limit)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    list.add(
                        TrackModel(
                            audioUrl = rs.getString("audioUrl"),
                            title = rs.getString("title"),
                            artist = rs.getString("artist"),
                            albumArtUrl = rs.getString("albumArtUrl")
                        )
                    )
                }
            }
        }
        list
    }

    suspend fun getPlaylists(): List<PlaylistModel> = withContext(Dispatchers.IO) {
        val playlists = mutableListOf<PlaylistModel>()
        getConnection().use { conn ->
            val sql = "SELECT * FROM playlists ORDER BY createdAt DESC"
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val id = rs.getString("id")
                    val title = rs.getString("title")
                    val desc = rs.getString("description") ?: ""
                    val cover = rs.getString("coverUrl")
                    val created = rs.getLong("createdAt")

                    // Get tracks
                    val tracks = getPlaylistTracks(id, conn)
                    playlists.add(
                        PlaylistModel(
                            id = id,
                            title = title,
                            description = desc,
                            coverUrl = cover ?: tracks.firstOrNull()?.albumArtUrl,
                            trackCount = tracks.size,
                            tracks = tracks,
                            createdAt = created
                        )
                    )
                }
            }
        }
        playlists
    }

    private fun getPlaylistTracks(playlistId: String, conn: Connection): List<TrackModel> {
        val tracks = mutableListOf<TrackModel>()
        val sql = "SELECT * FROM playlist_tracks WHERE playlistId = ? ORDER BY trackOrder ASC"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, playlistId)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                tracks.add(
                    TrackModel(
                        audioUrl = rs.getString("audioUrl"),
                        title = rs.getString("title"),
                        artist = rs.getString("artist"),
                        albumArtUrl = rs.getString("albumArtUrl"),
                        durationSeconds = rs.getLong("durationSeconds")
                    )
                )
            }
        }
        return tracks
    }

    suspend fun createPlaylist(title: String, description: String = ""): PlaylistModel = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val created = System.currentTimeMillis()
        getConnection().use { conn ->
            val sql = "INSERT INTO playlists (id, title, description, createdAt) VALUES (?, ?, ?, ?)"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, id)
                stmt.setString(2, title)
                stmt.setString(3, description)
                stmt.setLong(4, created)
                stmt.executeUpdate()
            }
        }
        PlaylistModel(id, title, description, null, 0, emptyList(), created)
    }

    suspend fun addTrackToPlaylist(playlistId: String, track: TrackModel) = withContext(Dispatchers.IO) {
        getConnection().use { conn ->
            val sql = """
                INSERT INTO playlist_tracks (playlistId, audioUrl, title, artist, albumArtUrl, durationSeconds, trackOrder)
                VALUES (?, ?, ?, ?, ?, ?, (SELECT COALESCE(MAX(trackOrder), 0) + 1 FROM playlist_tracks WHERE playlistId = ?))
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playlistId)
                stmt.setString(2, track.audioUrl)
                stmt.setString(3, track.title)
                stmt.setString(4, track.artist)
                stmt.setString(5, track.albumArtUrl)
                stmt.setLong(6, track.durationSeconds)
                stmt.setString(7, playlistId)
                stmt.executeUpdate()
            }
        }
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, audioUrl: String) = withContext(Dispatchers.IO) {
        getConnection().use { conn ->
            val sql = "DELETE FROM playlist_tracks WHERE playlistId = ? AND audioUrl = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playlistId)
                stmt.setString(2, audioUrl)
                stmt.executeUpdate()
            }
        }
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        getConnection().use { conn ->
            conn.prepareStatement("DELETE FROM playlist_tracks WHERE playlistId = ?").use {
                it.setString(1, playlistId)
                it.executeUpdate()
            }
            conn.prepareStatement("DELETE FROM playlists WHERE id = ?").use {
                it.setString(1, playlistId)
                it.executeUpdate()
            }
        }
    }

    suspend fun saveSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        getConnection().use { conn ->
            val sql = "INSERT INTO settings (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, key)
                stmt.setString(2, value)
                stmt.executeUpdate()
            }
        }
    }

    suspend fun getSetting(key: String, default: String): String = withContext(Dispatchers.IO) {
        getConnection().use { conn ->
            val sql = "SELECT value FROM settings WHERE key = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, key)
                val rs = stmt.executeQuery()
                if (rs.next()) rs.getString("value") else default
            }
        }
    }

    suspend fun saveGoogleUser(user: GoogleUser) = withContext(Dispatchers.IO) {
        getConnection().use { conn ->
            conn.prepareStatement("DELETE FROM google_account").executeUpdate()
            val sql = "INSERT INTO google_account (id, name, email, photoUrl) VALUES (?, ?, ?, ?)"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, user.id)
                stmt.setString(2, user.name)
                stmt.setString(3, user.email)
                stmt.setString(4, user.photoUrl)
                stmt.executeUpdate()
            }
        }
    }

    suspend fun getGoogleUser(): GoogleUser? = withContext(Dispatchers.IO) {
        getConnection().use { conn ->
            val sql = "SELECT * FROM google_account LIMIT 1"
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    GoogleUser(
                        id = rs.getString("id"),
                        name = rs.getString("name"),
                        email = rs.getString("email"),
                        photoUrl = rs.getString("photoUrl")
                    )
                } else null
            }
        }
    }

    suspend fun clearGoogleUser() = withContext(Dispatchers.IO) {
        getConnection().use { conn ->
            conn.prepareStatement("DELETE FROM google_account").executeUpdate()
        }
    }
}
