package com.gaminghub.musify.desktop.network

import com.gaminghub.musify.desktop.model.StreamingQuality
import com.gaminghub.musify.desktop.model.TrackModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DesktopYouTubeRepository {

    companion object {
        val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .dns(DesktopDns)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }

        private var isInitialized = false

        fun initializeExtractor() {
            if (!isInitialized) {
                try {
                    NewPipe.init(DesktopNewPipeDownloader(httpClient), Localization.DEFAULT)
                    isInitialized = true
                } catch (e: Exception) {
                    System.err.println("Failed to init NewPipe: ${e.message}")
                }
            }
        }
    }

    init {
        initializeExtractor()
    }

    private val streamCache = ConcurrentHashMap<String, String>()

    suspend fun searchTracks(query: String): List<TrackModel> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<TrackModel>()
        
        // 1. Try YouTube Music InnerTube Search (Official high-fidelity tracks)
        try {
            val ytMusicResults = searchYouTubeMusic(query)
            tracks.addAll(ytMusicResults)
        } catch (e: Exception) {
            System.err.println("YouTube Music search error: ${e.message}")
        }

        // 2. If results are sparse, augment with NewPipe Extractor search
        if (tracks.size < 15) {
            try {
                val service = ServiceList.YouTube
                val searchExtractor = service.getSearchExtractor(query)
                searchExtractor.fetchPage()

                val items = searchExtractor.initialPage?.items ?: emptyList()
                for (item in items) {
                    if (item is StreamInfoItem) {
                        val videoId = item.url.substringAfter("v=").substringBefore("&")
                        val highResArt = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                        val model = TrackModel(
                            title = cleanTitle(item.name ?: "Unknown Track"),
                            artist = item.uploaderName ?: "Unknown Artist",
                            audioUrl = item.url,
                            albumArtUrl = highResArt,
                            durationSeconds = item.duration
                        )
                        if (tracks.none { it.audioUrl == model.audioUrl }) {
                            tracks.add(model)
                        }
                    }
                }
            } catch (e: Exception) {
                System.err.println("NewPipe search error: ${e.message}")
            }
        }

        tracks
    }

    private fun searchYouTubeMusic(term: String): List<TrackModel> {
        val results = mutableListOf<TrackModel>()
        val url = "https://music.youtube.com/youtubei/v1/search?alt=json"

        val jsonBody = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20240722.01.00")
                    put("hl", "en")
                    put("gl", "US")
                })
            })
            put("query", term)
            put("params", "Eg-KAQwIABAAGAAgACgAMABqChAGEAkQChAFEAM%3D") // Music filter
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(mediaType))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .header("Referer", "https://music.youtube.com/")
            .header("Origin", "https://music.youtube.com")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()

        val responseString = response.body?.string() ?: return emptyList()
        val root = JSONObject(responseString)

        val contents = root.optJSONObject("contents")
            ?.optJSONObject("tabbedSearchResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents") ?: return emptyList()

        for (i in 0 until contents.length()) {
            val section = contents.optJSONObject(i)?.optJSONObject("musicShelfRenderer") ?: continue
            val shelfContents = section.optJSONArray("contents") ?: continue

            for (j in 0 until shelfContents.length()) {
                val item = shelfContents.optJSONObject(j)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                val flexColumns = item.optJSONArray("flexColumns") ?: continue

                // Title
                val title = flexColumns.optJSONObject(0)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")
                    ?.optJSONArray("runs")
                    ?.optJSONObject(0)
                    ?.optString("text") ?: continue

                // Artist
                val artistRuns = flexColumns.optJSONObject(1)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")
                    ?.optJSONArray("runs")

                var artist = "Unknown Artist"
                if (artistRuns != null && artistRuns.length() > 0) {
                    artist = artistRuns.optJSONObject(0)?.optString("text") ?: "Unknown Artist"
                }

                // Video ID
                val navEndpoint = item.optJSONObject("overlay")
                    ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                    ?.optJSONObject("content")
                    ?.optJSONObject("musicPlayButtonRenderer")
                    ?.optJSONObject("playNavigationEndpoint")

                val videoId = navEndpoint?.optJSONObject("watchEndpoint")?.optString("videoId")
                    ?: item.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")

                if (!videoId.isNullOrBlank()) {
                    val audioUrl = "https://www.youtube.com/watch?v=$videoId"
                    val artUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                    results.add(
                        TrackModel(
                            title = cleanTitle(title),
                            artist = artist,
                            audioUrl = audioUrl,
                            albumArtUrl = artUrl
                        )
                    )
                }
            }
        }

        return results
    }

    suspend fun extractStreamUrl(rawAudioUrl: String, quality: StreamingQuality = StreamingQuality.HIGH): String? = withContext(Dispatchers.IO) {
        val audioUrl = when {
            rawAudioUrl.length == 11 && !rawAudioUrl.contains("/") && !rawAudioUrl.contains(" ") -> {
                "https://www.youtube.com/watch?v=$rawAudioUrl"
            }
            rawAudioUrl.contains("youtube.com") && !rawAudioUrl.startsWith("http") -> {
                "https://$rawAudioUrl"
            }
            else -> rawAudioUrl
        }

        if (!audioUrl.contains("youtube.com") && !audioUrl.contains("youtu.be")) {
            return@withContext audioUrl
        }

        val cached = streamCache[audioUrl]
        if (cached != null) return@withContext cached

        // 1. Try NewPipe Extractor
        try {
            val service = ServiceList.YouTube
            val extractor = service.getStreamExtractor(audioUrl)
            extractor.fetchPage()

            val audioStreams = extractor.audioStreams
            if (!audioStreams.isNullOrEmpty()) {
                val targetBitrate = quality.targetBitrate
                val candidateStreams = audioStreams.sortedWith(
                    compareByDescending<AudioStream> { it.format == org.schabi.newpipe.extractor.MediaFormat.M4A }
                        .thenBy { kotlin.math.abs(it.bitrate - targetBitrate) }
                )

                for (candidate in candidateStreams) {
                    val streamUrl = candidate.url
                    if (!streamUrl.isNullOrBlank()) {
                        streamCache[audioUrl] = streamUrl
                        return@withContext streamUrl
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("NewPipe stream extraction attempt failed for $audioUrl: ${e.message}")
        }

        // 2. Fallback: YouTube Embed HTML scraping
        val videoId = extractVideoId(audioUrl)
        if (videoId != null) {
            val embedUrl = extractViaYouTubeEmbed(videoId)
            if (embedUrl != null) {
                streamCache[audioUrl] = embedUrl
                return@withContext embedUrl
            }

            // 3. Fallback: InnerTube Player API
            val innerTubeUrl = extractViaYouTubePlayerApi(videoId)
            if (innerTubeUrl != null) {
                streamCache[audioUrl] = innerTubeUrl
                return@withContext innerTubeUrl
            }
        }

        null
    }

    suspend fun resolveAndExtractTrack(track: TrackModel, quality: StreamingQuality = StreamingQuality.HIGH): String? = withContext(Dispatchers.IO) {
        // First try the track's existing audioUrl
        val directStream = extractStreamUrl(track.audioUrl, quality)
        if (directStream != null) return@withContext directStream

        // If direct extraction failed, search YouTube Music by artist + title as fallback
        if (track.title.isNotBlank()) {
            val query = "${track.artist} ${track.title}".trim()
            println("YouTubeRepository: direct stream extraction failed for '${track.title}', searching fallback for '$query'...")
            try {
                val searchResults = searchTracks(query)
                for (candidate in searchResults.take(3)) {
                    val candidateStream = extractStreamUrl(candidate.audioUrl, quality)
                    if (candidateStream != null) {
                        println("YouTubeRepository: fallback stream matched for '${candidate.title}'")
                        return@withContext candidateStream
                    }
                }
            } catch (e: Exception) {
                System.err.println("YouTubeRepository: fallback search failed: ${e.message}")
            }
        }

        null
    }

    private fun extractVideoId(url: String): String? {
        if (url.length == 11 && !url.contains("/")) return url
        val regex = Regex("(?:v=|/v/|youtu\\.be/|/embed/|/shorts/|watch\\?v%3D|watch\\?v=)([^#&?]{11})")
        val match = regex.find(url)
        return match?.groupValues?.get(1)?.takeIf { it.length == 11 }
    }

    private fun extractViaYouTubeEmbed(videoId: String): String? {
        try {
            val embedUrl = "https://www.youtube.com/embed/$videoId"
            val request = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                .header("Referer", "https://www.youtube.com/")
                .build()

            val html = httpClient.newCall(request).execute().use { it.body?.string() ?: "" }
            val regex = Regex("ytInitialPlayerResponse\\s*=\\s*(\\{.*?\\});")
            val match = regex.find(html)
            val jsonStr = match?.groupValues?.get(1) ?: return null

            val root = JSONObject(jsonStr)
            val streamingData = root.optJSONObject("streamingData") ?: return null
            val formats = streamingData.optJSONArray("adaptiveFormats") ?: return null

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
                println("Successfully extracted stream via YouTube Embed page for $videoId")
            }
            return bestUrl
        } catch (e: Exception) {
            System.err.println("YouTube Embed extraction error for $videoId: ${e.message}")
            return null
        }
    }

    private fun extractViaYouTubePlayerApi(videoId: String): String? {
        val clients = listOf(
            Pair("ANDROID_VR", "1.54.1"),
            Pair("ANDROID_MUSIC", "7.02.51"),
            Pair("TVHTML5", "7.20230405.00.00"),
            Pair("IOS", "19.45.4"),
            Pair("ANDROID", "19.45.36")
        )

        for ((clientName, clientVersion) in clients) {
            try {
                val jsonBody = JSONObject().apply {
                    put("videoId", videoId)
                    put("context", JSONObject().apply {
                        put("client", JSONObject().apply {
                            put("clientName", clientName)
                            put("clientVersion", clientVersion)
                            put("hl", "en")
                            put("gl", "US")
                        })
                    })
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val request = Request.Builder()
                    .url("https://www.youtube.com/youtubei/v1/player")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Content-Type", "application/json")
                    .post(jsonBody.toString().toRequestBody(mediaType))
                    .build()

                val responseBody = httpClient.newCall(request).execute().use { it.body?.string() ?: "" }
                val root = JSONObject(responseBody)
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
                    println("Successfully extracted stream via InnerTube client $clientName for $videoId")
                    return bestAudioUrl
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun cleanTitle(raw: String): String {
        return raw.replace(Regex("(?i)(\\(Official Video\\)|\\[Official Video\\]|\\(Official Audio\\)|\\[Official Audio\\]|\\(Lyric Video\\)|\\[Lyric Video\\])"), "")
            .trim()
    }
}
