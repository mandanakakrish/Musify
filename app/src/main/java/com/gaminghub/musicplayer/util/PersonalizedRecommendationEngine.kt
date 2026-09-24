package com.gaminghub.musicplayer.util

import android.util.Log
import com.gaminghub.musicplayer.TrackModel
import com.gaminghub.musicplayer.data.MusicDao
import com.gaminghub.musicplayer.data.repository.YouTubeRepository
import com.gaminghub.musicplayer.data.toModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import kotlin.math.exp
import kotlin.math.ln

/**
 * Advanced on-device AI/ML Personalized Recommendation Engine.
 * Modeled after Spotify's Discover/Daily Mix and YouTube Music's Supermix algorithms.
 *
 * Computes multi-dimensional taste vectors from implicit behavior (skips, completions, playcounts)
 * and explicit intent (favorites, downloads, followed artists), producing personalized feeds:
 * - My Supermix
 * - Daily Mixes (vibe-partitioned clusters)
 * - Listen Again (frequency & recency decay)
 * - Because You Like [Artist] (contextual collaborative radio)
 * - Discover Fresh (unheard high-affinity recommendations)
 */
object PersonalizedRecommendationEngine {
    private const val TAG = "PersonalizedRecEngine"

    enum class TimeSlot {
        MORNING,    // 5 AM - 11 AM: Acoustic, Devotional, Lo-Fi, Uplifting
        AFTERNOON,  // 11 AM - 5 PM: Pop Hits, Romantic, Commercial
        EVENING,    // 5 PM - 9 PM: Party, Dance, Punjabi Hip-Hop, High Energy
        NIGHT       // 9 PM - 5 AM: Chill, Lo-Fi, Romantic Melodies, Slowed
    }

    data class UserTasteProfile(
        val artistAffinities: Map<String, Double>,
        val vibeAffinities: Map<SmartRecommendationEngine.VibeType, Double>,
        val topArtists: List<String>,
        val topVibes: List<SmartRecommendationEngine.VibeType>,
        val currentTimeSlot: TimeSlot,
        val isColdStart: Boolean,
        val primaryMoodSummary: String
    )

    data class PersonalizedMix(
        val id: String,
        val title: String,
        val subtitle: String,
        val coverUrl: String,
        val vibe: SmartRecommendationEngine.VibeType?,
        val tracks: List<TrackModel>,
        val categoryTag: String = "All"
    )

    data class PersonalizedFeeds(
        val supermix: List<TrackModel> = emptyList(),
        val dailyMixes: List<PersonalizedMix> = emptyList(),
        val listenAgain: List<TrackModel> = emptyList(),
        val becauseYouLikeArtist: Pair<String, List<TrackModel>>? = null,
        val discoverFresh: List<TrackModel> = emptyList(),
        val tasteSummary: String = "Curating your personal sound..."
    )

    /**
     * Analyzes Room database state to compute real-time User Taste Profile.
     */
    suspend fun computeTasteProfile(dao: MusicDao): UserTasteProfile = withContext(Dispatchers.IO) {
        val allTracks = try { dao.getAllTracksSync() } catch (_: Exception) { emptyList() }
        val downloads = try { dao.getDownloadedTracksSync() } catch (_: Exception) { emptyList() }
        val favorites = try { dao.getFavoriteTracksSync() } catch (_: Exception) { emptyList() }
        val followedArtists = try { dao.getFollowedArtistsSync() } catch (_: Exception) { emptyList() }

        val downloadedUrls = downloads.map { it.audioUrl }.toSet()
        val favoriteUrls = favorites.map { it.audioUrl }.toSet()
        val followedNames = followedArtists.map { it.name.trim().lowercase(Locale.ROOT) }.toSet()

        val artistScores = mutableMapOf<String, Double>()
        val vibeScores = mutableMapOf<SmartRecommendationEngine.VibeType, Double>()

        var totalInteractions = 0

        allTracks.forEach { trackEntity ->
            val track = trackEntity.toModel()
            val primaryArtist = SmartRecommendationEngine.extractPrimaryArtist(track.artist)
            val vibe = SmartRecommendationEngine.detectVibe(track)

            var score = 0.0

            // Explicit signals
            if (downloadedUrls.contains(trackEntity.audioUrl)) {
                score += 15.0
                totalInteractions++
            }
            if (favoriteUrls.contains(trackEntity.audioUrl)) {
                score += 10.0
                totalInteractions++
            }
            if (followedNames.contains(primaryArtist.lowercase(Locale.ROOT))) {
                score += 12.0
                totalInteractions++
            }

            // Implicit behavioral signals
            if (trackEntity.playCount > 0) {
                score += ln(1.0 + trackEntity.playCount) * 4.0
                totalInteractions += trackEntity.playCount
            }
            score += trackEntity.completionCount * 3.0
            score -= trackEntity.skipCount * 2.5

            if (score > 0) {
                if (primaryArtist.isNotBlank() && !primaryArtist.equals("Unknown Artist", ignoreCase = true) && !primaryArtist.contains("Offline", ignoreCase = true)) {
                    artistScores[primaryArtist] = (artistScores[primaryArtist] ?: 0.0) + score
                }
                vibeScores[vibe] = (vibeScores[vibe] ?: 0.0) + score
            }
        }

        val currentTimeSlot = getCurrentTimeSlot()

        // Contextual Time-of-Day Boost (similar to Spotify / YT Music day-parting)
        val timeOfDayPreferredVibes = when (currentTimeSlot) {
            TimeSlot.MORNING -> listOf(SmartRecommendationEngine.VibeType.LOFI_ACOUSTIC_CHILL, SmartRecommendationEngine.VibeType.DEVOTIONAL_FOLK, SmartRecommendationEngine.VibeType.GENERAL_POP)
            TimeSlot.AFTERNOON -> listOf(SmartRecommendationEngine.VibeType.GENERAL_POP, SmartRecommendationEngine.VibeType.ROMANTIC_MELODY)
            TimeSlot.EVENING -> listOf(SmartRecommendationEngine.VibeType.PARTY_DANCE, SmartRecommendationEngine.VibeType.HIP_HOP_PUNJABI, SmartRecommendationEngine.VibeType.ROCK_EDM)
            TimeSlot.NIGHT -> listOf(SmartRecommendationEngine.VibeType.LOFI_ACOUSTIC_CHILL, SmartRecommendationEngine.VibeType.ROMANTIC_MELODY)
        }

        timeOfDayPreferredVibes.forEach { v ->
            vibeScores[v] = (vibeScores[v] ?: 1.0) * 1.3
        }

        val sortedArtists = artistScores.entries.sortedByDescending { it.value }.map { it.key }
        val sortedVibes = vibeScores.entries.sortedByDescending { it.value }.map { it.key }

        val isColdStart = totalInteractions < 2

        val summary = when {
            isColdStart -> "Exploring Trending & Global Hits"
            sortedVibes.take(2).contains(SmartRecommendationEngine.VibeType.ROMANTIC_MELODY) &&
            sortedVibes.take(2).contains(SmartRecommendationEngine.VibeType.LOFI_ACOUSTIC_CHILL) -> "Acoustic & Romantic Melodies"
            sortedVibes.take(2).contains(SmartRecommendationEngine.VibeType.HIP_HOP_PUNJABI) -> "Punjabi Hits & Urban Beats"
            sortedVibes.take(2).contains(SmartRecommendationEngine.VibeType.PARTY_DANCE) -> "High Energy Dance & Party Hits"
            sortedArtists.isNotEmpty() -> "Tailored for fans of ${sortedArtists.take(2).joinToString(" & ")}"
            else -> "Your Personalized Daily Soundscape"
        }

        UserTasteProfile(
            artistAffinities = artistScores,
            vibeAffinities = vibeScores,
            topArtists = sortedArtists,
            topVibes = if (sortedVibes.isNotEmpty()) sortedVibes else SmartRecommendationEngine.VibeType.values().toList(),
            currentTimeSlot = currentTimeSlot,
            isColdStart = isColdStart,
            primaryMoodSummary = summary
        )
    }

    /**
     * Dynamically builds personalized feeds including Supermix, Daily Mixes, Listen Again,
     * and Discover Fresh.
     */
    suspend fun computePersonalizedFeeds(
        dao: MusicDao,
        youtubeRepository: YouTubeRepository,
        trendingTracks: List<TrackModel>,
        topCharts: List<TrackModel>
    ): PersonalizedFeeds = withContext(Dispatchers.IO) {
        val profile = computeTasteProfile(dao)
        val allTrackEntities = try { dao.getAllTracksSync() } catch (_: Exception) { emptyList() }
        val allUserTracks = allTrackEntities.map { it.toModel() }
        val userKnownUrls = allUserTracks.mapNotNull { it.audioUrl }.toSet()

        // ── 1. Listen Again / Heavy Rotation ────────────────────────────────
        val now = System.currentTimeMillis()
        val listenAgainTracks = allTrackEntities
            .filter { it.playCount > 0 }
            .sortedByDescending { trackEntity ->
                // Recency-Frequency decay formula
                val lastPlayed = trackEntity.lastPlayedTimestamp ?: now
                val daysAgo = ((now - lastPlayed) / (1000.0 * 60 * 60 * 24)).coerceAtLeast(0.0)
                trackEntity.playCount * exp(-daysAgo / 10.0)
            }
            .map { it.toModel() }
            .take(12)

        // ── 2. Daily Mixes (Vibe-partitioned clusters) ──────────────────────
        val dailyMixes = mutableListOf<PersonalizedMix>()
        val topVibes = if (profile.isColdStart) {
            listOf(SmartRecommendationEngine.VibeType.ROMANTIC_MELODY, SmartRecommendationEngine.VibeType.HIP_HOP_PUNJABI, SmartRecommendationEngine.VibeType.PARTY_DANCE)
        } else {
            profile.topVibes.take(3)
        }

        topVibes.forEachIndexed { index, vibe ->
            val mixIndex = index + 1
            val vibeUserTracks = allUserTracks.filter { SmartRecommendationEngine.detectVibe(it) == vibe }
            val matchingTopArtists = profile.topArtists.filter { artist ->
                vibeUserTracks.any { it.artist.contains(artist, ignoreCase = true) }
            }.take(3)

            val query = buildDailyMixQuery(vibe, matchingTopArtists)
            val fetched = try {
                youtubeRepository.fetchMusic(query)
            } catch (e: Exception) {
                Log.w(TAG, "Failed fetching daily mix $mixIndex: ${e.message}")
                emptyList()
            }

            // Blend 40% known user tracks + 60% fresh matching tracks
            val blendedTracks = (vibeUserTracks.shuffled().take(6) + fetched.take(14))
                .distinctBy { it.audioUrl }
                .take(18)

            if (blendedTracks.isNotEmpty()) {
                val featuredArtists = matchingTopArtists.ifEmpty {
                    blendedTracks.map { SmartRecommendationEngine.extractPrimaryArtist(it.artist) }
                        .filter { it.isNotBlank() && !it.equals("Unknown", ignoreCase = true) && !it.contains("Offline", ignoreCase = true) }
                        .distinct()
                        .take(3)
                }

                val cover = blendedTracks.firstOrNull()?.albumArtUrl
                    ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80"

                dailyMixes.add(
                    PersonalizedMix(
                        id = "daily_mix_$mixIndex",
                        title = "Daily Mix $mixIndex",
                        subtitle = if (featuredArtists.isNotEmpty()) "Featuring ${featuredArtists.joinToString(", ")}" else getVibeDisplayName(vibe),
                        coverUrl = cover,
                        vibe = vibe,
                        tracks = blendedTracks,
                        categoryTag = mapVibeToCategory(vibe)
                    )
                )
            }
        }

        // ── 3. My Supermix (Signature blend of user taste + adjacent discoveries) ──
        val supermixCandidates = mutableListOf<TrackModel>()
        
        // 50% from user's most loved/downloaded tracks
        val coreTracks = (allUserTracks.filter { it.playcount > 1 } + allUserTracks.take(8)).distinctBy { it.audioUrl }
        supermixCandidates.addAll(coreTracks.shuffled().take(10))

        // 30% from user's top artists via YouTube Music
        if (profile.topArtists.isNotEmpty()) {
            val primarySeedArtist = profile.topArtists.first()
            try {
                val artistRadio = youtubeRepository.fetchMusic("$primarySeedArtist radio songs")
                supermixCandidates.addAll(artistRadio.take(8))
            } catch (_: Exception) {}
        }

        // 20% complementary trending matching top vibe
        val topVibe = profile.topVibes.firstOrNull() ?: SmartRecommendationEngine.VibeType.GENERAL_POP
        val vibeTrending = (trendingTracks + topCharts).filter { SmartRecommendationEngine.detectVibe(it) == topVibe }
        supermixCandidates.addAll(vibeTrending.take(6))

        val supermix = supermixCandidates.distinctBy { it.audioUrl }.shuffled().take(22)

        // ── 4. Because You Like [Top Artist] ─────────────────────────────────
        val becauseArtistPair: Pair<String, List<TrackModel>>? = if (profile.topArtists.isNotEmpty()) {
            val topArtist = profile.topArtists.first()
            try {
                val artistRecommendations = youtubeRepository.fetchMusic("$topArtist official songs")
                    .filter { it.audioUrl !in userKnownUrls }
                    .take(10)
                if (artistRecommendations.isNotEmpty()) {
                    Pair(topArtist, artistRecommendations)
                } else null
            } catch (_: Exception) { null }
        } else null

        // ── 5. Discover Fresh (High-affinity tracks never played before) ─────
        val discoverQuery = if (profile.topArtists.size >= 2) {
            "${profile.topArtists[0]} ${profile.topArtists[1]} similar trending hits"
        } else {
            "Latest Bollywood Punjabi Trending Music 2026"
        }
        val freshCandidates = try {
            youtubeRepository.fetchMusic(discoverQuery)
                .filter { it.audioUrl !in userKnownUrls }
                .take(12)
        } catch (_: Exception) { emptyList() }

        PersonalizedFeeds(
            supermix = if (supermix.isNotEmpty()) supermix else trendingTracks.take(15),
            dailyMixes = dailyMixes,
            listenAgain = if (listenAgainTracks.isNotEmpty()) listenAgainTracks else allUserTracks.take(8),
            becauseYouLikeArtist = becauseArtistPair,
            discoverFresh = freshCandidates,
            tasteSummary = profile.primaryMoodSummary
        )
    }

    private fun getCurrentTimeSlot(): TimeSlot {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> TimeSlot.MORNING
            in 11..16 -> TimeSlot.AFTERNOON
            in 17..20 -> TimeSlot.EVENING
            else -> TimeSlot.NIGHT
        }
    }

    private fun buildDailyMixQuery(vibe: SmartRecommendationEngine.VibeType, artists: List<String>): String {
        val artistPrefix = if (artists.isNotEmpty()) artists.joinToString(" ") + " " else ""
        return when (vibe) {
            SmartRecommendationEngine.VibeType.ROMANTIC_MELODY -> "${artistPrefix}Best Bollywood Hindi Love Songs"
            SmartRecommendationEngine.VibeType.HIP_HOP_PUNJABI -> "${artistPrefix}Punjabi Trending Hip Hop Beats"
            SmartRecommendationEngine.VibeType.PARTY_DANCE -> "${artistPrefix}Bollywood Club Party Dance Hits"
            SmartRecommendationEngine.VibeType.LOFI_ACOUSTIC_CHILL -> "${artistPrefix}Hindi Lo-Fi Chill Aesthetic Songs"
            SmartRecommendationEngine.VibeType.ROCK_EDM -> "${artistPrefix}EDM Bass Remix Songs"
            SmartRecommendationEngine.VibeType.DEVOTIONAL_FOLK -> "${artistPrefix}Devotional Folk Songs"
            SmartRecommendationEngine.VibeType.GENERAL_POP -> "${artistPrefix}Top Hindi Pop Songs"
        }
    }

    fun getVibeDisplayName(vibe: SmartRecommendationEngine.VibeType): String {
        return when (vibe) {
            SmartRecommendationEngine.VibeType.ROMANTIC_MELODY -> "Romantic Melodies"
            SmartRecommendationEngine.VibeType.HIP_HOP_PUNJABI -> "Punjabi & Hip-Hop"
            SmartRecommendationEngine.VibeType.PARTY_DANCE -> "Party & Dance"
            SmartRecommendationEngine.VibeType.LOFI_ACOUSTIC_CHILL -> "Lo-Fi & Chill"
            SmartRecommendationEngine.VibeType.ROCK_EDM -> "Remix & EDM"
            SmartRecommendationEngine.VibeType.DEVOTIONAL_FOLK -> "Devotional & Folk"
            SmartRecommendationEngine.VibeType.GENERAL_POP -> "Pop Hits"
        }
    }

    private fun mapVibeToCategory(vibe: SmartRecommendationEngine.VibeType): String {
        return when (vibe) {
            SmartRecommendationEngine.VibeType.PARTY_DANCE -> "Energize"
            SmartRecommendationEngine.VibeType.HIP_HOP_PUNJABI -> "Workout"
            SmartRecommendationEngine.VibeType.LOFI_ACOUSTIC_CHILL -> "Relax"
            SmartRecommendationEngine.VibeType.ROMANTIC_MELODY -> "Relax"
            SmartRecommendationEngine.VibeType.DEVOTIONAL_FOLK -> "Relax"
            SmartRecommendationEngine.VibeType.ROCK_EDM -> "Workout"
            SmartRecommendationEngine.VibeType.GENERAL_POP -> "Trending"
        }
    }
}
