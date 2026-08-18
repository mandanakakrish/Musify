package com.gaminghub.musify.ui.viewmodels

import android.app.Application
import android.content.ComponentName
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.gaminghub.musicplayer.MusicPlaybackService
import com.gaminghub.musify.TrackModel
import com.gaminghub.musify.LyricLine
import com.gaminghub.musify.data.repository.LyricsRepository
import com.gaminghub.musify.data.repository.YouTubeRepository
import com.gaminghub.musify.util.CommonUtils
import com.gaminghub.musify.util.StreamExtractionManager
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import androidx.compose.ui.platform.LocalContext
import com.gaminghub.musify.util.AnalyticsManager

@UnstableApi
class PlaybackViewModel(
    application: Application,
    private val analyticsManager: AnalyticsManager
) : AndroidViewModel(application) {
    private val tag = "PlaybackViewModel"
    private val youtubeRepository = YouTubeRepository()
    private val lyricsRepository = LyricsRepository()
    private val dao = com.gaminghub.musicplayer.data.MusicDatabase.getInstance(application).dao

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentTrack = MutableStateFlow<TrackModel?>(null)
    val currentTrack: StateFlow<TrackModel?> = _currentTrack

    private val _currentQueue = MutableStateFlow<List<TrackModel>>(emptyList())
    val currentQueue: StateFlow<List<TrackModel>> = _currentQueue

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _lyrics = MutableStateFlow<String?>(null)
    val lyrics: StateFlow<String?> = _lyrics

    private val _syncedLyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val syncedLyrics: StateFlow<List<LyricLine>> = _syncedLyrics

    private val _ambientColors = MutableStateFlow<List<Color>>(listOf(Color(0xFF1A1A1A), Color(0xFF000000)))
    val ambientColors: StateFlow<List<Color>> = _ambientColors

    val upNextQueue: StateFlow<List<TrackModel>> = combine(_currentQueue, _currentTrack) { queue, current ->
        if (current == null) queue
        else {
            val idx = queue.indexOfFirst { it.audioUrl == current.audioUrl }
            if (idx >= 0) queue.drop(idx + 1) else queue
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentLyricIndex = MutableStateFlow(-1)
    val currentLyricIndex: StateFlow<Int> = _currentLyricIndex

    private val _isSleepTimerRunning = MutableStateFlow(false)
    val isSleepTimerRunning: StateFlow<Boolean> = _isSleepTimerRunning

    private val _audioSessionId = MutableStateFlow(-1)
    val audioSessionId: StateFlow<Int> = _audioSessionId

    private val _sleepTimerMillis = MutableStateFlow(0L)
    val sleepTimerMillis: StateFlow<Long> = _sleepTimerMillis

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed

    private var countDownTimer: android.os.CountDownTimer? = null

    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private val mediaController: MediaController?
        get() = if (mediaControllerFuture?.isDone == true) try { mediaControllerFuture?.get() } catch (_: Exception) { null } else null

    private var exoPlayerRetryCount = 0
    private val MAX_RETRIES = 3
    private var lyricsJob: Job? = null

    private val _controllerInitialized = MutableStateFlow(false)
    val controllerInitialized: StateFlow<Boolean> = _controllerInitialized

    init {
        viewModelScope.launch(Dispatchers.IO) {
            initializeController()
            _controllerInitialized.value = true
        }
        startProgressTicker()
        restorePlaybackSession()
        
        viewModelScope.launch {
            _currentPosition.collect { pos ->
                val lines = _syncedLyrics.value
                if (lines.isNotEmpty()) {
                    val index = lines.indexOfLast { it.timeMs <= pos }
                    if (index != _currentLyricIndex.value) {
                        _currentLyricIndex.value = index
                    }
                }
            }
        }
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
                    _currentPosition.value = 0L
                    _currentLyricIndex.value = -1
                    val url = mediaItem?.mediaMetadata?.extras?.getString("original_url") ?: mediaItem?.mediaId
                    val track = _currentQueue.value.find { it.audioUrl == url }
                    track?.let { 
                        fetchLyrics(it)
                        extractColorsFromUrl(it.albumArtUrl)
                    }
                    preloadNextTracks()
                    savePlaybackSession()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _isLoading.value = (playbackState == Player.STATE_BUFFERING)
                    if (playbackState == Player.STATE_READY) {
                        _duration.value = controller.duration
                        _audioSessionId.value = MusicPlaybackService.currentAudioSessionId
                        _shuffleModeEnabled.value = controller.shuffleModeEnabled
                        _repeatMode.value = controller.repeatMode
                        _playbackSpeed.value = controller.playbackParameters.speed
                    }
                    if (playbackState == Player.STATE_ENDED) playNext()
                }

                override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                    _playbackSpeed.value = playbackParameters.speed
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    _shuffleModeEnabled.value = shuffleModeEnabled
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    _repeatMode.value = repeatMode
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e(tag, "Player Error: ${error.errorCode}")
                    handlePlayerError(error)
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    savePlaybackSession()
                }
            })
        }, MoreExecutors.directExecutor())
    }

    private fun startProgressTicker() {
        viewModelScope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    _currentPosition.value = controller.currentPosition
                    if (controller.duration > 0) _duration.value = controller.duration
                    delay(32) // High precision (approx 30fps) for smooth progress & lyrics
                    
                    // Periodic save (approx every 10 seconds)
                    if (controller.currentPosition > 0 && controller.currentPosition % 10000 < 100) {
                        savePlaybackSession()
                    }
                } else {
                    delay(100) // Lower frequency when paused
                }
            }
        }
    }

    fun playTrack(track: TrackModel, queue: List<TrackModel> = emptyList(), isRetry: Boolean = false) {
        analyticsManager.logFeatureClick("play_track")
        if (!isRetry) exoPlayerRetryCount = 0
        _currentTrack.value = track
        _currentQueue.value = if (queue.isNotEmpty()) queue else listOf(track)
        _isLoading.value = true
        
        viewModelScope.launch(Dispatchers.IO) {
            val controller = getOrWaitController() ?: return@launch
            
            // PRIORITY 1: Instant Start (Extracted Only)
            val url = track.audioUrl ?: return@launch
            val playUrl = StreamExtractionManager.extractPlayableUrl(url, fastStart = true, forceRefresh = isRetry)
            
            if (playUrl != null) {
                withContext(Dispatchers.Main) {
                    val mediaItem = createMediaItem(track, url, playUrl)
                    controller.setMediaItem(mediaItem)
                    controller.prepare()
                    controller.play()
                    _isPlaying.value = true
                    _isLoading.value = false
                    
                    // Start Buffering Watchdog
                    startBufferingWatchdog(controller, track)
                }
                
                // PRIORITY 2: Background Queue Extraction
                // We ONLY add items that have been pre-extracted to avoid Media3 trying to play HTML pages
                if (_currentQueue.value.size > 1) {
                    launch {
                        val fullQueue = _currentQueue.value
                        val currentIndex = fullQueue.indexOfFirst { it.audioUrl == track.audioUrl }.coerceAtLeast(0)
                        
                        // Extract next 5 tracks immediately for gapless transition
                        for (i in 1..5) {
                            val nextTrack = fullQueue.getOrNull(currentIndex + i) ?: break
                            val nextUrl = nextTrack.audioUrl ?: continue
                            val nextPlayUrl = StreamExtractionManager.extractPlayableUrl(nextUrl, fastStart = false)
                            
                            if (nextPlayUrl != null) {
                                withContext(Dispatchers.Main) {
                                    val nextMediaItem = createMediaItem(nextTrack, nextUrl, nextPlayUrl)
                                    // Add to the end of the current player playlist 
                                    // (Media3 handles duplicates or we can check index)
                                    if (controller.mediaItemCount <= (currentIndex + i)) {
                                        controller.addMediaItem(nextMediaItem)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    Log.e(tag, "Failed to extract playable URL for ${track.title}")
                }
            }
        }
    }

    private var watchdogJob: Job? = null

    private fun startBufferingWatchdog(player: Player, track: TrackModel) {
        watchdogJob?.cancel()
        watchdogJob = viewModelScope.launch {
            delay(15000) // 15 second threshold
            if (player.playbackState == Player.STATE_BUFFERING && _isPlaying.value) {
                Log.w(tag, "Watchdog triggered: Player stuck in buffering. Attempting recovery...")
                playTrack(track, _currentQueue.value, isRetry = true)
            }
        }
    }

    private fun createMediaItem(track: TrackModel, originalUrl: String, playUrl: String): MediaItem {
        return MediaItem.Builder()
            .setUri(playUrl.toUri())
            .setMediaId(originalUrl)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setArtworkUri(track.albumArtUrl?.toUri())
                    .setExtras(android.os.Bundle().apply { putString("original_url", originalUrl) })
                    .build()
            ).build()
    }

    private suspend fun getOrWaitController(): MediaController? {
        var controller = mediaController
        var wait = 0
        while (controller == null && wait < 5000) {
            delay(100)
            wait += 100
            controller = mediaController
        }
        return controller
    }

    private fun preloadNextTracks() {
        val queue = _currentQueue.value
        val current = _currentTrack.value ?: return
        val idx = queue.indexOfFirst { it.audioUrl == current.audioUrl }
        // Look ahead 5 tracks instead of 2 for better performance
        for (i in 1..5) {
            val next = queue.getOrNull(idx + i) ?: break
            val url = next.audioUrl ?: continue
            
            viewModelScope.launch {
                // First extract the playable URL
                val playableUrl = com.gaminghub.musify.util.StreamExtractionManager.extractPlayableUrl(url)
                if (playableUrl != null) {
                    // Then trigger the proactive cache download in the service
                    MusicPlaybackService.cacheTrack(getApplication(), playableUrl)
                }
            }
        }
    }

    private fun handlePlayerError(error: androidx.media3.common.PlaybackException) {
        val errorCode = error.errorCode
        Log.e(tag, "Handling Player Error: $errorCode (${error.message})")

        // 403 Forbidden / Error 152-4 or 404 Not Found usually means an expired/blocked YouTube stream URL
        val isExpiredOrBlockedUrl = errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                                    errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ||
                                    errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ||
                                    errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED

        if (isExpiredOrBlockedUrl && exoPlayerRetryCount < MAX_RETRIES) {
            exoPlayerRetryCount++
            val currentTrack = _currentTrack.value ?: return
            
            viewModelScope.launch {
                Log.w(tag, "Attempting stream recovery for track '${currentTrack.title}' (Attempt $exoPlayerRetryCount)")
                // Force a fresh extraction with fallback bitrate for the problematic track
                val newUrl = StreamExtractionManager.extractPlayableUrl(
                    url = currentTrack.audioUrl ?: "",
                    fastStart = (exoPlayerRetryCount % 2 == 1),
                    forceRefresh = true
                )
                
                if (newUrl != null) {
                    withContext(Dispatchers.Main) {
                        val controller = mediaController ?: return@withContext
                        val currentPos = controller.currentPosition
                        val mediaItem = createMediaItem(currentTrack, currentTrack.audioUrl ?: "", newUrl)
                        
                        // Replace the current item in the player without clearing the whole queue
                        val currentIndex = controller.currentMediaItemIndex
                        if (currentIndex != -1) {
                            controller.replaceMediaItem(currentIndex, mediaItem)
                            controller.seekTo(currentIndex, currentPos)
                            controller.prepare()
                            controller.play()
                        }
                    }
                }
            }
            return
        }

        // General retry logic for other network issues
        val isRetryable = errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                          errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
        
        if (isRetryable && exoPlayerRetryCount < MAX_RETRIES) {
            exoPlayerRetryCount++
            _currentTrack.value?.let { track ->
                viewModelScope.launch {
                    delay(1000L * exoPlayerRetryCount)
                    playTrack(track, _currentQueue.value, isRetry = true)
                }
            }
        }
    }

    fun togglePlayPause() {
        analyticsManager.logFeatureClick("toggle_play_pause")
        mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun playNext() {
        analyticsManager.logFeatureClick("play_next")
        val queue = _currentQueue.value
        val idx = queue.indexOfFirst { it.audioUrl == _currentTrack.value?.audioUrl }
        val next = queue.getOrNull(idx + 1)
        if (next != null) playTrack(next, queue)
    }

    fun playPrevious() {
        analyticsManager.logFeatureClick("play_previous")
        val queue = _currentQueue.value
        val idx = queue.indexOfFirst { it.audioUrl == _currentTrack.value?.audioUrl }
        val prev = queue.getOrNull(idx - 1)
        if (prev != null) playTrack(prev, queue)
    }

    fun setQueue(queue: List<TrackModel>) {
        _currentQueue.value = queue
    }

    fun seekTo(pos: Long) {
        mediaController?.seekTo(pos)
    }

    fun setShuffleModeEnabled(enabled: Boolean) {
        mediaController?.shuffleModeEnabled = enabled
        _shuffleModeEnabled.value = enabled
    }

    fun setRepeatMode(repeatMode: Int) {
        mediaController?.repeatMode = repeatMode
        _repeatMode.value = repeatMode
    }

    fun setPlaybackSpeed(speed: Float) {
        mediaController?.setPlaybackSpeed(speed)
        _playbackSpeed.value = speed
    }

    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    fun stopSleepTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        _isSleepTimerRunning.value = false
        _sleepTimerMillis.value = 0L
    }

    fun startSleepTimer(minutes: Int, stopAtEnd: Boolean) {
        stopSleepTimer()
        val totalMs = minutes * 60 * 1000L
        _isSleepTimerRunning.value = true
        _sleepTimerMillis.value = totalMs
        
        countDownTimer = object : android.os.CountDownTimer(totalMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _sleepTimerMillis.value = millisUntilFinished
            }

            override fun onFinish() {
                _isSleepTimerRunning.value = false
                _sleepTimerMillis.value = 0L
                if (stopAtEnd) {
                    val controller = mediaController
                    if (controller?.isPlaying == true) controller.pause()
                }
            }
        }.start()
    }

    fun playFromQueue(index: Int) {
        val queue = _currentQueue.value
        val track = queue.getOrNull(index) ?: return
        playTrack(track, queue)
    }

    fun reorderQueue(from: Int, to: Int) {
        val currentList = _currentQueue.value.toMutableList()
        if (from !in currentList.indices || to !in currentList.indices) return
        
        val item = currentList.removeAt(from)
        currentList.add(to, item)
        _currentQueue.value = currentList
        
        // Try to sync with Media3 controller if possible
        mediaController?.let { controller ->
            if (from < controller.mediaItemCount && to < controller.mediaItemCount) {
                controller.moveMediaItem(from, to)
            }
        }
    }

    /** Insert [track] right after the currently playing song. */
    fun addToQueueNext(track: TrackModel) {
        val queue = _currentQueue.value.toMutableList()
        val current = _currentTrack.value
        if (current == null) {
            // Nothing playing — start playing this track
            playTrack(track, listOf(track))
            return
        }
        val idx = queue.indexOfFirst { it.audioUrl == current.audioUrl }
        val insertAt = if (idx >= 0) idx + 1 else queue.size
        queue.add(insertAt, track)
        _currentQueue.value = queue
    }

    /** Append [track] to the end of the queue. */
    fun addToQueueEnd(track: TrackModel) {
        val queue = _currentQueue.value.toMutableList()
        if (_currentTrack.value == null) {
            playTrack(track, listOf(track))
            return
        }
        queue.add(track)
        _currentQueue.value = queue
    }

    private fun fetchLyrics(track: TrackModel) {
        lyricsJob?.cancel()
        _lyrics.value = "Searching..."
        _syncedLyrics.value = emptyList()
        _currentLyricIndex.value = -1

        lyricsJob = viewModelScope.launch(Dispatchers.IO) {
            val res = lyricsRepository.fetchLyricsFromNetwork(track.artist, track.title)
            if (isActive) {
                if (res != null) {
                    _syncedLyrics.value = CommonUtils.parseLrc(res.syncedLyrics ?: "")
                    _lyrics.value = if (_syncedLyrics.value.isEmpty()) res.plainLyrics else null
                } else {
                    _lyrics.value = "Lyrics not available"
                }
            }
        }
    }

    private fun extractColorsFromUrl(url: String?) {
        if (url.isNullOrBlank()) return
        viewModelScope.launch {
            try {
                val loader = ImageLoader(getApplication())
                val request = ImageRequest.Builder(getApplication())
                    .data(url)
                    .allowHardware(false)
                    .build()
                
                val result = (loader.execute(request) as? SuccessResult)?.drawable
                val bitmap = (result as? android.graphics.drawable.BitmapDrawable)?.bitmap
                
                if (bitmap != null) {
                    val palette = withContext(Dispatchers.Default) {
                        Palette.from(bitmap).generate()
                    }
                    val dominant = palette.getDominantColor(0xFF1A1A1A.toInt())
                    val vibrant = palette.getVibrantColor(dominant)
                    val darkVibrant = palette.getDarkVibrantColor(vibrant)
                    
                    _ambientColors.value = listOf(
                        Color(vibrant),
                        Color(darkVibrant)
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "Color extraction failed: ${e.message}")
            }
        }
    }

    private fun savePlaybackSession() {
        val controller = mediaController ?: return
        val currentQueue = _currentQueue.value
        val currentIndex = controller.currentMediaItemIndex
        val position = controller.currentPosition
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Save state
                dao.savePlaybackState(com.gaminghub.musify.data.PlaybackStateEntity(
                    currentTrackIndex = currentIndex,
                    positionMs = position,
                    isPlaying = controller.isPlaying,
                    shuffleMode = controller.shuffleModeEnabled,
                    repeatMode = controller.repeatMode
                ))
                
                // Save queue if it changed (optimization: could check hash)
                val persistentQueue = currentQueue.mapIndexed { index, track ->
                    com.gaminghub.musify.data.QueueTrackEntity(
                        queuePosition = index,
                        audioUrl = track.audioUrl ?: "",
                        title = track.title,
                        artist = track.artist,
                        albumArtUrl = track.albumArtUrl
                    )
                }
                dao.updatePersistentQueue(persistentQueue)
            } catch (e: Exception) {
                Log.e(tag, "Failed to save playback session", e)
            }
        }
    }

    private fun restorePlaybackSession() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val savedState = dao.getPlaybackState() ?: return@launch
                val savedQueue = dao.getQueueTracks().map { entity ->
                    TrackModel(
                        title = entity.title,
                        artist = entity.artist,
                        audioUrl = entity.audioUrl,
                        albumArtUrl = entity.albumArtUrl
                    )
                }
                
                if (savedQueue.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        _currentQueue.value = savedQueue
                        _currentTrack.value = savedQueue.getOrNull(savedState.currentTrackIndex)
                        
                        val controller = getOrWaitController() ?: return@withContext
                        
                        // We set original URLs initially. The Service/ExoPlayer will trigger 
                        // the Extraction on-demand or we pre-extract the current one.
                        val mediaItems = savedQueue.map { t ->
                            createMediaItem(t, t.audioUrl ?: "", t.audioUrl ?: "")
                        }
                        
                        controller.setMediaItems(mediaItems, savedState.currentTrackIndex, savedState.positionMs)
                        controller.prepare()
                        controller.shuffleModeEnabled = savedState.shuffleMode
                        controller.repeatMode = savedState.repeatMode
                        
                        // Sync UI state
                        _playbackSpeed.value = controller.playbackParameters.speed
                        _audioSessionId.value = MusicPlaybackService.currentAudioSessionId
                        
                        Log.d(tag, "Playback session seamlessly restored: ${savedQueue.size} items.")
                    }
                    
                    // Pre-extract the current restored track to make it ready for "Play" immediately
                    val currentTrack = savedQueue.getOrNull(savedState.currentTrackIndex)
                    currentTrack?.audioUrl?.let { url ->
                        StreamExtractionManager.extractPlayableUrl(url, fastStart = true)
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Restoration failed: ${e.message}")
            }
        }
    }
}
