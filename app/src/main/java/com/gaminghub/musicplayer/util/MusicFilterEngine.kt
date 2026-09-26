package com.gaminghub.musicplayer.util

import com.gaminghub.musicplayer.TrackModel

/**
 * Intelligent Music Filtering and Ranking Engine.
 * Ensures that only songs uploaded by official channels/publishers (and genuine studio tracks)
 * are fetched, while filtering out non-music YouTube content (vlogs, reactions, tutorials, memes, etc.).
 */
object MusicFilterEngine {

    // ── Negative Patterns (Disqualifiers for non-music videos) ──────────────────
    private val JUNK_TITLE_REGEX = Regex(
        "(?i)\\b(reaction|reacting|reacts|review|reviewing|unboxing|tutorial|how\\s*to|" +
        "guitar\\s*(lesson|cover|tab|tabs)|piano\\s*(lesson|tutorial|cover)|drum\\s*(lesson|cover|tutorial)|" +
        "bass\\s*(lesson|cover|tutorial)|karaoke\\s*version|instrumental\\s*only|" +
        "gameplay|walkthrough|playthrough|let's\\s*play|gaming\\s*moments|" +
        "analysis|breakdown|explained|behind\\s*the\\s*scenes|making\\s*of|bloopers|interview|" +
        "podcast|vlog|episode|ep\\.?\\s*\\d+|full\\s*movie|trailer|teaser|movie\\s*clip|" +
        "tiktok\\s*compilation|meme|shorts|10\\s*hours|10\\s*hrs|1\\s*hour\\s*loop|extended\\s*1\\s*hour|" +
        "speed\\s*up\\s*1\\s*hour|slowed\\s*\\+\\s*reverb\\s*1\\s*hour|sound\\s*effect|sfx)\\b"
    )

    private val JUNK_CHANNEL_REGEX = Regex(
        "(?i)\\b(gaming|gamer|vlogs|vlog|news|tech|reaction|reactions|reviews|podcast|" +
        "tv|clips|movies|cinema|comedy|memes|anime|fortnite|minecraft|roblox|pubg|cod|gta)\\b"
    )

    // ── Major & Prominent Official Record Labels & Publishers ─────────────────
    private val OFFICIAL_LABELS = setOf(
        "t-series", "tseries", "zee music company", "sony music", "sonymusicindiavevo",
        "universal music group", "warner music", "spinnin' records", "spinnin records",
        "monstercat", "ultra music", "ultra records", "def jam", "republic records",
        "interscope records", "atlantic records", "columbia records", "rca records",
        "epic records", "fueled by ramen", "owsla", "armada music", "dim mak",
        "mad decent", "nocopyrightsounds", "ncs", "trap nation", "mrsuicidesheep",
        "proximity", "selected.", "stmpd rcrds", "anjunabeats", "musical freedom",
        "kontor.tv", "speed records", "yrf", "yash raj films", "tips official",
        "saregama music", "aditya music", "lahari music", "geetha arts",
        "sony music south", "think music india", "white hill music", "times music",
        "eros now", "svf", "hybe labels", "sm entertainment", "jyp entertainment",
        "yg entertainment", "big hit labels", "starship", "virgin records",
        "island records", "capitol records", "asylum records", "parlophone records",
        "bad boy entertainment", "motown records", "sub pop", "epitaph records",
        "dirty hit", "fearless records", "hopeless records", "sumerian records",
        "century media records", "nuclear blast", "metal blade records",
        "napalm records", "audiotree", "colors", "tiny desk concerts", "vevo"
    )

    // ── Noise Cleaning Regex (for clean display titles) ─────────────────────────
    private val TITLE_CLEAN_REGEX = Regex(
        "(?i)(\\s*[\\[\\(](?:official\\s*(?:music\\s*video|video|audio|lyric\\s*video|visualizer|track|hd|4k|remastered|version|hq)?|audio|lyrics|lyric\\s*video|visualizer|closed-captioned|explicit|clean|4k|hd|hq|prod\\.?\\s*by[^\\)\\]]*|dir\\.?\\s*by[^\\)\\]]*)[\\]\\)]|\\s*\\|\\s*(?:official\\s*(?:music\\s*video|video|audio)|full\\s*song|lyrical\\s*video).*$)",
        RegexOption.IGNORE_CASE
    )

    /**
     * Assesses whether a video item is a random non-music video that should be discarded.
     */
    fun isJunkVideo(title: String, uploader: String, durationSeconds: Long): Boolean {
        // Discard very short clips (< 40s) as they are typically shorts/intros
        if (durationSeconds in 1..39) return true

        // Discard overly long uploads (> 15 mins = 900s) as they are typically podcasts/10-hour loops/movies
        if (durationSeconds > 900) return true

        // Discard titles containing blacklist terms
        if (JUNK_TITLE_REGEX.containsMatchIn(title)) return true

        // Discard channels containing blacklist tags unless it's a known official music topic channel
        if (!isTopicChannel(uploader) && JUNK_CHANNEL_REGEX.containsMatchIn(uploader)) return true

        return false
    }

    /**
     * Checks if the uploader is a YouTube Music Auto-Generated Artist Topic channel.
     * Topic channels represent the purest distributor audio feed (Universal, Sony, Warner, etc.).
     */
    fun isTopicChannel(uploader: String): Boolean {
        return uploader.endsWith(" - Topic", ignoreCase = true) || uploader.contains("- Topic", ignoreCase = true)
    }

    /**
     * Checks if the uploader is a VEVO channel.
     */
    fun isVevoChannel(uploader: String): Boolean {
        return uploader.endsWith("VEVO", ignoreCase = true) || uploader.contains("VEVO", ignoreCase = true)
    }

    /**
     * Checks if the uploader is a verified major/indie music label or publisher.
     */
    fun isOfficialLabel(uploader: String): Boolean {
        val lower = uploader.lowercase().trim()
        return OFFICIAL_LABELS.any { label -> lower.contains(label) }
    }

    /**
     * Checks if the channel name has strong official music artist indicators.
     */
    fun isOfficialArtistChannel(uploader: String): Boolean {
        if (isTopicChannel(uploader) || isVevoChannel(uploader) || isOfficialLabel(uploader)) return true
        val lower = uploader.lowercase()
        return lower.contains("official") || lower.contains("records") || lower.contains("music") || lower.contains("band")
    }

    /**
     * Computes a quality score for ranking music search results.
     * Higher score means higher probability of being the exact official track.
     */
    fun scoreTrack(title: String, uploader: String, durationSeconds: Long): Int {
        if (isJunkVideo(title, uploader, durationSeconds)) return -9999

        var score = 0

        // 1. Topic channel is top priority (+1000)
        if (isTopicChannel(uploader)) {
            score += 1000
        }

        // 2. VEVO channel (+850)
        if (isVevoChannel(uploader)) {
            score += 850
        }

        // 3. Official Music Label (+800)
        if (isOfficialLabel(uploader)) {
            score += 800
        }

        // 4. Official Artist Channel (+500)
        if (isOfficialArtistChannel(uploader)) {
            score += 500
        }

        // 5. Title contains official tags (+400)
        val lowerTitle = title.lowercase()
        if (lowerTitle.contains("official audio") || lowerTitle.contains("official music video") || lowerTitle.contains("official video")) {
            score += 400
        } else if (lowerTitle.contains("lyric video") || lowerTitle.contains("lyrics") || lowerTitle.contains("visualizer") || lowerTitle.contains("audio")) {
            score += 250
        }

        // 6. Optimal song duration: 50s to 600s (+150)
        if (durationSeconds in 50..600) {
            score += 150
        }

        // Base score for genuine music candidate (+100)
        score += 100

        return score
    }

    /**
     * Cleans metadata (removes "[Official Music Video]", " - Topic", etc.)
     * for a clean, beautiful UI presentation.
     */
    fun cleanTrack(
        rawTitle: String,
        rawUploader: String,
        audioUrl: String,
        albumArtUrl: String,
        album: String? = null
    ): TrackModel {
        var cleanTitle = rawTitle.replace(TITLE_CLEAN_REGEX, "").trim()
        cleanTitle = cleanTitle.trim(' ', '-', '|', ':', '•', '[', ']', '(', ')')

        var cleanArtist = rawUploader
            .replace(" - Topic", "", ignoreCase = true)
            .replace("- Topic", "", ignoreCase = true)
            .replace("VEVO", "", ignoreCase = true)
            .replace("Official", "", ignoreCase = true)
            .trim(' ', '-', '|', ':', '•')

        if (cleanTitle.contains(" - ") && isOfficialLabel(rawUploader)) {
            val parts = cleanTitle.split(" - ", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                cleanArtist = parts[0].trim()
                cleanTitle = parts[1].trim()
            }
        }

        return TrackModel(
            title = if (cleanTitle.isNotBlank()) cleanTitle else rawTitle,
            artist = if (cleanArtist.isNotBlank()) cleanArtist else rawUploader,
            audioUrl = audioUrl,
            albumArtUrl = albumArtUrl,
            album = album,
            uploaderChannel = rawUploader
        )
    }

    /**
     * Filters a list of raw stream candidates and returns only valid, high-quality music tracks,
     * sorted with the best official studio versions first.
     */
    fun filterAndRankCandidates(
        candidates: List<CandidateTrack>,
        officialOnly: Boolean = false
    ): List<TrackModel> {
        val scored = candidates.mapNotNull { candidate ->
            val score = scoreTrack(candidate.title, candidate.uploader, candidate.durationSeconds)
            if (score <= 0) {
                null // Discard junk or non-music
            } else if (officialOnly && score < 400) {
                null // Discard if official only requested and track does not meet official standard
            } else {
                Pair(score, candidate)
            }
        }

        // Sort descending by score (highest quality / official first)
        val sortedCandidates = scored.sortedByDescending { it.first }.map { it.second }

        // Deduplicate tracks with identical clean title and artist
        val seen = mutableSetOf<String>()
        val result = mutableListOf<TrackModel>()

        for (candidate in sortedCandidates) {
            val cleaned = cleanTrack(candidate.title, candidate.uploader, candidate.audioUrl, candidate.albumArtUrl, candidate.album)
            val key = "${cleaned.title.lowercase()}_${cleaned.artist.lowercase()}"
            if (seen.add(key)) {
                result.add(cleaned)
            }
        }

        return result
    }

    data class CandidateTrack(
        val title: String,
        val uploader: String,
        val audioUrl: String,
        val albumArtUrl: String,
        val durationSeconds: Long,
        val album: String? = null
    )
}
