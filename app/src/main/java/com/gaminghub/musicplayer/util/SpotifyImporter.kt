package com.gaminghub.musicplayer.util

import android.util.Log
import com.gaminghub.musicplayer.TrackModel
import com.gaminghub.musicplayer.data.repository.YouTubeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Spotify Public Link Importer and Metadata Resolver.
 * Resolves Spotify track, album, and playlist links into official playable tracks.
 */
object SpotifyImporter {
    private const val TAG = "SpotifyImporter"
    private val httpClient by lazy { OkHttpClient.Builder().build() }
    private val youtubeRepository by lazy { YouTubeRepository() }

    data class SpotifyImportResult(
        val playlistName: String,
        val tracks: List<TrackModel>
    )

    suspend fun importFromSpotifyUrl(spotifyUrl: String): SpotifyImportResult = withContext(Dispatchers.IO) {
        val cleanUrl = spotifyUrl.trim().split("?")[0]
        
        try {
            if (cleanUrl.contains("/track/")) {
                val track = parseSpotifyTrack(cleanUrl)
                if (track != null) {
                    return@withContext SpotifyImportResult("Spotify Track", listOf(track))
                }
            } else if (cleanUrl.contains("/playlist/") || cleanUrl.contains("/album/")) {
                return@withContext parseSpotifyPlaylistOrAlbum(cleanUrl)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importing Spotify URL: ${e.message}", e)
        }

        return@withContext SpotifyImportResult("Spotify Import", emptyList())
    }

    private suspend fun parseSpotifyTrack(trackUrl: String): TrackModel? {
        try {
            val oembedUrl = "https://open.spotify.com/oembed?url=${URLEncoder.encode(trackUrl, "UTF-8")}"
            val request = Request.Builder().url(oembedUrl).build()
            val response = httpClient.newCall(request).execute().use { it.body.string() }
            val json = JSONObject(response)
            val title = json.optString("title")
            val thumbnail = json.optString("thumbnail_url")

            if (title.isNotBlank()) {
                // Search for official track
                val results = youtubeRepository.fetchMusic(title, officialOnly = true)
                val topMatch = results.firstOrNull()
                if (topMatch != null) {
                    return topMatch.copy(albumArtUrl = thumbnail.ifBlank { topMatch.albumArtUrl })
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing single Spotify track: ${e.message}")
        }
        return null
    }

    private suspend fun parseSpotifyPlaylistOrAlbum(url: String): SpotifyImportResult {
        val tracks = mutableListOf<TrackModel>()
        var name = if (url.contains("/album/")) "Spotify Album" else "Spotify Playlist"

        try {
            // Fetch oEmbed for playlist title and thumbnail
            val oembedUrl = "https://open.spotify.com/oembed?url=${URLEncoder.encode(url, "UTF-8")}"
            val oembedRequest = Request.Builder().url(oembedUrl).build()
            try {
                val oembedRes = httpClient.newCall(oembedRequest).execute().use { it.body.string() }
                val json = JSONObject(oembedRes)
                val title = json.optString("title")
                if (title.isNotBlank()) name = title
            } catch (_: Exception) {}

            // Embed page scraping for track list
            val embedUrl = if (url.contains("open.spotify.com/embed/")) url else url.replace("open.spotify.com/", "open.spotify.com/embed/")
            val request = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", CommonUtils.CURRENT_USER_AGENT)
                .build()

            val html = httpClient.newCall(request).execute().use { it.body.string() }
            
            // Extract track names from Spotify Embed metadata
            val scriptRegex = Regex("<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>")
            val match = scriptRegex.find(html)
            
            if (match != null) {
                val jsonStr = match.groupValues[1]
                val root = JSONObject(jsonStr)
                val state = root.optJSONObject("props")?.optJSONObject("pageProps")?.optJSONObject("state")?.optJSONObject("data")
                val entity = state?.optJSONObject("entity") ?: state?.optJSONObject("album") ?: state?.optJSONObject("playlist")
                
                val trackList = entity?.optJSONObject("trackList")?.optJSONArray("items")
                    ?: entity?.optJSONArray("trackList")
                    ?: entity?.optJSONObject("tracks")?.optJSONArray("items")

                if (trackList != null) {
                    for (i in 0 until trackList.length()) {
                        val item = trackList.getJSONObject(i)
                        val tTitle = item.optString("title").ifBlank { item.optString("name") }
                        val tArtist = item.optString("subtitle").ifBlank { 
                            item.optJSONArray("artists")?.optJSONObject(0)?.optString("name") ?: ""
                        }

                        if (tTitle.isNotBlank()) {
                            val searchQuery = if (tArtist.isNotBlank()) "$tArtist $tTitle official" else "$tTitle official"
                            val resolved = youtubeRepository.fetchMusic(searchQuery, officialOnly = true).firstOrNull()
                            if (resolved != null) {
                                tracks.add(resolved)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Spotify playlist/album tracks: ${e.message}")
        }

        return SpotifyImportResult(name, tracks)
    }
}
