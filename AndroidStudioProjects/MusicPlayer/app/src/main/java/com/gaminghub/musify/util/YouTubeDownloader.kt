package com.gaminghub.musify.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.gaminghub.musify.TrackModel
import com.gaminghub.musicplayer.data.MusicDao
import com.gaminghub.musify.data.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class YouTubeDownloader(private val context: Context, private val dao: MusicDao) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun downloadTrack(track: TrackModel) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val playableUrl = StreamExtractionManager.extractPlayableUrl(track.audioUrl ?: return@launch, fastStart = false)
                if (playableUrl == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to get download link", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val fileName = "${track.artist} - ${track.title}.mp3".replace(Regex("[\\\\/:*?\"<>|]"), "_")
                val request = DownloadManager.Request(Uri.parse(playableUrl))
                    .setTitle("Downloading ${track.title}")
                    .setDescription(track.artist)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "Musify/$fileName")
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)

                downloadManager.enqueue(request)
                
                // Ensure track is in DB
                dao.insertTrack(track.toEntity())
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download started: ${track.title}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("YouTubeDownloader", "Download failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
