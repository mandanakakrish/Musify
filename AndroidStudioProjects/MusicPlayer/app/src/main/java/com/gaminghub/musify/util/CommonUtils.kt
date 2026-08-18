package com.gaminghub.musify.util

import com.gaminghub.musify.LyricLine

object CommonUtils {
    /**
     * Adaptive Identity Switching Strategy:
     * 1. DESKTOP for Discovery (NewPipe scraping compatibility)
     * 2. MOBILE for Playback (Bypass throttling/skipping)
     */
    const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"
    const val MOBILE_USER_AGENT = "com.google.android.youtube/19.45.36 (Linux; U; Android 15; en_US; Pixel 9 Pro XL; Build/AP3A.241005.015; HW/tensor-g4)"
    
    // Single synchronized User-Agent for player, extractor & stream verifier
    const val CURRENT_USER_AGENT = DESKTOP_USER_AGENT
    const val USER_AGENT = CURRENT_USER_AGENT

    /**
     * Parses LRC format string into a list of [LyricLine].
     * LRC format standard: [mm:ss.xx] Lyric text
     */
    fun parseLrc(lrcContent: String?): List<LyricLine> {
        if (lrcContent.isNullOrBlank()) return emptyList()
        
        val lines = mutableListOf<LyricLine>()
        val regex = Regex("\\[(\\d+):(\\d+\\.?\\d*)\\](.*)")
        
        lrcContent.lines().forEach { line ->
            regex.find(line)?.let { match ->
                try {
                    val min = match.groupValues[1].toLong()
                    val sec = match.groupValues[2].toFloat()
                    val text = match.groupValues[3].trim()
                    val timeMs = (min * 60 * 1000) + (sec * 1000).toLong()
                    lines.add(LyricLine(timeMs, text))
                } catch (e: Exception) {
                    // Skip malformed lines
                }
            }
        }
        return lines.sortedBy { it.timeMs }
    }
}
