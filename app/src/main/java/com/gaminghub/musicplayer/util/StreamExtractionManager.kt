package com.gaminghub.musicplayer.util

import android.util.Log
import kotlinx.coroutines.*
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import java.util.concurrent.ConcurrentHashMap
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import com.gaminghub.musicplayer.data.MusicDao

/**
 * Singleton manager to handle background stream extraction and caching.
 */
object StreamExtractionManager {
    private const val TAG = "StreamExtractionManager"
    
    private val streamUrlCache = ConcurrentHashMap<String, String>()
    private val activeExtractions = ConcurrentHashMap<String, Job>()
    private val extractionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var musicDao: MusicDao? = null
    private val defaultClient by lazy { OkHttpClient.Builder().build() }
    private var playbackClient: OkHttpClient? = null
    private var appContext: android.content.Context? = null

    fun init(dao: MusicDao, client: OkHttpClient, context: android.content.Context? = null) {
        musicDao = dao
        playbackClient = client
        if (context != null) appContext = context.applicationContext
    }

    fun setContext(context: android.content.Context) {
        appContext = context.applicationContext
    }

    private fun getTargetBitrate(): Int {
        val ctx = appContext ?: return 160_000
        val cm = ctx.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val isWifi = cm?.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true
        val prefs = ctx.getSharedPreferences("Musify_settings", android.content.Context.MODE_PRIVATE)

        val qualityStr = if (isWifi) {
            prefs.getString("wifi_streaming_quality", "320 kbps") ?: "320 kbps"
        } else {
            prefs.getString("streaming_quality", "160 kbps") ?: "160 kbps"
        }

        return when {
            qualityStr.contains("96") -> 96_000
            qualityStr.contains("320") -> 320_000
            else -> 160_000
        }
    }

    suspend fun extractPlayableUrl(url: String, fastStart: Boolean = true, forceRefresh: Boolean = false): String? {
        if (!url.contains("youtube.com") && !url.contains("youtu.be")) return url
        
        if (forceRefresh) streamUrlCache.remove(url)

        val cached = streamUrlCache[url]
        if (cached != null && !isUrlExpired(cached)) {
            if (fastStart || verifyUrl(cached)) return cached
            else streamUrlCache.remove(url)
        }

        val now = System.currentTimeMillis()
        val persistentCachedUrl = musicDao?.getValidCachedUrl(url, now)
        if (persistentCachedUrl != null && !isUrlExpired(persistentCachedUrl)) {
             if (fastStart || verifyUrl(persistentCachedUrl)) {
                 streamUrlCache[url] = persistentCachedUrl
                 return persistentCachedUrl
             }
        }
        
        val currentJob = currentCoroutineContext()[Job]
        val existingJob = activeExtractions[url]
        if (existingJob != null && existingJob !== currentJob) {
            try {
                withTimeout(12_000L) {
                    existingJob.join()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Wait for extraction of $url ended: ${e.message}")
            }
        }
        
        val stillCached = streamUrlCache[url]
        if (stillCached != null && !isUrlExpired(stillCached)) {
            if (fastStart || verifyUrl(stillCached)) return stillCached
        }

        if (currentJob != null) {
            activeExtractions.putIfAbsent(url, currentJob)
        }
        try {
            var retries = 2
            while (retries > 0) {
                try {
                    val service = ServiceList.YouTube
                    val extractor = service.getStreamExtractor(url)
                    withContext(Dispatchers.IO) {
                        withTimeout(12_000L) {
                            extractor.fetchPage()
                        }
                    }
                    
                    val audioStreams = extractor.audioStreams
                    if (!audioStreams.isNullOrEmpty()) {
                        val targetBitrate = getTargetBitrate()
                        val candidateStreams = if (targetBitrate <= 96_000) {
                            audioStreams.sortedWith(
                                compareByDescending<AudioStream> { it.format == org.schabi.newpipe.extractor.MediaFormat.M4A }
                                    .thenBy { kotlin.math.abs(it.bitrate - 96_000) }
                            )
                        } else if (targetBitrate >= 320_000) {
                            audioStreams.sortedWith(
                                compareByDescending<AudioStream> { it.format == org.schabi.newpipe.extractor.MediaFormat.M4A }
                                    .thenByDescending { it.bitrate }
                            )
                        } else {
                            audioStreams.sortedWith(
                                compareByDescending<AudioStream> { it.format == org.schabi.newpipe.extractor.MediaFormat.M4A }
                                    .thenBy { kotlin.math.abs(it.bitrate - 160_000) }
                            )
                        }
                        
                        for (candidate in candidateStreams) {
                            val candidateUrl = candidate.url ?: continue
                            if (candidateUrl.isNotBlank()) {
                                streamUrlCache[url] = candidateUrl
                                musicDao?.updateCachedUrl(
                                    url = url,
                                    playableUrl = candidateUrl,
                                    expiry = getExpirationTimestamp(candidateUrl)
                                )
                                Log.d(TAG, "Extracted audio stream (${candidate.format}, ${candidate.bitrate}bps) for $url")
                                return candidateUrl
                            }
                        }
                    }
                    retries--
                } catch (e: Exception) {
                    Log.e(TAG, "NewPipe Extraction attempt failed for $url: ${e.message}")
                    retries--
                }
            }

            val videoId = extractVideoId(url)
            if (videoId != null) {
                val embedUrl = extractViaYouTubeEmbed(videoId)
                if (embedUrl != null) {
                    streamUrlCache[url] = embedUrl
                    musicDao?.updateCachedUrl(
                        url = url,
                        playableUrl = embedUrl,
                        expiry = getExpirationTimestamp(embedUrl)
                    )
                    return embedUrl
                }

                val ytPlayerUrl = extractViaYouTubePlayerApi(videoId)
                if (ytPlayerUrl != null) {
                    streamUrlCache[url] = ytPlayerUrl
                    musicDao?.updateCachedUrl(
                        url = url,
                        playableUrl = ytPlayerUrl,
                        expiry = getExpirationTimestamp(ytPlayerUrl)
                    )
                    return ytPlayerUrl
                }
            }
            
            // Only return a cached URL if it hasn't expired yet. Passing 0 would return any
            // cached URL including expired ones, causing HTTP 403 → retry loops.
            return musicDao?.getValidCachedUrl(url, System.currentTimeMillis())
        } finally {
            if (currentJob != null && activeExtractions[url] === currentJob) {
                activeExtractions.remove(url)
            }
        }
    }

    private fun extractVideoId(url: String): String? {
        if (url.length == 11 && !url.contains("/")) return url
        val regex = Regex("(?:v=|/v/|youtu\\.be/|/embed/|/shorts/|watch\\?v%3D|watch\\?v=)([^#&?]*)" )
        val match = regex.find(url)
        return match?.groupValues?.get(1)?.takeIf { it.length == 11 }
    }

    private suspend fun extractViaYouTubeEmbed(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            val embedUrl = "https://www.youtube.com/embed/$videoId"
            val request = okhttp3.Request.Builder()
                .url(embedUrl)
                .header("User-Agent", CommonUtils.CURRENT_USER_AGENT)
                .build()

            val client = playbackClient ?: defaultClient
            val html = client.newCall(request).execute().use { it.body.string() }
            val regex = Regex("ytInitialPlayerResponse\\s*=\\s*(\\{.*?\\});")
            val match = regex.find(html)
            val jsonStr = match?.groupValues?.get(1) ?: return@withContext null

            val root = org.json.JSONObject(jsonStr)
            val streamingData = root.optJSONObject("streamingData") ?: return@withContext null
            val formats = streamingData.optJSONArray("adaptiveFormats") ?: return@withContext null

            var bestUrl: String? = null
            var maxBitrate = 0

            for (i in 0 until formats.length()) {
                val format = formats.getJSONObject(i)
                val mimeType = format.optString("mimeType")
                if (mimeType.contains("audio")) {
                    val directUrl = format.optString("url")
                    val bitrate = format.optInt("bitrate", 0)
                    if (directUrl.isNotBlank() && bitrate > maxBitrate) {
                        maxBitrate = bitrate
                        bestUrl = directUrl
                    }
                }
            }
            if (bestUrl != null) {
                Log.d(TAG, "Successfully extracted direct audio stream via YouTube Embed page")
            }
            return@withContext bestUrl
        } catch (e: Exception) {
            Log.e(TAG, "YouTube Embed extraction error: ${e.message}")
            return@withContext null
        }
    }

    private suspend fun extractViaYouTubePlayerApi(videoId: String): String? = withContext(Dispatchers.IO) {
        val clients = listOf(
            Pair("ANDROID_VR", "1.54.1"),
            Pair("ANDROID_MUSIC", "7.02.51"),
            Pair("ANDROID_TESTSUITE", "1.9"),
            Pair("TVHTML5", "7.20230405.00.00"),
            Pair("IOS", "19.45.4"),
            Pair("ANDROID", "19.45.36")
        )

        val client = playbackClient ?: defaultClient

        for ((clientName, clientVersion) in clients) {
            try {
                val jsonBody = """
                    {
                        "videoId": "$videoId",
                        "context": {
                            "client": {
                                "clientName": "$clientName",
                                "clientVersion": "$clientVersion",
                                "hl": "en",
                                "gl": "US"
                            }
                        }
                    }
                """.trimIndent()

                val request = okhttp3.Request.Builder()
                    .url("https://www.youtube.com/youtubei/v1/player")
                    .header("User-Agent", CommonUtils.CURRENT_USER_AGENT)
                    .header("Content-Type", "application/json")
                    .post(jsonBody.toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                val responseBody = client.newCall(request).execute().use { it.body.string() }
                val root = org.json.JSONObject(responseBody)
                val streamingData = root.optJSONObject("streamingData") ?: continue
                val formats = streamingData.optJSONArray("adaptiveFormats") ?: continue

                var bestAudioUrl: String? = null
                var maxBitrate = 0

                for (i in 0 until formats.length()) {
                    val format = formats.getJSONObject(i)
                    val mimeType = format.optString("mimeType")
                    if (mimeType.contains("audio")) {
                        var directUrl = format.optString("url")
                        
                        if (directUrl.isBlank()) {
                            val cipher = format.optString("signatureCipher").takeIf { it.isNotBlank() }
                                ?: format.optString("cipher").takeIf { it.isNotBlank() }
                            if (cipher != null) {
                                val params = cipher.split("&").associate {
                                    val pair = it.split("=")
                                    if (pair.size == 2) pair[0] to java.net.URLDecoder.decode(pair[1], "UTF-8") else "" to ""
                                }
                                if (params.containsKey("url") && !params.containsKey("s")) {
                                    directUrl = params["url"] ?: ""
                                }
                            }
                        }

                        val bitrate = format.optInt("bitrate", 0)
                        if (directUrl.isNotBlank() && bitrate > maxBitrate) {
                            maxBitrate = bitrate
                            bestAudioUrl = directUrl
                        }
                    }
                }

                if (bestAudioUrl != null) {
                    Log.d(TAG, "Successfully extracted direct audio stream via client: $clientName")
                    return@withContext bestAudioUrl
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting via InnerTube client $clientName: ${e.message}")
            }
        }
        return@withContext null
    }

    private fun getExpirationTimestamp(streamUrl: String): Long {
        try {
            val uri = android.net.Uri.parse(streamUrl)
            val expireParam = uri.getQueryParameter("expire")
            if (expireParam != null) {
                return expireParam.toLong() * 1000
            }
        } catch (e: Exception) {}
        return System.currentTimeMillis() + (4 * 3600 * 1000)
    }

    private suspend fun verifyUrl(streamUrl: String): Boolean = withContext(Dispatchers.IO) {
        if (streamUrl.contains("googlevideo.com")) {
            try {
                val client = playbackClient ?: defaultClient
                val request = okhttp3.Request.Builder()
                    .url(streamUrl)
                    .header("User-Agent", CommonUtils.CURRENT_USER_AGENT)
                    .header("Referer", "https://www.youtube.com/")
                    .header("Origin", "https://www.youtube.com")
                    // Range request: only fetch first 1 KB instead of the entire audio stream.
                    // Returns HTTP 206 Partial Content on success (valid URL) or 403/410 on expiry.
                    .header("Range", "bytes=0-1023")
                    .build()

                client.newCall(request).execute().use { response ->
                    Log.d(TAG, "Stream verification response code: ${response.code} for $streamUrl")
                    return@withContext response.code != 403 && response.code != 410
                }
            } catch (e: Exception) {
                Log.e(TAG, "Verification exception for $streamUrl: ${e.message}")
                return@withContext true
            }
        }
        return@withContext true
    }

    private fun isUrlExpired(streamUrl: String): Boolean {
        if (streamUrl.isBlank()) return true
        try {
            val uri = android.net.Uri.parse(streamUrl)
            val expireParam = uri.getQueryParameter("expire")
            if (expireParam != null) {
                val expireTimeSec = expireParam.toLong()
                val currentTimeSec = System.currentTimeMillis() / 1000
                return (currentTimeSec + 900) >= expireTimeSec
            }
        } catch (e: Exception) {
            return streamUrl.length < 20 
        }
        return false
    }

    fun preExtract(url: String) {
        if (url.isBlank() || streamUrlCache.containsKey(url)) return
        if (activeExtractions.containsKey(url)) return

        val job = extractionScope.launch {
            try {
                extractPlayableUrl(url, fastStart = false)
            } catch (e: Exception) {
                Log.w(TAG, "preExtract failed for $url: ${e.message}")
            } finally {
                activeExtractions.remove(url)
            }
        }
        activeExtractions[url] = job
    }

    fun getCachedUrl(url: String): String? = streamUrlCache[url]

    fun trimCache() {
        if (streamUrlCache.size > 100) {
            streamUrlCache.clear()
        }
    }
}
