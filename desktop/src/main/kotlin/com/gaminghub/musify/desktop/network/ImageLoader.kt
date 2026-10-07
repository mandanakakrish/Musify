package com.gaminghub.musify.desktop.network

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jetbrains.skia.Image
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

object DesktopImageLoader {

    private val memoryCache = ConcurrentHashMap<String, ImageBitmap>()
    private val diskCacheDir by lazy {
        val dir = File(System.getProperty("user.home"), ".musify/cache/images")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    suspend fun loadImage(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null

        memoryCache[url]?.let { return@withContext it }

        val hash = hashUrl(url)
        val cachedFile = File(diskCacheDir, "$hash.img")

        if (cachedFile.exists()) {
            try {
                val bytes = cachedFile.readBytes()
                val skiaImage = Image.makeFromEncoded(bytes)
                val bitmap = skiaImage.toComposeImageBitmap()
                memoryCache[url] = bitmap
                return@withContext bitmap
            } catch (_: Exception) {
                cachedFile.delete()
            }
        }

        try {
            val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
            val response = DesktopYouTubeRepository.httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    cachedFile.writeBytes(bytes)
                    val skiaImage = Image.makeFromEncoded(bytes)
                    val bitmap = skiaImage.toComposeImageBitmap()
                    memoryCache[url] = bitmap
                    return@withContext bitmap
                }
            }
        } catch (_: Exception) {}

        null
    }

    private fun hashUrl(url: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

@Composable
fun rememberAsyncImage(url: String?): ImageBitmap? {
    val bitmapState = remember(url) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url) {
        if (!url.isNullOrBlank()) {
            bitmapState.value = DesktopImageLoader.loadImage(url)
        } else {
            bitmapState.value = null
        }
    }

    return bitmapState.value
}
