package com.gaminghub.musify.desktop.download

import com.gaminghub.musify.desktop.model.TrackModel
import com.gaminghub.musify.desktop.network.DesktopYouTubeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.awt.Desktop
import java.io.File
import java.io.FileOutputStream

class DesktopDownloadManager(private val youtubeRepo: DesktopYouTubeRepository) {

    val downloadDir: File by lazy {
        val userHome = System.getProperty("user.home")
        val musicDir = File(userHome, "Music/Musify")
        if (!musicDir.exists()) musicDir.mkdirs()
        musicDir
    }

    private val _downloadingTracks = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadingTracks: StateFlow<Map<String, Float>> = _downloadingTracks.asStateFlow()

    private val _downloadedTracks = MutableStateFlow<List<TrackModel>>(emptyList())
    val downloadedTracks: StateFlow<List<TrackModel>> = _downloadedTracks.asStateFlow()

    init {
        scanLocalMusic()
    }

    fun scanLocalMusic(): List<TrackModel> {
        val tracks = mutableListOf<TrackModel>()
        if (downloadDir.exists()) {
            val files = downloadDir.listFiles { file ->
                file.isFile && (file.extension.equals("m4a", ignoreCase = true) ||
                        file.extension.equals("mp3", ignoreCase = true) ||
                        file.extension.equals("wav", ignoreCase = true) ||
                        file.extension.equals("aac", ignoreCase = true))
            } ?: emptyArray()

            for (file in files) {
                val cleanName = file.nameWithoutExtension
                val parts = cleanName.split(" - ", limit = 2)
                val artist = if (parts.size > 1) parts[0].trim() else "Local Artist"
                val title = if (parts.size > 1) parts[1].trim() else cleanName.trim()

                tracks.add(
                    TrackModel(
                        title = title,
                        artist = artist,
                        audioUrl = file.absolutePath,
                        isLocal = true,
                        localFilePath = file.absolutePath
                    )
                )
            }
        }
        _downloadedTracks.value = tracks
        return tracks
    }

    suspend fun downloadTrack(track: TrackModel): Boolean = withContext(Dispatchers.IO) {
        val streamUrl = youtubeRepo.extractStreamUrl(track.audioUrl) ?: return@withContext false

        val sanitizedTitle = track.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val sanitizedArtist = track.artist.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val targetFile = File(downloadDir, "$sanitizedArtist - $sanitizedTitle.m4a")

        try {
            val progressMap = _downloadingTracks.value.toMutableMap()
            progressMap[track.audioUrl] = 0.05f
            _downloadingTracks.value = progressMap

            val request = Request.Builder().url(streamUrl).header("User-Agent", "Mozilla/5.0").build()
            val response = DesktopYouTubeRepository.httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                removeDownloadProgress(track.audioUrl)
                return@withContext false
            }

            val body = response.body ?: run {
                removeDownloadProgress(track.audioUrl)
                return@withContext false
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            val progress = (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                            val currentMap = _downloadingTracks.value.toMutableMap()
                            currentMap[track.audioUrl] = progress
                            _downloadingTracks.value = currentMap
                        }
                    }
                }
            }

            removeDownloadProgress(track.audioUrl)
            scanLocalMusic()
            true
        } catch (e: Exception) {
            System.err.println("Download failed for ${track.title}: ${e.message}")
            if (targetFile.exists()) targetFile.delete()
            removeDownloadProgress(track.audioUrl)
            false
        }
    }

    private fun removeDownloadProgress(audioUrl: String) {
        val currentMap = _downloadingTracks.value.toMutableMap()
        currentMap.remove(audioUrl)
        _downloadingTracks.value = currentMap
    }

    fun isDownloaded(track: TrackModel): Boolean {
        val sanitizedTitle = track.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val sanitizedArtist = track.artist.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val file = File(downloadDir, "$sanitizedArtist - $sanitizedTitle.m4a")
        return file.exists()
    }

    fun openDownloadsFolder() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(downloadDir)
            }
        } catch (e: Exception) {
            System.err.println("Failed to open downloads folder: ${e.message}")
        }
    }
}
