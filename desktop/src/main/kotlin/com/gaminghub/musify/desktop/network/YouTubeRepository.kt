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

    suspend fun extractStreamUrl(audioUrl: String, quality: StreamingQuality = StreamingQuality.HIGH): String? = withContext(Dispatchers.IO) {
        if (!audioUrl.contains("youtube.com") && !audioUrl.contains("youtu.be")) {
            return@withContext audioUrl
        }

        val cached = streamCache[audioUrl]
        if (cached != null) return@withContext cached

        try {
            val service = ServiceList.YouTube
            val extractor = service.getStreamExtractor(audioUrl)
            extractor.fetchPage()

            val audioStreams = extractor.audioStreams
            if (!audioStreams.isNullOrEmpty()) {
                // Filter and sort by format and quality (M4A/AAC format is optimal for Windows Media Foundation)
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
            System.err.println("Stream extraction failed for $audioUrl: ${e.message}")
        }

        null
    }

    private fun cleanTitle(raw: String): String {
        return raw.replace(Regex("(?i)(\\(Official Video\\)|\\[Official Video\\]|\\(Official Audio\\)|\\[Official Audio\\]|\\(Lyric Video\\)|\\[Lyric Video\\])"), "")
            .trim()
    }
}
