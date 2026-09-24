package com.gaminghub.musicplayer.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.gaminghub.musicplayer.TrackModel

object LinkHelper {

    private val YOUTUBE_REGEX = Regex("(?:v=|/v/|youtu\\.be/|/embed/|/shorts/|watch\\?v%3D|watch\\?v=)([a-zA-Z0-9_-]{11})")

    /**
     * Extracts an 11-character YouTube video ID from a URL, URI string, or mixed text snippet.
     */
    fun extractVideoId(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val trimmed = text.trim()
        if (trimmed.length == 11 && !trimmed.contains("/") && !trimmed.contains(" ") && !trimmed.contains(".")) {
            return trimmed
        }
        val match = YOUTUBE_REGEX.find(trimmed)
        return match?.groupValues?.get(1)?.takeIf { it.length == 11 }
    }

    /**
     * Generates an official, clickable Musify App Link (verified domain).
     * Recognized by all messaging apps (WhatsApp, Telegram, SMS) as an active hyperlink
     * and automatically opens directly inside the Musify Android app.
     */
    fun generateSongUrl(track: TrackModel): String {
        val videoId = extractVideoId(track.audioUrl)
        return if (!videoId.isNullOrBlank()) {
            val encodedTitle = java.net.URLEncoder.encode(track.title.ifBlank { "Song" }, "UTF-8")
            val encodedArtist = java.net.URLEncoder.encode(track.artist.ifBlank { "Musify" }, "UTF-8")
            "https://musify-8df01.web.app/watch?v=$videoId&title=$encodedTitle&artist=$encodedArtist"
        } else if (track.audioUrl?.startsWith("http://") == true || track.audioUrl?.startsWith("https://") == true) {
            track.audioUrl
        } else {
            "https://musify-8df01.web.app"
        }
    }

    /**
     * Formats clean, professional share text containing the clickable link and app info.
     */
    fun generateShareText(track: TrackModel): String {
        val songUrl = generateSongUrl(track)
        val title = track.title.takeIf { it.isNotBlank() } ?: "Song"
        val artist = track.artist.takeIf { it.isNotBlank() && it != "Unknown" }
        val videoId = extractVideoId(track.audioUrl)

        return buildString {
            append("🎵 Listening to \"$title\"")
            if (artist != null) {
                append(" by $artist")
            }
            append(" on Musify! 🎧\n\n")

            append("▶️ Open in Musify:\n")
            append(songUrl)
            append("\n\n")

            if (!videoId.isNullOrBlank()) {
                append("App Deep Link: musify://play?id=$videoId\n\n")
            }

            append("Experience ad-free lossless music with Musify:\nhttps://github.com/mandanakakrish/Music-Player")
        }
    }

    /**
     * Copies the clickable HTTPS song link directly to the user's clipboard and provides visual feedback.
     */
    fun copySongLink(context: Context, track: TrackModel) {
        val url = generateSongUrl(track)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            val clip = ClipData.newPlainText("Song Link", url)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Link copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to access clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Launches the system share chooser with the formatted message and clickable URL.
     */
    fun shareSong(context: Context, track: TrackModel) {
        val shareText = generateShareText(track)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "🎵 ${track.title} - ${track.artist} (Musify)")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    /**
     * Shares a playlist with formatted track listing and app link.
     */
    fun sharePlaylist(context: Context, playlistName: String, tracks: List<TrackModel> = emptyList()) {
        val shareText = buildString {
            append("🎶 Check out my playlist \"$playlistName\" on Musify!\n\n")
            if (tracks.isNotEmpty()) {
                val previewTracks = tracks.take(5)
                previewTracks.forEachIndexed { i, t ->
                    append("${i + 1}. ${t.title} - ${t.artist}\n")
                }
                if (tracks.size > 5) {
                    append("...and ${tracks.size - 5} more tracks!\n")
                }
                append("\n")
            }
            append("Stream ad-free lossless music with Musify:\nhttps://github.com/mandanakakrish/Music-Player")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "🎶 $playlistName (Musify)")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Playlist"))
    }
}
