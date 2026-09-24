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
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import android.net.wifi.WifiManager
import android.os.PowerManager
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider

@UnstableApi
class MusicPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    companion object {
        var currentAudioSessionId: Int = -1
            private set

        private var exoplayerCache: SimpleCache? = null

        @Synchronized
        fun getCache(context: Context): SimpleCache {
            if (exoplayerCache == null) {
                val cacheDir = java.io.File(context.cacheDir, "exoplayer_cache")
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
        return START_STICKY
    }

    private fun acquireWakeLocks(timeoutMs: Long = 10 * 60_000L) {
        try {
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(timeoutMs)
            }
            if (wifiLock?.isHeld != true) {
                wifiLock?.acquire()
            }
        } catch (_: Exception) {}
    }

    private fun releaseWakeLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
        } catch (_: Exception) {}
    }

    override fun onCreate() {
        super.onCreate()

        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Musify::PlaybackWakeLock")?.apply {
                setReferenceCounted(false)
            }
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiLock = wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Musify::PlaybackWifiLock")?.apply {
                setReferenceCounted(false)
            }
        } catch (e: Exception) {
            android.util.Log.w("PlaybackService", "Could not initialize wake locks: ${e.message}")
        }
        
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
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        currentAudioSessionId = player.audioSessionId
        try {
            val settingsPrefs = getSharedPreferences("Musify_settings", Context.MODE_PRIVATE)
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

            override fun seekToNext() {
                val intent = Intent("com.gaminghub.musify.WIDGET_NEXT").setPackage(packageName)
                sendBroadcast(intent)
            }

            override fun seekToNextMediaItem() {
                val intent = Intent("com.gaminghub.musify.WIDGET_NEXT").setPackage(packageName)
                sendBroadcast(intent)
            }

            override fun seekToPrevious() {
                val intent = Intent("com.gaminghub.musify.WIDGET_PREV").setPackage(packageName)
                sendBroadcast(intent)
            }

            override fun seekToPreviousMediaItem() {
                val intent = Intent("com.gaminghub.musify.WIDGET_PREV").setPackage(packageName)
                sendBroadcast(intent)
            }
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
                        val intent = Intent("com.gaminghub.musify.WIDGET_NEXT").setPackage(packageName)
                        sendBroadcast(intent)
                        return androidx.media3.session.SessionResult.RESULT_SUCCESS
                    }
                    if (playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS || playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) {
                        val intent = Intent("com.gaminghub.musify.WIDGET_PREV").setPackage(packageName)
                        sendBroadcast(intent)
                        return androidx.media3.session.SessionResult.RESULT_SUCCESS
                    }
                    return super.onPlayerCommandRequest(session, controller, playerCommand)
                }

                override fun onPlaybackResumption(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                    return Futures.immediateFuture(
                        MediaSession.MediaItemsWithStartPosition(
                            ImmutableList.of(),
                            0,
                            C.TIME_UNSET
                        )
                    )
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
                if (isPlaying) {
                    acquireWakeLocks()
                } else if (player.playbackState != Player.STATE_BUFFERING && player.playbackState != Player.STATE_ENDED) {
                    releaseWakeLocks()
                }
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

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                when (playbackState) {
                    Player.STATE_BUFFERING, Player.STATE_READY -> {
                        if (player.playWhenReady) {
                            acquireWakeLocks()
                        }
                    }
                    Player.STATE_ENDED -> {
                        // Crucial: hold transition wakelock so CPU and network do not sleep while next song extracts
                        acquireWakeLocks(60_000L)
                    }
                    Player.STATE_IDLE -> {
                        if (!player.playWhenReady) {
                            releaseWakeLocks()
                        }
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                super.onPlayerError(error)
                android.util.Log.e("PlaybackService", "ExoPlayer Error: ${error.message} (Code: ${error.errorCode})")
            }
        })

        try {
            val filter = IntentFilter("com.gaminghub.musify.WIDGET_PLAY_PAUSE")
            ContextCompat.registerReceiver(this, widgetReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } catch (_: Exception) {}
    }

    private val widgetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.gaminghub.musify.WIDGET_PLAY_PAUSE") {
                val p = mediaSession?.player ?: return
                if (p.isPlaying) {
                    p.pause()
                } else if (p.playbackState != Player.STATE_IDLE) {
                    p.play()
                }
            }
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
        try {
            unregisterReceiver(widgetReceiver)
        } catch (_: Exception) {}
        currentAudioSessionId = -1
        releaseWakeLocks()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
