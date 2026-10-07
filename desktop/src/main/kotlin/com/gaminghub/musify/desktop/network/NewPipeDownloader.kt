package com.gaminghub.musify.desktop.network

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response

class DesktopNewPipeDownloader(private val client: OkHttpClient) : Downloader() {

    private val browserUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    override fun execute(request: Request): Response {
        val method = request.httpMethod()
        var url = request.url()
        val headers = request.headers()
        val data = request.dataToSend()

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
        } else if ("HEAD".equals(method, ignoreCase = true)) {
            okHttpRequestBuilder.head()
        } else {
            okHttpRequestBuilder.get()
        }

        val call = client.newCall(okHttpRequestBuilder.build())
        val okHttpResponse = call.execute()

        val responseBody = okHttpResponse.body?.string() ?: ""
        val responseHeaders = mutableMapOf<String, List<String>>()
        for (headerName in okHttpResponse.headers.names()) {
            responseHeaders[headerName] = okHttpResponse.headers.values(headerName)
        }

        val latestUrl = okHttpResponse.request.url.toString()
        return Response(
            okHttpResponse.code,
            okHttpResponse.message,
            responseHeaders,
            responseBody,
            latestUrl
        )
    }
}
