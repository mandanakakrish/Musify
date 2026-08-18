package com.gaminghub.musify.data.repository
 
import android.util.Log
 import com.gaminghub.musify.AlbumModel
import com.gaminghub.musify.ArtistModel
import com.gaminghub.musify.TrackModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.Extractor
import org.schabi.newpipe.extractor.channel.ChannelExtractor
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor

enum class SearchSource { YOUTUBE, YT_MUSIC }

class YouTubeRepository {
    private val tag = "YouTubeRepository"

    suspend fun fetchMusic(
        term: String,
        source: SearchSource,
        officialOnly: Boolean = false,
        rankAndFilter: (List<TrackModel>) -> List<TrackModel> = { it }
    ): List<TrackModel> = withContext(Dispatchers.IO) {
        try {
            val service = ServiceList.YouTube

            val searchExtractor = service.getSearchExtractor(term)
            searchExtractor.fetchPage()

            val tracks = ArrayList<TrackModel>()
            val fetchedItems = searchExtractor.initialPage.items
            if (fetchedItems != null) {
                for (infoItem in fetchedItems) {
                    when (infoItem) {
                        is StreamInfoItem -> {
                            val uploader = infoItem.uploaderName ?: "Unknown"
                            val cleanArtist = uploader
                                .replace(" - Topic", "", ignoreCase = true)
                                .replace(" VEVO", "", ignoreCase = true)
                                .replace(" Official", "", ignoreCase = true)
                                .trim()

                            if (officialOnly) {
                                val isOfficial = uploader.contains("Topic", ignoreCase = true) ||
                                        uploader.contains("VEVO", ignoreCase = true) ||
                                        uploader.contains("Official", ignoreCase = true)
                                if (!isOfficial) continue
                            }

                            val rawUrl = infoItem.thumbnails.firstOrNull()?.url
                            val highResArt = if (infoItem.url.contains("youtube.com") || infoItem.url.contains("youtu.be")) {
                                rawUrl?.replace("hqdefault", "maxresdefault")
                            } else {
                                rawUrl?.replace(Regex("=w\\d+-h\\d+.*"), "=w1200-h1200-l90-rj")
                                    ?.replace(Regex("s\\d+-c-k-c0x00ffffff-no-rj.*"), "s1200-p-l90-rj")
                                    ?.replace("/s\\d+/", "/s1200/")
                            } ?: rawUrl

                            tracks.add(
                                TrackModel(
                                    title = infoItem.name ?: "Unknown",
                                    artist = cleanArtist, // Use clean name for UI
                                    audioUrl = infoItem.url,
                                    albumArtUrl = highResArt ?: "",
                                    artistId = infoItem.uploaderUrl?.substringAfterLast("/"),
                                    uploaderName = uploader // Keep original for backend
                                )
                            )
                        }
                    }
                }
            }

            val resultTracks = rankAndFilter(tracks)
            
            // 2. If initial NewPipe search yields empty list, try official YouTube Web rendering parser
            val finalTracks = if (resultTracks.isNotEmpty()) {
                resultTracks
            } else {
                val webTracks = fetchFromYouTubeWeb(term)
                if (webTracks.isNotEmpty()) webTracks else getFallbackTracks()
            }

            return@withContext finalTracks
        } catch (e: Exception) {
            Log.e(tag, "Error fetching music for $term: ${e.message}", e)
            val webTracks = fetchFromYouTubeWeb(term)
            if (webTracks.isNotEmpty()) return@withContext webTracks
            return@withContext getFallbackTracks()
        }
    }

    private fun fetchFromYouTubeWeb(term: String): List<TrackModel> {
        val list = ArrayList<TrackModel>()
        try {
            val encodedTerm = java.net.URLEncoder.encode(term, "UTF-8")
            val searchUrl = "https://www.youtube.com/results?search_query=$encodedTerm"
            val request = okhttp3.Request.Builder()
                .url(searchUrl)
                .header("User-Agent", com.gaminghub.musify.util.CommonUtils.CURRENT_USER_AGENT)
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()
            
            val html = com.gaminghub.musify.Musify.sharedOkHttpClient.newCall(request).execute().use { it.body.string() }
            
            val regex = Regex("var ytInitialData = (\\{.*?\\});</script>")
            val match = regex.find(html) ?: Regex("window\\[\"ytInitialData\"\\] = (\\{.*?\\});").find(html)
            val jsonStr = match?.groupValues?.get(1) ?: return emptyList()
            
            val root = org.json.JSONObject(jsonStr)
            val contents = root.optJSONObject("contents")
                ?.optJSONObject("twoColumnSearchResultsRenderer")
                ?.optJSONObject("primaryContents")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: return emptyList()

            for (i in 0 until contents.length()) {
                val itemSection = contents.getJSONObject(i).optJSONObject("itemSectionRenderer") ?: continue
                val items = itemSection.optJSONArray("contents") ?: continue
                
                for (j in 0 until items.length()) {
                    val item = items.getJSONObject(j)
                    val videoRenderer = item.optJSONObject("videoRenderer") ?: continue
                    val videoId = videoRenderer.optString("videoId").takeIf { !it.isNullOrBlank() } ?: continue
                    
                    val title = videoRenderer.optJSONObject("title")
                        ?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: "Song"
                    val artist = videoRenderer.optJSONObject("ownerText")
                        ?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: "Artist"
                    
                    val thumbs = videoRenderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                    val thumbUrl = thumbs?.optJSONObject(thumbs.length() - 1)?.optString("url")
                        ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                    list.add(
                        TrackModel(
                            title = title,
                            artist = artist.replace(" - Topic", "", ignoreCase = true).trim(),
                            audioUrl = "https://www.youtube.com/watch?v=$videoId",
                            albumArtUrl = thumbUrl
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed parsing official youtube web search: ${e.message}")
        }
        return list
    }

    private fun getFallbackTracks(): List<TrackModel> = listOf(
        TrackModel(
            title = "Blinding Lights",
            artist = "The Weeknd",
            audioUrl = "https://www.youtube.com/watch?v=4NRXx6U8ABQ",
            albumArtUrl = "https://i.ytimg.com/vi/4NRXx6U8ABQ/maxresdefault.jpg"
        ),
        TrackModel(
            title = "Shape of You",
            artist = "Ed Sheeran",
            audioUrl = "https://www.youtube.com/watch?v=JGwWNGJdvx8",
            albumArtUrl = "https://i.ytimg.com/vi/JGwWNGJdvx8/maxresdefault.jpg"
        ),
        TrackModel(
            title = "Starboy",
            artist = "The Weeknd ft. Daft Punk",
            audioUrl = "https://www.youtube.com/watch?v=34Na4j8AVgA",
            albumArtUrl = "https://i.ytimg.com/vi/34Na4j8AVgA/maxresdefault.jpg"
        ),
        TrackModel(
            title = "As It Was",
            artist = "Harry Styles",
            audioUrl = "https://www.youtube.com/watch?v=H5v3kku4y6Q",
            albumArtUrl = "https://i.ytimg.com/vi/H5v3kku4y6Q/maxresdefault.jpg"
        ),
        TrackModel(
            title = "Levitating",
            artist = "Dua Lipa",
            audioUrl = "https://www.youtube.com/watch?v=TUVcZfQe-Kw",
            albumArtUrl = "https://i.ytimg.com/vi/TUVcZfQe-Kw/maxresdefault.jpg"
        ),
        TrackModel(
            title = "Flowers",
            artist = "Miley Cyrus",
            audioUrl = "https://www.youtube.com/watch?v=G7KNmW9a75Y",
            albumArtUrl = "https://i.ytimg.com/vi/G7KNmW9a75Y/maxresdefault.jpg"
        ),
        TrackModel(
            title = "Stay",
            artist = "The Kid LAROI & Justin Bieber",
            audioUrl = "https://www.youtube.com/watch?v=kTJczUoc26U",
            albumArtUrl = "https://i.ytimg.com/vi/kTJczUoc26U/maxresdefault.jpg"
        ),
        TrackModel(
            title = "Someone You Loved",
            artist = "Lewis Capaldi",
            audioUrl = "https://www.youtube.com/watch?v=zABLecsR5UE",
            albumArtUrl = "https://i.ytimg.com/vi/zABLecsR5UE/maxresdefault.jpg"
        )
    )

    suspend fun fetchRelatedMusic(url: String): List<TrackModel> = withContext(Dispatchers.IO) {
        if (url.isBlank() || !(url.contains("youtube.com") || url.contains("youtu.be"))) return@withContext emptyList()
        try {
            val extractor = ServiceList.YouTube.getStreamExtractor(url)
            extractor.fetchPage()
            val relatedList = ArrayList<TrackModel>()
            val items = extractor.relatedStreams as? List<*>
            if (items != null) {
                for (item in items) {
                    if (item is StreamInfoItem) {
                        relatedList.add(
                            TrackModel(
                                title = item.name ?: "Unknown",
                                artist = item.uploaderName ?: "Unknown",
                                audioUrl = item.url,
                                albumArtUrl = item.thumbnails.firstOrNull()?.url ?: ""
                            )
                        )
                    }
                }
            }
            return@withContext relatedList
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch related music: ${e.message}")
            return@withContext emptyList()
        }
    }

    suspend fun fetchArtistDetails(artistUrl: String): ArtistModel? = withContext(Dispatchers.IO) {
        try {
            val service = NewPipe.getService("YouTube")
            val extractor = service.getChannelExtractor(artistUrl)
            extractor.fetchPage()

            val topTracks = (extractor as? org.schabi.newpipe.extractor.ListExtractor<*>)?.getInitialPage()?.items
                ?.filterIsInstance<StreamInfoItem>()
                ?.map { item: StreamInfoItem ->
                    TrackModel(
                        title = item.name,
                        artist = extractor.name,
                        audioUrl = item.url,
                        albumArtUrl = item.thumbnails.firstOrNull()?.url ?: "",
                        artistId = extractor.id,
                        uploaderName = extractor.name
                    )
                } ?: emptyList()

            return@withContext ArtistModel(
                id = extractor.id,
                name = extractor.name.replace(" - Topic", ""),
                imageUrl = null, // FIXME: Unresolved reference 'avatarUrl' or 'getAvatarUrl()' in v0.26.0 JitPack
                bio = extractor.description,
                subscribers = (extractor as? ChannelExtractor)?.subscriberCount?.toString(),
                topTracks = topTracks
            )
        } catch (e: Exception) {
            Log.e(tag, "Error fetching artist details: ${e.message}")
            null
        }
    }

    suspend fun fetchAlbumDetails(albumUrl: String): AlbumModel? = withContext(Dispatchers.IO) {
        try {
            val service = NewPipe.getService("YouTube")
            val extractor = service.getPlaylistExtractor(albumUrl)
            extractor.fetchPage()

            val tracks = (extractor as? org.schabi.newpipe.extractor.ListExtractor<*>)?.getInitialPage()?.items
                ?.filterIsInstance<StreamInfoItem>()
                ?.map { item: StreamInfoItem ->
                    TrackModel(
                        title = item.name,
                        artist = item.uploaderName ?: "Unknown",
                        audioUrl = item.url,
                        albumArtUrl = item.thumbnails.firstOrNull()?.url ?: "",
                        artistId = item.uploaderUrl?.substringAfterLast("/")
                    )
                } ?: emptyList()

            return@withContext AlbumModel(
                id = extractor.id,
                title = extractor.name,
                artist = (extractor as? PlaylistExtractor)?.uploaderName ?: "Unknown",
                imageUrl = null, // FIXME: Unresolved reference 'thumbnailUrl' or 'getThumbnailUrl()' in v0.26.0 JitPack
                tracks = tracks
            )
        } catch (e: Exception) {
            Log.e(tag, "Error fetching album details: ${e.message}")
            null
        }
    }
}
