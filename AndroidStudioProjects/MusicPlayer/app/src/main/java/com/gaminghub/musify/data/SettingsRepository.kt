package com.gaminghub.musify.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("musify_settings", Context.MODE_PRIVATE)

    // Theme
    val isDarkMode: Boolean get() = prefs.getBoolean("is_dark_mode", true)
    fun setDarkMode(enabled: Boolean) = prefs.edit().putBoolean("is_dark_mode", enabled).apply()

    val useSystemTheme: Boolean get() = prefs.getBoolean("use_system_theme", false)
    fun setUseSystemTheme(enabled: Boolean) = prefs.edit().putBoolean("use_system_theme", enabled).apply()

    val accentColor: Long get() = prefs.getLong("accent_color", 0xFFE91E63) // Default Musify Pink
    fun setAccentColor(color: Long) = prefs.edit().putLong("accent_color", color).apply()

    val userName: String get() = prefs.getString("user_name", "User") ?: "User"
    fun setUserName(name: String) = prefs.edit().putString("user_name", name).apply()

    // App UI
    val useDenseMiniplayer: Boolean get() = prefs.getBoolean("use_dense_miniplayer", false)
    fun setUseDenseMiniplayer(enabled: Boolean) = prefs.edit().putBoolean("use_dense_miniplayer", enabled).apply()

    val artworkGestures: Boolean get() = prefs.getBoolean("artwork_gestures", true)
    fun setArtworkGestures(enabled: Boolean) = prefs.edit().putBoolean("artwork_gestures", enabled).apply()

    val volumeGestures: Boolean get() = prefs.getBoolean("volume_gestures", false)
    fun setVolumeGestures(enabled: Boolean) = prefs.edit().putBoolean("volume_gestures", enabled).apply()

    val useLessData: Boolean get() = prefs.getBoolean("use_less_data", false)
    fun setUseLessData(enabled: Boolean) = prefs.edit().putBoolean("use_less_data", enabled).apply()

    val playerBackground: String get() = prefs.getString("player_background", "Default") ?: "Default"
    fun setPlayerBackground(bg: String) = prefs.edit().putString("player_background", bg).apply()

    val showPlaylistsOnHome: Boolean get() = prefs.getBoolean("show_playlists_on_home", true)
    fun setShowPlaylistsOnHome(enabled: Boolean) = prefs.edit().putBoolean("show_playlists_on_home", enabled).apply()

    val showLastSession: Boolean get() = prefs.getBoolean("show_last_session", true)
    fun setShowLastSession(enabled: Boolean) = prefs.edit().putBoolean("show_last_session", enabled).apply()

    val loadLastSession: Boolean get() = prefs.getBoolean("load_last_session", true)
    fun setLoadLastSession(enabled: Boolean) = prefs.edit().putBoolean("load_last_session", enabled).apply()

    val skipPreviousBehavior: Boolean get() = prefs.getBoolean("skip_previous_behavior", true)
    fun setSkipPreviousBehavior(enabled: Boolean) = prefs.edit().putBoolean("skip_previous_behavior", enabled).apply()

    // Music & Playback
    val musicLanguage: String get() = prefs.getString("music_language", "None") ?: "None"
    fun setMusicLanguage(lang: String) = prefs.edit().putString("music_language", lang).apply()

    val streamingQuality: String get() = prefs.getString("streaming_quality", "96 kbps") ?: "96 kbps"
    fun setStreamingQuality(quality: String) = prefs.edit().putString("streaming_quality", quality).apply()

    val playbackGapSeconds: Int get() = prefs.getInt("playback_gap_seconds", 0)
    fun setPlaybackGapSeconds(seconds: Int) = prefs.edit().putInt("playback_gap_seconds", seconds).apply()

    val autoplay: Boolean get() = prefs.getBoolean("autoplay", true)
    fun setAutoplay(enabled: Boolean) = prefs.edit().putBoolean("autoplay", enabled).apply()

    val enforceRepeating: Boolean get() = prefs.getBoolean("enforce_repeating", false)
    fun setEnforceRepeating(enabled: Boolean) = prefs.edit().putBoolean("enforce_repeating", enabled).apply()

    // Premium Audio
    val crossfadeEnabled: Boolean get() = prefs.getBoolean("crossfade_enabled", false)
    fun setCrossfadeEnabled(enabled: Boolean) = prefs.edit().putBoolean("crossfade_enabled", enabled).apply()

    val crossfadeDurationSeconds: Int get() = prefs.getInt("crossfade_duration", 5)
    fun setCrossfadeDurationSeconds(seconds: Int) = prefs.edit().putInt("crossfade_duration", seconds).apply()

    val volumeNormalizationEnabled: Boolean get() = prefs.getBoolean("volume_normalization", false)
    fun setVolumeNormalizationEnabled(enabled: Boolean) = prefs.edit().putBoolean("volume_normalization", enabled).apply()

    // Equalizer
    val isEqualizerEnabled: Boolean get() = prefs.getBoolean("equalizer_enabled", false)
    fun setEqualizerEnabled(enabled: Boolean) = prefs.edit().putBoolean("equalizer_enabled", enabled).apply()

    val bassBoostLevel: Int get() = prefs.getInt("bass_boost_level", 0)
    fun setBassBoostLevel(level: Int) = prefs.edit().putInt("bass_boost_level", level).apply()

    val virtualizerLevel: Int get() = prefs.getInt("virtualizer_level", 0)
    fun setVirtualizerLevel(level: Int) = prefs.edit().putInt("virtualizer_level", level).apply()

    /**
     * Stores equalizer bands as a comma-separated string of "bandIndex:level".
     * Example: "0:500,1:-200,2:0"
     */
    fun getEqualizerBands(): Map<Int, Int> {
        val raw = prefs.getString("equalizer_bands", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        return raw.split(",").associate {
            val parts = it.split(":")
            parts[0].toInt() to parts[1].toInt()
        }
    }

    fun setEqualizerBands(bands: Map<Int, Int>) {
        val serialized = bands.map { "${it.key}:${it.value}" }.joinToString(",")
        prefs.edit().putString("equalizer_bands", serialized).apply()
    }
}
