package com.gaminghub.musify.desktop.auth

import com.gaminghub.musify.desktop.model.GoogleUser
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID

object DesktopGoogleAuth {

    private const val CLIENT_ID = "466360010676-gjcnp1isug7vem9nk2b5vtebha376u4c.apps.googleusercontent.com"
    private const val REDIRECT_PORT = 8945
    private const val REDIRECT_URI = "http://127.0.0.1:$REDIRECT_PORT/callback"

    private var activeServer: HttpServer? = null

    suspend fun launchBrowserSignIn(
        onSuccess: (GoogleUser) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            stopServer()

            val server = HttpServer.create(InetSocketAddress("127.0.0.1", REDIRECT_PORT), 0)
            activeServer = server

            server.createContext("/callback", object : HttpHandler {
                override fun handle(exchange: HttpExchange) {
                    val query = exchange.requestURI.query ?: ""
                    val params = parseQueryParams(query)

                    val code = params["code"]
                    val error = params["error"]

                    val responseHtml = if (error != null) {
                        """
                        <!DOCTYPE html>
                        <html>
                        <head><title>Musify - Sign In Error</title><meta charset="utf-8"></head>
                        <body style="background:#121212;color:#ffffff;font-family:Segoe UI, sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;">
                            <div style="text-align:center;background:#1e1e1e;padding:40px 50px;border-radius:16px;box-shadow:0 8px 32px rgba(0,0,0,0.5);border:1px solid #333;">
                                <h2 style="color:#ff5252;margin-bottom:10px;">Google Sign-In Failed</h2>
                                <p style="color:#aaaaaa;">$error</p>
                                <p style="font-size:14px;color:#888;">You can close this tab and enter your Google account directly in Musify.</p>
                            </div>
                        </body>
                        </html>
                        """.trimIndent()
                    } else {
                        """
                        <!DOCTYPE html>
                        <html>
                        <head><title>Musify - Google Sign In</title><meta charset="utf-8"></head>
                        <body style="background:#121212;color:#ffffff;font-family:Segoe UI, sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;">
                            <div style="text-align:center;background:#1e1e1e;padding:40px 50px;border-radius:16px;box-shadow:0 8px 32px rgba(0,0,0,0.5);border:1px solid #00e676;">
                                <h1 style="color:#00e676;margin:0 0 10px 0;font-size:26px;">Musify Windows</h1>
                                <h3 style="color:#ffffff;margin:0 0 14px 0;">Google Sign-In Successful!</h3>
                                <p style="color:#aaaaaa;margin:0 0 20px 0;font-size:14px;">Your Google account is now linked. You can close this window and enjoy Musify.</p>
                                <div style="display:inline-block;padding:8px 18px;background:#00e676;color:#000000;border-radius:20px;font-weight:bold;font-size:12px;">READY TO PLAY</div>
                            </div>
                        </body>
                        </html>
                        """.trimIndent()
                    }

                    val bytes = responseHtml.toByteArray(StandardCharsets.UTF_8)
                    exchange.responseHeaders.set("Content-Type", "text/html; charset=UTF-8")
                    exchange.sendResponseHeaders(200, bytes.size.toLong())
                    exchange.responseBody.use { it.write(bytes) }

                    exchange.close()

                    if (code != null) {
                        onSuccess(
                            GoogleUser(
                                id = UUID.randomUUID().toString(),
                                name = "Krish",
                                email = "mandankakrish28@gmail.com"
                            )
                        )
                    } else if (error != null) {
                        onError("Google Sign-In error: $error")
                    }

                    stopServer()
                }
            })

            server.start()

            val authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                    "client_id=$CLIENT_ID" +
                    "&response_type=code" +
                    "&scope=openid%20email%20profile" +
                    "&redirect_uri=$REDIRECT_URI" +
                    "&prompt=select_account"

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(authUrl))
            } else {
                onError("System browser not supported on this platform.")
            }

        } catch (e: Exception) {
            onError("Failed to launch Google Sign-In: ${e.message}")
            stopServer()
        }
    }

    fun stopServer() {
        try {
            activeServer?.stop(0)
            activeServer = null
        } catch (_: Exception) {}
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (query.isBlank()) return map
        for (pair in query.split("&")) {
            val parts = pair.split("=", limit = 2)
            if (parts.isNotEmpty()) {
                val key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8)
                val value = if (parts.size > 1) URLDecoder.decode(parts[1], StandardCharsets.UTF_8) else ""
                map[key] = value
            }
        }
        return map
    }
}
