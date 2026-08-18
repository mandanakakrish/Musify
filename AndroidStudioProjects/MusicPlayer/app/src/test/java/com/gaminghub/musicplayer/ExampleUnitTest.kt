package com.gaminghub.musicplayer

import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Test
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ExampleUnitTest {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Test
    fun testYouTubeSearch() {
        println("--- Testing NewPipe Search Extractor ---")
        val term = "trending songs"
        try {
            org.schabi.newpipe.extractor.NewPipe.init(
                object : org.schabi.newpipe.extractor.downloader.Downloader() {
                    override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): org.schabi.newpipe.extractor.downloader.Response {
                        val okReq = okhttp3.Request.Builder().url(request.url())
                        request.headers()?.forEach { (k, v) ->
                            v.forEach { okReq.addHeader(k, it) }
                        }
                        if (request.httpMethod().equals("POST", ignoreCase = true)) {
                            val mediaType = "application/json".toMediaTypeOrNull()
                            val body = (request.dataToSend() ?: byteArrayOf()).toRequestBody(mediaType)
                            okReq.post(body)
                        }
                        val resp = client.newCall(okReq.build()).execute()
                        val body = resp.body?.string() ?: ""
                        return org.schabi.newpipe.extractor.downloader.Response(
                            resp.code,
                            resp.message,
                            resp.headers.toMultimap(),
                            body,
                            resp.request.url.toString()
                        )
                    }
                }
            )

            val service = org.schabi.newpipe.extractor.ServiceList.YouTube
            val searchExtractor = service.getSearchExtractor(term)
            searchExtractor.fetchPage()

            val items = searchExtractor.initialPage.items
            println("NewPipe Search Items Count: ${items.size}")
            items.take(5).forEach { item ->
                println("   -> Item: ${item.name} (${item.url})")
            }
        } catch (e: Exception) {
            println("NewPipe Search Failed: ${e.message}")
            e.printStackTrace()
        }
    }

    @Test
    fun testNewPipeExtractor() {
        println("--- Testing NewPipe Extractor Directly ---")
        val videoUrl = "https://www.youtube.com/watch?v=4NRXx6U8ABQ" // The Weeknd - Blinding Lights

        try {
            org.schabi.newpipe.extractor.NewPipe.init(
                object : org.schabi.newpipe.extractor.downloader.Downloader() {
                    override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): org.schabi.newpipe.extractor.downloader.Response {
                        val okReq = okhttp3.Request.Builder().url(request.url())
                        request.headers()?.forEach { (k, v) ->
                            v.forEach { okReq.addHeader(k, it) }
                        }
                        if (request.httpMethod().equals("POST", ignoreCase = true)) {
                            val mediaType = "application/json".toMediaTypeOrNull()
                            val body = (request.dataToSend() ?: byteArrayOf()).toRequestBody(mediaType)
                            okReq.post(body)
                        }
                        val resp = client.newCall(okReq.build()).execute()
                        val body = resp.body?.string() ?: ""
                        return org.schabi.newpipe.extractor.downloader.Response(
                            resp.code,
                            resp.message,
                            resp.headers.toMultimap(),
                            body,
                            resp.request.url.toString()
                        )
                    }
                }
            )

            val service = org.schabi.newpipe.extractor.ServiceList.YouTube
            val extractor = service.getStreamExtractor(videoUrl)
            extractor.fetchPage()

            println("NewPipe Extracted Title: ${extractor.name}")
            val audioStreams = extractor.audioStreams
            println("NewPipe Audio Streams Count: ${audioStreams?.size ?: 0}")
            audioStreams?.forEach {
                println("   -> Format: ${it.format}, Bitrate: ${it.bitrate}, URL: ${it.url?.take(70)}...")
            }
        } catch (e: Exception) {
            println("NewPipe Extractor Failed: ${e::class.java.simpleName} - ${e.message}")
            e.printStackTrace()
        }
    }
}