package com.gaminghub.musicplayer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.gaminghub.musicplayer.TrackModel
import com.gaminghub.musicplayer.data.TrackEntity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile

/**
 * Metadata container representing track details stored in the Musify custom audio file trailer.
 */
data class MusifyTrackMetadata(
    val title: String,
    val artist: String,
    val audioUrl: String,
    val albumArtUrl: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val durationSeconds: Long = 0L,
    val uploaderChannel: String? = null,
    val isDevpick: Boolean = false,
    val playCount: Int = 0,
    val coverArtBase64: String? = null
)

fun MusifyTrackMetadata.toTrackModel(localPath: String? = null, resolvedArtUrl: String? = null): TrackModel {
    return TrackModel(
        title = title,
        artist = artist,
        audioUrl = localPath ?: audioUrl,
        albumArtUrl = resolvedArtUrl ?: albumArtUrl,
        album = album,
        genre = genre,
        durationSeconds = durationSeconds,
        uploaderChannel = uploaderChannel,
        isDevpick = isDevpick,
        playcount = playCount
    )
}

fun MusifyTrackMetadata.toTrackEntity(localPath: String, resolvedArtUrl: String? = null): TrackEntity {
    return TrackEntity(
        audioUrl = audioUrl,
        title = title,
        artist = artist,
        albumArtUrl = resolvedArtUrl ?: albumArtUrl,
        localPath = localPath,
        album = album,
        genre = genre,
        isDevpick = isDevpick,
        playCount = playCount
    )
}

/**
 * Utility to append and parse hidden End-Of-File (EOF) metadata trailers on .musify.m4a / .musify.mp3 audio files.
 *
 * Trailing bytes are ignored by standard media decoders (Stagefright, ExoPlayer, FFmpeg, VLC, Samsung Music),
 * keeping playback 100% functional across any music player while preserving Musify's private track metadata
 * across app uninstalls and reinstalls.
 */
object MusifyFileMetadataHelper {
    private const val TAG = "MusifyFileMeta"

    // 16-byte fixed magic tags for fast seeking
    // NOTE: MAGIC_FOOTER was fixed from 15→16 bytes in v1.7.4 to match the 16-byte read offset.
    // MAGIC_FOOTER_LEGACY is kept for reading files written before the fix (backward compat).
    private const val MAGIC_HEADER = "MUSIFY_META_V01\n"
    private const val MAGIC_FOOTER = "MUSIFY_TAIL_V01\n"           // exactly 16 bytes
    private const val MAGIC_FOOTER_LEGACY = "MUSIFY_TAG_V01\n"     // 15 bytes — legacy read only

    /**
     * Appends Musify metadata to the end of an audio file without corrupting its playback.
     */
    fun appendMetadata(file: File, metadata: MusifyTrackMetadata): Boolean {
        if (!file.exists() || !file.isFile) {
            Log.e(TAG, "Cannot append metadata: file does not exist: ${file.absolutePath}")
            return false
        }

        return try {
            val json = JSONObject().apply {
                put("title", metadata.title)
                put("artist", metadata.artist)
                put("audioUrl", metadata.audioUrl)
                put("albumArtUrl", metadata.albumArtUrl ?: "")
                put("album", metadata.album ?: "")
                put("genre", metadata.genre ?: "")
                put("durationSeconds", metadata.durationSeconds)
                put("uploaderChannel", metadata.uploaderChannel ?: "")
                put("isDevpick", metadata.isDevpick)
                put("playCount", metadata.playCount)
                if (!metadata.coverArtBase64.isNullOrBlank()) {
                    put("coverArtBase64", metadata.coverArtBase64)
                }
            }

            val jsonBytes = json.toString().toByteArray(Charsets.UTF_8)
            val payloadLength = jsonBytes.size

            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(raf.length())
                raf.write(MAGIC_HEADER.toByteArray(Charsets.UTF_8))
                raf.write(jsonBytes)
                raf.writeInt(payloadLength) // 4 bytes big-endian
                raf.write(MAGIC_FOOTER.toByteArray(Charsets.UTF_8))
            }

            Log.d(TAG, "Appended ${payloadLength + 36} bytes of metadata to ${file.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to append metadata to ${file.name}: ${e.message}", e)
            false
        }
    }

    /**
     * Quick check whether a file has a Musify EOF metadata trailer.
     * Supports both the current 16-byte footer and the legacy 15-byte footer.
     */
    fun hasMusifyMetadata(file: File): Boolean {
        if (!file.exists() || !file.isFile || file.length() < 38L) return false
        return try {
            RandomAccessFile(file, "r").use { raf ->
                // Check current 16-byte footer
                raf.seek(file.length() - 16L)
                val footerBytes = ByteArray(16)
                raf.readFully(footerBytes)
                val footerStr = String(footerBytes, Charsets.UTF_8)
                if (footerStr == MAGIC_FOOTER) return true
                // Check legacy 15-byte footer (files written before v1.7.4 fix)
                raf.seek(file.length() - 15L)
                val legacyFooterBytes = ByteArray(15)
                raf.readFully(legacyFooterBytes)
                String(legacyFooterBytes, Charsets.UTF_8) == MAGIC_FOOTER_LEGACY
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Reads Musify metadata from the audio file's EOF trailer.
     */
    fun readMetadata(file: File): MusifyTrackMetadata? {
        if (!file.exists() || !file.isFile || file.length() < 38L) return null
        return try {
            java.io.FileInputStream(file).use { fis ->
                readFromChannel(fis.channel, file.length(), file.name)
            }
        } catch (e: Exception) {
            // Fallback to RandomAccessFile
            try {
                RandomAccessFile(file, "r").use { raf ->
                    readFromChannel(raf.channel, file.length(), file.name)
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to read metadata from ${file.name}: ${e2.message}")
                null
            }
        }
    }

    /**
     * Reads Musify metadata from an Android content:// Uri via openFileDescriptor.
     */
    fun readMetadata(context: Context, uri: Uri): MusifyTrackMetadata? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                java.io.FileInputStream(pfd.fileDescriptor).use { fis ->
                    readFromChannel(fis.channel, pfd.statSize, uri.toString())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read metadata from Uri $uri: ${e.message}")
            null
        }
    }

    private fun readFromChannel(channel: java.nio.channels.FileChannel, fileLength: Long, debugName: String): MusifyTrackMetadata? {
        if (fileLength < 38L) return null
        return try {
            // Determine footer size: try current 16-byte footer first, fall back to legacy 15-byte
            val footerSize: Int
            channel.position(fileLength - 16L)
            val footerBuffer16 = java.nio.ByteBuffer.allocate(16)
            channel.read(footerBuffer16)
            val footer16Str = String(footerBuffer16.array(), Charsets.UTF_8)
            footerSize = when {
                footer16Str == MAGIC_FOOTER -> 16
                else -> {
                    // Check legacy 15-byte footer
                    channel.position(fileLength - 15L)
                    val footerBuffer15 = java.nio.ByteBuffer.allocate(15)
                    channel.read(footerBuffer15)
                    val footer15Str = String(footerBuffer15.array(), Charsets.UTF_8)
                    if (footer15Str == MAGIC_FOOTER_LEGACY) 15 else return null
                }
            }

            // 2. Read 4-byte payload length preceding footer
            channel.position(fileLength - footerSize - 4L)
            val lengthBuffer = java.nio.ByteBuffer.allocate(4)
            channel.read(lengthBuffer)
            lengthBuffer.flip()
            val jsonLength = lengthBuffer.int
            if (jsonLength <= 0 || jsonLength > 1_000_000 || jsonLength + 36L > fileLength) {
                Log.w(TAG, "Invalid json length $jsonLength in $debugName")
                return null
            }

            // 3. Verify magic header
            val headerOffset = fileLength - footerSize - 4L - jsonLength - 16L
            if (headerOffset < 0) return null
            channel.position(headerOffset)
            val headerBuffer = java.nio.ByteBuffer.allocate(16)
            channel.read(headerBuffer)
            val headerStr = String(headerBuffer.array(), Charsets.UTF_8)
            if (headerStr != MAGIC_HEADER) {
                Log.w(TAG, "Magic header mismatch in $debugName")
                return null
            }

            // 4. Read JSON payload
            val jsonBuffer = java.nio.ByteBuffer.allocate(jsonLength)
            channel.read(jsonBuffer)
            val jsonStr = String(jsonBuffer.array(), Charsets.UTF_8)
            val json = JSONObject(jsonStr)

            MusifyTrackMetadata(
                title = json.optString("title", debugName),
                artist = json.optString("artist", "Unknown Artist"),
                audioUrl = json.optString("audioUrl", ""),
                albumArtUrl = json.optString("albumArtUrl").takeIf { it.isNotBlank() },
                album = json.optString("album").takeIf { it.isNotBlank() },
                genre = json.optString("genre").takeIf { it.isNotBlank() },
                durationSeconds = json.optLong("durationSeconds", 0L),
                uploaderChannel = json.optString("uploaderChannel").takeIf { it.isNotBlank() },
                isDevpick = json.optBoolean("isDevpick", false),
                playCount = json.optInt("playCount", 0),
                coverArtBase64 = json.optString("coverArtBase64").takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading metadata channel for $debugName: ${e.message}")
            null
        }
    }

    /**
     * Extracts base64 cover art from metadata and writes it to internal app cache so it can be loaded offline.
     */
    fun saveExtractedCoverArt(context: Context, audioUrl: String, base64Art: String): String? {
        return try {
            val artDir = File(context.filesDir, "musify_art").apply { if (!exists()) mkdirs() }
            val filename = "art_${audioUrl.hashCode().toString().replace("-", "n")}.jpg"
            val artFile = File(artDir, filename)
            if (!artFile.exists() || artFile.length() == 0L) {
                val bytes = Base64.decode(base64Art, Base64.DEFAULT)
                artFile.writeBytes(bytes)
            }
            Uri.fromFile(artFile).toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract and save cover art: ${e.message}")
            null
        }
    }

    /**
     * Downloads and compresses a lightweight thumbnail (<= 40KB) to embed in the audio file.
     */
    fun fetchAndCompressImageBase64(imageUrl: String?, client: OkHttpClient): String? {
        if (imageUrl.isNullOrBlank() || !imageUrl.startsWith("http")) return null
        return try {
            val request = Request.Builder()
                .url(imageUrl)
                .header("User-Agent", CommonUtils.CURRENT_USER_AGENT)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val bytes = response.body?.bytes() ?: return null

            if (bytes.size <= 35 * 1024) {
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else {
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
                val scaled = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
                val bos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 75, bos)
                Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch artwork for embedding: ${e.message}")
            null
        }
    }

    /**
     * Cleans raw filenames or titles (e.g. "Barbaadiyan.musify", "Aayi_Nai__From__Stree_2___-720316462.mp3")
     * into clean, user-friendly display titles ("Barbaadiyan", "Aayi Nai (From Stree 2)").
     */
    fun cleanDisplayTitle(rawTitle: String): String {
        var cleaned = rawTitle.trim()
        val extensions = listOf(".musify.m4a", ".musify.mp3", ".musify", ".m4a", ".mp3", ".wav", ".aac", ".ogg")
        for (ext in extensions) {
            if (cleaned.endsWith(ext, ignoreCase = true)) {
                cleaned = cleaned.substring(0, cleaned.length - ext.length)
                break
            }
        }
        // Remove trailing hash suffix like _1621664564 or _-720316462 or _308792695
        cleaned = cleaned.replace(Regex("_[0-9\\-]+$"), "")
        // Clean multi-underscores used by filename sanitizers
        cleaned = cleaned.replace("____", " - ").replace("___", " ").replace("__", " ")
        cleaned = cleaned.replace("_", " ").trim()
        return cleaned.ifBlank { rawTitle }
    }

    /**
     * Extracts ID3 title and artist from an audio file using MediaMetadataRetriever if present.
     */
    fun extractId3Metadata(file: File): Pair<String?, String?> {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
            retriever.release()
            Pair(
                title?.takeIf { it.isNotBlank() },
                artist?.takeIf { it.isNotBlank() && it != "<unknown>" }
            )
        } catch (_: Exception) {
            Pair(null, null)
        }
    }
}
