package com.gaminghub.musify.desktop.audio

import com.gaminghub.musify.desktop.network.DesktopYouTubeRepository
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Local HTTP Stream Proxy Server for Desktop Audio Playback.
 *
 * Bridges JavaFX MediaPlayer / Windows Media Foundation and remote YouTube streams:
 * 1. JavaFX Media on Windows requires:
 *    - Valid Content-Length in HEAD responses
 *    - Byte-range (HTTP 206 Partial Content) support
 *    - Recognized audio extension (.m4a)
 *    - Browser User-Agent headers (googlevideo.com rejects without it with 403 Forbidden).
 * 2. This proxy runs on localhost loopback (127.0.0.1) and handles HEAD and GET Range requests.
 * 3. Includes persistent disk caching in ~/.musify/cache/ so played tracks can be served locally
 *    with zero latency and 100% reliable random seeking.
 */
object DesktopStreamProxy {

    private var server: HttpServer? = null
    private var boundPort: Int = 0

    // Cache of active stream mappings: key -> remoteUrl
    private val streamMap = ConcurrentHashMap<String, String>()

    val cacheDir: File by lazy {
        val userHome = System.getProperty("user.home")
        val dir = File(userHome, ".musify/cache")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    @Synchronized
    fun ensureStarted(): Int {
        if (server != null && boundPort > 0) return boundPort

        try {
            val http = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            http.executor = Executors.newCachedThreadPool { runnable ->
                Thread(runnable, "Musify-StreamProxy-Worker").apply { isDaemon = true }
            }

            http.createContext("/stream.m4a", object : HttpHandler {
                override fun handle(exchange: HttpExchange) {
                    handleProxyRequest(exchange)
                }
            })

            http.start()
            server = http
            boundPort = http.address.port
            println("Musify DesktopStreamProxy active at http://127.0.0.1:$boundPort/stream.m4a")
        } catch (e: Exception) {
            System.err.println("Failed to start DesktopStreamProxy: ${e.message}")
        }

        return boundPort
    }

    fun getProxyUrl(remoteUrl: String): String {
        val port = ensureStarted()
        val key = java.lang.Integer.toHexString(remoteUrl.hashCode())
        streamMap[key] = remoteUrl

        // If track is already fully cached on disk, return proxy URL pointing to local cache
        return "http://127.0.0.1:$port/stream.m4a?sid=$key"
    }

    private fun handleProxyRequest(exchange: HttpExchange) {
        val method = exchange.requestMethod.uppercase()
        val rangeHeader = exchange.requestHeaders.getFirst("Range")

        try {
            val query = exchange.requestURI.query ?: ""
            val sid = parseQueryParam(query, "sid")
            val remoteUrl = if (sid != null) streamMap[sid] else null

            if (sid == null || remoteUrl == null) {
                exchange.sendResponseHeaders(404, -1)
                exchange.close()
                return
            }

            // Check if we have a complete local cached copy
            val cachedFile = File(cacheDir, "$sid.m4a")
            if (cachedFile.exists() && cachedFile.length() > 64 * 1024) {
                serveFromLocalCache(exchange, cachedFile, method, rangeHeader)
                return
            }

            // Otherwise, proxy from remote YouTube stream
            serveFromRemote(exchange, remoteUrl, sid, method, rangeHeader)
        } catch (_: Exception) {
            // Client closed stream connection during playback / seek
        } finally {
            try { exchange.close() } catch (_: Exception) {}
        }
    }

    private fun serveFromLocalCache(
        exchange: HttpExchange,
        file: File,
        method: String,
        rangeHeader: String?
    ) {
        val fileLength = file.length()
        exchange.responseHeaders.set("Content-Type", "audio/mp4")
        exchange.responseHeaders.set("Accept-Ranges", "bytes")

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            // Range request: bytes=start-end or bytes=start-
            val rangeVal = rangeHeader.substringAfter("bytes=").trim()
            val parts = rangeVal.split("-")
            val start = parts[0].toLongOrNull() ?: 0L
            val end = if (parts.size > 1 && parts[1].isNotBlank()) {
                parts[1].toLongOrNull() ?: (fileLength - 1L)
            } else {
                fileLength - 1L
            }.coerceAtMost(fileLength - 1L)

            val contentLength = (end - start + 1L).coerceAtLeast(0L)

            exchange.responseHeaders.set("Content-Range", "bytes $start-$end/$fileLength")
            exchange.responseHeaders.set("Content-Length", contentLength.toString())

            if (method == "HEAD") {
                exchange.sendResponseHeaders(206, -1)
                exchange.close()
                return
            }

            exchange.sendResponseHeaders(206, contentLength)
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(start)
                val buffer = ByteArray(32 * 1024)
                var remaining = contentLength
                val os = exchange.responseBody
                while (remaining > 0) {
                    val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                    val read = raf.read(buffer, 0, toRead)
                    if (read == -1) break
                    os.write(buffer, 0, read)
                    remaining -= read
                }
            }
        } else {
            // Full file request
            exchange.responseHeaders.set("Content-Length", fileLength.toString())

            if (method == "HEAD") {
                exchange.sendResponseHeaders(200, -1)
                exchange.close()
                return
            }

            exchange.sendResponseHeaders(200, fileLength)
            val os = exchange.responseBody
            file.inputStream().use { fis ->
                val buffer = ByteArray(32 * 1024)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    os.write(buffer, 0, read)
                }
            }
        }
    }

    private fun serveFromRemote(
        exchange: HttpExchange,
        remoteUrl: String,
        sid: String,
        method: String,
        rangeHeader: String?
    ) {
        val reqBuilder = Request.Builder()
            .url(remoteUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Referer", "https://www.youtube.com/")

        if (rangeHeader != null) {
            reqBuilder.header("Range", rangeHeader)
        }

        if (method == "HEAD") {
            reqBuilder.head()
        }

        val okResp = DesktopYouTubeRepository.httpClient.newCall(reqBuilder.build()).execute()
        val statusCode = okResp.code
        val contentType = okResp.header("Content-Type") ?: "audio/mp4"

        // For HEAD requests, OkHttp's body.contentLength() is 0, so header must be parsed directly!
        val contentLength = okResp.header("Content-Length")?.toLongOrNull()
            ?: okResp.body?.contentLength()
            ?: -1L

        val contentRange = okResp.header("Content-Range")

        exchange.responseHeaders.set("Content-Type", contentType)
        exchange.responseHeaders.set("Accept-Ranges", "bytes")
        if (contentRange != null) {
            exchange.responseHeaders.set("Content-Range", contentRange)
        }
        if (contentLength > 0) {
            exchange.responseHeaders.set("Content-Length", contentLength.toString())
        }

        if (method == "HEAD") {
            exchange.sendResponseHeaders(if (statusCode in 200..299) 200 else statusCode, -1)
            exchange.close()
            okResp.close()
            return
        }

        exchange.sendResponseHeaders(statusCode, if (contentLength > 0) contentLength else 0)

        val os: OutputStream = exchange.responseBody
        val isFirstRangeOrFull = rangeHeader == null || rangeHeader.startsWith("bytes=0-")
        val partFile = if (isFirstRangeOrFull && contentLength > 100_000L) File(cacheDir, "$sid.part") else null
        var partFos: FileOutputStream? = null

        if (partFile != null) {
            try { partFos = FileOutputStream(partFile) } catch (_: Exception) {}
        }

        var totalBytesWritten = 0L

        okResp.body?.byteStream()?.use { input ->
            val buffer = ByteArray(32 * 1024)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                try {
                    os.write(buffer, 0, bytesRead)
                    totalBytesWritten += bytesRead
                    partFos?.write(buffer, 0, bytesRead)
                } catch (_: Exception) {
                    break
                }
            }
        }

        try { partFos?.close() } catch (_: Exception) {}
        try { os.close() } catch (_: Exception) {}
        okResp.close()

        // If full track was successfully streamed, finalize cache file
        if (partFile != null && totalBytesWritten > 0 && totalBytesWritten == contentLength) {
            val targetFile = File(cacheDir, "$sid.m4a")
            partFile.renameTo(targetFile)
        }
    }

    private fun parseQueryParam(query: String, key: String): String? {
        val pairs = query.split("&")
        for (pair in pairs) {
            val parts = pair.split("=", limit = 2)
            if (parts.size == 2 && parts[0] == key) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name())
            }
        }
        return null
    }

    fun stop() {
        try {
            server?.stop(0)
            server = null
            boundPort = 0
            streamMap.clear()
        } catch (_: Exception) {}
    }
}
