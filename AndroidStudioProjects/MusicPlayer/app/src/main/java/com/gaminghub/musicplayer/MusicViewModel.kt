package com.gaminghub.musicplayer

import android.app.Application
import android.content.ComponentName
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.gaminghub.musicplayer.data.MusicDatabase
import com.gaminghub.musicplayer.data.PlaylistEntity
import com.gaminghub.musicplayer.data.toEntity
import com.gaminghub.musicplayer.data.toModel
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import com.gaminghub.musify.util.StreamExtractionManager
import java.util.concurrent.TimeUnit

@UnstableApi
class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "MusicViewModel"

    private val _trendingTracks = MutableStateFlow<List<TrackModel>>(emptyList())
    val trendingTracks: StateFlow<List<TrackModel>> = _trendingTracks

    // Local device tracks — loaded from MediaStore
    private val _localTracks = MutableStateFlow<List<TrackModel>>(emptyList())
    val localTracks: StateFlow<List<TrackModel>> = _localTracks

    val localArtists: StateFlow<Map<String, List<TrackModel>>> = _localTracks
        .map { list -> list.groupBy { it.artist.ifBlank { "Unknown" } } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val localGenres: StateFlow<Map<String, List<TrackModel>>> = _localTracks
        .map { list -> list.filter { !it.genre.isNullOrBlank() }.groupBy { it.genre!! } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val localFolders: StateFlow<Map<String, List<TrackModel>>> = _localTracks
        .map { list -> list.groupBy { it.audioUrl?.substringBeforeLast("/", "Unknown") ?: "Unknown" } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    private val _searchTracks = MutableStateFlow<List<TrackModel>>(emptyList())
    val searchTracks: StateFlow<List<TrackModel>> = _searchTracks

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _currentTrack = MutableStateFlow<TrackModel?>(null)
    val currentTrack: StateFlow<TrackModel?> = _currentTrack

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentQueue = MutableStateFlow<List<TrackModel>>(emptyList())
    val currentQueue: StateFlow<List<TrackModel>> = _currentQueue

    /** Tracks that come *after* the currently playing song in the queue. */
    val upNextQueue: StateFlow<List<TrackModel>> = combine(_currentQueue, _currentTrack) { queue, current ->
        if (current == null) queue
        else {
            val idx = queue.indexOfFirst { it.audioUrl == current.audioUrl }
            if (idx >= 0) queue.drop(idx + 1) else queue
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val tracks: StateFlow<List<TrackModel>> = _trendingTracks
    val topCharts: StateFlow<List<TrackModel>> = _trendingTracks
    val youtubeMusic: StateFlow<List<TrackModel>> = _trendingTracks

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val db = MusicDatabase.getInstance(application)
    private val dao = db.dao

    val favoriteTracks: StateFlow<List<TrackModel>> = dao.getFavoriteTracks()
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentTracks: StateFlow<List<TrackModel>> = dao.getRecentHistory()
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = dao.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val albums: StateFlow<Map<String, List<TrackModel>>> = tracks
        .map { list -> list.filter { it.album != null }.groupBy { it.album ?: "Unknown" } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val artists: StateFlow<Map<String, List<TrackModel>>> = tracks
        .map { list -> list.groupBy { it.artist ?: "Unknown" } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val genres: StateFlow<Map<String, List<TrackModel>>> = tracks
        .map { list -> list.filter { it.genre != null }.groupBy { it.genre ?: "Unknown" } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val folders: StateFlow<Map<String, List<TrackModel>>> = tracks
        .map { list -> list.groupBy { it.audioUrl?.substringBeforeLast("/", "Unknown") ?: "Unknown" } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private val mediaController: MediaController?
        get() = if (mediaControllerFuture?.isDone == true) try { mediaControllerFuture?.get() } catch (_: Exception) { null } else null

    private var exoPlayerRetryCount = 0
    private val MAX_RETRIES = 3

    private val youtubeRepository = com.gaminghub.musify.data.repository.YouTubeRepository()

    init {
        _trendingTracks.value = getFallbackTracks()
        initializeController()
        fetchTrendingMusic()
        startProgressUpdater()
        loadLocalTracks()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(getApplication(), ComponentName(getApplication(), MusicPlaybackService::class.java))
        mediaControllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        mediaControllerFuture?.addListener({
            val controller = mediaController ?: return@addListener
            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val url = mediaItem?.mediaMetadata?.extras?.getString("original_url") ?: mediaItem?.mediaId
                    val track = _currentQueue.value.find { it.audioUrl == url } ?: 
                                 _trendingTracks.value.find { it.audioUrl == url } ?:
                                 _searchTracks.value.find { it.audioUrl == url }
                    _currentTrack.value = track
                    
                    track?.let {
                        viewModelScope.launch(Dispatchers.IO) {
                            dao.recordHistory(it.audioUrl ?: "", it.toEntity())
                        }
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _isLoading.value = (playbackState == Player.STATE_BUFFERING)
                    if (playbackState == Player.STATE_READY) {
                        _duration.value = controller.duration
                    }
                }
                
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e(tag, "Player Error: ${error.message} (Code: ${error.errorCode})")
                    _isLoading.value = false
                    // Retry on error if it's a transient issue
                    if ((error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                        error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED) &&
                        exoPlayerRetryCount < MAX_RETRIES) {
                        exoPlayerRetryCount++
                        Log.w(tag, "Retrying playback ($exoPlayerRetryCount/$MAX_RETRIES) due to transient error.")
                        currentTrack.value?.let { playTrack(it, _currentQueue.value, true) }
                    } else {
                        Log.e(tag, "Max retries reached or fatal error. Not retrying.")
                    }
                }
            })
        }, MoreExecutors.directExecutor())
    }

    private fun startProgressUpdater() {
        viewModelScope.launch {
            while (true) {
                mediaController?.let {
                    if (it.isPlaying) {
                        _currentPosition.value = it.currentPosition
                    }
                }
                delay(1000)
            }
        }
    }

    fun fetchTrendingMusic() {
        fetchMusic("trending music", _trendingTracks)
    }

    fun loadLocalTracks() {
        viewModelScope.launch(Dispatchers.IO) {
            val tracks = mutableListOf<TrackModel>()
            val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.GENRE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
            var cursor: Cursor? = null
            try {
                cursor = getApplication<Application>().contentResolver.query(
                    uri, projection, selection, null, sortOrder
                )
                cursor?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val genreCol = c.getColumnIndex(MediaStore.Audio.Media.GENRE)
                    val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    while (c.moveToNext()) {
                        val id = c.getLong(idCol)
                        val title = c.getString(titleCol) ?: "Unknown"
                        val artist = c.getString(artistCol) ?: "Unknown Artist"
                        val album = c.getString(albumCol)
                        val genre = if (genreCol >= 0) c.getString(genreCol) else null
                        val path = c.getString(dataCol) ?: continue
                        val albumId = c.getLong(albumIdCol)
                        val artUri = Uri.parse("content://media/external/audio/albumart/$albumId").toString()
                        tracks.add(
                            TrackModel(
                                title = title,
                                artist = artist,
                                audioUrl = path,
                                albumArtUrl = artUri,
                                album = album,
                                genre = genre
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading local tracks: ${e.message}", e)
            } finally {
                cursor?.close()
            }
            withContext(Dispatchers.Main) {
                _localTracks.value = tracks
                Log.d(tag, "Loaded ${tracks.size} local tracks from device")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isNotEmpty()) {
            searchMusic(query)
        }
    }

    fun searchMusic(query: String) {
        fetchMusic(query, _searchTracks)
    }

    private fun fetchMusic(term: String, stateFlow: MutableStateFlow<List<TrackModel>>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(tag, "Fetching music for: $term")
                _isLoading.value = true
                val results = youtubeRepository.fetchMusic(term, com.gaminghub.musify.data.repository.SearchSource.YOUTUBE)
                
                val mappedTracks = results.map { item ->
                    TrackModel(
                        title = item.title,
                        artist = item.artist,
                        audioUrl = item.audioUrl,
                        albumArtUrl = item.albumArtUrl,
                        album = item.album,
                        genre = item.genre
                    )
                }
                
                withContext(Dispatchers.Main) {
                    if (mappedTracks.isNotEmpty()) {
                        stateFlow.value = mappedTracks
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error fetching music for $term: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun getFallbackTracks(): List<TrackModel> = listOf(
        TrackModel(
            title = "Blinding Lights",
            artist = "The Weeknd",
            audioUrl = "https://www.youtube.com/watch?v=4NRXx6U8ABQ",
            albumArtUrl = "https://i.ytimg.com/vi/4NRXx6U8ABQ/maxresdefault.jpg"
        ),
        TrackModel(
            title = "Shape of You",
            artist = "Ed Sheeran",
            audioUrl = "https://www.youtube.com/watch?v=JGwWNGJdvx8",
            albumArtUrl = "https://i.ytimg.com/vi/JGwWNGJdvx8/maxresdefault.jpg"
        ),
        TrackModel(
            title = "Starboy",
            artist = "The Weeknd ft. Daft Punk",
            audioUrl = "https://www.youtube.com/watch?v=34Na4j8AVgA",
            albumArtUrl = "https://i.ytimg.com/vi/34Na4j8AVgA/maxresdefault.jpg"
        ),
        TrackModel(
            title = "As It Was",
            artist = "Harry Styles",
            audioUrl = "https://www.youtube.com/watch?v=H5v3kku4y6Q",
            albumArtUrl = "https://i.ytimg.com/vi/H5v3kku4y6Q/maxresdefault.jpg"
        ),
        TrackModel(
            title = "Levitating",
            artist = "Dua Lipa",
            audioUrl = "https://www.youtube.com/watch?v=TUVcZfQe-Kw",
            albumArtUrl = "https://i.ytimg.com/vi/TUVcZfQe-Kw/maxresdefault.jpg"
        ),
        TrackModel(
            title = "Flowers",
            artist = "Miley Cyrus",
            audioUrl = "https://www.youtube.com/watch?v=G7KNmW9a75Y",
            albumArtUrl = "https://i.ytimg.com/vi/G7KNmW9a75Y/maxresdefault.jpg"
        ),
        TrackModel(
            title = "Stay",
            artist = "The Kid LAROI & Justin Bieber",
            audioUrl = "https://www.youtube.com/watch?v=kTJczUoc26U",
            albumArtUrl = "https://i.ytimg.com/vi/kTJczUoc26U/maxresdefault.jpg"
        ),
        TrackModel(
            title = "Someone You Loved",
            artist = "Lewis Capaldi",
            audioUrl = "https://www.youtube.com/watch?v=zABLecsR5UE",
            albumArtUrl = "https://i.ytimg.com/vi/zABLecsR5UE/maxresdefault.jpg"
        )
    )

    fun playTrack(track: TrackModel, queue: List<TrackModel> = emptyList(), isRetry: Boolean = false) {
        if (!isRetry) {
            exoPlayerRetryCount = 0
        }
        _currentTrack.value = track
        _currentQueue.value = if (queue.isNotEmpty()) queue else listOf(track)
        _isLoading.value = true

        viewModelScope.launch {
            // Ensure MediaController is connected
            var controller = mediaController
            if (controller == null) {
                var waitMs = 0
                while (mediaControllerFuture?.isDone != true && waitMs < 5000) {
                    delay(100)
                    waitMs += 100
                }
                controller = mediaController
            }

            if (controller == null) {
                Log.e(tag, "Controller not available after waiting, cannot play")
                _isLoading.value = false
                return@launch
            }

            withContext(Dispatchers.IO) {
                val url = track.audioUrl ?: return@withContext
                Log.d(tag, "Attempting to play track '${track.title}' with URL: $url")
                
                // Use centralized StreamExtractionManager for caching, verification and audio stream extraction
                val playUrl = StreamExtractionManager.extractPlayableUrl(url, fastStart = true, forceRefresh = isRetry)

                if (playUrl != null && !playUrl.contains("youtube.com/watch") && !playUrl.contains("youtu.be/")) {
                    withContext(Dispatchers.Main) {
                        val mediaItem = MediaItem.Builder()
                            .setUri(playUrl.toUri())
                            .setMediaId(url)
                            .setMediaMetadata(
                                androidx.media3.common.MediaMetadata.Builder()
                                    .setTitle(track.title)
                                    .setArtist(track.artist)
                                    .setArtworkUri(track.albumArtUrl?.toUri() ?: "".toUri())
                                    .setExtras(android.os.Bundle().apply { putString("original_url", url) })
                                    .build()
                            )
                            .build()
                            
                        controller.setMediaItem(mediaItem)
                        controller.prepare()
                        controller.play()
                        _isPlaying.value = true
                        _isLoading.value = false
                        Log.d(tag, "Playback started successfully for '${track.title}'")
                    }
                } else {
                    Log.e(tag, "Failed to extract playable audio stream URL for '${track.title}' ($url)")
                    withContext(Dispatchers.Main) {
                        _isLoading.value = false
                    }
                }
            }
        }
    }

    fun togglePlayPause() {
        mediaController?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertPlaylist(PlaylistEntity(name = name.trim()))
        }
    }

    fun deletePlaylist(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deletePlaylist(id)
        }
    }

    fun renamePlaylist(id: Int, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.renamePlaylist(id, newName.trim())
        }
    }

    fun toggleFavorite(track: TrackModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val url = track.audioUrl ?: return@launch
            dao.toggleFavorite(url, track.toEntity())
        }
    }

    fun isFavorite(url: String): Flow<Boolean> {
        return dao.getFavoriteTracks().map { list -> list.any { it.audioUrl == url } }
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
    }

    fun playNext() {
        mediaController?.seekToNext()
    }

    fun playPrevious() {
        mediaController?.seekToPrevious()
    }

    fun setPlaybackSpeed(speed: Float) {
        mediaController?.setPlaybackParameters(
            androidx.media3.common.PlaybackParameters(speed.coerceIn(0.25f, 3f))
        )
    }

    fun setRepeatMode(repeatMode: Int) {
        // repeatMode: Player.REPEAT_MODE_OFF / REPEAT_MODE_ONE / REPEAT_MODE_ALL
        mediaController?.repeatMode = repeatMode
    }

    fun setShuffleModeEnabled(enabled: Boolean) {
        mediaController?.shuffleModeEnabled = enabled
    }

    /** Move a song in the Up-Next list from [fromIndex] to [toIndex] (indices within upNextQueue). */
    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val queue = _currentQueue.value.toMutableList()
        val current = _currentTrack.value
        val offset = if (current == null) 0
                     else (queue.indexOfFirst { it.audioUrl == current.audioUrl } + 1).coerceAtLeast(0)
        val absFrom = offset + fromIndex
        val absTo   = offset + toIndex
        if (absFrom !in queue.indices || absTo !in queue.indices) return
        val item = queue.removeAt(absFrom)
        queue.add(absTo, item)
        _currentQueue.value = queue
    }

    /** Jump playback to the track at [upNextIndex] within upNextQueue. */
    fun playFromQueue(upNextIndex: Int) {
        val queue = _currentQueue.value
        val current = _currentTrack.value
        val offset = if (current == null) 0
                     else (queue.indexOfFirst { it.audioUrl == current.audioUrl } + 1).coerceAtLeast(0)
        val absIdx = offset + upNextIndex
        if (absIdx !in queue.indices) return
        val track = queue[absIdx]
        playTrack(track, queue)
    }

    override fun onCleared() {
        super.onCleared()
        mediaControllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
