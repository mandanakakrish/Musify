package com.gaminghub.musicplayer.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Centralized Proxy Manager for Musify.
 * Configures system-wide ProxySelector, OkHttpClient routing, and ExoPlayer streaming.
 */
object NetworkProxyManager {
    private const val TAG = "NetworkProxyManager"

    @Volatile
    private var activeProxy: Proxy? = null

    @Volatile
    var isProxyEnabled: Boolean = false
        private set

    @Volatile
    var currentHost: String = "103.47.67.134"
        private set

    @Volatile
    var currentPort: Int = 8080
        private set

    val proxySelector: ProxySelector = object : ProxySelector() {
        override fun select(uri: URI?): List<Proxy> {
            val p = activeProxy
            return if (isProxyEnabled && p != null) {
                listOf(p)
            } else {
                listOf(Proxy.NO_PROXY)
            }
        }

        override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
            Log.w(TAG, "Proxy connection failed for $uri at $sa: ${ioe?.message}")
        }
    }

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("Musify_settings", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("use_proxy", false)
        val address = prefs.getString("proxy_address", "103.47.67.134:8080") ?: "103.47.67.134:8080"

        try {
            ProxySelector.setDefault(proxySelector)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set default ProxySelector: ${e.message}")
        }

        updateProxy(enabled, address)
    }

    fun updateProxy(enabled: Boolean, address: String) {
        isProxyEnabled = enabled
        try {
            val clean = address.trim()
            if (clean.isNotBlank()) {
                val parts = clean.split(":")
                val host = parts[0].trim()
                val port = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 8080

                currentHost = host
                currentPort = port

                if (enabled && host.isNotBlank()) {
                    val socketAddress = InetSocketAddress(host, port)
                    activeProxy = Proxy(Proxy.Type.HTTP, socketAddress)

                    System.setProperty("http.proxyHost", host)
                    System.setProperty("http.proxyPort", port.toString())
                    System.setProperty("https.proxyHost", host)
                    System.setProperty("https.proxyPort", port.toString())
                    Log.i(TAG, "Proxy enabled and active: $host:$port")
                } else {
                    activeProxy = null
                    clearSystemProxy()
                    Log.i(TAG, "Proxy disabled, routing direct")
                }
            } else {
                activeProxy = null
                clearSystemProxy()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying proxy $address: ${e.message}")
            activeProxy = null
            clearSystemProxy()
        }
    }

    private fun clearSystemProxy() {
        System.clearProperty("http.proxyHost")
        System.clearProperty("http.proxyPort")
        System.clearProperty("https.proxyHost")
        System.clearProperty("https.proxyPort")
    }

    fun getProxy(): Proxy? = if (isProxyEnabled) activeProxy else null

    /**
     * Tests connectivity of a specific proxy host and port against YouTube servers.
     */
    suspend fun testProxyConnection(host: String, port: Int): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(host.trim(), port))
            val testClient = OkHttpClient.Builder()
                .proxy(proxy)
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(6, TimeUnit.SECONDS)
                .build()

            val startTime = System.currentTimeMillis()
            val request = Request.Builder()
                .url("https://www.youtube.com/generate_204")
                .header("User-Agent", "Mozilla/5.0")
                .build()

            testClient.newCall(request).execute().use { response ->
                val duration = System.currentTimeMillis() - startTime
                if (response.isSuccessful || response.code == 204 || response.code == 302 || response.code == 200) {
                    Result.success(duration)
                } else {
                    Result.failure(IOException("Proxy responded with HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
