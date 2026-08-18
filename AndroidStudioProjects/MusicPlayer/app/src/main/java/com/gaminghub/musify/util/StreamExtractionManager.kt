package com.gaminghub.musify.util

import android.util.Log
import kotlinx.coroutines.*
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import java.util.concurrent.ConcurrentHashMap
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import com.gaminghub.musify.Musify

/**
 * Singleton manager to handle background stream extraction and caching.
 * Centralizing this allows multiple ViewModels to trigger pre-extraction
 * and share the same cache for a smoother user experience.
 */
object StreamExtractionManager {
    private const val TAG = "StreamExtractionManager"
    
    // Concurrent cache for stream URLs
    private val streamUrlCache = ConcurrentHashMap<String, String>()
    
    // Track extraction jobs to avoid redundant work
    private val activeExtractions = ConcurrentHashMap<String, Job>()
    
    private val extractionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var musicDao: com.gaminghub.musicplayer.data.MusicDao? = null
    private var playbackClient: okhttp3.OkHttpClient? = null

    /**
     * Initializes the manager with a DAO and the synchronized playback client.
     */
    fun init(dao: com.gaminghub.musicplayer.data.MusicDao, client: okhttp3.OkHttpClient) {
        musicDao = dao
        playbackClient = client
    }

    /**
     * Extracts a playable URL for a given YouTube URL.
     * Uses caching and retries for robustness.
     */
    suspend fun extractPlayableUrl(url: String, fastStart: Boolean = true, forceRefresh: Boolean = false): String? {
        if (!url.contains("youtube.com") && !url.contains("youtu.be")) return url
        
        // Clear if refreshing
        if (forceRefresh) streamUrlCache.remove(url)

        // 1. Immediate memory cache check
        val cached = streamUrlCache[url]
        if (cached != null && !isUrlExpired(cached)) {
            if (verifyUrl(cached)) return cached
            else streamUrlCache.remove(url)
        }

        // 2. Persistent Database Check (Offline Fallback)
        val now = System.currentTimeMillis()
        val persistentCachedUrl = musicDao?.getValidCachedUrl(url, now)
        if (persistentCachedUrl != null && !isUrlExpired(persistentCachedUrl)) {
             if (verifyUrl(persistentCachedUrl)) {
                 streamUrlCache[url] = persistentCachedUrl
                 return persistentCachedUrl
             }
        }
        
        // Wait if an extraction is already in progress
        activeExtractions[url]?.join()
        
        val stillCached = streamUrlCache[url]
        if (stillCached != null && !isUrlExpired(stillCached)) {
            if (verifyUrl(stillCached)) return stillCached
        }

        // 3. Primary: NewPipe Extractor (High performance, signature deciphering & direct CDN streams)
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
                    val candidateStreams = audioStreams.sortedByDescending { it.bitrate }
                    
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

        // 4. Secondary Fallback: YouTube Embed & Player API
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
        
        return musicDao?.getValidCachedUrl(url, 0)
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

            val html = Musify.sharedOkHttpClient.newCall(request).execute().use { it.body.string() }
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

                val responseBody = Musify.sharedOkHttpClient.newCall(request).execute().use { it.body.string() }
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
                return expireParam.toLong() * 1000 // Convert to MS
            }
        } catch (e: Exception) {}
        return System.currentTimeMillis() + (4 * 3600 * 1000) // Default 4 hours
    }

    /**
     * Performs a 1-byte HEAD/GET request to verify the stream URL is playable.
     * Prevents delivering 403 Forbidden / Error 152-4 links to the player.
     */
    private suspend fun verifyUrl(streamUrl: String): Boolean = withContext(Dispatchers.IO) {
        if (streamUrl.contains("googlevideo.com")) {
            try {
                val client = playbackClient ?: return@withContext true
                val request = okhttp3.Request.Builder()
                    .url(streamUrl)
                    .header("User-Agent", CommonUtils.CURRENT_USER_AGENT)
                    .header("Referer", "https://www.youtube.com/")
                    .header("Origin", "https://www.youtube.com")
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

    /**
     * YouTube URLs often have an 'expire' parameter which is a Unix timestamp in SECONDS.
     * We proactively consider it expired if it's within 15 minutes of that time.
     */
    private fun isUrlExpired(streamUrl: String): Boolean {
        if (streamUrl.isBlank()) return true
        try {
            val uri = android.net.Uri.parse(streamUrl)
            val expireParam = uri.getQueryParameter("expire")
            if (expireParam != null) {
                val expireTimeSec = expireParam.toLong()
                val currentTimeSec = System.currentTimeMillis() / 1000
                // Proactive refresh: if current time + 900s (15m) > expire, it's expired
                return (currentTimeSec + 900) >= expireTimeSec
            }
        } catch (e: Exception) {
            // If we can't parse it, assume it's risky but only expire if clearly broken
            return streamUrl.length < 20 
        }
        return false
    }

    /**
     * Triggers a background extraction without blocking.
     * Useful for pre-fetching search results or the upcoming queue.
     */
    fun preExtract(url: String) {
        if (url.isBlank() || streamUrlCache.containsKey(url)) return
        if (activeExtractions.containsKey(url)) return

        val job = extractionScope.launch {
            try {
                extractPlayableUrl(url, fastStart = false) // Pre-fetch high quality
            } finally {
                activeExtractions.remove(url)
            }
        }
        activeExtractions[url] = job
    }

    /**
     * Retrieves the cached URL if it exists.
     */
    fun getCachedUrl(url: String): String? = streamUrlCache[url]

    /**
     * Clears old entries from the cache if it grows too large.
     */
    fun trimCache() {
        if (streamUrlCache.size > 100) {
            // Simple logic: just clear and let it rebuild
            streamUrlCache.clear()
        }
    }
}
