package com.gaminghub.musicplayer.util

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext

/**
 * Turbo multi-part chunked audio downloader for Musify.
 * YouTube/GoogleVideo servers enforce an aggressive bitrate throttle (~16-40 KB/s) on continuous
 * full-stream GET requests, but deliver multi-part HTTP Range requests (1 MB chunks) at full,
 * unthrottled line speeds (5 to 15+ MB/s).
 */
object TurboAudioDownloader {
    private const val TAG = "TurboAudioDownloader"
    private const val CHUNK_SIZE = 1024 * 1024L // 1 MB chunks for optimal YouTube burst delivery
    private const val MAX_CONCURRENT_CHUNKS = 4

    suspend fun download(
        playUrl: String,
        destFile: File,
        client: OkHttpClient,
        onProgress: (bytesDownloaded: Long, totalBytes: Long, speedBytesPerSec: Long, percent: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val totalBytes = probeContentLength(playUrl, client)
        if (totalBytes > 0 && totalBytes > CHUNK_SIZE) {
            try {
                downloadInParallelChunks(playUrl, destFile, totalBytes, client, onProgress)
                return@withContext
            } catch (e: Exception) {
                if (coroutineContext.isActive) {
                    Log.w(TAG, "Parallel chunk download fallback due to: ${e.message}")
                } else {
                    throw e
                }
            }
        }

        // Direct sequential stream fallback if server does not support Range requests
        downloadDirectStream(playUrl, destFile, client, onProgress)
    }

    private fun probeContentLength(playUrl: String, client: OkHttpClient): Long {
        try {
            val probeRequest = Request.Builder()
                .url(playUrl)
                .header("User-Agent", CommonUtils.CURRENT_USER_AGENT)
                .header("Range", "bytes=0-0")
                .header("Accept-Encoding", "identity")
                .build()

            client.newCall(probeRequest).execute().use { response ->
                if (response.code == 206) {
                    val contentRange = response.header("Content-Range")
                    if (contentRange != null) {
                        val total = contentRange.substringAfterLast("/").trim().toLongOrNull()
                        if (total != null && total > 0) return total
                    }
                } else if (response.isSuccessful) {
                    val length = response.body?.contentLength() ?: -1L
                    if (length > 0) return length
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "probeContentLength failed: ${e.message}")
        }
        return -1L
    }

    private suspend fun downloadInParallelChunks(
        playUrl: String,
        destFile: File,
        totalBytes: Long,
        client: OkHttpClient,
        onProgress: (Long, Long, Long, Int) -> Unit
    ) = coroutineScope {
        val raf = RandomAccessFile(destFile, "rw")
        try {
            raf.setLength(totalBytes)

            val chunks = mutableListOf<LongRange>()
            var start = 0L
            while (start < totalBytes) {
                val end = minOf(start + CHUNK_SIZE - 1, totalBytes - 1)
                chunks.add(start..end)
                start = end + 1
            }

            val downloadedBytes = AtomicLong(0L)
            var lastSampleTime = System.currentTimeMillis()
            var bytesSinceLastSample = 0L
            var currentSpeed = 0L
            val progressLock = Any()

            val semaphore = Semaphore(MAX_CONCURRENT_CHUNKS)
            val writeLock = Any()

            val jobs = chunks.map { range ->
                launch {
                    semaphore.withPermit {
                        if (!isActive) return@withPermit
                        downloadChunk(playUrl, range, client, raf, writeLock) { bytesRead ->
                            val currentTotal = downloadedBytes.addAndGet(bytesRead.toLong())
                            synchronized(progressLock) {
                                bytesSinceLastSample += bytesRead
                                val now = System.currentTimeMillis()
                                val timeDiff = now - lastSampleTime
                                if (timeDiff >= 150) {
                                    currentSpeed = (bytesSinceLastSample * 1000L) / timeDiff.coerceAtLeast(1)
                                    lastSampleTime = now
                                    bytesSinceLastSample = 0L

                                    val percent = ((currentTotal * 100) / totalBytes).toInt().coerceIn(0, 100)
                                    onProgress(currentTotal, totalBytes, currentSpeed, percent)
                                }
                            }
                        }
                    }
                }
            }

            jobs.joinAll()
            onProgress(totalBytes, totalBytes, currentSpeed, 100)
        } finally {
            try { raf.close() } catch (_: Exception) {}
        }
    }

    private fun downloadChunk(
        playUrl: String,
        range: LongRange,
        client: OkHttpClient,
        raf: RandomAccessFile,
        writeLock: Any,
        onBytesRead: (Int) -> Unit
    ) {
        var attempt = 0
        while (attempt < 3) {
            attempt++
            try {
                val request = Request.Builder()
                    .url(playUrl)
                    .header("User-Agent", CommonUtils.CURRENT_USER_AGENT)
                    .header("Range", "bytes=${range.first}-${range.last}")
                    .header("Accept-Encoding", "identity")
                    .header("Connection", "keep-alive")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful && response.code != 206) {
                        throw Exception("HTTP ${response.code} for range $range")
                    }

                    val body = response.body ?: throw Exception("Empty body for range $range")
                    val buffer = ByteArray(65536) // 64 KB
                    var writeOffset = range.first
                    body.byteStream().use { input ->
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            synchronized(writeLock) {
                                raf.seek(writeOffset)
                                raf.write(buffer, 0, read)
                            }
                            writeOffset += read
                            onBytesRead(read)
                        }
                    }
                }
                return
            } catch (e: Exception) {
                if (attempt >= 3) throw e
                Thread.sleep(150L * attempt)
            }
        }
    }

    private fun downloadDirectStream(
        playUrl: String,
        destFile: File,
        client: OkHttpClient,
        onProgress: (Long, Long, Long, Int) -> Unit
    ) {
        val request = Request.Builder()
            .url(playUrl)
            .header("User-Agent", CommonUtils.CURRENT_USER_AGENT)
            .header("Accept-Encoding", "identity")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }
            val body = response.body ?: throw Exception("Empty response body")
            val contentLength = body.contentLength()
            val buffer = ByteArray(131072) // 128 KB
            var totalDownloaded = 0L
            var lastSampleTime = System.currentTimeMillis()
            var bytesSinceLastSample = 0L
            var currentSpeed = 0L

            body.byteStream().buffered(131072).use { input ->
                java.io.BufferedOutputStream(destFile.outputStream(), 131072).use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead
                        bytesSinceLastSample += bytesRead

                        val now = System.currentTimeMillis()
                        val timeDiff = now - lastSampleTime
                        if (timeDiff >= 200) {
                            currentSpeed = (bytesSinceLastSample * 1000L) / timeDiff.coerceAtLeast(1)
                            lastSampleTime = now
                            bytesSinceLastSample = 0L

                            val percent = if (contentLength > 0) {
                                ((totalDownloaded * 100) / contentLength).toInt().coerceIn(0, 100)
                            } else 0
                            onProgress(totalDownloaded, contentLength, currentSpeed, percent)
                        }
                    }
                    output.flush()
                }
            }
        }
    }
}
