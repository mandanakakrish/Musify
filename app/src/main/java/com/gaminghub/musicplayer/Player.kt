package com.gaminghub.musicplayer

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Musify home-screen widget.
 *
 * Displays the currently playing track (album art, title, artist) and
 * provides Prev / Play-Pause / Next controls that forward commands to
 * MainActivity → MusicViewModel via broadcast.
 *
 * The widget is refreshed whenever MusicViewModel calls
 * [MusicWidgetUpdater.update] (called from MainActivity on track/state changes).
 */
class Player : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, null, null, false)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_CLICK_PLAY_PAUSE, ACTION_PLAY_PAUSE -> {
                context.sendBroadcast(Intent(ACTION_PLAY_PAUSE).setPackage(context.packageName))
            }
            ACTION_CLICK_NEXT, ACTION_NEXT -> {
                context.sendBroadcast(Intent(ACTION_NEXT).setPackage(context.packageName))
            }
            ACTION_CLICK_PREV, ACTION_PREV -> {
                context.sendBroadcast(Intent(ACTION_PREV).setPackage(context.packageName))
            }
            ACTION_UPDATE_WIDGET -> {
                val title = intent.getStringExtra(EXTRA_TITLE)
                val artist = intent.getStringExtra(EXTRA_ARTIST)
                val artUrl = intent.getStringExtra(EXTRA_ART_URL)
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)

                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, Player::class.java)
                )
                if (ids.isNotEmpty()) {
                    // Load album art in background then update all widget instances
                    CoroutineScope(Dispatchers.IO).launch {
                        val bitmap = tryLoadBitmap(context, artUrl)
                        withContext(Dispatchers.Main) {
                            for (id in ids) {
                                updateWidget(context, manager, id, title, artist, isPlaying, bitmap)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_CLICK_PLAY_PAUSE = "com.gaminghub.musify.WIDGET_CLICK_PLAY_PAUSE"
        const val ACTION_CLICK_NEXT       = "com.gaminghub.musify.WIDGET_CLICK_NEXT"
        const val ACTION_CLICK_PREV       = "com.gaminghub.musify.WIDGET_CLICK_PREV"

        const val ACTION_PLAY_PAUSE = "com.gaminghub.musify.WIDGET_PLAY_PAUSE"
        const val ACTION_NEXT      = "com.gaminghub.musify.WIDGET_NEXT"
        const val ACTION_PREV      = "com.gaminghub.musify.WIDGET_PREV"
        const val ACTION_UPDATE_WIDGET = "com.gaminghub.musify.WIDGET_UPDATE"

        const val EXTRA_TITLE     = "extra_title"
        const val EXTRA_ARTIST    = "extra_artist"
        const val EXTRA_ART_URL   = "extra_art_url"
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        /** Load a URL into a Bitmap, capped to 256×256 to stay within RemoteViews limits. */
        private fun tryLoadBitmap(context: Context, url: String?): Bitmap? {
            if (url.isNullOrBlank()) return null
            return try {
                val connection = URL(url).openConnection().apply {
                    connectTimeout = 4000
                    readTimeout    = 4000
                }
                connection.getInputStream().use { stream ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeStream(stream, null, opts)
                }
            } catch (e: Exception) {
                Log.w("MusicWidget", "Failed to load album art: ${e.message}")
                null
            }
        }
    }
}

/** Build and push RemoteViews for one widget instance. */
private fun updateWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    title: String?,
    artist: String?,
    isPlaying: Boolean,
    bitmap: Bitmap? = null
) {
    val views = RemoteViews(context.packageName, R.layout.player)

    // ── Open app on root tap ──────────────────────────────────────────────
    val openApp = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    views.setOnClickPendingIntent(R.id.widget_root, openApp)
    views.setOnClickPendingIntent(R.id.widget_album_art, openApp)

    // ── Control buttons ───────────────────────────────────────────────────
    fun actionIntent(action: String, reqCode: Int): PendingIntent {
        val intent = Intent(context, Player::class.java).apply { this.action = action }
        return PendingIntent.getBroadcast(
            context, reqCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
    views.setOnClickPendingIntent(R.id.widget_btn_prev,       actionIntent(Player.ACTION_CLICK_PREV,       10))
    views.setOnClickPendingIntent(R.id.widget_btn_play_pause, actionIntent(Player.ACTION_CLICK_PLAY_PAUSE, 11))
    views.setOnClickPendingIntent(R.id.widget_btn_next,       actionIntent(Player.ACTION_CLICK_NEXT,       12))

    // ── Song info ─────────────────────────────────────────────────────────
    views.setTextViewText(R.id.widget_song_title,  title  ?: context.getString(R.string.app_name))
    views.setTextViewText(R.id.widget_song_artist, artist ?: context.getString(R.string.discover_music))

    // ── Play/Pause icon ───────────────────────────────────────────────────
    views.setImageViewResource(
        R.id.widget_btn_play_pause,
        if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
    )

    // ── Album art ─────────────────────────────────────────────────────────
    if (bitmap != null) {
        views.setImageViewBitmap(R.id.widget_album_art, bitmap)
    } else {
        views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_launcher_circle)
    }

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

/**
 * Call this from MainActivity (or MusicViewModel) whenever the current
 * track or playback state changes to push an update to all widget instances.
 *
 * Example:
 *   MusicWidgetUpdater.update(context, track?.title, track?.artist, track?.albumArtUrl, isPlaying)
 */
object MusicWidgetUpdater {
    fun update(
        context: Context,
        title: String?,
        artist: String?,
        artUrl: String?,
        isPlaying: Boolean
    ) {
        val intent = Intent(context, Player::class.java).apply {
            action = Player.ACTION_UPDATE_WIDGET
            putExtra(Player.EXTRA_TITLE,      title)
            putExtra(Player.EXTRA_ARTIST,     artist)
            putExtra(Player.EXTRA_ART_URL,    artUrl)
            putExtra(Player.EXTRA_IS_PLAYING, isPlaying)
        }
        context.sendBroadcast(intent)
    }
}