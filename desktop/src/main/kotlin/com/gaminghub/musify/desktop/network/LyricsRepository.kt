package com.gaminghub.musify.desktop.network

import com.gaminghub.musify.desktop.model.LyricLine
import com.gaminghub.musify.desktop.model.LyricsData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class DesktopLyricsRepository {

    private val httpClient = DesktopYouTubeRepository.httpClient

    suspend fun fetchLyrics(artist: String, title: String): LyricsData? = withContext(Dispatchers.IO) {
        val cleanArtist = cleanSearchTerm(artist)
        val cleanTitle = cleanSearchTerm(title)

        // Try exact match first
        var data = fetchFromLrcLibExact(cleanArtist, cleanTitle)
        if (data != null && (data.syncedLyrics.isNotEmpty() || !data.plainLyrics.isNullOrBlank())) {
            return@withContext data
        }

        // Try search query fallback
        data = fetchFromLrcLibSearch("$cleanArtist $cleanTitle")
        data
    }

    private fun fetchFromLrcLibExact(artist: String, title: String): LyricsData? {
        return try {
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val url = "https://lrclib.net/api/get?artist=$encodedArtist&track=$encodedTitle"

            val request = Request.Builder().url(url).header("User-Agent", "Musify-Desktop/1.7.5").build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null

            val jsonStr = response.body?.string() ?: return null
            parseLyricsJson(JSONObject(jsonStr))
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchFromLrcLibSearch(query: String): LyricsData? {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://lrclib.net/api/search?q=$encodedQuery"

            val request = Request.Builder().url(url).header("User-Agent", "Musify-Desktop/1.7.5").build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null

            val jsonStr = response.body?.string() ?: return null
            val array = JSONArray(jsonStr)
            if (array.length() > 0) {
                parseLyricsJson(array.getJSONObject(0))
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun parseLyricsJson(obj: JSONObject): LyricsData {
        val syncedStr = obj.optString("syncedLyrics", "")
        val plainStr = obj.optString("plainLyrics", "")
        val isInstrumental = obj.optBoolean("instrumental", false)

        val parsedLines = mutableListOf<LyricLine>()
        if (syncedStr.isNotBlank()) {
            val lines = syncedStr.lines()
            val regex = Regex("^\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)$")
            for (line in lines) {
                val match = regex.find(line.trim())
                if (match != null) {
                    val min = match.groupValues[1].toLongOrNull() ?: 0L
                    val sec = match.groupValues[2].toLongOrNull() ?: 0L
                    val msRaw = match.groupValues[3]
                    val ms = if (msRaw.length == 2) msRaw.toLong() * 10 else msRaw.toLong()
                    val totalMs = (min * 60 + sec) * 1000 + ms
                    val text = match.groupValues[4].trim()
                    parsedLines.add(LyricLine(totalMs, text))
                }
            }
        }

        return LyricsData(
            plainLyrics = plainStr.ifBlank { null },
            syncedLyrics = parsedLines.sortedBy { it.timeMs },
            isInstrumental = isInstrumental
        )
    }

    private fun cleanSearchTerm(term: String): String {
        return term.replace(Regex("(?i)(\\(.*?Official.*?\\)|\\[.*?Official.*?\\]|\\(.*?Video.*?\\)|\\[.*?Video.*?\\]|\\bfeat\\b.*|\\bft\\b.*|- Topic$)"), "")
            .trim()
    }
}
