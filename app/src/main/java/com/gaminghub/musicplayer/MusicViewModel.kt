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
import com.gaminghub.musicplayer.data.FollowedArtistEntity
import com.gaminghub.musicplayer.data.MusicDatabase
import com.gaminghub.musicplayer.data.PlaylistEntity
import com.gaminghub.musicplayer.data.PlaylistTrackEntity
import com.gaminghub.musicplayer.data.repository.LyricsRepository
import com.gaminghub.musicplayer.data.repository.SearchSource
import com.gaminghub.musicplayer.data.repository.YouTubeRepository
import com.gaminghub.musicplayer.data.TrackEntity
import com.gaminghub.musicplayer.data.toEntity
import com.gaminghub.musicplayer.data.toModel
import com.gaminghub.musicplayer.util.CommonUtils
import com.gaminghub.musicplayer.util.MusifyFileMetadataHelper
import com.gaminghub.musicplayer.util.MusifyTrackMetadata
import com.gaminghub.musicplayer.util.toTrackEntity
import com.gaminghub.musicplayer.util.SmartRecommendationEngine
import com.gaminghub.musicplayer.util.StreamExtractionManager
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

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

    private val _youtubeMusic = MutableStateFlow<List<TrackModel>>(emptyList())
    val youtubeMusic: StateFlow<List<TrackModel>> = _youtubeMusic

    private val _searchTracks = MutableStateFlow<List<TrackModel>>(emptyList())
    val searchTracks: StateFlow<List<TrackModel>> = _searchTracks

    private val _topCharts = MutableStateFlow<List<TrackModel>>(emptyList())
    val topCharts: StateFlow<List<TrackModel>> = _topCharts

    private val _currentTrack = MutableStateFlow<TrackModel?>(null)
    val currentTrack: StateFlow<TrackModel?> = _currentTrack

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering

    private val _isShuffled = MutableStateFlow(false)
    val isShuffled: StateFlow<Boolean> = _isShuffled

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode

    private val playbackPrefs = application.getSharedPreferences("musify_playback_prefs", Context.MODE_PRIVATE)
    private val _isSpeedLocked = MutableStateFlow(playbackPrefs.getBoolean("is_speed_locked", false))
    val isSpeedLocked: StateFlow<Boolean> = _isSpeedLocked

    private val _playbackSpeed = MutableStateFlow(
        if (playbackPrefs.getBoolean("is_speed_locked", false)) {
            playbackPrefs.getFloat("locked_speed", 1.0f)
        } else {
            1.0f
        }
    )
    val playbackSpeed: StateFlow<Float> = _playbackSpeed

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    private val _isSearchSubmitted = MutableStateFlow(false)
    val isSearchSubmitted: StateFlow<Boolean> = _isSearchSubmitted.asStateFlow()

    private val searchDebounceFlow = MutableStateFlow("")
    private val searchCache = android.util.LruCache<String, List<TrackModel>>(100)

    private val _currentQueue = MutableStateFlow<List<TrackModel>>(emptyList())
    val currentQueue: StateFlow<List<TrackModel>> = _currentQueue

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive

    /** Tracks that come *after* the currently playing song in the queue. */
    val upNextQueue: StateFlow<List<TrackModel>> = combine(_currentQueue, _currentTrack) { queue, current ->
        if (current == null) queue
        else {
            val idx = queue.indexOfFirst { it.audioUrl == current.audioUrl }
            if (idx >= 0) queue.drop(idx + 1) else queue
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val categories = listOf("All", "Trending", "Pop", "Hip-Hop", "Lo-Fi", "Rock", "Bollywood", "Acoustic", "Workout", "Relax", "Party")
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _selectedChartCountry = MutableStateFlow("India")
    val selectedChartCountry: StateFlow<String> = _selectedChartCountry

    private val _selectedYouTubeCategory = MutableStateFlow("All")
    val selectedYouTubeCategory: StateFlow<String> = _selectedYouTubeCategory

    val tracks: StateFlow<List<TrackModel>> = _trendingTracks

    private val _plainLyrics = MutableStateFlow<String?>(null)
    val plainLyrics: StateFlow<String?> = _plainLyrics

    private val _syncedLyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val syncedLyrics: StateFlow<List<LyricLine>> = _syncedLyrics

    private val _currentLyricIndex = MutableStateFlow(-1)
    val currentLyricIndex: StateFlow<Int> = _currentLyricIndex

    private val _sleepTimerMillis = MutableStateFlow(0L)
    val sleepTimerMillis: StateFlow<Long> = _sleepTimerMillis

    private var sleepTimerJob: Job? = null
    private var lyricsJob: Job? = null
    private val lyricsRepository = LyricsRepository()

    private val db = MusicDatabase.getInstance(application)
    private val dao = db.dao

    val favoriteTracks: StateFlow<List<TrackModel>> = dao.getFavoriteTracks()
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val followedArtists: StateFlow<List<FollowedArtistEntity>> = dao.getFollowedArtists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val downloadedTracks: StateFlow<List<TrackModel>> = dao.getDownloadedTracks()
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _downloadingUrls = MutableStateFlow<Set<String>>(emptySet())
    val downloadingUrls: StateFlow<Set<String>> = _downloadingUrls.asStateFlow()

    val downloadedUrls: StateFlow<Set<String>> = dao.getDownloadedTracks()
        .map { list -> list.map { it.audioUrl }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    val favoriteUrls: StateFlow<Set<String>> = dao.getFavoriteTracks()
        .map { list -> list.mapNotNull { it.audioUrl }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    val totalSongsPlayed: StateFlow<Int> = dao.getTotalSongsPlayedCount()
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val mostPlayedTracks: StateFlow<List<TrackModel>> = dao.getMostPlayedTracks()
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allTimeTopCharts: StateFlow<List<TrackModel>> = dao.getTop50Tracks()
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val weeklyTopCharts: StateFlow<List<TrackModel>> = flow {
        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        emitAll(dao.getTopPlayedTracksWithCountSince(sevenDaysAgo).map { list ->
            list.map { it.toModel() }
        })
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ── Global Weekly & All-Time Top Songs (Across ALL Users on Cloud Firestore) ───
    private val _globalWeeklyTopCharts = MutableStateFlow<List<TrackModel>>(emptyList())
    val globalWeeklyTopCharts: StateFlow<List<TrackModel>> = _globalWeeklyTopCharts.asStateFlow()

    private val _globalAllTimeTopCharts = MutableStateFlow<List<TrackModel>>(emptyList())
    val globalAllTimeTopCharts: StateFlow<List<TrackModel>> = _globalAllTimeTopCharts.asStateFlow()

    private val _isGlobalChartsLoading = MutableStateFlow(false)
    val isGlobalChartsLoading: StateFlow<Boolean> = _isGlobalChartsLoading.asStateFlow()

    fun loadGlobalTopCharts(forceRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isGlobalChartsLoading.value = true
            try {
                // 1. Fetch Weekly from Cloud
                val weeklyRes = com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.fetchWeeklyTopSongsAllUsers()
                val weeklyList = weeklyRes.getOrNull() ?: emptyList()

                // 2. Fetch All-Time from Cloud
                val allTimeRes = com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.fetchGlobalAllTimeTopSongsAllUsers()
                val allTimeList = allTimeRes.getOrNull() ?: emptyList()

                if (weeklyList.isNotEmpty() || allTimeList.isNotEmpty()) {
                    if (weeklyList.isNotEmpty()) _globalWeeklyTopCharts.value = weeklyList
                    if (allTimeList.isNotEmpty()) _globalAllTimeTopCharts.value = allTimeList
                } else {
                    // Seed from local if cloud has no records yet
                    com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.seedGlobalChartsFromLocal(getApplication())
                    val retryWeekly = com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.fetchWeeklyTopSongsAllUsers()
                    val retryAllTime = com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.fetchGlobalAllTimeTopSongsAllUsers()
                    val finalWeekly = retryWeekly.getOrNull() ?: emptyList()
                    val finalAllTime = retryAllTime.getOrNull() ?: emptyList()
                    if (finalWeekly.isNotEmpty()) _globalWeeklyTopCharts.value = finalWeekly
                    if (finalAllTime.isNotEmpty()) _globalAllTimeTopCharts.value = finalAllTime
                }

                // Resilient local fallback if cloud is empty or device is offline
                if (_globalWeeklyTopCharts.value.isEmpty()) {
                    val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
                    val localWeekly = dao.getTopPlayedTracksWithCountSince(sevenDaysAgo).firstOrNull()?.map { it.toModel() } ?: emptyList()
                    if (localWeekly.isNotEmpty()) _globalWeeklyTopCharts.value = localWeekly
                }
                if (_globalAllTimeTopCharts.value.isEmpty()) {
                    val localAllTime = dao.getTop50Tracks().firstOrNull()?.map { it.toModel() } ?: emptyList()
                    if (localAllTime.isNotEmpty()) _globalAllTimeTopCharts.value = localAllTime
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to load global cloud charts: ${e.message}")
                if (_globalWeeklyTopCharts.value.isEmpty()) {
                    val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
                    val localWeekly = dao.getTopPlayedTracksWithCountSince(sevenDaysAgo).firstOrNull()?.map { it.toModel() } ?: emptyList()
                    if (localWeekly.isNotEmpty()) _globalWeeklyTopCharts.value = localWeekly
                }
                if (_globalAllTimeTopCharts.value.isEmpty()) {
                    val localAllTime = dao.getTop50Tracks().firstOrNull()?.map { it.toModel() } ?: emptyList()
                    if (localAllTime.isNotEmpty()) _globalAllTimeTopCharts.value = localAllTime
                }
            } finally {
                _isGlobalChartsLoading.value = false
            }
        }
    }

    fun loadGlobalWeeklyTopCharts(forceRefresh: Boolean = false) = loadGlobalTopCharts(forceRefresh)


    val topMostPlayedTrack: StateFlow<TrackModel?> = dao.getTopMostPlayedTrack()
        .map { it?.toModel() }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val devPickTracks: StateFlow<List<TrackModel>> = dao.getDevPickTracks()
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun getTracksForPlaylist(playlistId: Int): Flow<List<TrackModel>> = dao.getTracksForPlaylist(playlistId)
        .map { list -> list.map { it.toModel() } }

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

    // ── Personalized Recommendation Feeds (Spotify & YouTube Music Architecture) ──
    private val _personalizedFeeds = MutableStateFlow(com.gaminghub.musicplayer.util.PersonalizedRecommendationEngine.PersonalizedFeeds())
    val personalizedFeeds: StateFlow<com.gaminghub.musicplayer.util.PersonalizedRecommendationEngine.PersonalizedFeeds> = _personalizedFeeds.asStateFlow()

    val supermix: StateFlow<List<TrackModel>> = _personalizedFeeds
        .map { it.supermix }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val dailyMixes: StateFlow<List<com.gaminghub.musicplayer.util.PersonalizedRecommendationEngine.PersonalizedMix>> = _personalizedFeeds
        .map { it.dailyMixes }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val listenAgain: StateFlow<List<TrackModel>> = _personalizedFeeds
        .map { it.listenAgain }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val becauseYouLikeArtist: StateFlow<Pair<String, List<TrackModel>>?> = _personalizedFeeds
        .map { it.becauseYouLikeArtist }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val discoverFresh: StateFlow<List<TrackModel>> = _personalizedFeeds
        .map { it.discoverFresh }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val tasteSummary: StateFlow<String> = _personalizedFeeds
        .map { it.tasteSummary }
        .stateIn(viewModelScope, SharingStarted.Lazily, "Curating your personal soundscape...")

    private var personalizedJob: Job? = null
    fun refreshPersonalizedFeeds() {
        personalizedJob?.cancel()
        personalizedJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val feeds = com.gaminghub.musicplayer.util.PersonalizedRecommendationEngine.computePersonalizedFeeds(
                    dao = dao,
                    youtubeRepository = youtubeRepository,
                    trendingTracks = _trendingTracks.value,
                    topCharts = _topCharts.value
                )
                withContext(Dispatchers.Main) {
                    _personalizedFeeds.value = feeds
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed computing personalized feeds: ${e.message}")
            }
        }
    }

    private var currentTrackStartTimeMs: Long = 0L
    private var currentTrackAudioUrl: String? = null

    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private val mediaController: MediaController?
        get() = if (mediaControllerFuture?.isDone == true) try { mediaControllerFuture?.get() } catch (_: Exception) { null } else null

    private var exoPlayerRetryCount = 0
    private val MAX_RETRIES = 3

    private val youtubeRepository = YouTubeRepository()

    private val downloadHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    init {
        StreamExtractionManager.setContext(getApplication())
        _trendingTracks.value = emptyList()
        _topCharts.value = emptyList()
        _youtubeMusic.value = emptyList()
        initializeController()
        fetchTrendingMusic()
        loadTopCharts()
        loadYouTubeMusic()
        startProgressUpdater()
        loadLocalTracks()
        refreshPersonalizedFeeds()
        loadGlobalWeeklyTopCharts()

        viewModelScope.launch {
            searchDebounceFlow
                .debounce(350L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isNotBlank() && !_isSearchSubmitted.value) {
                        val suggestions = com.gaminghub.musicplayer.util.SearchSuggestionEngine.getSuggestions(query)
                        _searchSuggestions.value = suggestions
                        val settingsPrefs = getApplication<Application>().getSharedPreferences("Musify_settings", Context.MODE_PRIVATE)
                        val liveSearch = settingsPrefs.getBoolean("live_search", true)
                        if (liveSearch && query.trim().length >= 3) {
                            searchMusic(query.trim())
                        }
                    } else if (query.isBlank()) {
                        _searchSuggestions.value = emptyList()
                    }
                }
        }

        setupNetworkSync()
    }

    private fun setupNetworkSync() {
        val networkMonitor = com.gaminghub.musicplayer.util.NetworkMonitor.getInstance(getApplication())
        val authManager = com.gaminghub.musicplayer.auth.AuthManager.getInstance(getApplication())
        networkMonitor.addOnConnectionRestoredListener {
            val syncUserId = authManager.getSyncUserId()
            Log.d(tag, "Network connection restored: full bidirectional cloud sync for user $syncUserId")
            com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.syncAll(getApplication(), syncUserId)
            loadGlobalWeeklyTopCharts()
        }
        if (networkMonitor.isOnline.value) {
            viewModelScope.launch(Dispatchers.IO) {
                val syncUserId = authManager.getSyncUserId()
                Log.d(tag, "App startup online: full bidirectional cloud sync for user $syncUserId")
                com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.syncAll(getApplication(), syncUserId)
                loadGlobalWeeklyTopCharts()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            authManager.googleEmail.collect { email ->
                if (!email.isNullOrBlank()) {
                    val syncUserId = authManager.getSyncUserId()
                    Log.d(tag, "Active user account detected ($email): syncing cloud to local")
                    com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.syncAll(getApplication(), syncUserId)
                    refreshPersonalizedFeeds()
                }
            }
        }

        val playbackFilter = IntentFilter().apply {
            addAction("com.gaminghub.musify.WIDGET_PLAY_PAUSE")
            addAction("com.gaminghub.musify.WIDGET_NEXT")
            addAction("com.gaminghub.musify.WIDGET_PREV")
        }
        ContextCompat.registerReceiver(
            getApplication(),
            playbackReceiver,
            playbackFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private val playbackReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.gaminghub.musify.WIDGET_PLAY_PAUSE" -> togglePlayPause()
                "com.gaminghub.musify.WIDGET_NEXT" -> playNext(fromUser = true)
                "com.gaminghub.musify.WIDGET_PREV" -> playPrevious()
            }
        }
    }

    private fun initializeController() {
        val sessionToken = SessionToken(getApplication(), ComponentName(getApplication(), MusicPlaybackService::class.java))
        mediaControllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        mediaControllerFuture?.addListener({
            val controller = mediaController ?: return@addListener
            val settingsPrefs = getApplication<Application>().getSharedPreferences("Musify_settings", Context.MODE_PRIVATE)
            val enforceRepeating = settingsPrefs.getBoolean("enforce_repeating", false)
            if (enforceRepeating) {
                val savedRepeat = settingsPrefs.getInt("enforced_repeat_mode", Player.REPEAT_MODE_OFF)
                controller.repeatMode = savedRepeat
                _repeatMode.value = savedRepeat
            } else {
                _repeatMode.value = controller.repeatMode
            }
            _isShuffled.value = controller.shuffleModeEnabled
            if (_isSpeedLocked.value) {
                controller.playbackParameters = androidx.media3.common.PlaybackParameters(_playbackSpeed.value)
            } else {
                _playbackSpeed.value = controller.playbackParameters.speed
            }
            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                    if (!_isSpeedLocked.value) {
                        _playbackSpeed.value = playbackParameters.speed
                    }
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    _repeatMode.value = repeatMode
                    val sPrefs = getApplication<Application>().getSharedPreferences("Musify_settings", Context.MODE_PRIVATE)
                    if (sPrefs.getBoolean("enforce_repeating", false)) {
                        sPrefs.edit().putInt("enforced_repeat_mode", repeatMode).apply()
                    }
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    _isShuffled.value = shuffleModeEnabled
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val url = mediaItem?.mediaMetadata?.extras?.getString("original_url") ?: mediaItem?.mediaId
                    val elapsed = System.currentTimeMillis() - currentTrackStartTimeMs
                    if (currentTrackAudioUrl != null && elapsed > 0) {
                        val prevUrl = currentTrackAudioUrl!!
                        if (elapsed < 30_000L && reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                            viewModelScope.launch(Dispatchers.IO) { dao.incrementSkipCount(prevUrl) }
                        } else if (elapsed >= 45_000L) {
                            viewModelScope.launch(Dispatchers.IO) { dao.incrementCompletionCount(prevUrl) }
                        }
                    }
                    currentTrackStartTimeMs = System.currentTimeMillis()
                    currentTrackAudioUrl = url

                    val resolvedTrack = _currentQueue.value.find { it.audioUrl == url } ?: 
                                         _trendingTracks.value.find { it.audioUrl == url } ?:
                                         _searchTracks.value.find { it.audioUrl == url } ?:
                                         _topCharts.value.find { it.audioUrl == url } ?:
                                         _youtubeMusic.value.find { it.audioUrl == url } ?:
                                         _localTracks.value.find { it.audioUrl == url } ?:
                                         if (_currentTrack.value?.audioUrl == url) _currentTrack.value else null

                    if (resolvedTrack != null) {
                        _currentTrack.value = resolvedTrack
                    } else if (mediaItem != null && url != null) {
                        val meta = mediaItem.mediaMetadata
                        _currentTrack.value = TrackModel(
                            title = meta.title?.toString() ?: "Unknown Song",
                            artist = meta.artist?.toString() ?: "Unknown Artist",
                            audioUrl = url,
                            albumArtUrl = meta.artworkUri?.toString()
                        )
                    }

                    _currentTrack.value?.let { current ->
                        fetchLyrics(current)
                        viewModelScope.launch(Dispatchers.IO) {
                            dao.recordHistory(current.audioUrl ?: "", current.toEntity())
                            val authManager = com.gaminghub.musicplayer.auth.AuthManager.getInstance(getApplication())
                            val syncUserId = authManager.getSyncUserId()
                            com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.recordPlayEventRealtime(
                                context = getApplication(),
                                userId = syncUserId,
                                track = current
                            )
                            loadGlobalWeeklyTopCharts()
                        }
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _isLoading.value = (playbackState == Player.STATE_BUFFERING)
                    if (playbackState == Player.STATE_READY) {
                        _duration.value = controller.duration
                        exoPlayerRetryCount = 0
                    }
                    if (playbackState == Player.STATE_ENDED) {
                        currentTrackAudioUrl?.let { url ->
                            viewModelScope.launch(Dispatchers.IO) { dao.incrementCompletionCount(url) }
                        }
                        playNext()
                    }
                }
                
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e(tag, "Player Error: ${error.message} (Code: ${error.errorCode})")
                    _isLoading.value = false
                    val isRetryable = error.errorCode in 2000..2999 || 
                                      error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                                      error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                                      error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED
                    if (isRetryable && exoPlayerRetryCount < MAX_RETRIES) {
                        exoPlayerRetryCount++
                        Log.w(tag, "Retrying playback ($exoPlayerRetryCount/$MAX_RETRIES) with fresh stream extraction.")
                        _currentTrack.value?.let { playTrack(it, _currentQueue.value, isRetry = true) }
                    } else {
                        Log.e(tag, "Max retries reached or unrecoverable error for '${_currentTrack.value?.title}', advancing to next song.")
                        exoPlayerRetryCount = 0
                        playNext()
                    }
                }
            })
            if (settingsPrefs.getBoolean("load_last_session", true)) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        if (controller.currentMediaItem == null && _currentTrack.value == null) {
                            val history = dao.getRecentHistory().first()
                            if (history.isNotEmpty()) {
                                val lastTrack = history.first().toModel()
                                val models = history.take(25).map { it.toModel() }
                                withContext(Dispatchers.Main) {
                                    if (_currentTrack.value == null) {
                                        _currentTrack.value = lastTrack
                                        _currentQueue.value = models
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(tag, "Failed to restore last session: ${e.message}")
                    }
                }
            }
        }, MoreExecutors.directExecutor())
    }

    private fun startProgressUpdater() {
        viewModelScope.launch {
            while (true) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        val pos = controller.currentPosition
                        _currentPosition.value = pos
                        val lines = _syncedLyrics.value
                        if (lines.isNotEmpty()) {
                            val idx = lines.indexOfLast { it.timeMs <= pos }
                            if (idx != _currentLyricIndex.value) {
                                _currentLyricIndex.value = idx
                            }
                        }
                    }
                }
                delay(250L)
            }
        }
    }

    fun fetchTrendingMusic() {
        val sPrefs = getApplication<Application>().getSharedPreferences("Musify_settings", Context.MODE_PRIVATE)
        val musicLang = sPrefs.getString("music_language", "All / Global") ?: "All / Global"
        val query = if (musicLang == "All / Global") {
            "Global Top Hits Official Music"
        } else {
            "$musicLang Top Hits Official Music"
        }
        fetchMusic(query, _trendingTracks)
    }

    fun setSelectedChartCountry(country: String) {
        _selectedChartCountry.value = country
        loadTopCharts(isGlobal = false)
    }

    fun loadTopCharts(isGlobal: Boolean = true) {
        val sPrefs = getApplication<Application>().getSharedPreferences("Musify_settings", Context.MODE_PRIVATE)
        val savedCountry = sPrefs.getString("spotify_charts_location", null)
        val country = savedCountry ?: _selectedChartCountry.value
        val query = if (isGlobal && savedCountry == null) {
            "Top 50 Global Hits official music songs"
        } else {
            "Top 50 Songs $country official music"
        }
        fetchMusic(query, _topCharts)
    }

    fun selectYouTubeCategory(category: String) {
        _selectedYouTubeCategory.value = category
        loadYouTubeMusic(category = category)
    }

    fun loadYouTubeMusic(category: String? = null, country: String? = null) {
        val cat = category ?: _selectedYouTubeCategory.value
        val cnt = country ?: _selectedChartCountry.value
        val query = when (cat) {
            "All" -> "YouTube Music Top 100 Songs $cnt official"
            "Trending" -> "YouTube Music Trending 20 $cnt official"
            "Top 100" -> "Top 100 Songs $cnt official music"
            "Music Videos" -> "Top 100 Official Music Videos $cnt"
            "Pop" -> "YouTube Music Pop Hits official"
            "Rock" -> "YouTube Music Rock Hits official"
            "Hip-Hop" -> "YouTube Music Hip Hop Rap official"
            "Bollywood" -> "Bollywood Top 50 Songs official"
            "Punjabi" -> "Punjabi Top 50 Songs official"
            "EDM" -> "EDM Dance Hits official"
            "Lo-Fi" -> "Lo-Fi Chill Beats official"
            else -> "$cat official music songs"
        }
        fetchMusic(query, _youtubeMusic)
    }

    suspend fun restoreMusifyDownloads() = withContext(Dispatchers.IO) {
        try {
            val candidateDirs = mutableListOf<java.io.File>()

            val publicMusic = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)
            if (publicMusic != null && publicMusic.exists()) {
                candidateDirs.add(java.io.File(publicMusic, "Musify"))
                candidateDirs.add(publicMusic)
            }
            val publicDownload = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (publicDownload != null && publicDownload.exists()) {
                candidateDirs.add(java.io.File(publicDownload, "Musify"))
                candidateDirs.add(publicDownload)
            }
            val extAppMusic = getApplication<Application>().getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)
            if (extAppMusic != null && extAppMusic.exists()) {
                candidateDirs.add(java.io.File(extAppMusic, "Musify"))
                candidateDirs.add(extAppMusic)
            }

            var restoredCount = 0
            val scannedPaths = mutableSetOf<String>()
            val allDbTracks = dao.getAllTracksSync()

            for (dir in candidateDirs) {
                if (!dir.exists() || !dir.isDirectory) continue
                val files = dir.listFiles() ?: continue
                for (file in files) {
                    if (!file.isFile || !scannedPaths.add(file.absolutePath)) continue
                    val nameLower = file.name.lowercase()
                    val isMusifyCandidate = nameLower.contains(".musify.") ||
                            nameLower.endsWith(".musify") ||
                            nameLower.endsWith(".m4a") ||
                            nameLower.endsWith(".mp3")
                    if (!isMusifyCandidate) continue

                    val meta = MusifyFileMetadataHelper.readMetadata(file)
                    if (meta != null && meta.audioUrl.isNotBlank()) {
                        val existing = dao.getTrackByUrl(meta.audioUrl)
                        var resolvedArt = meta.albumArtUrl
                        if (!meta.coverArtBase64.isNullOrBlank()) {
                            val localArtUri = MusifyFileMetadataHelper.saveExtractedCoverArt(
                                getApplication(), meta.audioUrl, meta.coverArtBase64
                            )
                            if (localArtUri != null) {
                                resolvedArt = localArtUri
                            }
                        }

                        if (existing == null) {
                            val newEntity = meta.toTrackEntity(
                                localPath = file.absolutePath,
                                resolvedArtUrl = resolvedArt
                            )
                            dao.insertTrack(newEntity)
                            restoredCount++
                            Log.d(tag, "Restored offline track from storage: ${meta.title} (${file.name})")
                        } else if (existing.localPath != file.absolutePath) {
                            dao.updateLocalPath(meta.audioUrl, file.absolutePath)
                            if (existing.albumArtUrl.isNullOrBlank() && resolvedArt != null) {
                                dao.insertTrack(existing.copy(localPath = file.absolutePath, albumArtUrl = resolvedArt))
                            }
                            restoredCount++
                            Log.d(tag, "Updated localPath for track: ${meta.title} -> ${file.absolutePath}")
                        }
                    } else {
                        // Legacy download or file without trailer: reconcile with DB tracks
                        val cleanTitle = MusifyFileMetadataHelper.cleanDisplayTitle(file.name)
                        val match = allDbTracks.firstOrNull { db ->
                            db.localPath == file.absolutePath ||
                            db.title.equals(cleanTitle, ignoreCase = true) ||
                            (cleanTitle.length >= 4 && db.title.contains(cleanTitle, ignoreCase = true)) ||
                            (cleanTitle.length >= 4 && cleanTitle.contains(db.title, ignoreCase = true))
                        }

                        if (match != null) {
                            dao.updateLocalPath(match.audioUrl, file.absolutePath)
                            restoredCount++
                            Log.d(tag, "Reconciled legacy download with DB track: ${file.name} -> ${match.title} (${match.artist})")
                            // Upgrade file with trailer metadata so future reads are instantaneous
                            try {
                                val upgradedMeta = MusifyTrackMetadata(
                                    title = match.title,
                                    artist = match.artist,
                                    audioUrl = match.audioUrl,
                                    albumArtUrl = match.albumArtUrl,
                                    album = match.album,
                                    genre = match.genre,
                                    isDevpick = match.isDevpick,
                                    playCount = match.playCount
                                )
                                MusifyFileMetadataHelper.appendMetadata(file, upgradedMeta)
                            } catch (_: Exception) {}
                        } else if (dir.name.equals("Musify", ignoreCase = true) || file.name.contains(".musify")) {
                            // Offline file in Musify directory without existing DB match
                            val (id3Title, id3Artist) = MusifyFileMetadataHelper.extractId3Metadata(file)
                            val finalTitle = id3Title ?: cleanTitle
                            val finalArtist = id3Artist ?: "Offline Audio"
                            dao.insertTrack(
                                TrackEntity(
                                    audioUrl = file.absolutePath,
                                    title = finalTitle,
                                    artist = finalArtist,
                                    localPath = file.absolutePath
                                )
                            )
                            restoredCount++
                        }
                    }
                }
            }
            if (restoredCount > 0) {
                Log.d(tag, "restoreMusifyDownloads finished. Restored/updated $restoredCount tracks.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error during restoreMusifyDownloads: ${e.message}", e)
        }
    }

    fun loadLocalTracks() {
        viewModelScope.launch(Dispatchers.IO) {
            restoreMusifyDownloads()
            val allDbTracks = dao.getAllTracksSync()
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
                    val durationCol = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
                    val sPrefs = getApplication<Application>().getSharedPreferences("Musify_settings", Context.MODE_PRIVATE)
                    val minAudioLengthSec = sPrefs.getInt("min_audio_length_sec", 30)
                    val excludedFolders = sPrefs.getStringSet("excluded_folders", emptySet()) ?: emptySet()

                    while (c.moveToNext()) {
                        val duration = if (durationCol >= 0) c.getLong(durationCol) else 0L
                        if (duration > 0 && duration < minAudioLengthSec * 1000L) {
                            continue
                        }
                        val path = c.getString(dataCol) ?: continue
                        if (excludedFolders.isNotEmpty() && excludedFolders.any { excluded -> path.contains(excluded, ignoreCase = true) }) {
                            continue
                        }
                        val id = c.getLong(idCol)
                        var title = c.getString(titleCol) ?: "Unknown"
                        var artist = c.getString(artistCol) ?: "Unknown Artist"
                        var album = c.getString(albumCol)
                        var genre = if (genreCol >= 0) c.getString(genreCol) else null
                        val albumId = c.getLong(albumIdCol)
                        var artUri = "content://media/external/audio/albumart/$albumId".toUri().toString()

                        val file = java.io.File(path)
                        val contentUri = android.content.ContentUris.withAppendedId(uri, id)
                        val meta = if (file.exists()) {
                            MusifyFileMetadataHelper.readMetadata(file)
                        } else {
                            MusifyFileMetadataHelper.readMetadata(getApplication(), contentUri)
                        }

                        var effectiveAudioUrl = path
                        var isDevpick = false
                        var playcount = 0

                        if (meta != null && meta.audioUrl.isNotBlank()) {
                            title = meta.title
                            artist = meta.artist
                            album = meta.album ?: album
                            genre = meta.genre ?: genre
                            isDevpick = meta.isDevpick
                            playcount = meta.playCount
                            effectiveAudioUrl = meta.audioUrl

                            var resolvedArt = meta.albumArtUrl
                            if (!meta.coverArtBase64.isNullOrBlank()) {
                                val localArtUri = MusifyFileMetadataHelper.saveExtractedCoverArt(
                                    getApplication(), meta.audioUrl, meta.coverArtBase64
                                )
                                if (localArtUri != null) resolvedArt = localArtUri
                            }
                            if (resolvedArt != null) {
                                artUri = resolvedArt
                            }

                            // Restore into Room database so Downloads screen has it immediately
                            val existing = dao.getTrackByUrl(meta.audioUrl)
                            if (existing == null) {
                                dao.insertTrack(
                                    meta.toTrackEntity(
                                        localPath = path,
                                        resolvedArtUrl = resolvedArt
                                    )
                                )
                                Log.d(tag, "Restored track from MediaStore scan: ${meta.title} ($path)")
                            } else if (existing.localPath != path) {
                                dao.updateLocalPath(meta.audioUrl, path)
                            }
                        } else {
                            // Legacy or downloaded file without trailer
                            val cleanedTitle = MusifyFileMetadataHelper.cleanDisplayTitle(title)
                            val matchingDbTrack = allDbTracks.firstOrNull { db ->
                                db.localPath == path ||
                                db.title.equals(cleanedTitle, ignoreCase = true) ||
                                (cleanedTitle.length >= 4 && db.title.contains(cleanedTitle, ignoreCase = true)) ||
                                (cleanedTitle.length >= 4 && cleanedTitle.contains(db.title, ignoreCase = true))
                            }

                            if (matchingDbTrack != null) {
                                title = matchingDbTrack.title
                                if (matchingDbTrack.artist.isNotBlank() && matchingDbTrack.artist != "<unknown>") {
                                    artist = matchingDbTrack.artist
                                }
                                album = matchingDbTrack.album ?: album
                                genre = matchingDbTrack.genre ?: genre
                                isDevpick = matchingDbTrack.isDevpick
                                playcount = matchingDbTrack.playCount
                                effectiveAudioUrl = matchingDbTrack.audioUrl
                                if (!matchingDbTrack.albumArtUrl.isNullOrBlank()) {
                                    artUri = matchingDbTrack.albumArtUrl
                                }
                                if (matchingDbTrack.localPath != path) {
                                    dao.updateLocalPath(matchingDbTrack.audioUrl, path)
                                }
                            } else {
                                title = cleanedTitle
                                if (file.exists() && (artist == "<unknown>" || artist.isBlank())) {
                                    val (id3Title, id3Artist) = MusifyFileMetadataHelper.extractId3Metadata(file)
                                    if (!id3Artist.isNullOrBlank()) artist = id3Artist
                                    if (!id3Title.isNullOrBlank() && (title == "Unknown" || title.isBlank())) title = id3Title
                                }
                                if (artist == "<unknown>") {
                                    artist = "Offline Audio"
                                }
                                if (path.contains("/Musify/") || file.name.contains(".musify")) {
                                    dao.insertTrack(
                                        TrackEntity(
                                            audioUrl = path,
                                            title = title,
                                            artist = artist,
                                            albumArtUrl = artUri,
                                            localPath = path,
                                            album = album,
                                            genre = genre
                                        )
                                    )
                                }
                            }
                        }

                        tracks.add(
                            TrackModel(
                                title = title,
                                artist = artist,
                                audioUrl = effectiveAudioUrl,
                                albumArtUrl = artUri,
                                album = album,
                                genre = genre,
                                isDevpick = isDevpick,
                                playcount = playcount,
                                isLocal = true
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading local tracks: ${e.message}", e)
            } finally {
                cursor?.close()
            }

            // Two-Way Sync with Room Database Records
            val downloadedEntities = dao.getDownloadedTracksSync()
            val downloadedMap = downloadedEntities.associateBy { it.localPath ?: it.audioUrl }

            val syncedTracks = tracks.map { track ->
                val path = track.audioUrl ?: ""
                val matchedEntity = downloadedMap[path]
                    ?: downloadedEntities.find { 
                        com.gaminghub.musicplayer.util.SmartRecommendationEngine.normalizeTitle(it.title) == 
                        com.gaminghub.musicplayer.util.SmartRecommendationEngine.normalizeTitle(track.title)
                    }

                if (matchedEntity != null) {
                    track.copy(
                        albumArtUrl = matchedEntity.albumArtUrl ?: track.albumArtUrl,
                        isDevpick = matchedEntity.isDevpick,
                        genre = matchedEntity.genre ?: track.genre,
                        album = matchedEntity.album ?: track.album,
                        playcount = matchedEntity.playCount
                    )
                } else {
                    track
                }
            }

            withContext(Dispatchers.Main) {
                _localTracks.value = syncedTracks
                Log.d(tag, "Loaded and synced ${syncedTracks.size} local tracks from device")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _isSearchSubmitted.value = false
        if (query.isBlank()) {
            _searchSuggestions.value = emptyList()
            _searchTracks.value = emptyList()
        } else {
            searchDebounceFlow.value = query.trim()
        }
    }

    fun submitSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        _searchQuery.value = trimmed
        _isSearchSubmitted.value = true
        _searchSuggestions.value = emptyList()
        searchMusic(trimmed)
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        val query = when (category) {
            "All" -> "Global Top Hits Official Music"
            "Trending" -> "Trending Official Songs 2026"
            "Pop" -> "Pop Hits Official Music"
            "Hip-Hop" -> "Hip Hop Hits Official Songs"
            "Rock" -> "Rock Hits Official Tracks"
            "Bollywood" -> "Bollywood Top Songs Official"
            "Punjabi" -> "Punjabi Top Songs Official"
            "EDM" -> "EDM Hits Official Music"
            "Lo-Fi" -> "Lofi Beats Chill Music"
            else -> "$category Official Songs"
        }
        fetchMusic(query, _trendingTracks)
    }

    fun searchMusic(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        _isSearchSubmitted.value = true
        _searchSuggestions.value = emptyList()

        val cacheKey = trimmed.lowercase()
        val cached = searchCache.get(cacheKey)
        if (!cached.isNullOrEmpty()) {
            _searchTracks.value = cached
            _isLoading.value = false
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                val results = youtubeRepository.fetchMusic(trimmed, SearchSource.YT_MUSIC)
                val localMatches = dao.searchTracks(trimmed).map { it.toModel() }
                val devPicks = dao.getDevPickTracksSync()
                val devPickUrls = devPicks.map { it.audioUrl }.toSet()

                // Merge and update isDevpick flag
                val combined = (localMatches + results).distinctBy { it.audioUrl ?: it.title }
                    .map { track ->
                        val isDev = track.isDevpick || (track.audioUrl != null && devPickUrls.contains(track.audioUrl))
                        if (isDev != track.isDevpick) track.copy(isDevpick = isDev) else track
                    }

                // DevPick Deduplication & Uniqueness:
                // If a DevPick track exists, remove other non-DevPick duplicate versions of that same song
                val devPickCanonicalTitles = combined
                    .filter { it.isDevpick }
                    .map { com.gaminghub.musicplayer.util.SmartRecommendationEngine.normalizeTitle(it.title) }
                    .filter { it.length > 2 }
                    .toSet()

                val uniqueTracks = combined.filter { track ->
                    if (track.isDevpick) {
                        true
                    } else {
                        val canonical = com.gaminghub.musicplayer.util.SmartRecommendationEngine.normalizeTitle(track.title)
                        !devPickCanonicalTitles.contains(canonical)
                    }
                }

                // Sort: DevPicked songs that match query come FIRST
                val sorted = uniqueTracks.sortedWith(
                    compareByDescending<TrackModel> { it.isDevpick }
                        .thenByDescending {
                            if (it.title.contains(trimmed, ignoreCase = true)) 2 else if (it.artist.contains(trimmed, ignoreCase = true)) 1 else 0
                        }
                )

                searchCache.put(cacheKey, sorted)

                withContext(Dispatchers.Main) {
                    _searchTracks.value = sorted
                }
            } catch (e: Exception) {
                Log.e(tag, "Search error: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchTrack(query: String) {
        submitSearch(query)
    }

    fun toggleDevPick(track: TrackModel, isAdmin: Boolean) {
        if (!isAdmin) {
            android.widget.Toast.makeText(getApplication(), "Admin access required to toggle Dev's Pick", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val url = track.audioUrl ?: return
        val newDevPickState = !track.isDevpick

        viewModelScope.launch(Dispatchers.IO) {
            dao.insertTrack(track.toEntity().copy(isDevpick = newDevPickState))
            dao.updateDevPick(url, newDevPickState)

            // Cloud Firestore sync for DevPicks with videoId / songId
            com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.updateDevPick(
                track = track.copy(isDevpick = newDevPickState),
                isDevpick = newDevPickState
            )

            withContext(Dispatchers.Main) {
                val updater: (TrackModel) -> TrackModel = { t ->
                    if (t.audioUrl == url) t.copy(isDevpick = newDevPickState) else t
                }
                _trendingTracks.update { it.map(updater) }
                _searchTracks.update { it.map(updater) }
                _topCharts.update { it.map(updater) }
                _youtubeMusic.update { it.map(updater) }
                _currentQueue.update { it.map(updater) }
                _localTracks.update { it.map(updater) }
                if (_currentTrack.value?.audioUrl == url) {
                    _currentTrack.value = _currentTrack.value?.copy(isDevpick = newDevPickState)
                }

                val msg = if (newDevPickState) "★ Marked '${track.title}' as Dev's Pick!" else "Removed '${track.title}' from Dev's Picks"
                android.widget.Toast.makeText(getApplication(), msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchMusic(term: String, stateFlow: MutableStateFlow<List<TrackModel>>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(tag, "Fetching music for: $term")
                _isLoading.value = true
                val results = youtubeRepository.fetchMusic(term, SearchSource.YT_MUSIC)
                
                val mappedTracks = results.map { item ->
                    TrackModel(
                        title = item.title,
                        artist = item.artist,
                        audioUrl = item.audioUrl,
                        albumArtUrl = item.albumArtUrl,
                        album = item.album,
                        genre = item.genre,
                        isDevpick = item.isDevpick,
                        uploaderChannel = item.uploaderChannel
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

    suspend fun fetchTracksForQuery(query: String): List<TrackModel> = withContext(Dispatchers.IO) {
        try {
            val results = youtubeRepository.fetchMusic(query, SearchSource.YT_MUSIC)
            results.map { item ->
                TrackModel(
                    title = item.title,
                    artist = item.artist,
                    audioUrl = item.audioUrl,
                    albumArtUrl = item.albumArtUrl,
                    album = item.album,
                    genre = item.genre,
                    isDevpick = item.isDevpick,
                    uploaderChannel = item.uploaderChannel
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching tracks for query: $query", e)
            emptyList()
        }
    }

    private fun getFallbackTracks(): List<TrackModel> = emptyList()

    fun playTrack(track: TrackModel, queue: List<TrackModel> = emptyList(), isRetry: Boolean = false) {
        if (!isRetry) {
            exoPlayerRetryCount = 0
        }
        _currentTrack.value = track
        currentTrackStartTimeMs = System.currentTimeMillis()
        currentTrackAudioUrl = track.audioUrl
        val activeQueue = when {
            queue.isNotEmpty() -> queue
            _currentQueue.value.isNotEmpty() -> _currentQueue.value
            else -> listOf(track)
        }
        _currentQueue.value = activeQueue
        _isLoading.value = true

        // Proactively maintain queue depth so playback never runs out of upcoming songs
        val currentIdx = activeQueue.indexOfFirst { it.audioUrl == track.audioUrl }
        if (currentIdx >= 0) {
            val nextTrack = activeQueue.getOrNull(currentIdx + 1)
            val nextTrack2 = activeQueue.getOrNull(currentIdx + 2)
            nextTrack?.audioUrl?.let { if (!isTrackDownloaded(it)) StreamExtractionManager.preExtract(it) }
            nextTrack2?.audioUrl?.let { if (!isTrackDownloaded(it)) StreamExtractionManager.preExtract(it) }

            if (activeQueue.size - (currentIdx + 1) <= 4) {
                populateSmartNextQueue(track)
            }
        } else if (activeQueue.size <= 4) {
            populateSmartNextQueue(track)
        }

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
                
                // Check if track is a local device file or already downloaded offline
                val existingEntity = dao.getTrackByUrl(url)
                val localFile = if (existingEntity?.localPath != null) java.io.File(existingEntity.localPath) else null
                val isLocalValid = localFile != null && localFile.exists() && localFile.length() > 0
                val isDirectLocalPath = url.startsWith("/") || url.startsWith("content://") || url.startsWith("file://")

                val settingsPrefs = getApplication<Application>().getSharedPreferences("Musify_settings", Context.MODE_PRIVATE)
                val streamDownloaded = settingsPrefs.getBoolean("stream_downloaded", true)

                val playUri: Uri? = when {
                    isDirectLocalPath -> url.toUri()
                    isLocalValid && streamDownloaded -> {
                        Log.d(tag, "Playing track offline from local storage: ${localFile?.absolutePath}")
                        Uri.fromFile(localFile)
                    }
                    else -> {
                        // Online stream extraction
                        val playUrl = StreamExtractionManager.extractPlayableUrl(url, fastStart = true, forceRefresh = isRetry)
                        if (playUrl != null && !playUrl.contains("youtube.com/watch") && !playUrl.contains("youtu.be/")) {
                            playUrl.toUri()
                        } else null
                    }
                }

                if (playUri != null) {
                    withContext(Dispatchers.Main) {
                        val mediaItem = MediaItem.Builder()
                            .setUri(playUri)
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
                        controller.setPlaybackParameters(
                            androidx.media3.common.PlaybackParameters(_playbackSpeed.value)
                        )
                        controller.prepare()
                        controller.play()
                        _isPlaying.value = true
                        _isLoading.value = false
                        Log.d(tag, "Playback started successfully for '${track.title}'")

                        // Pre-extract next track now that current playback has started
                        val playingIdx = _currentQueue.value.indexOfFirst { it.audioUrl == track.audioUrl }
                        if (playingIdx >= 0) {
                            _currentQueue.value.getOrNull(playingIdx + 1)?.audioUrl?.let { nextUrl ->
                                if (!isTrackDownloaded(nextUrl)) StreamExtractionManager.preExtract(nextUrl)
                            }
                        }
                    }
                } else {
                    Log.e(tag, "Failed to resolve playable audio source for '${track.title}' ($url)")
                    withContext(Dispatchers.Main) {
                        _isLoading.value = false
                        // Automatically advance to the next song instead of halting playback
                        Log.w(tag, "Advancing to next track due to extraction failure for '${track.title}'")
                        playNext()
                    }
                }
            }
        }
    }

    private val _activeDownloads = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, DownloadProgress>> = _activeDownloads.asStateFlow()
    private val downloadJobs = mutableMapOf<String, Job>()

    fun isTrackDownloaded(audioUrl: String?): Boolean {
        if (audioUrl == null) return false
        if (audioUrl.startsWith("/") || audioUrl.startsWith("file:") || audioUrl.startsWith("content:")) return true
        return downloadedUrls.value.contains(audioUrl)
    }

    fun isTrackDownloaded(track: TrackModel?): Boolean {
        if (track == null) return false
        if (track.isLocal) return true
        val url = track.audioUrl ?: return false
        if (url.startsWith("/") || url.startsWith("file:") || url.startsWith("content:")) return true
        if (downloadedUrls.value.contains(url)) return true
        return false
    }

    fun isTrackDownloading(audioUrl: String?): Boolean {
        if (audioUrl == null) return false
        return _activeDownloads.value.containsKey(audioUrl) || _downloadingUrls.value.contains(audioUrl)
    }

    fun toggleDownload(track: TrackModel) {
        val url = track.audioUrl ?: return
        if (isTrackDownloaded(track)) {
            deleteDownload(track)
        } else if (_activeDownloads.value.containsKey(url)) {
            cancelDownload(url)
        } else {
            downloadTrack(track)
        }
    }

    fun cancelDownload(audioUrl: String) {
        downloadJobs[audioUrl]?.cancel()
        downloadJobs.remove(audioUrl)
        _activeDownloads.update { it - audioUrl }
        _downloadingUrls.update { it - audioUrl }
        android.widget.Toast.makeText(getApplication(), "Download cancelled", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun downloadTrack(track: TrackModel) {
        val url = track.audioUrl ?: return
        if (_activeDownloads.value.containsKey(url)) return

        val job = viewModelScope.launch {
            _downloadingUrls.update { it + url }
            _activeDownloads.update { current ->
                current + (url to DownloadProgress(
                    audioUrl = url,
                    track = track,
                    isIndeterminate = true
                ))
            }
            android.widget.Toast.makeText(getApplication(), "Starting download: ${track.title}", android.widget.Toast.LENGTH_SHORT).show()

            withContext(Dispatchers.IO) {
                var tempFile: java.io.File? = null
                try {
                    val playUrl = StreamExtractionManager.extractPlayableUrl(url, fastStart = true)
                    if (playUrl.isNullOrBlank() || playUrl.contains("youtube.com/watch")) {
                        throw Exception("Could not resolve audio stream for download")
                    }

                    val cleanTitle = track.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(50)
                    val baseExt = if (playUrl.contains(".mp4") || playUrl.contains("m4a") || playUrl.contains("mime=audio%2Fmp4")) "m4a" else "mp3"
                    val ext = ".$baseExt"
                    
                    val publicMusic = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)
                    val publicDownload = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    
                    val downloadDir = try {
                        if (publicMusic != null && (publicMusic.exists() || publicMusic.mkdirs())) {
                            java.io.File(publicMusic, "Musify").apply { if (!exists()) mkdirs() }
                        } else if (publicDownload != null && (publicDownload.exists() || publicDownload.mkdirs())) {
                            java.io.File(publicDownload, "Musify").apply { if (!exists()) mkdirs() }
                        } else {
                            java.io.File(
                                getApplication<Application>().getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC) ?: getApplication<Application>().filesDir,
                                "Musify"
                            ).apply { if (!exists()) mkdirs() }
                        }
                    } catch (_: Exception) {
                        java.io.File(
                            getApplication<Application>().getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC) ?: getApplication<Application>().filesDir,
                            "Musify"
                        ).apply { if (!exists()) mkdirs() }
                    }

                    val baseFile = java.io.File(downloadDir, "$cleanTitle.musify$ext")
                    val destFile = if (baseFile.exists()) {
                        val existingMeta = MusifyFileMetadataHelper.readMetadata(baseFile)
                        if (existingMeta != null && existingMeta.audioUrl == url) {
                            baseFile
                        } else {
                            val suffix = url.hashCode().toString().replace("-", "n").takeLast(6)
                            java.io.File(downloadDir, "${cleanTitle}_$suffix.musify$ext")
                        }
                    } else {
                        baseFile
                    }
                    tempFile = destFile

                    val request = Request.Builder()
                        .url(playUrl)
                        .header("User-Agent", CommonUtils.CURRENT_USER_AGENT)
                        .header("Accept-Encoding", "identity")
                        .build()

                    val response = downloadHttpClient.newCall(request).execute()
                    if (!response.isSuccessful) {
                        throw Exception("HTTP ${response.code}: ${response.message}")
                    }

                    val body = response.body ?: throw Exception("Empty response body from stream source")
                    val contentLength = body.contentLength()

                    val buffer = ByteArray(131072) // 128 KB buffer for turbo download throughput
                    var bytesRead: Int
                    var totalDownloaded = 0L
                    var lastSampleTime = System.currentTimeMillis()
                    var bytesSinceLastSample = 0L
                    var currentSpeed = 0L

                    body.byteStream().buffered(131072).use { input ->
                        java.io.BufferedOutputStream(destFile.outputStream(), 131072).use { output ->
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                if (!isActive) {
                                    destFile.delete()
                                    throw CancellationException("Download cancelled")
                                }
                                output.write(buffer, 0, bytesRead)
                                totalDownloaded += bytesRead
                                bytesSinceLastSample += bytesRead

                                val now = System.currentTimeMillis()
                                val timeDiff = now - lastSampleTime
                                if (timeDiff >= 200) {
                                    currentSpeed = (bytesSinceLastSample * 1000L) / timeDiff.coerceAtLeast(1)
                                    lastSampleTime = now
                                    bytesSinceLastSample = 0L

                                    val percent = if (contentLength > 0) {
                                        ((totalDownloaded * 100) / contentLength).toInt().coerceIn(0, 100)
                                    } else 0

                                    _activeDownloads.update { current ->
                                        current + (url to DownloadProgress(
                                             audioUrl = url,
                                             track = track,
                                             bytesDownloaded = totalDownloaded,
                                             totalBytes = contentLength,
                                             speedBytesPerSec = currentSpeed,
                                             progressPercent = percent,
                                             isIndeterminate = contentLength <= 0
                                        ))
                                    }
                                }
                            }
                            output.flush()
                        }
                    }

                    // Fetch compressed cover art thumbnail for 100% offline self-healing display
                    val coverArtBase64 = MusifyFileMetadataHelper.fetchAndCompressImageBase64(
                        track.albumArtUrl, downloadHttpClient
                    )

                    // Append Musify EOF metadata trailer (song name, artist, genre, image, audio url)
                    val meta = MusifyTrackMetadata(
                        title = track.title,
                        artist = track.artist,
                        audioUrl = url,
                        albumArtUrl = track.albumArtUrl,
                        album = track.album,
                        genre = track.genre,
                        durationSeconds = track.durationSeconds,
                        uploaderChannel = track.uploaderChannel,
                        isDevpick = track.isDevpick,
                        playCount = track.playcount,
                        coverArtBase64 = coverArtBase64
                    )
                    MusifyFileMetadataHelper.appendMetadata(destFile, meta)

                    // Scan file with MediaScanner so it is indexed in device storage and playable by any media player
                    try {
                        android.media.MediaScannerConnection.scanFile(
                            getApplication(),
                            arrayOf(destFile.absolutePath),
                            arrayOf(if (baseExt == "m4a") "audio/mp4" else "audio/mpeg")
                        ) { path, scannedUri ->
                            Log.d(tag, "MediaScanner indexed downloaded track: $path -> $scannedUri")
                        }
                    } catch (_: Exception) {}

                    // Save in database
                    val existingEntity = dao.getTrackByUrl(url)
                    if (existingEntity == null) {
                        dao.insertTrack(track.toEntity().copy(localPath = destFile.absolutePath))
                    } else {
                        dao.updateLocalPath(url, destFile.absolutePath)
                    }

                    // Reload device local tracks
                    loadLocalTracks()

                    withContext(Dispatchers.Main) {
                        _activeDownloads.update { it - url }
                        _downloadingUrls.update { it - url }
                        android.widget.Toast.makeText(getApplication(), "Saved '${track.title}' to device Music storage!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: CancellationException) {
                    tempFile?.delete()
                    _activeDownloads.update { it - url }
                    _downloadingUrls.update { it - url }
                } catch (e: Exception) {
                    Log.e(tag, "Download failed for ${track.title}: ${e.message}", e)
                    tempFile?.delete()
                    withContext(Dispatchers.Main) {
                        _activeDownloads.update { it - url }
                        _downloadingUrls.update { it - url }
                        android.widget.Toast.makeText(getApplication(), "Download failed for '${track.title}': ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        downloadJobs[url] = job
    }

    fun deleteDownload(track: TrackModel) {
        val url = track.audioUrl ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entity = dao.getTrackByUrl(url)
                val pathToDelete = entity?.localPath ?: (if (url.startsWith("/")) url else null)
                if (pathToDelete != null) {
                    val file = java.io.File(pathToDelete)
                    if (file.exists()) {
                        file.delete()
                        try {
                            android.media.MediaScannerConnection.scanFile(
                                getApplication(),
                                arrayOf(file.absolutePath),
                                null,
                                null
                            )
                        } catch (_: Exception) {}
                    }
                }
                dao.updateLocalPath(url, null)
                loadLocalTracks()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication(), "Deleted offline download: ${track.title}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to delete download: ${e.message}", e)
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
            val cleanName = name.trim()
            if (cleanName.isBlank()) return@launch
            dao.insertPlaylist(PlaylistEntity(name = cleanName))
            val created = dao.getPlaylistsSync().find { it.name.equals(cleanName, ignoreCase = true) }
            if (created != null) {
                val authManager = com.gaminghub.musicplayer.auth.AuthManager.getInstance(getApplication())
                com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.savePlaylist(
                    userId = authManager.getSyncUserId(),
                    playlistId = created.id.toLong(),
                    name = cleanName,
                    tracks = emptyList()
                )
            }
        }
    }

    fun deletePlaylist(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deletePlaylist(id)
            val authManager = com.gaminghub.musicplayer.auth.AuthManager.getInstance(getApplication())
            com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.deletePlaylist(
                userId = authManager.getSyncUserId(),
                playlistId = id.toLong()
            )
        }
    }

    fun mergePlaylists(playlistIds: List<Int>, targetName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanName = targetName.trim()
            if (cleanName.isBlank() || playlistIds.size < 2) return@launch
            dao.insertPlaylist(PlaylistEntity(name = cleanName))
            val target = dao.getPlaylistsSync().find { it.name.equals(cleanName, ignoreCase = true) } ?: return@launch
            val allTracks = mutableListOf<TrackEntity>()
            for (id in playlistIds) {
                val plTracks = dao.getTracksForPlaylistSync(id)
                for (t in plTracks) {
                    if (allTracks.none { it.audioUrl == t.audioUrl }) {
                        allTracks.add(t)
                        dao.addTrackToPlaylist(PlaylistTrackEntity(playlistId = target.id, audioUrl = t.audioUrl))
                    }
                }
            }
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(getApplication(), "Merged ${playlistIds.size} playlists into '$cleanName' (${allTracks.size} songs)", android.widget.Toast.LENGTH_SHORT).show()
            }
            val authManager = com.gaminghub.musicplayer.auth.AuthManager.getInstance(getApplication())
            com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.savePlaylist(
                userId = authManager.getSyncUserId(),
                playlistId = target.id.toLong(),
                name = cleanName,
                tracks = allTracks.map { it.toModel() }
            )
        }
    }

    fun addTrackToPlaylist(playlistId: Int, track: TrackModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val url = track.audioUrl ?: return@launch
            dao.insertTrack(track.toEntity())
            dao.addTrackToPlaylist(PlaylistTrackEntity(playlistId = playlistId, audioUrl = url))
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(getApplication(), "Added '${track.title}' to playlist", android.widget.Toast.LENGTH_SHORT).show()
            }
            val playlist = dao.getPlaylistsSync().find { it.id == playlistId }
            if (playlist != null) {
                val tracks = dao.getTracksForPlaylistSync(playlistId).map { it.toModel() }
                val authManager = com.gaminghub.musicplayer.auth.AuthManager.getInstance(getApplication())
                com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.savePlaylist(
                    userId = authManager.getSyncUserId(),
                    playlistId = playlistId.toLong(),
                    name = playlist.name,
                    tracks = tracks
                )
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: Int, audioUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.removeTrackFromPlaylist(playlistId, audioUrl)
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(getApplication(), "Removed from playlist", android.widget.Toast.LENGTH_SHORT).show()
            }
            val playlist = dao.getPlaylistsSync().find { it.id == playlistId }
            if (playlist != null) {
                val tracks = dao.getTracksForPlaylistSync(playlistId).map { it.toModel() }
                val authManager = com.gaminghub.musicplayer.auth.AuthManager.getInstance(getApplication())
                com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.savePlaylist(
                    userId = authManager.getSyncUserId(),
                    playlistId = playlistId.toLong(),
                    name = playlist.name,
                    tracks = tracks
                )
            }
        }
    }

    fun renamePlaylist(id: Int, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanName = newName.trim()
            if (cleanName.isBlank()) return@launch
            dao.renamePlaylist(id, cleanName)
            val tracks = dao.getTracksForPlaylistSync(id).map { it.toModel() }
            val authManager = com.gaminghub.musicplayer.auth.AuthManager.getInstance(getApplication())
            com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.savePlaylist(
                userId = authManager.getSyncUserId(),
                playlistId = id.toLong(),
                name = cleanName,
                tracks = tracks
            )
        }
    }

    fun toggleFavorite(track: TrackModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val url = track.audioUrl ?: return@launch
            if (url.isBlank()) return@launch
            dao.toggleFavorite(url, track.toEntity())
            refreshPersonalizedFeeds()
            val updated = dao.getTrackByUrl(url)
            val isFav = updated?.isFavorite ?: false
            val authManager = com.gaminghub.musicplayer.auth.AuthManager.getInstance(getApplication())
            com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.updateFavorite(
                userId = authManager.getSyncUserId(),
                track = track,
                isFavorite = isFav
            )
        }
    }

    fun clearRecentHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearRecentHistory()
            val authManager = com.gaminghub.musicplayer.auth.AuthManager.getInstance(getApplication())
            com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.clearHistory(
                userId = authManager.getSyncUserId()
            )
        }
    }

    fun removeFromRecentHistory(track: TrackModel) {
        val url = track.audioUrl ?: return
        val videoId = com.gaminghub.musicplayer.util.LinkHelper.extractVideoId(url) ?: ""
        viewModelScope.launch(Dispatchers.IO) {
            dao.removeFromRecentHistory(url, videoId)
            val authManager = com.gaminghub.musicplayer.auth.AuthManager.getInstance(getApplication())
            com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.removeFromHistory(
                userId = authManager.getSyncUserId(),
                track = track
            )
        }
    }

    fun isFavorite(url: String): Flow<Boolean> = flow {
        favoriteUrls.collect { set ->
            emit(set.contains(url))
        }
    }.distinctUntilChanged()

    fun isFavoriteSync(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return favoriteUrls.value.contains(url)
    }

    fun getBaseSongLikes(track: TrackModel): Long {
        val seed = ((track.title.hashCode() xor track.artist.hashCode()).toLong() and 0x7FFFFFFF)
        return 1200L + (seed % 88000L)
    }

    fun getSongLikes(track: TrackModel): Flow<Long> = flow {
        val base = getBaseSongLikes(track)
        favoriteUrls.collect { set ->
            val isFav = track.audioUrl != null && set.contains(track.audioUrl)
            emit(if (isFav) base + 1 else base)
        }
    }.distinctUntilChanged()

    fun getSongLikesFormatted(track: TrackModel): Flow<String> = flow {
        getSongLikes(track).collect { count ->
            emit(formatCount(count))
        }
    }.distinctUntilChanged()

    fun getSongLikesFormattedSync(track: TrackModel): String {
        val base = getBaseSongLikes(track)
        val isFav = track.audioUrl != null && favoriteUrls.value.contains(track.audioUrl)
        return formatCount(if (isFav) base + 1 else base)
    }

    fun getBaseArtistFollowers(artistName: String): Long {
        val clean = artistName.trim().lowercase()
        val seed = (clean.hashCode().toLong() and 0x7FFFFFFF)
        return 50_000L + (seed % 4_950_000L)
    }

    fun formatCount(count: Long): String {
        return when {
            count >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }

    fun getArtistFollowersFormatted(artistName: String): Flow<String> {
        return dao.getFollowedArtists().map { list ->
            val isFollowed = list.any { it.name.equals(artistName.trim(), ignoreCase = true) || it.id.equals(artistName.trim(), ignoreCase = true) }
            val base = getBaseArtistFollowers(artistName)
            val total = if (isFollowed) base + 1 else base
            formatCount(total)
        }
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
    }

    private var recommendationJob: Job? = null

    fun populateSmartNextQueue(seedTrack: TrackModel? = _currentTrack.value) {
        recommendationJob?.cancel()
        recommendationJob = viewModelScope.launch(Dispatchers.IO) {
            val current = seedTrack ?: _currentTrack.value ?: return@launch
            val existingUrls = _currentQueue.value.mapNotNull { it.audioUrl }.toSet()
            val recommendations = SmartRecommendationEngine.computeNextRecommendations(
                currentTrack = current,
                trendingTracks = _trendingTracks.value,
                topCharts = _topCharts.value,
                dao = dao,
                youtubeRepository = youtubeRepository,
                existingQueueUrls = existingUrls,
                count = 10
            )
            if (recommendations.isNotEmpty()) {
                // Pre-extract streams for upcoming recommendations so playback transitions seamlessly
                recommendations.take(2).forEach { rec ->
                    rec.audioUrl?.let { url ->
                        if (!isTrackDownloaded(url)) {
                            StreamExtractionManager.preExtract(url)
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    val updatedQueue = _currentQueue.value.toMutableList()
                    recommendations.forEach { rec ->
                        if (!updatedQueue.any { it.audioUrl == rec.audioUrl }) {
                            updatedQueue.add(rec)
                        }
                    }
                    _currentQueue.value = updatedQueue
                    Log.d(tag, "Smart auto-queue populated with ${recommendations.size} recommendations")
                }
            }
        }
    }

    private fun triggerSmartAutoPlay() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _currentTrack.value
            val existingUrls = _currentQueue.value.mapNotNull { it.audioUrl }.toSet()
            val recommendations = SmartRecommendationEngine.computeNextRecommendations(
                currentTrack = current,
                trendingTracks = _trendingTracks.value,
                topCharts = _topCharts.value,
                dao = dao,
                youtubeRepository = youtubeRepository,
                existingQueueUrls = existingUrls,
                count = 10
            )
            
            val nextTrack = recommendations.firstOrNull()
                ?: _trendingTracks.value.firstOrNull { it.audioUrl !in existingUrls }
                ?: _topCharts.value.firstOrNull { it.audioUrl !in existingUrls }
                ?: downloadedTracks.value.firstOrNull { it.audioUrl != current?.audioUrl && it.audioUrl !in existingUrls }
                ?: favoriteTracks.value.firstOrNull { it.audioUrl != current?.audioUrl && it.audioUrl !in existingUrls }
                ?: _currentQueue.value.firstOrNull()
                ?: _trendingTracks.value.firstOrNull()
                ?: downloadedTracks.value.firstOrNull()
                ?: favoriteTracks.value.firstOrNull()

            if (nextTrack != null) {
                nextTrack.audioUrl?.let { url ->
                    if (!isTrackDownloaded(url)) {
                        StreamExtractionManager.preExtract(url)
                    }
                }
                withContext(Dispatchers.Main) {
                    val updatedQueue = if (recommendations.isNotEmpty()) {
                        (_currentQueue.value + recommendations).distinctBy { it.audioUrl }
                    } else {
                        if (!_currentQueue.value.any { it.audioUrl == nextTrack.audioUrl }) {
                            _currentQueue.value + nextTrack
                        } else {
                            _currentQueue.value
                        }
                    }
                    playTrack(nextTrack, updatedQueue)
                }
            } else {
                Log.w(tag, "Smart auto-play could not find any track to play!")
            }
        }
    }

    fun playNext(fromUser: Boolean = false) {
        val current = _currentTrack.value
        val queue = _currentQueue.value

        // Handle Repeat One: if triggered automatically (not explicit user action), loop current song
        if (_repeatMode.value == Player.REPEAT_MODE_ONE && !fromUser && current != null) {
            seekTo(0)
            mediaController?.play()
            return
        }

        if (queue.isNotEmpty() && current != null) {
            val idx = queue.indexOfFirst { it.audioUrl == current.audioUrl }

            // Handle Shuffle Mode: choose random track from queue (excluding current if possible)
            if (_isShuffled.value && queue.size > 1) {
                val remaining = queue.filterIndexed { index, _ -> index != idx }
                val nextTrack = remaining.randomOrNull() ?: queue.first()
                playTrack(nextTrack, queue)
                if (queue.size <= 4) {
                    populateSmartNextQueue(nextTrack)
                }
                return
            }

            // Normal sequential advance
            if (idx >= 0 && idx + 1 < queue.size) {
                val nextTrack = queue[idx + 1]
                playTrack(nextTrack, queue)
                if (queue.size - (idx + 1) <= 4) {
                    populateSmartNextQueue(nextTrack)
                }
                return
            }

            // End of queue reached: check Repeat All
            if (_repeatMode.value == Player.REPEAT_MODE_ALL) {
                val firstTrack = queue.first()
                playTrack(firstTrack, queue)
                return
            }
        }

        // End of queue with Repeat Off: check autoplay setting
        val settingsPrefs = getApplication<Application>().getSharedPreferences("Musify_settings", Context.MODE_PRIVATE)
        val autoplayEnabled = settingsPrefs.getBoolean("autoplay", true)
        if (autoplayEnabled) {
            triggerSmartAutoPlay()
        } else {
            mediaController?.pause()
        }
    }

    fun playPrevious() {
        val settingsPrefs = getApplication<Application>().getSharedPreferences("Musify_settings", Context.MODE_PRIVATE)
        val replayOnSkip = settingsPrefs.getBoolean("replay_skip_previous", false)
        val currentPos = _currentPosition.value

        if (replayOnSkip || currentPos > 3000L) {
            seekTo(0)
            return
        }

        val current = _currentTrack.value
        val queue = _currentQueue.value
        if (queue.isNotEmpty() && current != null) {
            val idx = queue.indexOfFirst { it.audioUrl == current.audioUrl }
            if (idx > 0) {
                playTrack(queue[idx - 1], queue)
                return
            } else if (_repeatMode.value == Player.REPEAT_MODE_ALL) {
                playTrack(queue.last(), queue)
                return
            }
        }
        seekTo(0)
    }

    fun setSpeedLock(enabled: Boolean) {
        _isSpeedLocked.value = enabled
        playbackPrefs.edit().putBoolean("is_speed_locked", enabled).apply()
        if (enabled) {
            playbackPrefs.edit().putFloat("locked_speed", _playbackSpeed.value).apply()
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 3.0f)
        _playbackSpeed.value = clamped
        if (_isSpeedLocked.value) {
            playbackPrefs.edit().putFloat("locked_speed", clamped).apply()
        }
        mediaController?.setPlaybackParameters(
            androidx.media3.common.PlaybackParameters(clamped)
        )
    }

    fun updateTrackArtist(track: TrackModel, newArtistName: String) {
        val cleanName = newArtistName.trim()
        if (cleanName.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val url = track.audioUrl ?: return@launch
            dao.updateTrackArtist(url, cleanName)

            withContext(Dispatchers.Main) {
                val updater: (TrackModel) -> TrackModel = { t ->
                    if (t.audioUrl == url) t.copy(artist = cleanName) else t
                }
                _trendingTracks.update { it.map(updater) }
                _searchTracks.update { it.map(updater) }
                _topCharts.update { it.map(updater) }
                _youtubeMusic.update { it.map(updater) }
                _currentQueue.update { it.map(updater) }
                _localTracks.update { it.map(updater) }
                if (_currentTrack.value?.audioUrl == url) {
                    _currentTrack.value = _currentTrack.value?.copy(artist = cleanName)
                }
                android.widget.Toast.makeText(getApplication(), "Linked '${track.title}' to $cleanName", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setRepeatMode(repeatMode: Int) {
        // repeatMode: Player.REPEAT_MODE_OFF / REPEAT_MODE_ONE / REPEAT_MODE_ALL
        _repeatMode.value = repeatMode
        mediaController?.repeatMode = repeatMode
        val sPrefs = getApplication<Application>().getSharedPreferences("Musify_settings", Context.MODE_PRIVATE)
        if (sPrefs.getBoolean("enforce_repeating", false)) {
            sPrefs.edit().putInt("enforced_repeat_mode", repeatMode).apply()
        }
    }

    fun setShuffleModeEnabled(enabled: Boolean) {
        _isShuffled.value = enabled
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

    fun fetchLyrics(track: TrackModel) {
        lyricsJob?.cancel()
        _plainLyrics.value = "Searching lyrics..."
        _syncedLyrics.value = emptyList()
        _currentLyricIndex.value = -1

        val isLocalTrack = track.audioUrl?.let { it.startsWith("/") || it.startsWith("content://") || it.startsWith("file://") } == true
        if (isLocalTrack) {
            val sPrefs = getApplication<Application>().getSharedPreferences("Musify_settings", Context.MODE_PRIVATE)
            val searchLocalLyrics = sPrefs.getBoolean("search_local_lyrics", true)
            if (!searchLocalLyrics) {
                _plainLyrics.value = "Local lyrics search disabled in settings"
                return
            }
        }

        lyricsJob = viewModelScope.launch(Dispatchers.IO) {
            val res = lyricsRepository.fetchLyricsFromNetwork(track.artist, track.title)
            if (isActive) {
                if (res != null) {
                    val parsed = CommonUtils.parseLrc(res.syncedLyrics ?: "")
                    _syncedLyrics.value = parsed
                    _plainLyrics.value = if (parsed.isEmpty()) (res.plainLyrics ?: "No plain lyrics found") else null
                } else {
                    _plainLyrics.value = "Lyrics not available for this track"
                }
            }
        }
    }

    fun addToQueue(track: TrackModel) {
        val queue = _currentQueue.value.toMutableList()
        if (!queue.any { it.audioUrl == track.audioUrl }) {
            queue.add(track)
            _currentQueue.value = queue
        }
    }

    fun playNextTrackInQueue(track: TrackModel) {
        val current = _currentTrack.value
        val queue = _currentQueue.value.toMutableList()
        val curIdx = queue.indexOfFirst { it.audioUrl == current?.audioUrl }
        val insertAt = if (curIdx >= 0) curIdx + 1 else 0
        queue.add(insertAt, track)
        _currentQueue.value = queue
    }

    /**
     * Plays a track selected from search results.
     * Filters out other search results that are versions/remixes/covers of the SAME song,
     * retains genuinely different songs (if any), and immediately auto-populates Up Next with
     * high-affinity recommendations ("songs like that, not same as that").
     */
    fun playTrackFromSearch(track: TrackModel, searchResults: List<TrackModel>) {
        val seedCanonical = SmartRecommendationEngine.normalizeTitle(track.title)
        val seedTokens = seedCanonical.split(" ").filter { it.length > 2 }.toSet()

        // Filter out any songs in search results that are identical or variants of the seed song
        val distinctDifferentSongs = searchResults.filter { candidate ->
            val candidateUrl = candidate.audioUrl ?: return@filter false
            if (candidateUrl == track.audioUrl) return@filter false

            val candCanonical = SmartRecommendationEngine.normalizeTitle(candidate.title)
            if (candCanonical == seedCanonical) return@filter false

            // Check significant word overlap
            val candTokens = candCanonical.split(" ").filter { it.length > 2 }.toSet()
            if (seedTokens.isNotEmpty() && candTokens.isNotEmpty()) {
                val intersection = seedTokens.intersect(candTokens)
                val overlap = intersection.size.toDouble() / minOf(seedTokens.size, candTokens.size)
                if (overlap >= 0.70) return@filter false
            }

            true
        }.distinctBy { SmartRecommendationEngine.normalizeTitle(it.title) }

        val initialQueue = mutableListOf(track)
        initialQueue.addAll(distinctDifferentSongs.take(3))

        playTrack(track, initialQueue)
        populateSmartNextQueue(track)
    }

    /**
     * Plays a single track and generates a full song radio based on its artist, genre, and vibe.
     */
    fun playRadioForTrack(track: TrackModel) {
        playTrack(track, listOf(track))
        populateSmartNextQueue(track)
    }

    /** Removes a track from the Up Next queue by its index within upNextQueue. */
    fun removeFromQueue(upNextIndex: Int) {
        val queue = _currentQueue.value.toMutableList()
        val current = _currentTrack.value
        val offset = if (current == null) 0
                     else (queue.indexOfFirst { it.audioUrl == current.audioUrl } + 1).coerceAtLeast(0)
        val absIdx = offset + upNextIndex
        if (absIdx in queue.indices) {
            val removed = queue.removeAt(absIdx)
            _currentQueue.value = queue
            android.widget.Toast.makeText(getApplication(), "Removed '${removed.title}' from queue", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /** Removes a specific track from the current queue. */
    fun removeTrackFromQueue(track: TrackModel) {
        val queue = _currentQueue.value.toMutableList()
        if (queue.removeIf { it.audioUrl == track.audioUrl }) {
            _currentQueue.value = queue
            android.widget.Toast.makeText(getApplication(), "Removed '${track.title}' from queue", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        val millis = minutes * 60 * 1000L
        _sleepTimerMillis.value = millis
        sleepTimerJob = viewModelScope.launch {
            var remaining = millis
            while (remaining > 0) {
                delay(1000)
                remaining -= 1000
                _sleepTimerMillis.value = remaining
            }
            mediaController?.pause()
            _isPlaying.value = false
            _sleepTimerMillis.value = 0L
        }
    }

    fun isArtistFollowed(artistName: String): Flow<Boolean> {
        val cleanName = artistName.trim()
        return dao.isArtistFollowed(cleanName)
    }

    fun toggleFollowArtist(artistName: String, imageUrl: String? = null) {
        val cleanName = artistName.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val isFollowed = dao.isArtistFollowed(cleanName).first()
            val authManager = com.gaminghub.musicplayer.auth.AuthManager.getInstance(getApplication())
            val syncUserId = authManager.getSyncUserId()
            if (isFollowed) {
                dao.deleteFollowedArtist(cleanName)
                com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.updateFollowedArtist(
                    userId = syncUserId,
                    artistName = cleanName,
                    imageUrl = imageUrl,
                    isFollowed = false
                )
            } else {
                dao.insertFollowedArtist(
                    FollowedArtistEntity(
                        id = cleanName,
                        name = cleanName,
                        imageUrl = imageUrl
                    )
                )
                com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.updateFollowedArtist(
                    userId = syncUserId,
                    artistName = cleanName,
                    imageUrl = imageUrl,
                    isFollowed = true
                )
            }
        }
    }

    suspend fun fetchArtistTopTracks(artistName: String): List<TrackModel> = withContext(Dispatchers.IO) {
        youtubeRepository.fetchMusic("$artistName top songs official", officialOnly = true)
    }

    fun startSongRadio(seedTrack: TrackModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val radioTracks = youtubeRepository.fetchMusic("${seedTrack.artist} similar official songs", officialOnly = true)
            withContext(Dispatchers.Main) {
                if (radioTracks.isNotEmpty()) {
                    val combinedQueue = listOf(seedTrack) + radioTracks.filter { it.audioUrl != seedTrack.audioUrl }
                    playTrack(seedTrack, combinedQueue)
                }
            }
        }
    }

    fun startArtistRadio(artistName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val radioTracks = youtubeRepository.fetchMusic("$artistName radio mix official songs", officialOnly = true)
            withContext(Dispatchers.Main) {
                if (radioTracks.isNotEmpty()) {
                    playTrack(radioTracks.first(), radioTracks)
                }
            }
        }
    }

    fun importSpotifyPlaylist(url: String, onComplete: (String, Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = com.gaminghub.musicplayer.util.SpotifyImporter.importFromSpotifyUrl(url)
            if (result.tracks.isNotEmpty()) {
                val playlistEntity = PlaylistEntity(name = result.playlistName)
                dao.insertPlaylist(playlistEntity)
                val allPlaylists = dao.getPlaylistsSync()
                val createdPlaylist = allPlaylists.find { it.name == result.playlistName }
                if (createdPlaylist != null) {
                    for (track in result.tracks) {
                        dao.insertTrack(track.toEntity())
                        dao.addTrackToPlaylist(
                            PlaylistTrackEntity(
                                playlistId = createdPlaylist.id,
                                audioUrl = track.audioUrl ?: ""
                            )
                        )
                    }
                    val authManager = com.gaminghub.musicplayer.auth.AuthManager.getInstance(getApplication())
                    com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.savePlaylist(
                        userId = authManager.getSyncUserId(),
                        playlistId = createdPlaylist.id.toLong(),
                        name = result.playlistName,
                        tracks = result.tracks
                    )
                }
            }
            withContext(Dispatchers.Main) {
                onComplete(result.playlistName, result.tracks.size)
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerMillis.value = 0L
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(playbackReceiver)
        } catch (_: Exception) {}
        sleepTimerJob?.cancel()
        lyricsJob?.cancel()
        downloadJobs.values.forEach { it.cancel() }
        mediaControllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}

data class DownloadProgress(
    val audioUrl: String,
    val track: TrackModel,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val progressPercent: Int = 0,
    val isIndeterminate: Boolean = false
) {
    val downloadedFormatted: String
        get() = formatBytes(bytesDownloaded)

    val totalFormatted: String
        get() = if (totalBytes > 0) formatBytes(totalBytes) else "Calculating..."

    val speedFormatted: String
        get() = "${formatBytes(speedBytesPerSec)}/s"

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format(java.util.Locale.US, "%.1f MB", mb)
        } else {
            String.format(java.util.Locale.US, "%.0f KB", kb)
        }
    }
}
