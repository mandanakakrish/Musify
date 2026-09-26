package com.gaminghub.musicplayer.data.repository

import android.util.Log
import com.gaminghub.musicplayer.util.CommonUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.tasks.await

data class RealtimeArtist(
    val name: String = "",
    val imageUrl: String = "",
    val bannerUrl: String = "",
    val genre: String = "Various",
    val category: String = "All",
    val tagline: String = "",
    val bio: String = "",
    val followerCount: Long = 0L,
    val isVerified: Boolean = true,
    val isFeatured: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "realtime_sync"
)

object ArtistSyncManager {
    private const val TAG = "ArtistSyncManager"
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val scope = CoroutineScope(Dispatchers.IO)

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // In-memory cache for ultra-fast access
    private val artistCache = ConcurrentHashMap<String, RealtimeArtist>()

    // Live Flow of all synced artists from Firestore
    private val _syncedArtists = MutableStateFlow<Map<String, RealtimeArtist>>(emptyMap())
    val syncedArtists: StateFlow<Map<String, RealtimeArtist>> = _syncedArtists.asStateFlow()

    init {
        listenToFirestoreArtists()
    }

    /**
     * Sanitizes artist names for Firestore document IDs (e.g. "Arijit Singh" -> "arijit_singh")
     */
    fun sanitizeArtistId(artistName: String): String {
        return artistName.trim().lowercase()
            .replace(Regex("[^a-z0-9_]"), "_")
            .trim('_')
            .ifBlank { "unknown_artist" }
    }

    /**
     * Real-time listener on the Firestore 'artists' collection.
     * When any user or admin edits/adds an artist, updates propagate to the entire app instantly.
     */
    private fun listenToFirestoreArtists() {
        try {
            firestore.collection("artists").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error listening to artists collection: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val map = HashMap<String, RealtimeArtist>()
                    for (doc in snapshot.documents) {
                        try {
                            val artist = doc.toObject(RealtimeArtist::class.java)
                            if (artist != null && artist.name.isNotBlank()) {
                                val key = sanitizeArtistId(artist.name)
                                map[key] = artist
                                artistCache[key] = artist
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error mapping artist doc ${doc.id}: ${e.message}")
                        }
                    }
                    _syncedArtists.value = map
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed setting up firestore artist listener: ${e.message}")
        }
    }

    /**
     * Fetches artist details in real-time.
     * Priority: In-Memory Cache -> Firestore Collection -> Live Web / Deezer / YouTube Music API.
     */
    suspend fun getArtistDetails(artistName: String): RealtimeArtist = withContext(Dispatchers.IO) {
        val cleanName = artistName.trim()
        if (cleanName.isBlank()) return@withContext RealtimeArtist(name = "Unknown Artist")
        val key = sanitizeArtistId(cleanName)

        // 1. Check in-memory cache
        artistCache[key]?.let { cached ->
            if (cached.imageUrl.isNotBlank()) return@withContext cached
        }

        // 2. Check Firestore directly
        try {
            val doc = firestore.collection("artists").document(key).get().await()
            if (doc.exists()) {
                val artist = doc.toObject(RealtimeArtist::class.java)
                if (artist != null && artist.imageUrl.isNotBlank()) {
                    artistCache[key] = artist
                    return@withContext artist
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore artist fetch error for $cleanName: ${e.message}")
        }

        // 3. Fallback: Fetch Live Realtime Details from Open Web APIs (Deezer / YouTube Music)
        val fetched = fetchRealtimeArtistFromWeb(cleanName)
        artistCache[key] = fetched

        // Automatically save to Firestore so all users benefit from the sync
        saveArtistToFirestore(fetched, updatedBy = "auto_sync")

        return@withContext fetched
    }

    /**
     * Saves or updates an artist profile in Firebase Firestore.
     * Accessible by Admin users to link images, update banners, genres, and taglines.
     */
    fun saveArtistToFirestore(artist: RealtimeArtist, updatedBy: String = "admin") {
        if (artist.name.isBlank()) return
        val key = sanitizeArtistId(artist.name)
        val dataToSave = artist.copy(
            updatedAt = System.currentTimeMillis(),
            updatedBy = updatedBy
        )

        scope.launch {
            try {
                firestore.collection("artists")
                    .document(key)
                    .set(dataToSave, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d(TAG, "Successfully synced artist ${artist.name} to Firestore ($updatedBy)")
                        artistCache[key] = dataToSave
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Failed syncing artist to Firestore: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Error saving artist: ${e.message}")
            }
        }
    }

    /**
     * Live fetch from Deezer + YouTube Music fallback to get crystal clear images and fan count.
     */
    private fun fetchRealtimeArtistFromWeb(artistName: String): RealtimeArtist {
        // Try Deezer first (instant, high-res 1000x1000 images, real follower count)
        try {
            val encoded = URLEncoder.encode(artistName, "UTF-8")
            val request = Request.Builder()
                .url("https://api.deezer.com/search/artist?q=$encoded")
                .header("User-Agent", CommonUtils.CURRENT_USER_AGENT)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body.string()
                val root = JSONObject(jsonStr)
                val data = root.optJSONArray("data")
                if (data != null && data.length() > 0) {
                    val item = data.getJSONObject(0)
                    val name = item.optString("name", artistName)
                    val fans = item.optLong("nb_fan", 0L)
                    val picXl = item.optString("picture_xl").ifBlank {
                        item.optString("picture_big").ifBlank { item.optString("picture_medium") }
                    }

                    if (picXl.isNotBlank()) {
                        return RealtimeArtist(
                            name = name,
                            imageUrl = picXl,
                            bannerUrl = picXl,
                            followerCount = fans,
                            isVerified = true,
                            tagline = "Verified Artist",
                            genre = "Music"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Deezer artist fetch error for $artistName: ${e.message}")
        }

        // Fallback: YouTube Music Artist Search (Google CDN =w800-h800)
        try {
            val jsonPayload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240101.01.00")
                        put("hl", "en")
                        put("gl", "IN")
                    })
                })
                put("query", artistName)
                put("params", "EgWKAQIgAWoSEAoQCRADEAUQBBAOEBAQFRAR") // Artists filter
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/search")
                .header("User-Agent", CommonUtils.CURRENT_USER_AGENT)
                .header("Referer", "https://music.youtube.com/")
                .header("Origin", "https://music.youtube.com")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body.string()
                val root = JSONObject(jsonStr)
                val contents = root.optJSONObject("contents")
                    ?.optJSONObject("tabbedSearchResultsRenderer")
                    ?.optJSONArray("tabs")?.optJSONObject(0)
                    ?.optJSONObject("tabRenderer")?.optJSONObject("content")
                    ?.optJSONObject("sectionListRenderer")?.optJSONArray("contents")
                    ?.optJSONObject(0)?.optJSONObject("musicShelfRenderer")?.optJSONArray("contents")

                if (contents != null && contents.length() > 0) {
                    val item = contents.getJSONObject(0).optJSONObject("musicResponsiveListItemRenderer")
                    if (item != null) {
                        val thumbs = item.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")
                            ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                        val rawThumb = thumbs?.optJSONObject(thumbs.length() - 1)?.optString("url") ?: ""
                        val highResThumb = rawThumb.replace(Regex("=w\\d+-h\\d+.*"), "=w800-h800-p-l90-rj")

                        val title = item.optJSONArray("flexColumns")?.optJSONObject(0)
                            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                            ?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: artistName

                        if (highResThumb.isNotBlank()) {
                            return RealtimeArtist(
                                name = title,
                                imageUrl = highResThumb,
                                bannerUrl = highResThumb,
                                isVerified = true,
                                tagline = "Official Artist"
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "YouTube Music artist fetch error for $artistName: ${e.message}")
        }

        // Default fallback with generated initials / placeholder
        return RealtimeArtist(
            name = artistName,
            imageUrl = "",
            bannerUrl = "",
            isVerified = false,
            tagline = "Artist"
        )
    }
}
