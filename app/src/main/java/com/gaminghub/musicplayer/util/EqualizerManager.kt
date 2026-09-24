package com.gaminghub.musicplayer.util

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import com.gaminghub.musicplayer.MusicPlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PresetData(
    val name: String,
    val bandLevels: List<Short> // 5 band levels in milliBels (-1500 to 1500)
)

class EqualizerManager private constructor(context: Context) {
    private val tag = "EqualizerManager"
    private val prefs: SharedPreferences = context.getSharedPreferences("musify_equalizer_prefs", Context.MODE_PRIVATE)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _isEnabled = MutableStateFlow(prefs.getBoolean("eq_enabled", true))
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _selectedPreset = MutableStateFlow(prefs.getString("eq_preset", "Flat") ?: "Flat")
    val selectedPreset: StateFlow<String> = _selectedPreset.asStateFlow()

    // 5 standard bands in dB (-15 to +15)
    private val _bandLevels = MutableStateFlow(loadBandLevels())
    val bandLevels: StateFlow<List<Int>> = _bandLevels.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow(prefs.getInt("eq_bass_boost", 0))
    val bassBoostStrength: StateFlow<Int> = _bassBoostStrength.asStateFlow()

    private val _virtualizerStrength = MutableStateFlow(prefs.getInt("eq_virtualizer", 0))
    val virtualizerStrength: StateFlow<Int> = _virtualizerStrength.asStateFlow()

    val presets = listOf(
        PresetData("Flat", listOf(0, 0, 0, 0, 0)),
        PresetData("Bass Boost", listOf(900, 600, 200, 0, 0)),
        PresetData("Rock", listOf(600, 300, -100, 400, 700)),
        PresetData("Pop", listOf(-200, 300, 700, 400, -100)),
        PresetData("Hip Hop", listOf(800, 400, 0, 300, 500)),
        PresetData("Electronic", listOf(700, 400, 0, 300, 600)),
        PresetData("Classical", listOf(500, 300, -200, 400, 500)),
        PresetData("Vocal Boost", listOf(-300, 0, 700, 800, 300)),
        PresetData("Jazz", listOf(400, 200, -200, 200, 500)),
        PresetData("Custom", listOf(0, 0, 0, 0, 0))
    )

    init {
        val audioSessionId = MusicPlaybackService.currentAudioSessionId
        if (audioSessionId > 0) {
            initAudioEffects(audioSessionId)
        }
    }

    fun initAudioEffects(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        try {
            release()
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _isEnabled.value
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _isEnabled.value
                try {
                    if (strengthSupported) {
                        setStrength((_bassBoostStrength.value * 7.5f).toInt().coerceIn(0, 750).toShort())
                    }
                } catch (_: Exception) {}
            }
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = _isEnabled.value
                try {
                    if (strengthSupported) {
                        setStrength((_virtualizerStrength.value * 8).coerceIn(0, 800).toShort())
                    }
                } catch (_: Exception) {}
            }

            applyCurrentSettings()
            Log.d(tag, "Equalizer initialized cleanly with anti-clipping headroom for session: $audioSessionId")
        } catch (e: Exception) {
            Log.w(tag, "Could not initialize hardware equalizer: ${e.message}")
        }
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        prefs.edit().putBoolean("eq_enabled", enabled).apply()
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
        } catch (_: Exception) {}
    }

    fun setPreset(presetName: String) {
        _selectedPreset.value = presetName
        prefs.edit().putString("eq_preset", presetName).apply()

        val preset = presets.find { it.name == presetName }
        if (preset != null && presetName != "Custom") {
            val levelsInDb = preset.bandLevels.map { (it / 100) }
            _bandLevels.value = levelsInDb
            saveBandLevels(levelsInDb)
            applyCurrentSettings()
        }
    }

    fun setBandLevel(bandIndex: Int, levelDb: Int) {
        val clamped = levelDb.coerceIn(-15, 15)
        val current = _bandLevels.value.toMutableList()
        if (bandIndex in 0 until current.size) {
            current[bandIndex] = clamped
            _bandLevels.value = current
            _selectedPreset.value = "Custom"
            prefs.edit().putString("eq_preset", "Custom").apply()
            saveBandLevels(current)
            applyCurrentSettings()
        }
    }

    fun setBassBoost(strengthPercent: Int) {
        val clamped = strengthPercent.coerceIn(0, 100)
        _bassBoostStrength.value = clamped
        prefs.edit().putInt("eq_bass_boost", clamped).apply()
        try {
            bassBoost?.let {
                if (it.strengthSupported) {
                    val safeStrength = (clamped * 7.5f).toInt().coerceIn(0, 750).toShort()
                    it.setStrength(safeStrength)
                }
            }
        } catch (_: Exception) {}
    }

    fun setVirtualizer(strengthPercent: Int) {
        val clamped = strengthPercent.coerceIn(0, 100)
        _virtualizerStrength.value = clamped
        prefs.edit().putInt("eq_virtualizer", clamped).apply()
        try {
            virtualizer?.let {
                if (it.strengthSupported) {
                    val safeStrength = (clamped * 8).coerceIn(0, 800).toShort()
                    it.setStrength(safeStrength)
                }
            }
        } catch (_: Exception) {}
    }

    private fun applyCurrentSettings() {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands
            val levels = _bandLevels.value
            // Anti-clipping headroom: calculate the peak boost across all bands
            // and apply subtractive gain so the highest band never exceeds 0 dBFS.
            // This preserves the exact tone curve without digital clipping or audio cracking.
            val maxBoost = maxOf(0, levels.maxOrNull() ?: 0)
            for (i in 0 until minOf(numBands.toInt(), levels.size)) {
                val db = levels[i]
                val safeDb = db - maxBoost
                val mb = (safeDb * 100).toShort().coerceIn(eq.bandLevelRange[0], eq.bandLevelRange[1])
                eq.setBandLevel(i.toShort(), mb)
            }
        } catch (e: Exception) {
            Log.w(tag, "applyCurrentSettings error: ${e.message}")
        }
    }

    private fun loadBandLevels(): List<Int> {
        val saved = prefs.getString("eq_band_levels", null)
        if (!saved.isNullOrBlank()) {
            val parts = saved.split(",").mapNotNull { it.toIntOrNull() }
            if (parts.size == 5) return parts
        }
        return listOf(0, 0, 0, 0, 0)
    }

    private fun saveBandLevels(levels: List<Int>) {
        prefs.edit().putString("eq_band_levels", levels.joinToString(",")).apply()
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
    }

    companion object {
        @Volatile
        private var INSTANCE: EqualizerManager? = null

        fun getInstance(context: Context): EqualizerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EqualizerManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
