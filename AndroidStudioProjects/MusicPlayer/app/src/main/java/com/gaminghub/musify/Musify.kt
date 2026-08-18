package com.gaminghub.musify

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlinx.coroutines.launch
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import java.util.concurrent.TimeUnit
import com.gaminghub.musicplayer.NewPipeDownloader

class Musify : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // 2% of disk or 50MB
                    .build()
            }
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        try {
            val cookieJar = object : CookieJar {
                private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    val domain = (url.topPrivateDomain() ?: url.host).removePrefix(".")
                    synchronized(cookieStore) {
                        val domainCookies = cookieStore.getOrPut(domain) { mutableListOf() }
                        cookies.forEach { newCookie ->
                            // Proactively reject 'PENDING' consent cookies which block extraction
                            if (newCookie.name == "CONSENT" && newCookie.value.contains("PENDING", true)) return@forEach
                            
                            domainCookies.removeAll { it.name == newCookie.name }
                            domainCookies.add(newCookie)
                        }
                    }
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    val host = url.host
                    val result = mutableListOf<Cookie>()
                    
                    synchronized(cookieStore) {
                        cookieStore.forEach { (domain, domainCookies) ->
                            if (host == domain || host.endsWith(".$domain")) {
                                result.addAll(domainCookies)
                            }
                        }
                    }

                    if (host.contains("youtube", ignoreCase = true) || host.contains("google", ignoreCase = true)) {
                        // RFC 6265 strictly forbids leading dots in cookie domains for Builder.
                        // We normalize to ensure no dots slip through from host or manual strings.
                        val rawDomain = if (host.contains("youtube")) "youtube.com" else "google.com"
                        val cookieDomain = rawDomain.removePrefix(".")
                        
                        // Ensure CONSENT, SOCS, and PREF are present to avoid redirect loops
                        if (result.none { it.name == "CONSENT" }) {
                            try {
                                result.add(Cookie.Builder()
                                    .name("CONSENT")
                                    .value("YES+cb.20250210-09-p0.en+FX+908")
                                    .domain(cookieDomain)
                                    .path("/")
                                    .build())
                            } catch (_: Exception) {
                                // Fallback to host-only if domain build fails
                                result.add(Cookie.Builder()
                                    .name("CONSENT")
                                    .value("YES+cb.20250210-09-p0.en+FX+908")
                                    .domain(host.removePrefix("."))
                                    .path("/")
                                    .build())
                            }
                        }
                        
                        if (result.none { it.name == "SOCS" }) {
                            result.add(Cookie.Builder()
                                .name("SOCS")
                                .value("CAESEwgDEgk0ODE3Nzk3MjQaAmVuIAEaBgiA_LyaBg")
                                .domain(cookieDomain)
                                .path("/")
                                .build())
                        }

                        if (result.none { it.name == "PREF" }) {
                            result.add(Cookie.Builder()
                                .name("PREF")
                                .value("f6=40000000&hl=en")
                                .domain(cookieDomain)
                                .path("/")
                                .build())
                        }
                    }
                    
                    return result.filter { it.expiresAt > System.currentTimeMillis() }
                }
            }

            // Unify into a single high-performance shared client
            sharedOkHttpClient = OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()

            // Perform essential library initialization synchronously to guarantee readiness before UI interactions
            kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    // Initialize NewPipe with the unified client
                    NewPipe.init(NewPipeDownloader(sharedOkHttpClient), org.schabi.newpipe.extractor.localization.Localization.DEFAULT)
                    
                    // Initialize the Extraction Manager with the unified client
                    com.gaminghub.musify.util.StreamExtractionManager.init(
                        com.gaminghub.musicplayer.data.MusicDatabase.getInstance(this@Musify).dao,
                        sharedOkHttpClient
                    )
                } catch (e: Exception) {
                    android.util.Log.e("Musify", "Library Init Failed: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        lateinit var sharedOkHttpClient: OkHttpClient
            private set
    }
}
