package com.gaminghub.musicplayer

import android.app.Application
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.launch
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.Localization
import java.util.UUID
import java.util.concurrent.TimeUnit


class MusicPlayerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Only DynamicColors must stay on main thread — it hooks into Activity lifecycle
        DynamicColors.applyToActivitiesIfAvailable(this)

        // Everything else moves to a background IO thread to avoid blocking the main thread
        // and delaying the first Compose frame (which causes the 3-4 sec blank-screen gap)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                com.gaminghub.musicplayer.util.NetworkProxyManager.init(this@MusicPlayerApplication)
            } catch (_: Exception) {}

            try {
                com.google.firebase.FirebaseApp.initializeApp(this@MusicPlayerApplication)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    try {
                        com.google.firebase.firestore.FirebaseFirestore.setLoggingEnabled(BuildConfig.DEBUG)
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}

            try {
                val cookieJar = object : CookieJar {
                    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()
                    private val visitorId = UUID.randomUUID().toString().replace("-", "").take(16)

                    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                        val domain = url.topPrivateDomain() ?: url.host
                        synchronized(cookieStore) {
                            val domainCookies = cookieStore.getOrPut(domain) { mutableListOf() }
                            cookies.forEach { newCookie ->
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

                        if (host.contains("youtube") || host.contains("google")) {
                            if (result.none { it.name == "CONSENT" }) {
                                result.add(Cookie.Builder()
                                    .name("CONSENT")
                                    .value("YES+cb.20230531-04-p0.en+FX+908")
                                    .domain(host).path("/").build())
                            }
                            if (result.none { it.name == "PREF" }) {
                                result.add(Cookie.Builder()
                                    .name("PREF")
                                    .value("f4=4000000&tz=UTC&hl=en&gl=US&f6=40000000&f7=100")
                                    .domain(host).path("/").build())
                            }
                            if (result.none { it.name == "SOCS" }) {
                                result.add(Cookie.Builder()
                                    .name("SOCS")
                                    .value("CAESEwgDEgk0ODE3Nzk3MjQaAmVuIAEaBgiA_LyaBg")
                                    .domain(host).path("/").build())
                            }
                            if (result.none { it.name == "VISITOR_INFO1_LIVE" }) {
                                result.add(Cookie.Builder()
                                    .name("VISITOR_INFO1_LIVE")
                                    .value(visitorId)
                                    .domain(host).path("/").build())
                            }
                        }

                        return result.filter { it.expiresAt > System.currentTimeMillis() }
                    }
                }

                val client = OkHttpClient.Builder()
                    .cookieJar(cookieJar)
                    .proxySelector(com.gaminghub.musicplayer.util.NetworkProxyManager.proxySelector)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .retryOnConnectionFailure(true)
                    .build()

                NewPipe.init(NewPipeDownloader(client), Localization.DEFAULT)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
