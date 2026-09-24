package com.gaminghub.musicplayer.data.repository

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection

data class LyricsResult(val plainLyrics: String?, val syncedLyrics: String?, val isInstrumental: Boolean)

class LyricsRepository {
    private val tag = "LyricsRepository"

    suspend fun fetchLyricsFromNetwork(artist: String, title: String): LyricsResult? = withContext(Dispatchers.IO) {
        val searchAttempts = refinedSearch(artist, title)
        var bestResponse: String? = null

        for ((a, t) in searchAttempts) {
            try {
                val getUrl = java.net.URL("https://lrclib.net/api/get?artist=${Uri.encode(a)}&track=${Uri.encode(t)}")
                bestResponse = fetchRawFromLrcLib(getUrl)
                if (bestResponse != null) break
                
                val searchUrl = java.net.URL("https://lrclib.net/api/search?q=${Uri.encode("$a $t")}")
                bestResponse = fetchRawFromLrcLib(searchUrl, isSearch = true)
                if (bestResponse != null) break
            } catch (e: Exception) {
                Log.e(tag, "Attempt failed for $a - $t: ${e.message}")
            }
        }

        if (bestResponse != null) {
            val synced = extractJsonField(bestResponse, "syncedLyrics")
            val plain = extractJsonField(bestResponse, "plainLyrics")
            val isInstrumental = bestResponse.contains("\"instrumental\":true")
            return@withContext LyricsResult(plain, synced, isInstrumental)
        }
        return@withContext null
    }

    private fun refinedSearch(artist: String, title: String): List<Pair<String, String>> {
        val queries = mutableListOf<Pair<String, String>>()
        val noiseRegex = Regex("(?i)(\\(.*?Official.*?\\)|\\[.*?Official.*?\\]|\\(.*?Video.*?\\)|\\[.*?Video.*?\\]|\\(.*?Lyrics.*?\\)|\\[.*?Lyrics.*?\\]|\\bfeat\\b.*|\\bft\\b.*|- Topic$|[\\(\\)\\[\\]])")
        
        val cleanA = artist.replace(noiseRegex, "").trim()
        val cleanT = title.replace(noiseRegex, "").trim()
        
        queries.add(cleanA to cleanT)
        if (cleanT.contains(" - ")) {
            val parts = cleanT.split(" - ", limit = 2)
            queries.add(parts[0].trim() to parts[1].trim())
        }
        queries.add(cleanT to cleanA)
        return queries.distinct()
    }

    private fun extractJsonField(json: String, field: String): String? {
        return try {
            val obj = org.json.JSONObject(json)
            if (obj.isNull(field)) null else obj.get(field).toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchRawFromLrcLib(url: java.net.URL, isSearch: Boolean = false): String? {
        return try {
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("User-Agent", "MusifyMusicPlayer/1.2 (https://github.com)")
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }.trim()
                if (isSearch) {
                    try {
                        val array = org.json.JSONArray(response)
                        if (array.length() > 0) array.getJSONObject(0).toString() else null 
                    } catch (_: Exception) {
                        null
                    }
                } else {
                    response
                }
            } else null
        } catch (e: Exception) {
            Log.e(tag, "Network error fetching from LRCLIB: ${e.message}")
            null
        }
    }
}
