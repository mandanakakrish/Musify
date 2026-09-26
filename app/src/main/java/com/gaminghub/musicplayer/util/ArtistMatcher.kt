package com.gaminghub.musicplayer.util

import com.gaminghub.musicplayer.TrackModel

/**
 * Intelligent Multi-Artist Matching and Relationship Engine.
 * Handles many-to-many relationships:
 * - One artist has many songs.
 * - One song has many artists (collaborations, features, duets, bands).
 */
object ArtistMatcher {

    /**
     * Regex matching common artist delimiters and collaboration connectors:
     * Comma, Ampersand, "and", "feat.", "ft.", "featuring", "/", "x", "X", "+", ";", "with".
     */
    private val DELIMITER_REGEX = Regex(
        "\\s*(?:,|&|;|/|\\+|\\bx\\b|\\bX\\b|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\band\\b|\\bwith\\b)\\s*",
        RegexOption.IGNORE_CASE
    )

    /**
     * Splits a multi-artist string into individual clean artist names.
     * E.g.:
     *  - "Mithoon, Arijit Singh & Shashaa Tirupati" -> ["Mithoon", "Arijit Singh", "Shashaa Tirupati"]
     *  - "Diljit Dosanjh x Sia" -> ["Diljit Dosanjh", "Sia"]
     *  - "The Weeknd ft. Daft Punk" -> ["The Weeknd", "Daft Punk"]
     *  - "Pritam, Arijit Singh, Shilpa Rao" -> ["Pritam", "Arijit Singh", "Shilpa Rao"]
     */
    fun splitArtists(rawArtist: String): List<String> {
        if (rawArtist.isBlank()) return emptyList()
        return rawArtist
            .split(DELIMITER_REGEX)
            .map { cleanArtistName(it) }
            .filter { it.isNotBlank() && it.length >= 2 && !it.equals("Various Artists", ignoreCase = true) }
            .distinct()
    }

    /**
     * Removes noise, brackets, quotes, and punctuation around an artist name.
     */
    private fun cleanArtistName(name: String): String {
        return name.trim()
            .trim('\"', '\'', '(', ')', '[', ']', '{', '}', '-', '•', ':')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Checks if a track belongs to a target artist based on many-to-many relationship:
     * 1. Direct or split match in song artist field (e.g., "Arijit Singh" in "Jasleen Royal & Arijit Singh")
     * 2. Substring match for compound names
     * 3. Featured credit in song title (e.g., "Song Name (feat. Arijit Singh)")
     */
    fun matchesArtist(songArtist: String, songTitle: String, targetArtist: String): Boolean {
        val cleanTarget = targetArtist.trim().lowercase()
        if (cleanTarget.isBlank()) return false

        val lowerSongArtist = songArtist.trim().lowercase()
        if (lowerSongArtist == cleanTarget) return true

        // 1. Check each split artist name
        val splitList = splitArtists(songArtist)
        for (artist in splitList) {
            val lowerA = artist.lowercase()
            if (lowerA == cleanTarget) return true
            if (cleanTarget.length >= 4 && lowerA.contains(cleanTarget)) return true
            if (lowerA.length >= 4 && cleanTarget.contains(lowerA)) return true
        }

        // 2. Substring match in artist credits (e.g. "Arijit Singh" in "Pritam & Arijit Singh")
        if (cleanTarget.length >= 4 && lowerSongArtist.contains(cleanTarget)) return true

        // 3. Featured in track title: "Title (feat. Arijit Singh)" or "Title ft. Arijit Singh"
        val lowerTitle = songTitle.lowercase()
        if (lowerTitle.contains(cleanTarget)) {
            val hasFeatureIndicator = lowerTitle.contains("feat") ||
                    lowerTitle.contains("ft") ||
                    lowerTitle.contains("with") ||
                    lowerTitle.contains("&") ||
                    lowerTitle.contains(",")
            if (hasFeatureIndicator) return true
        }

        return false
    }

    /**
     * Checks if a [TrackModel] belongs to the given artist.
     */
    fun matchesArtist(track: TrackModel, targetArtist: String): Boolean {
        return matchesArtist(track.artist, track.title, targetArtist)
    }

    /**
     * Filters a list of tracks to only those that feature the target artist,
     * maintaining max limit (e.g. 20 songs).
     */
    fun filterTracksForArtist(tracks: List<TrackModel>, targetArtist: String, maxCount: Int = 20): List<TrackModel> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<TrackModel>()

        for (track in tracks) {
            if (matchesArtist(track, targetArtist)) {
                val key = track.title.lowercase().replace(Regex("[^a-z0-9]"), "")
                if (seen.add(key)) {
                    result.add(track)
                    if (result.size >= maxCount) break
                }
            }
        }
        return result
    }
}
