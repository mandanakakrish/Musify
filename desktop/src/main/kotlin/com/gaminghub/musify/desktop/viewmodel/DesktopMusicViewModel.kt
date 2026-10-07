package com.gaminghub.musify.desktop.viewmodel

import com.gaminghub.musify.desktop.audio.DesktopAudioPlayer
import com.gaminghub.musify.desktop.data.DesktopDatabase
import com.gaminghub.musify.desktop.download.DesktopDownloadManager
import com.gaminghub.musify.desktop.model.*
import com.gaminghub.musify.desktop.network.DesktopLyricsRepository
import com.gaminghub.musify.desktop.network.DesktopYouTubeRepository
import com.gaminghub.musify.desktop.sync.DesktopFirestoreSyncManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.awt.SystemTray
import java.awt.TrayIcon

class DesktopMusicViewModel {

    val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val youtubeRepo = DesktopYouTubeRepository()
    val lyricsRepo = DesktopLyricsRepository()
    val database = DesktopDatabase()
    val downloadManager = DesktopDownloadManager(youtubeRepo)
    val syncManager = DesktopFirestoreSyncManager

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val audioPlayer: DesktopAudioPlayer = DesktopAudioPlayer {
        handleTrackEnded()
    }

    // Playback state
    private val _currentTrack = MutableStateFlow<TrackModel?>(null)
    val currentTrack: StateFlow<TrackModel?> = _currentTrack.asStateFlow()

    val isPlaying = audioPlayer.isPlaying
    val positionMs = audioPlayer.positionMs
    val durationMs = audioPlayer.durationMs
    val isBuffering = audioPlayer.isBuffering
    val volume = audioPlayer.volume
    val isMuted = audioPlayer.isMuted
    val equalizerBands = audioPlayer.equalizerBands

    // Queue state
    private val _queue = MutableStateFlow<List<TrackModel>>(emptyList())
    val queue: StateFlow<List<TrackModel>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(-1)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    // Navigation & UI state
    private val _currentScreen = MutableStateFlow(DesktopScreen.HOME)
    val currentScreen: StateFlow<DesktopScreen> = _currentScreen.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<PlaylistModel?>(null)
    val selectedPlaylist: StateFlow<PlaylistModel?> = _selectedPlaylist.asStateFlow()

    private val _isQueueOpen = MutableStateFlow(false)
    val isQueueOpen: StateFlow<Boolean> = _isQueueOpen.asStateFlow()

    private val _isLyricsOpen = MutableStateFlow(false)
    val isLyricsOpen: StateFlow<Boolean> = _isLyricsOpen.asStateFlow()

    private val _isMiniPlayer = MutableStateFlow(false)
    val isMiniPlayer: StateFlow<Boolean> = _isMiniPlayer.asStateFlow()

    private val _isEqualizerOpen = MutableStateFlow(false)
    val isEqualizerOpen: StateFlow<Boolean> = _isEqualizerOpen.asStateFlow()

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<TrackModel>>(emptyList())
    val searchResults: StateFlow<List<TrackModel>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Lyrics state
    private val _lyrics = MutableStateFlow<LyricsData?>(null)
    val lyrics: StateFlow<LyricsData?> = _lyrics.asStateFlow()

    private val _currentLyricIndex = MutableStateFlow(-1)
    val currentLyricIndex: StateFlow<Int> = _currentLyricIndex.asStateFlow()

    // Library state
    private val _favorites = MutableStateFlow<List<TrackModel>>(emptyList())
    val favorites: StateFlow<List<TrackModel>> = _favorites.asStateFlow()

    private val _playlists = MutableStateFlow<List<PlaylistModel>>(emptyList())
    val playlists: StateFlow<List<PlaylistModel>> = _playlists.asStateFlow()

    private val _history = MutableStateFlow<List<TrackModel>>(emptyList())
    val history: StateFlow<List<TrackModel>> = _history.asStateFlow()

    // Settings
    private val _streamingQuality = MutableStateFlow(StreamingQuality.HIGH)
    val streamingQuality: StateFlow<StreamingQuality> = _streamingQuality.asStateFlow()

    // Google Auth State
    private val _googleUser = MutableStateFlow<GoogleUser?>(null)
    val googleUser: StateFlow<GoogleUser?> = _googleUser.asStateFlow()

    private val _showGoogleSignInDialog = MutableStateFlow(false)
    val showGoogleSignInDialog: StateFlow<Boolean> = _showGoogleSignInDialog.asStateFlow()

    var onShowToast: ((String) -> Unit)? = null

    init {
        loadLibraryData()
        startLyricsSyncJob()
    }

    private fun loadLibraryData() {
        scope.launch {
            _favorites.value = database.getFavorites()
            _playlists.value = database.getPlaylists()
            _history.value = database.getHistory()
            val savedUser = database.getGoogleUser()
            _googleUser.value = savedUser

            // If user is already logged in with a real Google email, sync Android cloud library!
            val email = savedUser?.email
            if (!email.isNullOrBlank() && email != "google.user@gmail.com") {
                syncWithCloud(showToast = false)
            }
        }
    }

    private fun startLyricsSyncJob() {
        scope.launch {
            positionMs.collect { pos ->
                val syncList = _lyrics.value?.syncedLyrics ?: emptyList()
                if (syncList.isNotEmpty()) {
                    var activeIndex = -1
                    for (i in syncList.indices) {
                        if (pos >= syncList[i].timeMs) {
                            activeIndex = i
                        } else {
                            break
                        }
                    }
                    _currentLyricIndex.value = activeIndex
                } else {
                    _currentLyricIndex.value = -1
                }
            }
        }
    }

    fun playTrack(track: TrackModel, newQueue: List<TrackModel>? = null) {
        scope.launch {
            _currentTrack.value = track

            if (newQueue != null) {
                _queue.value = newQueue
                _queueIndex.value = newQueue.indexOfFirst { it.audioUrl == track.audioUrl }.coerceAtLeast(0)
            } else {
                if (_queue.value.isEmpty() || !_queue.value.any { it.audioUrl == track.audioUrl }) {
                    _queue.value = listOf(track)
                    _queueIndex.value = 0
                } else {
                    _queueIndex.value = _queue.value.indexOfFirst { it.audioUrl == track.audioUrl }
                }
            }

            // Record to history & database
            database.recordPlay(track)
            _history.value = database.getHistory()

            // Fetch lyrics in background
            fetchLyricsForCurrentTrack(track)

            // Resolve and stream audio
            if (track.isLocal && track.localFilePath != null) {
                audioPlayer.play(track.localFilePath)
            } else {
                val streamUrl = youtubeRepo.extractStreamUrl(track.audioUrl, _streamingQuality.value)
                if (streamUrl != null) {
                    audioPlayer.play(streamUrl)
                } else {
                    onShowToast?.invoke("Unable to stream audio for '${track.title}'")
                }
            }
        }
    }

    fun togglePlayPause() {
        if (_currentTrack.value == null && _queue.value.isNotEmpty()) {
            playTrack(_queue.value.first())
        } else {
            audioPlayer.togglePlayPause()
        }
    }

    fun playNext() {
        val q = _queue.value
        if (q.isEmpty()) return

        val nextIndex = if (_isShuffle.value) {
            (0 until q.size).filter { it != _queueIndex.value }.randomOrNull() ?: 0
        } else {
            val idx = _queueIndex.value + 1
            if (idx < q.size) idx else if (_repeatMode.value == RepeatMode.ALL) 0 else -1
        }

        if (nextIndex in q.indices) {
            _queueIndex.value = nextIndex
            playTrack(q[nextIndex])
        }
    }

    fun playPrevious() {
        val q = _queue.value
        if (q.isEmpty()) return

        if (positionMs.value > 3000L) {
            audioPlayer.seekTo(0L)
            return
        }

        val prevIndex = if (_isShuffle.value) {
            (0 until q.size).filter { it != _queueIndex.value }.randomOrNull() ?: 0
        } else {
            val idx = _queueIndex.value - 1
            if (idx >= 0) idx else if (_repeatMode.value == RepeatMode.ALL) q.size - 1 else 0
        }

        if (prevIndex in q.indices) {
            _queueIndex.value = prevIndex
            playTrack(q[prevIndex])
        }
    }

    private fun handleTrackEnded() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                _currentTrack.value?.let { playTrack(it) }
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                if (_queueIndex.value < _queue.value.size - 1) {
                    playNext()
                }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        audioPlayer.seekTo(positionMs)
    }

    fun setVolume(vol: Float) {
        audioPlayer.setVolume(vol)
    }

    fun toggleMute() {
        audioPlayer.toggleMute()
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleFavorite(track: TrackModel) {
        scope.launch {
            val isFav = database.toggleFavorite(track)
            _favorites.value = database.getFavorites()
            onShowToast?.invoke(if (isFav) "Added to Liked Songs" else "Removed from Liked Songs")

            val email = _googleUser.value?.email
            if (!email.isNullOrBlank() && email != "google.user@gmail.com") {
                syncManager.syncFavoriteToCloud(email, track, isFav)
            }
        }
    }

    fun isTrackFavorite(track: TrackModel): Boolean {
        return _favorites.value.any { it.audioUrl == track.audioUrl }
    }

    fun addToQueue(track: TrackModel) {
        val currentList = _queue.value.toMutableList()
        currentList.add(track)
        _queue.value = currentList
        onShowToast?.invoke("Added '${track.title}' to queue")
    }

    fun playNextInQueue(track: TrackModel) {
        val currentList = _queue.value.toMutableList()
        val insertIndex = (_queueIndex.value + 1).coerceIn(0, currentList.size)
        currentList.add(insertIndex, track)
        _queue.value = currentList
        onShowToast?.invoke("Playing '${track.title}' next")
    }

    fun removeFromQueue(index: Int) {
        if (index in _queue.value.indices) {
            val currentList = _queue.value.toMutableList()
            currentList.removeAt(index)
            _queue.value = currentList
            if (index < _queueIndex.value) {
                _queueIndex.value -= 1
            }
        }
    }

    fun clearQueue() {
        val curr = _currentTrack.value
        _queue.value = if (curr != null) listOf(curr) else emptyList()
        _queueIndex.value = if (curr != null) 0 else -1
    }

    fun search(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        scope.launch {
            _isSearching.value = true
            try {
                val results = youtubeRepo.searchTracks(query)
                _searchResults.value = results
            } catch (e: Exception) {
                System.err.println("Search failed: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }

    private fun fetchLyricsForCurrentTrack(track: TrackModel) {
        scope.launch {
            _lyrics.value = null
            _currentLyricIndex.value = -1
            val result = lyricsRepo.fetchLyrics(track.artist, track.title)
            _lyrics.value = result
        }
    }

    fun navigateTo(screen: DesktopScreen) {
        _currentScreen.value = screen
    }

    fun openPlaylist(playlist: PlaylistModel) {
        _selectedPlaylist.value = playlist
        _currentScreen.value = DesktopScreen.PLAYLIST_DETAIL
    }

    fun createPlaylist(title: String, description: String = "") {
        scope.launch {
            val p = database.createPlaylist(title, description)
            _playlists.value = database.getPlaylists()
            onShowToast?.invoke("Created playlist '$title'")

            val email = _googleUser.value?.email
            if (!email.isNullOrBlank() && email != "google.user@gmail.com") {
                syncManager.syncPlaylistToCloud(email, p)
            }
        }
    }

    fun addTrackToPlaylist(playlist: PlaylistModel, track: TrackModel) {
        scope.launch {
            database.addTrackToPlaylist(playlist.id, track)
            _playlists.value = database.getPlaylists()
            onShowToast?.invoke("Added '${track.title}' to ${playlist.title}")

            val email = _googleUser.value?.email
            if (!email.isNullOrBlank() && email != "google.user@gmail.com") {
                val updated = _playlists.value.find { it.id == playlist.id } ?: playlist
                syncManager.syncPlaylistToCloud(email, updated)
            }
        }
    }

    fun removeTrackFromPlaylist(playlist: PlaylistModel, track: TrackModel) {
        scope.launch {
            database.removeTrackFromPlaylist(playlist.id, track.audioUrl)
            _playlists.value = database.getPlaylists()
            _selectedPlaylist.value = _playlists.value.find { it.id == playlist.id }

            val email = _googleUser.value?.email
            if (!email.isNullOrBlank() && email != "google.user@gmail.com") {
                val updated = _playlists.value.find { it.id == playlist.id } ?: playlist
                syncManager.syncPlaylistToCloud(email, updated)
            }
        }
    }

    fun deletePlaylist(playlist: PlaylistModel) {
        scope.launch {
            database.deletePlaylist(playlist.id)
            _playlists.value = database.getPlaylists()
            if (_selectedPlaylist.value?.id == playlist.id) {
                _currentScreen.value = DesktopScreen.LIBRARY
            }
            onShowToast?.invoke("Deleted playlist '${playlist.title}'")

            val email = _googleUser.value?.email
            if (!email.isNullOrBlank() && email != "google.user@gmail.com") {
                syncManager.deletePlaylistFromCloud(email, playlist.id)
            }
        }
    }

    fun downloadTrack(track: TrackModel) {
        scope.launch {
            onShowToast?.invoke("Starting download: '${track.title}'")
            val success = downloadManager.downloadTrack(track)
            if (success) {
                onShowToast?.invoke("Downloaded '${track.title}' to Music folder!")
            } else {
                onShowToast?.invoke("Failed to download '${track.title}'")
            }
        }
    }

    fun setStreamingQuality(quality: StreamingQuality) {
        _streamingQuality.value = quality
        onShowToast?.invoke("Audio quality set to ${quality.label}")
    }

    fun toggleMiniPlayer() {
        _isMiniPlayer.value = !_isMiniPlayer.value
    }

    fun toggleQueue() {
        _isQueueOpen.value = !_isQueueOpen.value
    }

    fun toggleLyrics() {
        _isLyricsOpen.value = !_isLyricsOpen.value
    }

    fun openEqualizer() {
        _isEqualizerOpen.value = true
    }

    fun closeEqualizer() {
        _isEqualizerOpen.value = false
        if (_currentScreen.value == DesktopScreen.EQUALIZER) {
            _currentScreen.value = DesktopScreen.HOME
        }
    }

    fun toggleEqualizer() {
        if (_isEqualizerOpen.value || _currentScreen.value == DesktopScreen.EQUALIZER) {
            closeEqualizer()
        } else {
            openEqualizer()
        }
    }

    fun openGoogleSignIn() {
        _showGoogleSignInDialog.value = true
    }

    fun closeGoogleSignIn() {
        _showGoogleSignInDialog.value = false
    }

    fun signInWithGoogle(user: GoogleUser) {
        scope.launch {
            database.saveGoogleUser(user)
            _googleUser.value = user
            _showGoogleSignInDialog.value = false
            onShowToast?.invoke("Signed in as ${user.name} (${user.email})")

            // Automatically sync all playlists & favorites from Android account!
            syncWithCloud(showToast = true)
        }
    }

    fun syncWithCloud(showToast: Boolean = true) {
        val user = _googleUser.value
        val email = user?.email
        if (email.isNullOrBlank() || email == "google.user@gmail.com") {
            if (showToast) onShowToast?.invoke("Please sign in with your Google account first.")
            return
        }

        scope.launch {
            _isSyncing.value = true
            try {
                if (showToast) onShowToast?.invoke("Syncing library with Android cloud ($email)...")
                val result = syncManager.pullFromCloud(email, database)
                _playlists.value = database.getPlaylists()
                _favorites.value = database.getFavorites()
                _history.value = database.getHistory()
                if (showToast) {
                    onShowToast?.invoke(result.message)
                }
            } catch (e: Exception) {
                if (showToast) onShowToast?.invoke("Sync error: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun signOutGoogle() {
        scope.launch {
            database.clearGoogleUser()
            _googleUser.value = null
            onShowToast?.invoke("Signed out of Google")
        }
    }
}
