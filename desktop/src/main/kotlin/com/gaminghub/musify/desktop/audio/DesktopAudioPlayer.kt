package com.gaminghub.musify.desktop.audio

import javafx.application.Platform
import javafx.scene.media.EqualizerBand
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class DesktopAudioPlayer(private val onTrackEnded: () -> Unit) {

    private var mediaPlayer: MediaPlayer? = null
    private var isFxStarted = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _volume = MutableStateFlow(0.8f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    // 10-band Equalizer gains in dB (default 0 dB)
    private val _equalizerBands = MutableStateFlow(FloatArray(10) { 0f })
    val equalizerBands: StateFlow<FloatArray> = _equalizerBands.asStateFlow()

    private val _equalizerEnabled = MutableStateFlow(true)
    val equalizerEnabled: StateFlow<Boolean> = _equalizerEnabled.asStateFlow()

    init {
        initJavaFx()
    }

    private fun initJavaFx() {
        try {
            Platform.startup {}
            isFxStarted = true
        } catch (_: IllegalStateException) {
            // Already started
            isFxStarted = true
        } catch (e: Exception) {
            System.err.println("JavaFX Platform startup error: ${e.message}")
        }
    }

    fun play(source: String) {
        val mediaUri = if (source.startsWith("http://") || source.startsWith("https://")) {
            source
        } else {
            File(source).toURI().toString()
        }

        Platform.runLater {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.dispose()

                val media = Media(mediaUri)
                val player = MediaPlayer(media)
                mediaPlayer = player

                player.volume = if (_isMuted.value) 0.0 else _volume.value.toDouble()

                // Apply equalizer
                applyEqualizerToPlayer(player)

                player.setOnReady {
                    _durationMs.value = media.duration.toMillis().toLong()
                    player.play()
                    _isPlaying.value = true
                    _isBuffering.value = false
                }

                player.currentTimeProperty().addListener { _, _, newTime ->
                    _positionMs.value = newTime.toMillis().toLong()
                }

                player.statusProperty().addListener { _, _, newStatus ->
                    when (newStatus) {
                        MediaPlayer.Status.PLAYING -> {
                            _isPlaying.value = true
                            _isBuffering.value = false
                        }
                        MediaPlayer.Status.PAUSED -> {
                            _isPlaying.value = false
                            _isBuffering.value = false
                        }
                        MediaPlayer.Status.STALLED -> {
                            _isBuffering.value = true
                        }
                        MediaPlayer.Status.STOPPED -> {
                            _isPlaying.value = false
                            _isBuffering.value = false
                        }
                        else -> {}
                    }
                }

                player.setOnEndOfMedia {
                    _isPlaying.value = false
                    _positionMs.value = 0L
                    onTrackEnded()
                }

                player.setOnError {
                    System.err.println("MediaPlayer error: ${player.error?.message}")
                    _isPlaying.value = false
                    _isBuffering.value = false
                }

            } catch (e: Exception) {
                System.err.println("Failed to play audio: ${e.message}")
                _isPlaying.value = false
                _isBuffering.value = false
            }
        }
    }

    fun togglePlayPause() {
        Platform.runLater {
            val player = mediaPlayer ?: return@runLater
            if (player.status == MediaPlayer.Status.PLAYING) {
                player.pause()
                _isPlaying.value = false
            } else {
                player.play()
                _isPlaying.value = true
            }
        }
    }

    fun pause() {
        Platform.runLater {
            mediaPlayer?.pause()
            _isPlaying.value = false
        }
    }

    fun resume() {
        Platform.runLater {
            mediaPlayer?.play()
            _isPlaying.value = true
        }
    }

    fun seekTo(positionMs: Long) {
        Platform.runLater {
            mediaPlayer?.seek(Duration.millis(positionMs.toDouble()))
            _positionMs.value = positionMs
        }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        Platform.runLater {
            if (!_isMuted.value) {
                mediaPlayer?.volume = clamped.toDouble()
            }
        }
    }

    fun toggleMute() {
        val newMuted = !_isMuted.value
        _isMuted.value = newMuted
        Platform.runLater {
            mediaPlayer?.volume = if (newMuted) 0.0 else _volume.value.toDouble()
        }
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex in 0..9) {
            val bands = _equalizerBands.value.clone()
            bands[bandIndex] = gainDb.coerceIn(-12f, 12f)
            _equalizerBands.value = bands

            Platform.runLater {
                mediaPlayer?.let { applyEqualizerToPlayer(it) }
            }
        }
    }

    fun setEqualizerPreset(presetName: String) {
        val gains = when (presetName.lowercase()) {
            "bass boost" -> floatArrayOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f)
            "treble boost" -> floatArrayOf(0f, 0f, 0f, 0f, 1f, 2f, 4f, 6f, 7f, 8f)
            "rock" -> floatArrayOf(5f, 3f, 2f, 0f, -1f, 0f, 2f, 4f, 5f, 5f)
            "pop" -> floatArrayOf(-1f, 1f, 3f, 4f, 3f, 1f, -1f, 0f, 2f, 3f)
            "jazz" -> floatArrayOf(3f, 2f, 1f, 2f, -1f, -1f, 0f, 2f, 3f, 4f)
            "electronic" -> floatArrayOf(5f, 4f, 2f, 0f, -2f, 2f, 1f, 3f, 5f, 6f)
            "classical" -> floatArrayOf(4f, 3f, 2f, 2f, -1f, -1f, 0f, 2f, 3f, 4f)
            "vocal boost" -> floatArrayOf(-2f, -1f, 0f, 3f, 5f, 5f, 3f, 1f, 0f, -1f)
            "acoustic" -> floatArrayOf(3f, 3f, 2f, 1f, 1f, 2f, 3f, 3f, 2f, 1f)
            else -> FloatArray(10) { 0f } // Flat
        }
        _equalizerBands.value = gains
        Platform.runLater {
            mediaPlayer?.let { applyEqualizerToPlayer(it) }
        }
    }

    private fun applyEqualizerToPlayer(player: MediaPlayer) {
        val eq = player.audioEqualizer ?: return
        eq.isEnabled = _equalizerEnabled.value
        val fxBands = eq.bands
        val currentGains = _equalizerBands.value

        // Standard 10 band center frequencies (Hz): 32, 64, 125, 250, 500, 1k, 2k, 4k, 8k, 16k
        val defaultFreqs = doubleArrayOf(32.0, 64.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)

        if (fxBands.isEmpty()) {
            for (i in 0 until 10) {
                val band = EqualizerBand(defaultFreqs[i], defaultFreqs[i] * 0.7, currentGains[i].toDouble())
                fxBands.add(band)
            }
        } else {
            for (i in 0 until minOf(fxBands.size, 10)) {
                fxBands[i].gain = currentGains[i].toDouble()
            }
        }
    }

    fun release() {
        Platform.runLater {
            mediaPlayer?.stop()
            mediaPlayer?.dispose()
            mediaPlayer = null
        }
    }
}
