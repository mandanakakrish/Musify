package com.gaminghub.musify.ui.viewmodels

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.media.audiofx.Visualizer
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import com.gaminghub.musify.data.SettingsRepository
import androidx.lifecycle.ViewModelProvider
import android.app.Application
import androidx.lifecycle.AndroidViewModel

import com.gaminghub.musify.util.AnalyticsManager

class AudioEngineViewModel(
    application: Application,
    private val analyticsManager: AnalyticsManager
) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)

    private val _isEqualizerEnabled = MutableStateFlow(settingsRepository.isEqualizerEnabled)
    val isEqualizerEnabled: StateFlow<Boolean> = _isEqualizerEnabled

    private val _equalizerBands = MutableStateFlow<Map<Int, Int>>(settingsRepository.getEqualizerBands())
    val equalizerBands: StateFlow<Map<Int, Int>> = _equalizerBands

    private val _bassBoostLevel = MutableStateFlow(settingsRepository.bassBoostLevel)
    val bassBoostLevel: StateFlow<Int> = _bassBoostLevel

    private val _virtualizerLevel = MutableStateFlow(settingsRepository.virtualizerLevel)
    val virtualizerLevel: StateFlow<Int> = _virtualizerLevel

    private val _fftData = MutableStateFlow(ByteArray(0))
    val fftData: StateFlow<ByteArray> = _fftData

    private val _waveformData = MutableStateFlow(ByteArray(0))
    val waveformData: StateFlow<ByteArray> = _waveformData

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var visualizer: Visualizer? = null
    private var currentSessionId: Int = -1
    private var lastInitTime: Long = 0

    fun toggleEqualizer(enabled: Boolean) {
        _isEqualizerEnabled.value = enabled
        settingsRepository.setEqualizerEnabled(enabled)
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        virtualizer?.enabled = enabled
    }

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun updateBandLevel(band: Int, level: Int) {
        val current = _equalizerBands.value.toMutableMap()
        current[band] = level
        _equalizerBands.value = current
        settingsRepository.setEqualizerBands(current)
        try {
            equalizer?.setBandLevel(band.toShort(), level.toShort())
        } catch (_: Exception) {}
    }

    fun updateBassBoost(level: Int) {
        _bassBoostLevel.value = level
        settingsRepository.setBassBoostLevel(level)
        try {
            bassBoost?.setStrength(level.toShort())
        } catch (_: Exception) {}
    }

    fun updateVirtualizer(level: Int) {
        _virtualizerLevel.value = level
        settingsRepository.setVirtualizerLevel(level)
        try {
            virtualizer?.setStrength(level.toShort())
        } catch (_: Exception) {}
    }

    fun startVisualizer(sessionId: Int) = initAudioEffects(sessionId)

    fun initAudioEffects(sessionId: Int) {
        if (sessionId <= 0 || sessionId == currentSessionId) {
            return
        }
        
        // Debounce: prevent rapid re-init (within 500ms)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastInitTime < 500) return
        lastInitTime = currentTime
        
        currentSessionId = sessionId
        
        try {
            stopVisualizer()
            visualizer = Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                        waveform?.let { _waveformData.value = it.copyOf() }
                    }
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        fft?.let { _fftData.value = it.copyOf() }
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, true)
                enabled = true
            }

            equalizer = Equalizer(0, sessionId).apply {
                enabled = _isEqualizerEnabled.value
                val savedBands = _equalizerBands.value
                if (savedBands.isNotEmpty()) {
                    savedBands.forEach { (band, level) ->
                        try { setBandLevel(band.toShort(), level.toShort()) } catch (_: Exception) {}
                    }
                } else {
                    // First time: capture current defaults
                    val bands = mutableMapOf<Int, Int>()
                    for (i in 0 until numberOfBands) {
                        bands[i.toInt()] = getBandLevel(i.toShort()).toInt()
                    }
                    _equalizerBands.value = bands
                    settingsRepository.setEqualizerBands(bands)
                }
            }
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = _isEqualizerEnabled.value
                setStrength(_bassBoostLevel.value.toShort())
            }
            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = _isEqualizerEnabled.value
                setStrength(_virtualizerLevel.value.toShort())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopVisualizer() {
        try {
            visualizer?.apply {
                enabled = false
                release()
            }
            visualizer = null
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        stopVisualizer()
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
    }
}
