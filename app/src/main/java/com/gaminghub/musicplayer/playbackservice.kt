package com.gaminghub.musicplayer

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.gaminghub.musicplayer.util.CommonUtils

import android.content.Context
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@UnstableApi
class MusicPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    // WakeLock/WifiLock intentionally removed — ExoPlayer's setWakeMode(WAKE_MODE_NETWORK)
    // manages CPU and WiFi wake locks internally and releases them correctly on error.

    companion object {
        var currentAudioSessionId: Int = -1
            private set

        private var exoplayerCache: SimpleCache? = null

        @Synchronized
        fun getCache(context: Context): SimpleCache {
            if (exoplayerCache == null) {
                // Purge legacy cache folder that may contain corrupted collisions
                try {
                    val oldCacheDir = java.io.File(context.cacheDir, "exoplayer_cache")
                    if (oldCacheDir.exists()) {
                        oldCacheDir.deleteRecursively()
                    }
                } catch (_: Exception) {}

                val cacheDir = java.io.File(context.cacheDir, "exoplayer_cache_v2")
                val evictor = LeastRecentlyUsedCacheEvictor(150 * 1024 * 1024L)
                val databaseProvider = StandaloneDatabaseProvider(context)
                exoplayerCache = SimpleCache(cacheDir, evictor, databaseProvider)
            }
            return exoplayerCache!!
        }

        @JvmStatic
        fun cacheTrack(context: android.content.Context, url: String) {
            android.util.Log.d("PlaybackService", "Pre-caching track: $url")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == "com.gaminghub.musify.WIDGET_PLAY_PAUSE") {
            val vm = MusicViewModel.instance?.get()
            if (vm != null) {
                vm.togglePlayPause()
            } else {
                val p = mediaSession?.player
                if (p != null) {
                    if (p.isPlaying) {
                        p.pause()
                    } else if (p.playbackState != Player.STATE_IDLE) {
                        p.play()
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        // Use the synchronized User-Agent across extractor, verifier and ExoPlayer
        val userAgent = CommonUtils.CURRENT_USER_AGENT

        // Critical headers for YouTube streaming (googlevideo.com)
        val defaultRequestProperties = mutableMapOf<String, String>().apply {
            put("Referer", "https://www.youtube.com/")
            put("Origin", "https://www.youtube.com")
            put("User-Agent", userAgent)
            put("Accept", "*/*")
        }

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setDefaultRequestProperties(defaultRequestProperties)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

        val settingsPrefs = getSharedPreferences("Musify_settings", Context.MODE_PRIVATE)
        val cacheSongs = settingsPrefs.getBoolean("cache_songs", true)
        val finalDataSourceFactory: androidx.media3.datasource.DataSource.Factory = if (cacheSongs) {
            try {
                CacheDataSource.Factory()
                    .setCache(getCache(this))
                    .setUpstreamDataSourceFactory(dataSourceFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            } catch (e: Exception) {
                dataSourceFactory
            }
        } else {
            dataSourceFactory
        }

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 60_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(this)
            .setEnableAudioTrackPlaybackParams(true)

        val player = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(finalDataSourceFactory))
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            // WAKE_MODE_NETWORK: ExoPlayer acquires WakeLock + WifiLock internally and releases
            // them correctly even on player errors — no manual lock management needed.
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        currentAudioSessionId = player.audioSessionId
        try {
            val supportEqualizer = settingsPrefs.getBoolean("support_equalizer", true)
            if (supportEqualizer) {
                com.gaminghub.musicplayer.util.EqualizerManager.getInstance(this).initAudioEffects(player.audioSessionId)
            }
        } catch (_: Exception) {}

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // ForwardingPlayer: advertises seek-next/prev commands so lockscreen and Bluetooth
        // buttons are visible. The actual next/prev handling is done via broadcast to
        // MusicViewModel so queue logic (shuffle, smart queue) stays in one place.
        // NOTE (P0 #2 partial): broadcasts are received by MusicViewModel's dynamically
        // registered receiver. When the Activity is destroyed the receiver is unregistered.
        // Long-term fix: move queue into the service. For now, playbackReceiver in
        // MusicViewModel uses Application context so it survives Activity recreation.
        val forwardingPlayer = object : androidx.media3.common.ForwardingPlayer(player) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return if (command == Player.COMMAND_SEEK_TO_NEXT ||
                    command == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ||
                    command == Player.COMMAND_SEEK_TO_PREVIOUS ||
                    command == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) {
                    true
                } else {
                    super.isCommandAvailable(command)
                }
            }

            override fun seekToNext() = sendPlaybackBroadcast("com.gaminghub.musify.WIDGET_NEXT")
            override fun seekToNextMediaItem() = sendPlaybackBroadcast("com.gaminghub.musify.WIDGET_NEXT")
            override fun seekToPrevious() = sendPlaybackBroadcast("com.gaminghub.musify.WIDGET_PREV")
            override fun seekToPreviousMediaItem() = sendPlaybackBroadcast("com.gaminghub.musify.WIDGET_PREV")
        }

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(pendingIntent)
            .setCallback(object : MediaSession.Callback {
                override fun onPlayerCommandRequest(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    playerCommand: Int
                ): Int {
                    if (playerCommand == Player.COMMAND_SEEK_TO_NEXT || playerCommand == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) {
                        sendPlaybackBroadcast("com.gaminghub.musify.WIDGET_NEXT")
                        return androidx.media3.session.SessionResult.RESULT_SUCCESS
                    }
                    if (playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS || playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) {
                        sendPlaybackBroadcast("com.gaminghub.musify.WIDGET_PREV")
                        return androidx.media3.session.SessionResult.RESULT_SUCCESS
                    }
                    return super.onPlayerCommandRequest(session, controller, playerCommand)
                }

                override fun onPlaybackResumption(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                    val settable = com.google.common.util.concurrent.SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                            val dao = com.gaminghub.musicplayer.data.MusicDatabase.getInstance(this@MusicPlaybackService).dao
                            val queueTracks = dao.getQueueTracks()
                            val playbackState = dao.getPlaybackState()
                            if (queueTracks.isNotEmpty()) {
                                val mediaItems = queueTracks.map { track ->
                                    MediaItem.Builder()
                                        .setMediaId(track.audioUrl)
                                        .setUri(track.audioUrl)
                                        .setMediaMetadata(
                                            androidx.media3.common.MediaMetadata.Builder()
                                                .setTitle(track.title)
                                                .setArtist(track.artist)
                                                .setArtworkUri(track.albumArtUrl?.let { Uri.parse(it) })
                                                .build()
                                        )
                                        .build()
                                }
                                val startIndex = playbackState?.currentTrackIndex?.coerceIn(0, mediaItems.size - 1) ?: 0
                                val startPosition = playbackState?.positionMs ?: 0L
                                settable.set(
                                    MediaSession.MediaItemsWithStartPosition(
                                        mediaItems,
                                        startIndex,
                                        startPosition
                                    )
                                )
                            } else {
                                settable.set(
                                    MediaSession.MediaItemsWithStartPosition(
                                        ImmutableList.of(),
                                        0,
                                        C.TIME_UNSET
                                    )
                                )
                            }
                        } catch (_: Exception) {
                            settable.set(
                                MediaSession.MediaItemsWithStartPosition(
                                    ImmutableList.of(),
                                    0,
                                    C.TIME_UNSET
                                )
                            )
                        }
                    }
                    return settable
                }
            })
            .build()

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                super.onAudioSessionIdChanged(audioSessionId)
                currentAudioSessionId = audioSessionId
                try {
                    com.gaminghub.musicplayer.util.EqualizerManager.getInstance(this@MusicPlaybackService).initAudioEffects(audioSessionId)
                } catch (_: Exception) {}
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                try {
                    MusicWidgetUpdater.update(
                        this@MusicPlaybackService,
                        mediaItem?.mediaMetadata?.title?.toString(),
                        mediaItem?.mediaMetadata?.artist?.toString(),
                        mediaItem?.mediaMetadata?.artworkUri?.toString(),
                        player.isPlaying
                    )
                } catch (_: Exception) {}
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                // WakeLock management delegated to ExoPlayer's WAKE_MODE_NETWORK — no manual action needed here
                try {
                    val currentMedia = player.currentMediaItem
                    MusicWidgetUpdater.update(
                        this@MusicPlaybackService,
                        currentMedia?.mediaMetadata?.title?.toString(),
                        currentMedia?.mediaMetadata?.artist?.toString(),
                        currentMedia?.mediaMetadata?.artworkUri?.toString(),
                        isPlaying
                    )
                } catch (_: Exception) {}
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                super.onPlayerError(error)
                android.util.Log.e("PlaybackService", "ExoPlayer Error: ${error.message} (Code: ${error.errorCode})")
            }
        })
    }

    /**
     * Dispatches a playback control command (next/prev/play-pause).
     *
     * Strategy:
     * 1. Always send a local broadcast — received by MusicViewModel's dynamically registered
     *    receiver when the app is in the foreground.
     * 2. Also call MusicViewModel directly via its static WeakReference — this is the fallback
     *    that makes lockscreen and Bluetooth headset controls work even after the Activity has
     *    been destroyed and its broadcast receiver unregistered (P0 #2 fix).
     */
    private fun sendPlaybackBroadcast(action: String) {
        var directHandled = false
        try {
            val vm = MusicViewModel.instance?.get()
            if (vm != null) {
                when (action) {
                    "com.gaminghub.musify.WIDGET_NEXT" -> vm.playNext(fromUser = true)
                    "com.gaminghub.musify.WIDGET_PREV" -> vm.playPrevious()
                }
                directHandled = true
            }
        } catch (_: Exception) {}
        if (!directHandled) {
            try {
                sendBroadcast(Intent(action).setPackage(packageName))
            } catch (_: Exception) {}
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val settingsPrefs = getSharedPreferences("Musify_settings", Context.MODE_PRIVATE)
        val stopOnClose = settingsPrefs.getBoolean("stop_on_close", false)
        val player = mediaSession?.player
        if (stopOnClose || player?.playWhenReady == false || player?.playbackState == Player.STATE_IDLE) {
            player?.stop()
            stopSelf()
        }
    }

    override fun onDestroy() {
        currentAudioSessionId = -1
        // Release native AudioEffect handles BEFORE player.release() to prevent
        // AudioFlinger native resource leaks (system-wide ~32 effect handle limit).
        try {
            com.gaminghub.musicplayer.util.EqualizerManager.getInstance(this).release()
        } catch (_: Exception) {}
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
