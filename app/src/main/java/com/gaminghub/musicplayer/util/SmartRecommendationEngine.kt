package com.gaminghub.musicplayer.util

import android.util.Log
import com.gaminghub.musicplayer.TrackModel
import com.gaminghub.musicplayer.data.MusicDao
import com.gaminghub.musicplayer.data.repository.YouTubeRepository
import com.gaminghub.musicplayer.data.toModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Intelligent recommendation engine that computes high-affinity "Up Next" / Song Radio
 * queues tailored specifically to the currently playing song's category, vibe, mood, and artist,
 * while strictly eliminating duplicate/same songs.
 */
object SmartRecommendationEngine {
    private const val TAG = "SmartRecommendationEngine"

    enum class VibeType {
        PARTY_DANCE,
        ROMANTIC_MELODY,
        HIP_HOP_PUNJABI,
        LOFI_ACOUSTIC_CHILL,
        ROCK_EDM,
        DEVOTIONAL_FOLK,
        GENERAL_POP
    }

    suspend fun computeNextRecommendations(
        currentTrack: TrackModel?,
        trendingTracks: List<TrackModel>,
        topCharts: List<TrackModel>,
        dao: MusicDao,
        youtubeRepository: YouTubeRepository,
        existingQueueUrls: Set<String>,
        count: Int = 12
    ): List<TrackModel> = withContext(Dispatchers.IO) {
        if (currentTrack == null) return@withContext emptyList()

        val candidates = mutableListOf<ScoredTrack>()
        val seenUrls = HashSet(existingQueueUrls)
        currentTrack.audioUrl?.let { seenUrls.add(it) }

        val seedCanonicalTitle = normalizeTitle(currentTrack.title)
        val seenCanonicalTitles = HashSet<String>().apply {
            add(seedCanonicalTitle)
        }

        // 1. Detect Vibe & Mood of Current Song
        val vibe = detectVibe(currentTrack)
        val primaryArtist = extractPrimaryArtist(currentTrack.artist)

        // 2. Fetch Artists & Frequent Collaborators (Same Vibe & Artist Circle)
        if (primaryArtist.isNotBlank() && !primaryArtist.equals("Unknown Artist", ignoreCase = true)) {
            try {
                val query = when (vibe) {
                    VibeType.PARTY_DANCE -> "$primaryArtist dance party songs"
                    VibeType.ROMANTIC_MELODY -> "$primaryArtist romantic love songs"
                    VibeType.HIP_HOP_PUNJABI -> "$primaryArtist top hits official"
                    VibeType.LOFI_ACOUSTIC_CHILL -> "$primaryArtist acoustic lofi chill songs"
                    VibeType.ROCK_EDM -> "$primaryArtist remix edm songs"
                    VibeType.DEVOTIONAL_FOLK -> "$primaryArtist folk songs"
                    VibeType.GENERAL_POP -> "$primaryArtist top songs official"
                }
                val artistTracks = youtubeRepository.fetchMusic(query)
                artistTracks.forEach { track ->
                    addCandidateIfUnique(track, candidates, seenUrls, seenCanonicalTitles, score = 95.0, reason = "Artist & Vibe Match")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Artist query failed: ${e.message}")
            }
        }

        // 3. Category & Vibe-Aligned Radio Search (Similar Vibe, Not Same Song)
        try {
            val vibeSearchQuery = buildVibeQuery(currentTrack, vibe)
            val vibeTracks = youtubeRepository.fetchMusic(vibeSearchQuery)
            vibeTracks.forEach { track ->
                addCandidateIfUnique(track, candidates, seenUrls, seenCanonicalTitles, score = 88.0, reason = "Vibe Radio ($vibe)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibe query failed: ${e.message}")
        }

        // 4. User's Liked / Favorites that match the Same Vibe/Genre
        try {
            val favorites = dao.getFavoriteTracks().firstOrNull()?.map { it.toModel() } ?: emptyList()
            favorites.filter { fav ->
                val favVibe = detectVibe(fav)
                favVibe == vibe || fav.artist.contains(primaryArtist, ignoreCase = true)
            }.shuffled().take(6).forEach { track ->
                addCandidateIfUnique(track, candidates, seenUrls, seenCanonicalTitles, score = 92.0, reason = "Liked Vibe Match")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Favorites matching failed: ${e.message}")
        }

        // 5. User's Most Played Tracks with Matching Vibe
        try {
            val mostPlayed = dao.getTop50Tracks().firstOrNull()?.map { it.toModel() } ?: emptyList()
            mostPlayed.filter { mp ->
                detectVibe(mp) == vibe
            }.take(5).forEach { track ->
                addCandidateIfUnique(track, candidates, seenUrls, seenCanonicalTitles, score = 86.0, reason = "Most Played Vibe")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Most played matching failed: ${e.message}")
        }

        // 6. Complementary Trending Tracks matching the same vibe
        val chartPool = (topCharts + trendingTracks).filter { detectVibe(it) == vibe }
        chartPool.shuffled().take(6).forEach { track ->
            addCandidateIfUnique(track, candidates, seenUrls, seenCanonicalTitles, score = 78.0, reason = "Trending in Vibe")
        }

        // Sort by affinity score with light variety jitter to keep playback fresh
        val distinctResults = candidates
            .sortedByDescending { it.score + (Math.random() * 8.0) }
            .map { it.track }
            .take(count)

        Log.d(TAG, "Computed ${distinctResults.size} unique up-next tracks for '${currentTrack.title}' (Vibe: $vibe)")
        distinctResults
    }

    private fun addCandidateIfUnique(
        track: TrackModel,
        candidates: MutableList<ScoredTrack>,
        seenUrls: MutableSet<String>,
        seenCanonicalTitles: MutableSet<String>,
        score: Double,
        reason: String
    ) {
        val url = track.audioUrl ?: return
        if (seenUrls.contains(url)) return

        val canonical = normalizeTitle(track.title)
        if (canonical.length > 2 && seenCanonicalTitles.contains(canonical)) return

        // Prevent title variants, remixes, and covers of already-seen songs
        val candTokens = canonical.split(" ").filter { it.length > 2 }.toSet()
        if (candTokens.isNotEmpty()) {
            for (seen in seenCanonicalTitles) {
                val seenTokens = seen.split(" ").filter { it.length > 2 }.toSet()
                if (seenTokens.isNotEmpty()) {
                    val overlap = seenTokens.intersect(candTokens).size.toDouble() / minOf(seenTokens.size, candTokens.size)
                    if (overlap >= 0.70) return
                }
            }
        }

        seenUrls.add(url)
        if (canonical.length > 2) {
            seenCanonicalTitles.add(canonical)
        }
        var adjustedScore = score
        if (track.skipCount > 0) {
            adjustedScore -= (track.skipCount * 5.0)
        }
        if (track.completionCount > 0) {
            adjustedScore += (track.completionCount * 3.0)
        }
        candidates.add(ScoredTrack(track, adjustedScore, reason))
    }

    fun detectVibe(track: TrackModel): VibeType {
        val text = "${track.title} ${track.artist} ${track.genre ?: ""}".lowercase(Locale.ROOT)

        return when {
            text.containsAny("party", "dance", "club", "dj", "remix", "bhangra", "thumka", "aayi nai", "kamariya", "saki", "muqabla", "daru", "sharaabi") ->
                VibeType.PARTY_DANCE
            text.containsAny("love", "romantic", "dil", "ishq", "pyaar", "mehboob", "raataan", "kesariya", "tumhare", "apna bana le", "gulaab", "naina", "humsafar") ->
                VibeType.ROMANTIC_MELODY
            text.containsAny("punjabi", "hip hop", "rap", "shubh", "aujla", "dhillon", "badshah", "honey singh", "desi kalakaar", "drift", "phonk", "satisfya") ->
                VibeType.HIP_HOP_PUNJABI
            text.containsAny("lofi", "lo-fi", "chill", "slowed", "reverb", "acoustic", "mitraz", "finding her", "bedardeya", "sad", "alone", "unplugged") ->
                VibeType.LOFI_ACOUSTIC_CHILL
            text.containsAny("rock", "edm", "bass", "electro", "metal", "electronic") ->
                VibeType.ROCK_EDM
            text.containsAny("ambe", "maiyya", "bhajan", "devotional", "aarti", "folk", "garba", "gujarati") ->
                VibeType.DEVOTIONAL_FOLK
            else ->
                VibeType.GENERAL_POP
        }
    }

    private fun buildVibeQuery(track: TrackModel, vibe: VibeType): String {
        val artist = extractPrimaryArtist(track.artist)
        return when (vibe) {
            VibeType.PARTY_DANCE -> "Top Bollywood Punjabi Party Dance Songs 2026"
            VibeType.ROMANTIC_MELODY -> "Best Hindi Romantic Melodies Love Songs"
            VibeType.HIP_HOP_PUNJABI -> "Top Punjabi Hip Hop Trending Songs"
            VibeType.LOFI_ACOUSTIC_CHILL -> "Hindi Lo-Fi Chill Acoustic Songs"
            VibeType.ROCK_EDM -> "Top EDM Remix Bass Hits"
            VibeType.DEVOTIONAL_FOLK -> "Top Folk Devotional Songs"
            VibeType.GENERAL_POP -> if (artist.isNotBlank()) "$artist similar songs radio" else "${track.title} song radio"
        }
    }

    fun extractPrimaryArtist(artistStr: String): String {
        val cleaned = artistStr
            .replace(Regex("(?i)\\(?offline audio\\)?"), "")
            .replace(Regex("(?i)(feat\\.?|ft\\.?|official|audio|video|vevo|&|/|,).*"), "")
            .trim()
        return if (cleaned.equals("Offline", ignoreCase = true) || cleaned.isBlank()) "" else cleaned
    }

    fun normalizeTitle(title: String): String {
        return title
            .lowercase(Locale.ROOT)
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("(?i)\\b(official|music|video|audio|lyrics?|lyrical|song|full|hd|4k|remix|lofi|slowed|reverb|acoustic|cover|feat|ft)\\b"), " ")
            .replace(Regex("[^a-zA-Z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }

    private data class ScoredTrack(
        val track: TrackModel,
        val score: Double,
        val reason: String
    )
}
