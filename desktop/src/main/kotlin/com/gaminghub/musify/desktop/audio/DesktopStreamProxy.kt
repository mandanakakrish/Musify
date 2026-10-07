package com.gaminghub.musify.desktop.audio

import com.gaminghub.musify.desktop.network.DesktopYouTubeRepository
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import okhttp3.Request
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Local HTTP Stream Proxy Server for desktop audio playback.
 *
 * Bridges JavaFX MediaPlayer and remote YouTube streams (googlevideo.com):
 * 1. JavaFX Media expects an HTTP URL ending with a recognizable audio extension (.m4a)
 *    and cannot set custom headers like User-Agent or Referer.
 * 2. googlevideo.com requires browser User-Agent headers, otherwise it rejects requests with HTTP 403 Forbidden.
 * 3. This proxy runs on localhost loopback (127.0.0.1) and handles HEAD and GET Range requests,
 *    forwarding them via OkHttp with the proper headers to YouTube and streaming bytes back to JavaFX.
 */
object DesktopStreamProxy {

    private var server: HttpServer? = null
    private var boundPort: Int = 0

    // Cache of active stream mappings: key -> remoteUrl
    private val streamMap = ConcurrentHashMap<String, String>()

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
        return "http://127.0.0.1:$port/stream.m4a?sid=$key"
    }

    private fun handleProxyRequest(exchange: HttpExchange) {
        try {
            val query = exchange.requestURI.query ?: ""
            val sid = parseQueryParam(query, "sid")
            val remoteUrl = if (sid != null) streamMap[sid] else null

            if (remoteUrl == null) {
                exchange.sendResponseHeaders(404, -1)
                exchange.close()
                return
            }

            val method = exchange.requestMethod.uppercase()
            val rangeHeader = exchange.requestHeaders.getFirst("Range")

            val reqBuilder = Request.Builder()
                .url(remoteUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Referer", "https://www.youtube.com/")

            if (rangeHeader != null) {
                reqBuilder.header("Range", rangeHeader)
            }

            val okResp = DesktopYouTubeRepository.httpClient.newCall(reqBuilder.build()).execute()
            val statusCode = okResp.code
            val contentType = okResp.header("Content-Type") ?: "audio/mp4"
            val contentLength = okResp.body?.contentLength() ?: -1L
            val contentRange = okResp.header("Content-Range")

            exchange.responseHeaders.set("Content-Type", contentType)
            exchange.responseHeaders.set("Accept-Ranges", "bytes")
            if (contentRange != null) {
                exchange.responseHeaders.set("Content-Range", contentRange)
            }

            if (method == "HEAD") {
                exchange.sendResponseHeaders(if (statusCode in 200..299) 200 else statusCode, -1)
                exchange.close()
                okResp.close()
                return
            }

            exchange.sendResponseHeaders(statusCode, if (contentLength > 0) contentLength else 0)

            val os: OutputStream = exchange.responseBody
            okResp.body?.byteStream()?.use { input ->
                val buffer = ByteArray(32 * 1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    try {
                        os.write(buffer, 0, bytesRead)
                    } catch (_: Exception) {
                        break
                    }
                }
            }
            try { os.close() } catch (_: Exception) {}
            okResp.close()
        } catch (_: Exception) {
            // Client closed stream connection
        } finally {
            try { exchange.close() } catch (_: Exception) {}
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
