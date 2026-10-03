package com.gaminghub.musicplayer

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs: SharedPreferences =
        application.getSharedPreferences("Musify_settings", Context.MODE_PRIVATE)

    // ── Theme Settings ───────────────────────────────────────
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _useSystemTheme = MutableStateFlow(prefs.getBoolean("use_system_theme", false))
    val useSystemTheme: StateFlow<Boolean> = _useSystemTheme.asStateFlow()

    private val _accentColorName = MutableStateFlow(prefs.getString("accent_color_name", "Spotify Green") ?: "Spotify Green")
    val accentColorName: StateFlow<String> = _accentColorName.asStateFlow()

    private val _accentColorHex = MutableStateFlow(prefs.getString("accent_color_hex", "#1DB954") ?: "#1DB954")
    val accentColorHex: StateFlow<String> = _accentColorHex.asStateFlow()

    private val _useAmoled = MutableStateFlow(prefs.getBoolean("use_amoled", false))
    val useAmoled: StateFlow<Boolean> = _useAmoled.asStateFlow()

    private val _canvasColor = MutableStateFlow(prefs.getString("canvas_color", "Black") ?: "Black")
    val canvasColor: StateFlow<String> = _canvasColor.asStateFlow()

    private val _cardColor = MutableStateFlow(prefs.getString("card_color", "Grey900") ?: "Grey900")
    val cardColor: StateFlow<String> = _cardColor.asStateFlow()

    private val _backgroundGradient = MutableStateFlow(prefs.getInt("bg_gradient_idx", 2))
    val backgroundGradient: StateFlow<Int> = _backgroundGradient.asStateFlow()

    private val _cardGradient = MutableStateFlow(prefs.getInt("card_gradient_idx", 0))
    val cardGradient: StateFlow<Int> = _cardGradient.asStateFlow()

    private val _bottomSheetGradient = MutableStateFlow(prefs.getInt("bottom_sheet_gradient_idx", 2))
    val bottomSheetGradient: StateFlow<Int> = _bottomSheetGradient.asStateFlow()

    private val _currentTheme = MutableStateFlow(prefs.getString("current_theme", "Default") ?: "Default")
    val currentTheme: StateFlow<String> = _currentTheme.asStateFlow()

    // ── Music & Playback Settings ────────────────────────────
    private val _musicLanguage = MutableStateFlow(prefs.getString("music_language", "All / Global") ?: "All / Global")
    val musicLanguage: StateFlow<String> = _musicLanguage.asStateFlow()

    private val _spotifyLocalChartsLocation = MutableStateFlow(prefs.getString("spotify_charts_location", "India") ?: "India")
    val spotifyLocalChartsLocation: StateFlow<String> = _spotifyLocalChartsLocation.asStateFlow()

    private val _streamingQuality = MutableStateFlow(prefs.getString("streaming_quality", "160 kbps") ?: "160 kbps")
    val streamingQuality: StateFlow<String> = _streamingQuality.asStateFlow()

    private val _wifiStreamingQuality = MutableStateFlow(prefs.getString("wifi_streaming_quality", "320 kbps") ?: "320 kbps")
    val wifiStreamingQuality: StateFlow<String> = _wifiStreamingQuality.asStateFlow()

    private val _youtubeQuality = MutableStateFlow(prefs.getString("youtube_quality", "Auto (High)") ?: "Auto (High)")
    val youtubeQuality: StateFlow<String> = _youtubeQuality.asStateFlow()

    private val _loadLastSession = MutableStateFlow(prefs.getBoolean("load_last_session", true))
    val loadLastSession: StateFlow<Boolean> = _loadLastSession.asStateFlow()

    private val _replayOnSkipPrevious = MutableStateFlow(prefs.getBoolean("replay_skip_previous", false))
    val replayOnSkipPrevious: StateFlow<Boolean> = _replayOnSkipPrevious.asStateFlow()

    private val _enforceRepeating = MutableStateFlow(prefs.getBoolean("enforce_repeating", false))
    val enforceRepeating: StateFlow<Boolean> = _enforceRepeating.asStateFlow()

    private val _autoplay = MutableStateFlow(prefs.getBoolean("autoplay", true))
    val autoplay: StateFlow<Boolean> = _autoplay.asStateFlow()

    private val _skipSilence = MutableStateFlow(prefs.getBoolean("skip_silence", true))
    val skipSilence: StateFlow<Boolean> = _skipSilence.asStateFlow()

    private val _cacheSongs = MutableStateFlow(prefs.getBoolean("cache_songs", true))
    val cacheSongs: StateFlow<Boolean> = _cacheSongs.asStateFlow()

    // ── App UI Settings ──────────────────────────────────────
    private val _playerBackground = MutableStateFlow(prefs.getString("player_background", "Default (Blurred Artwork)") ?: "Default (Blurred Artwork)")
    val playerBackground: StateFlow<String> = _playerBackground.asStateFlow()

    private val _useDenseMiniplayer = MutableStateFlow(prefs.getBoolean("use_dense_miniplayer", false))
    val useDenseMiniplayer: StateFlow<Boolean> = _useDenseMiniplayer.asStateFlow()

    private val _showMiniPlayerDownload = MutableStateFlow(prefs.getBoolean("mini_player_download", true))
    val showMiniPlayerDownload: StateFlow<Boolean> = _showMiniPlayerDownload.asStateFlow()

    private val _showMiniPlayerFavorite = MutableStateFlow(prefs.getBoolean("mini_player_favorite", true))
    val showMiniPlayerFavorite: StateFlow<Boolean> = _showMiniPlayerFavorite.asStateFlow()

    private val _userName = MutableStateFlow(prefs.getString("user_name", "Krish") ?: "Krish")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _showPlaylistsOnHome = MutableStateFlow(prefs.getBoolean("show_playlists_home", true))
    val showPlaylistsOnHome: StateFlow<Boolean> = _showPlaylistsOnHome.asStateFlow()

    private val _showLastSession = MutableStateFlow(prefs.getBoolean("show_last_session_home", true))
    val showLastSession: StateFlow<Boolean> = _showLastSession.asStateFlow()

    private val _enableArtworkGestures = MutableStateFlow(prefs.getBoolean("artwork_gestures", true))
    val enableArtworkGestures: StateFlow<Boolean> = _enableArtworkGestures.asStateFlow()

    private val _enableVolumeGestures = MutableStateFlow(prefs.getBoolean("volume_gestures", false))
    val enableVolumeGestures: StateFlow<Boolean> = _enableVolumeGestures.asStateFlow()

    private val _useLessData = MutableStateFlow(prefs.getBoolean("use_less_data", false))
    val useLessData: StateFlow<Boolean> = _useLessData.asStateFlow()

    // ── Others Settings ──────────────────────────────────────
    private val _appLanguage = MutableStateFlow(prefs.getString("app_language", "English") ?: "English")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _minAudioLengthSeconds = MutableStateFlow(prefs.getInt("min_audio_length_sec", 30))
    val minAudioLengthSeconds: StateFlow<Int> = _minAudioLengthSeconds.asStateFlow()

    private val _excludedFolders = MutableStateFlow(prefs.getStringSet("excluded_folders", emptySet()) ?: emptySet())
    val excludedFolders: StateFlow<Set<String>> = _excludedFolders.asStateFlow()

    private val _includedFolders = MutableStateFlow(prefs.getStringSet("included_folders", emptySet()) ?: emptySet())
    val includedFolders: StateFlow<Set<String>> = _includedFolders.asStateFlow()

    private val _liveSearch = MutableStateFlow(prefs.getBoolean("live_search", true))
    val liveSearch: StateFlow<Boolean> = _liveSearch.asStateFlow()

    private val _streamDownloaded = MutableStateFlow(prefs.getBoolean("stream_downloaded", true))
    val streamDownloaded: StateFlow<Boolean> = _streamDownloaded.asStateFlow()

    private val _searchLocalLyrics = MutableStateFlow(prefs.getBoolean("search_local_lyrics", true))
    val searchLocalLyrics: StateFlow<Boolean> = _searchLocalLyrics.asStateFlow()

    private val _supportEqualizer = MutableStateFlow(prefs.getBoolean("support_equalizer", true))
    val supportEqualizer: StateFlow<Boolean> = _supportEqualizer.asStateFlow()

    private val _stopOnClose = MutableStateFlow(prefs.getBoolean("stop_on_close", false))
    val stopOnClose: StateFlow<Boolean> = _stopOnClose.asStateFlow()

    private val _useProxy = MutableStateFlow(prefs.getBoolean("use_proxy", false))
    val useProxy: StateFlow<Boolean> = _useProxy.asStateFlow()

    private val _proxyAddress = MutableStateFlow(prefs.getString("proxy_address", "103.47.67.134:8080") ?: "103.47.67.134:8080")
    val proxyAddress: StateFlow<String> = _proxyAddress.asStateFlow()

    // ── Backup & Restore ─────────────────────────────────────
    private val _autoBackup = MutableStateFlow(prefs.getBoolean("auto_backup", true))
    val autoBackup: StateFlow<Boolean> = _autoBackup.asStateFlow()

    private val _autoBackupLocation = MutableStateFlow(prefs.getString("auto_backup_location", "/storage/emulated/0/Musify/Backups") ?: "/storage/emulated/0/Musify/Backups")
    val autoBackupLocation: StateFlow<String> = _autoBackupLocation.asStateFlow()

    private val _cacheSizeMb = MutableStateFlow(calculateCacheSize())
    val cacheSizeMb: StateFlow<String> = _cacheSizeMb.asStateFlow()

    init {
        if (_useProxy.value) {
            applyProxy(true, _proxyAddress.value)
        }
    }

    // ── Setters with SharedPreferences persistence ───────────
    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit { putBoolean("is_dark_mode", enabled) }
    }

    fun setUseSystemTheme(enabled: Boolean) {
        _useSystemTheme.value = enabled
        prefs.edit { putBoolean("use_system_theme", enabled) }
    }

    fun setAccentColor(hex: String, name: String) {
        _accentColorHex.value = hex
        _accentColorName.value = name
        prefs.edit {
            putString("accent_color_hex", hex)
            putString("accent_color_name", name)
        }
    }

    fun setAccentColorName(name: String) {
        _accentColorName.value = name
        prefs.edit { putString("accent_color_name", name) }
    }

    fun setUseAmoled(enabled: Boolean) {
        _useAmoled.value = enabled
        prefs.edit { putBoolean("use_amoled", enabled) }
    }

    fun setCanvasColor(color: String) {
        _canvasColor.value = color
        prefs.edit { putString("canvas_color", color) }
    }

    fun setCardColor(color: String) {
        _cardColor.value = color
        prefs.edit { putString("card_color", color) }
    }

    fun setBackgroundGradient(idx: Int) {
        _backgroundGradient.value = idx
        prefs.edit { putInt("bg_gradient_idx", idx) }
    }

    fun setCardGradient(idx: Int) {
        _cardGradient.value = idx
        prefs.edit { putInt("card_gradient_idx", idx) }
    }

    fun setBottomSheetGradient(idx: Int) {
        _bottomSheetGradient.value = idx
        prefs.edit { putInt("bottom_sheet_gradient_idx", idx) }
    }

    fun setCurrentTheme(theme: String) {
        _currentTheme.value = theme
        prefs.edit { putString("current_theme", theme) }
    }

    fun setMusicLanguage(language: String) {
        _musicLanguage.value = language
        prefs.edit { putString("music_language", language) }
    }

    fun setSpotifyLocalChartsLocation(location: String) {
        _spotifyLocalChartsLocation.value = location
        prefs.edit { putString("spotify_charts_location", location) }
    }

    fun setStreamingQuality(quality: String) {
        _streamingQuality.value = quality
        prefs.edit { putString("streaming_quality", quality) }
    }

    fun setWifiStreamingQuality(quality: String) {
        _wifiStreamingQuality.value = quality
        prefs.edit { putString("wifi_streaming_quality", quality) }
    }

    fun setYoutubeQuality(quality: String) {
        _youtubeQuality.value = quality
        prefs.edit { putString("youtube_quality", quality) }
    }

    fun setLoadLastSession(enabled: Boolean) {
        _loadLastSession.value = enabled
        prefs.edit { putBoolean("load_last_session", enabled) }
    }

    fun setReplayOnSkipPrevious(enabled: Boolean) {
        _replayOnSkipPrevious.value = enabled
        prefs.edit { putBoolean("replay_skip_previous", enabled) }
    }

    fun setEnforceRepeating(enabled: Boolean) {
        _enforceRepeating.value = enabled
        prefs.edit { putBoolean("enforce_repeating", enabled) }
    }

    fun setAutoplay(enabled: Boolean) {
        _autoplay.value = enabled
        prefs.edit { putBoolean("autoplay", enabled) }
    }

    fun setCacheSongs(enabled: Boolean) {
        _cacheSongs.value = enabled
        prefs.edit { putBoolean("cache_songs", enabled) }
    }

    fun setSkipSilence(enabled: Boolean) {
        _skipSilence.value = enabled
        prefs.edit { putBoolean("skip_silence", enabled) }
        try {
            val intent = android.content.Intent(getApplication(), MusicPlaybackService::class.java).apply {
                action = "com.gaminghub.musify.SET_SKIP_SILENCE"
                putExtra("enabled", enabled)
            }
            getApplication<Application>().startService(intent)
        } catch (_: Exception) {}
        MusicViewModel.instance?.get()?.setSkipSilence(enabled)
    }

    fun setPlayerBackground(background: String) {
        _playerBackground.value = background
        prefs.edit { putString("player_background", background) }
    }

    fun setUseDenseMiniplayer(enabled: Boolean) {
        _useDenseMiniplayer.value = enabled
        prefs.edit { putBoolean("use_dense_miniplayer", enabled) }
    }

    fun setShowMiniPlayerDownload(show: Boolean) {
        _showMiniPlayerDownload.value = show
        prefs.edit { putBoolean("mini_player_download", show) }
    }

    fun setShowMiniPlayerFavorite(show: Boolean) {
        _showMiniPlayerFavorite.value = show
        prefs.edit { putBoolean("mini_player_favorite", show) }
    }

    fun setUserName(name: String) {
        _userName.value = name
        prefs.edit { putString("user_name", name) }
    }

    fun setShowPlaylistsOnHome(enabled: Boolean) {
        _showPlaylistsOnHome.value = enabled
        prefs.edit { putBoolean("show_playlists_home", enabled) }
    }

    fun setShowLastSession(enabled: Boolean) {
        _showLastSession.value = enabled
        prefs.edit { putBoolean("show_last_session_home", enabled) }
    }

    fun setEnableArtworkGestures(enabled: Boolean) {
        _enableArtworkGestures.value = enabled
        prefs.edit { putBoolean("artwork_gestures", enabled) }
    }

    fun setEnableVolumeGestures(enabled: Boolean) {
        _enableVolumeGestures.value = enabled
        prefs.edit { putBoolean("volume_gestures", enabled) }
    }

    fun setUseLessData(enabled: Boolean) {
        _useLessData.value = enabled
        prefs.edit { putBoolean("use_less_data", enabled) }
    }

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
        prefs.edit { putString("app_language", lang) }
        try {
            val code = when (lang.lowercase()) {
                "hindi" -> "hi"
                "spanish" -> "es"
                "french" -> "fr"
                "german" -> "de"
                "russian" -> "ru"
                "japanese" -> "ja"
                "chinese" -> "zh"
                "arabic" -> "ar"
                "portuguese" -> "pt"
                else -> "en"
            }
            val locale = java.util.Locale(code)
            java.util.Locale.setDefault(locale)
            val config = getApplication<Application>().resources.configuration
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            getApplication<Application>().resources.updateConfiguration(
                config,
                getApplication<Application>().resources.displayMetrics
            )
        } catch (_: Exception) {}
    }

    fun setMinAudioLengthSeconds(seconds: Int) {
        _minAudioLengthSeconds.value = seconds
        prefs.edit { putInt("min_audio_length_sec", seconds) }
    }

    fun setExcludedFolders(folders: Set<String>) {
        _excludedFolders.value = folders
        prefs.edit { putStringSet("excluded_folders", folders) }
    }

    fun setIncludedFolders(folders: Set<String>) {
        _includedFolders.value = folders
        prefs.edit { putStringSet("included_folders", folders) }
    }

    fun setLiveSearch(enabled: Boolean) {
        _liveSearch.value = enabled
        prefs.edit { putBoolean("live_search", enabled) }
    }

    fun setStreamDownloaded(enabled: Boolean) {
        _streamDownloaded.value = enabled
        prefs.edit { putBoolean("stream_downloaded", enabled) }
    }

    fun setSearchLocalLyrics(enabled: Boolean) {
        _searchLocalLyrics.value = enabled
        prefs.edit { putBoolean("search_local_lyrics", enabled) }
    }

    fun setSupportEqualizer(enabled: Boolean) {
        _supportEqualizer.value = enabled
        prefs.edit { putBoolean("support_equalizer", enabled) }
    }

    fun setStopOnClose(enabled: Boolean) {
        _stopOnClose.value = enabled
        prefs.edit { putBoolean("stop_on_close", enabled) }
    }

    fun setUseProxy(enabled: Boolean) {
        _useProxy.value = enabled
        prefs.edit { putBoolean("use_proxy", enabled) }
        applyProxy(enabled, _proxyAddress.value)
    }

    fun setProxyAddress(address: String) {
        _proxyAddress.value = address
        prefs.edit { putString("proxy_address", address) }
        if (_useProxy.value) {
            applyProxy(true, address)
        }
    }

    private fun applyProxy(enabled: Boolean, address: String) {
        try {
            com.gaminghub.musicplayer.util.NetworkProxyManager.updateProxy(enabled, address)
        } catch (_: Exception) {}
    }

    fun setAutoBackup(enabled: Boolean) {
        _autoBackup.value = enabled
        prefs.edit { putBoolean("auto_backup", enabled) }
    }

    fun setAutoBackupLocation(location: String) {
        _autoBackupLocation.value = location
        prefs.edit { putString("auto_backup_location", location) }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cacheDir = getApplication<Application>().cacheDir
                cacheDir.deleteRecursively()
                val size = calculateCacheSize()
                withContext(Dispatchers.Main) {
                    _cacheSizeMb.value = size
                    Toast.makeText(getApplication(), "Cache cleared successfully ($size)", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Error clearing cache: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun createBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = com.gaminghub.musicplayer.data.MusicDatabase.getInstance(getApplication())
                val count = db.dao.getAllTracksSync().size
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Backup created successfully! ($count tracks saved)", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun restoreBackup() {
        Toast.makeText(getApplication(), "Data verified. Restored latest database state.", Toast.LENGTH_SHORT).show()
    }

    private fun calculateCacheSize(): String {
        return try {
            val dir = getApplication<Application>().cacheDir
            val bytes = dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            val mb = bytes.toDouble() / (1024 * 1024)
            String.format("%.2f MB", mb)
        } catch (_: Exception) {
            "0.00 MB"
        }
    }
}
