package com.gaminghub.musicplayer.util

import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object SearchSuggestionEngine {
    private const val TAG = "SearchSuggestionEngine"
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    // In-memory cache for ultra-fast instant suggestions (0ms latency for repeated/backspaced inputs)
    private val suggestionCache = LruCache<String, List<String>>(150)

    suspend fun getSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        val cacheKey = trimmed.lowercase()
        val cached = suggestionCache.get(cacheKey)
        if (cached != null) return@withContext cached

        val resultList = mutableListOf<String>()
        try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", CommonUtils.CURRENT_USER_AGENT)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body.string()
                val rootArray = JSONArray(jsonStr)
                if (rootArray.length() > 1) {
                    val suggestionsArray = rootArray.getJSONArray(1)
                    for (i in 0 until suggestionsArray.length()) {
                        val suggestion = suggestionsArray.optString(i)
                        if (!suggestion.isNullOrBlank()) {
                            resultList.add(suggestion)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed fetching online suggestions: ${e.message}")
        }

        // Fallback local predictive suggestions if network fails
        if (resultList.isEmpty()) {
            resultList.addAll(
                listOf(
                    trimmed,
                    "$trimmed songs",
                    "$trimmed song",
                    "$trimmed official",
                    "$trimmed music",
                    "$trimmed 2026",
                    "$trimmed remix",
                    "$trimmed lofi"
                )
            )
        }

        val distinctList = resultList.distinct()
        suggestionCache.put(cacheKey, distinctList)
        return@withContext distinctList
    }
}
