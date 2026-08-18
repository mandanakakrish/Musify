package com.gaminghub.musify.data.repository

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.gaminghub.musify.TrackModel

class LocalMediaRepository(private val context: Context) {
    private val tag = "LocalMediaRepository"

    fun getLocalTracks(): List<TrackModel> {
        val tracks = mutableListOf<TrackModel>()
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.GENRE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri, projection, selection, null, sortOrder
            )
            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val genreCol = c.getColumnIndex(MediaStore.Audio.Media.GENRE)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                while (c.moveToNext()) {
                    c.getLong(idCol)
                    val title = c.getString(titleCol) ?: "Unknown"
                    val artist = c.getString(artistCol) ?: "Unknown Artist"
                    val album = c.getString(albumCol)
                    val genre = if (genreCol >= 0) c.getString(genreCol) else null
                    val path = c.getString(dataCol) ?: continue
                    val albumId = c.getLong(albumIdCol)
                    val artUri = Uri.parse("content://media/external/audio/albumart/$albumId").toString()
                    tracks.add(
                        TrackModel(
                            title = title,
                            artist = artist,
                            audioUrl = path,
                            albumArtUrl = artUri,
                            album = album,
                            genre = genre
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error loading local tracks: ${e.message}", e)
        } finally {
            cursor?.close()
        }
        return tracks
    }
}
