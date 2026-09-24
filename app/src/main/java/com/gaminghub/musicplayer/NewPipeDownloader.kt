package com.gaminghub.musicplayer

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

import com.gaminghub.musicplayer.util.CommonUtils

class NewPipeDownloader(private val client: OkHttpClient) : Downloader() {

    private val tag = "NewPipeDownloader"
    
    private val browserUserAgent = CommonUtils.CURRENT_USER_AGENT

    override fun execute(request: Request): Response {
        val method = request.httpMethod()
        var url = request.url()
        val headers = request.headers()
        val data = request.dataToSend()

        // Use standard bypass for watch pages only
        if ((url.contains("youtube.com/watch") || url.contains("youtu.be/")) && !url.contains("bpctr")) {
            val separator = if (url.contains("?")) "&" else "?"
            url += "${separator}bpctr=9999999999&has_verified=1"
        }

        val okHttpRequestBuilder = okhttp3.Request.Builder().url(url)
        
        okHttpRequestBuilder.header("User-Agent", browserUserAgent)
        okHttpRequestBuilder.header("Accept", "*/*")
        okHttpRequestBuilder.header("Accept-Language", "en-US,en;q=0.9")
        
        if (url.contains("youtube.com") || url.contains("googlevideo.com")) {
            val isMusic = url.contains("music.youtube.com")
            val domain = if (isMusic) "music.youtube.com" else "www.youtube.com"
            val origin = "https://$domain"
            
            okHttpRequestBuilder.header("Referer", "$origin/")
            okHttpRequestBuilder.header("Origin", origin)
        }

        if (headers != null) {
            for (key in headers.keys) {
                val values = headers[key]
                if (!values.isNullOrEmpty()) {
                    okHttpRequestBuilder.header(key, values[0])
                    for (i in 1 until values.size) {
                        okHttpRequestBuilder.addHeader(key, values[i])
                    }
                }
            }
        }

        if ("POST".equals(method, ignoreCase = true)) {
            val contentType = headers?.get("Content-Type")?.get(0) ?: "application/json"
            val body = data?.toRequestBody(contentType.toMediaTypeOrNull()) ?: "".toByteArray().toRequestBody()
            okHttpRequestBuilder.post(body)
        } else {
            okHttpRequestBuilder.method(method, null)
        }

        val okHttpRequest = okHttpRequestBuilder.build()
        val okHttpResponse = try {
            client.newCall(okHttpRequest).execute()
        } catch (e: Exception) {
            Log.e(tag, "Network error: ${e.message}")
            throw e
        }

        val responseBody = okHttpResponse.body.string()
        
        // Log if we are still hitting the reload loop
        if (responseBody.contains("window.location.reload()") || okHttpResponse.code == 429) {
            Log.w(tag, "Bot detection triggered for $url. Code: ${okHttpResponse.code}")
        }
        
        return Response(
            okHttpResponse.code, 
            okHttpResponse.message, 
            okHttpResponse.headers.toMultimap(),
            responseBody, 
            okHttpResponse.request.url.toString()
        )
    }
}
