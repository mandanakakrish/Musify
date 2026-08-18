package com.gaminghub.musify.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gaminghub.musify.TrackModel
import com.gaminghub.musicplayer.data.MusicDatabase
import com.gaminghub.musicplayer.data.PlaylistEntity
import com.gaminghub.musify.data.toEntity
import com.gaminghub.musify.data.toModel
import com.gaminghub.musify.data.FollowedArtistEntity
import com.gaminghub.musify.ArtistModel
import com.gaminghub.musify.data.repository.LocalMediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

import com.gaminghub.musify.util.AnalyticsManager

class LibraryViewModel(
    application: Application,
    private val analyticsManager: AnalyticsManager
) : AndroidViewModel(application) {
    private val dao = MusicDatabase.getInstance(application).dao
    private val localMediaRepository = LocalMediaRepository(application)
    private val cloudSyncRepository by lazy { com.gaminghub.musify.data.repository.CloudSyncRepository(dao) }
    private val youtubeDownloader = com.gaminghub.musify.util.YouTubeDownloader(application, dao)

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring

    private val _localTracks = MutableStateFlow<List<TrackModel>>(emptyList())
    val localTracks: StateFlow<List<TrackModel>> = _localTracks

    val localArtists: StateFlow<Map<String, List<TrackModel>>> = _localTracks
        .map { list -> list.groupBy { it.artist }.toSortedMap() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val localGenres: StateFlow<Map<String, List<TrackModel>>> = _localTracks
        .map { list -> list.groupBy { it.genre ?: "Unknown" }.toSortedMap() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val localFolders: StateFlow<Map<String, List<TrackModel>>> = _localTracks
        .map { list -> list.groupBy { File(it.audioUrl ?: "").parent ?: "Root" }.toSortedMap() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val uniqueTracksPlayedCount: StateFlow<Int> = dao.getUniqueTracksPlayedCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val mostPlayedTrack: StateFlow<TrackModel?> = dao.getMostPlayedTrack()
        .map { it?.toModel() }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val favoriteTracks: StateFlow<List<TrackModel>> = dao.getFavoriteTracks()
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentTracks: StateFlow<List<TrackModel>> = dao.getRecentHistory()
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val top50Tracks: StateFlow<List<TrackModel>> = dao.getTop50Tracks()
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = dao.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val playlistThumbnails: StateFlow<Map<Int, List<String?>>> = playlists
        .map { list ->
            list.associate { playlist ->
                playlist.id to dao.getTracksForPlaylistSync(playlist.id).take(4).map { it.albumArtUrl }
            }
        }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val playlistCounts: StateFlow<Map<Int, Int>> = playlists
        .map { list ->
            list.associate { playlist ->
                playlist.id to dao.getTracksForPlaylistSync(playlist.id).size
            }
        }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    /** Top 5 favorite artists based on combined favorites and play counts. */
    val topArtists: StateFlow<List<String>> = combine(favoriteTracks, recentTracks) { favorites, recent ->
        val scores = mutableMapOf<String, Int>()
        favorites.forEach { scores[it.artist] = (scores[it.artist] ?: 0) + 10 }
        recent.take(50).forEach { scores[it.artist] = (scores[it.artist] ?: 0) + 2 }
        scores.entries.sortedByDescending { it.value }.take(5).map { it.key }.filter { it.isNotBlank() }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadLocalTracks()
    }

    fun loadLocalTracks() {
        viewModelScope.launch(Dispatchers.IO) {
            _localTracks.value = localMediaRepository.getLocalTracks()
        }
    }

    fun createPlaylist(name: String) {
        analyticsManager.logFeatureClick("create_playlist")
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertPlaylist(PlaylistEntity(name = name))
        }
    }

    fun deletePlaylist(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deletePlaylist(id)
        }
    }

    fun renamePlaylist(id: Int, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.renamePlaylist(id, newName)
        }
    }

    fun addTrackToPlaylist(playlistId: Int, track: TrackModel) {
        analyticsManager.logFeatureClick("add_to_playlist")
        viewModelScope.launch(Dispatchers.IO) {
            dao.addTrackToPlaylist(com.gaminghub.musify.data.PlaylistTrackEntity(playlistId = playlistId, audioUrl = track.audioUrl ?: ""))
            dao.insertTrack(track.toEntity())
        }
    }

    fun toggleFavorite(track: TrackModel) {
        analyticsManager.logFeatureClick("toggle_favorite")
        viewModelScope.launch(Dispatchers.IO) {
            val url = track.audioUrl ?: return@launch
            val existing = dao.getTrackByUrl(url)
            val isFav = existing?.isFavorite ?: false
            if (!isFav) {
                dao.insertTrack(track.toEntity().copy(isFavorite = true))
                dao.updateFavorite(url, true)
            } else {
                dao.updateFavorite(url, false)
            }
        }
    }

    fun downloadTrack(track: TrackModel) {
        analyticsManager.logFeatureClick("download_track")
        youtubeDownloader.downloadTrack(track)
    }

    fun isFavorite(url: String): Flow<Boolean> = dao.isFavorite(url)

    fun getTracksInPlaylist(playlistId: Int): Flow<List<TrackModel>> = 
        dao.getTracksForPlaylist(playlistId).map { list -> list.map { it.toModel() } }

    fun recordHistory(track: TrackModel) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.recordHistory(track.audioUrl ?: "", track.toEntity())
        }
    }

    fun exportPlaylistToJson(playlistId: Int): String {
        val tracks = dao.getTracksForPlaylistSync(playlistId)
        val array = JSONArray()
        tracks.forEach {
            val obj = JSONObject()
            obj.put("title", it.title)
            obj.put("artist", it.artist)
            obj.put("audioUrl", it.audioUrl)
            obj.put("albumArtUrl", it.albumArtUrl)
            array.put(obj)
        }
        return array.toString()
    }

    fun importJoytifyPlaylist(json: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val array = JSONArray(json)
                val playlistName = "Imported Playlist ${System.currentTimeMillis() / 1000}"
                dao.insertPlaylist(PlaylistEntity(name = playlistName))
                val playlistId = dao.getPlaylistsSync().last().id
                
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val track = TrackModel(
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        audioUrl = obj.getString("audioUrl"),
                        albumArtUrl = obj.getString("albumArtUrl")
                    )
                    addTrackToPlaylist(playlistId, track)
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Import failed", e)
            }
        }
    }

    fun importSpotifyPlaylist(url: String) {
        // Basic implementation: Extract ID from URL and create a placeholder
        viewModelScope.launch(Dispatchers.IO) {
            val name = "Spotify Import ${url.takeLast(8)}"
            dao.insertPlaylist(PlaylistEntity(name = name))
        }
    }

    fun toggleFollowArtist(artist: ArtistModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val isFollowed = dao.isArtistFollowed(artist.id).first()
            if (isFollowed) {
                dao.deleteFollowedArtist(artist.id)
            } else {
                dao.insertFollowedArtist(artist.toEntity())
            }
        }
    }

    fun isArtistFollowed(id: String): Flow<Boolean> = dao.isArtistFollowed(id)

    fun restoreFromCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRestoring.value = true
            try {
                cloudSyncRepository.downloadFromCloud()
                loadLocalTracks() // Refresh UI if any local changes occurred
            } finally {
                _isRestoring.value = false
            }
        }
    }
}
