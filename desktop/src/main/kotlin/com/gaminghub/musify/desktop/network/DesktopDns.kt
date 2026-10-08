package com.gaminghub.musify.desktop.network

import okhttp3.Dns
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException

/**
 * Resilient Multi-Tier DNS Resolver for Desktop Musify:
 * 1. System DNS (Standard WinSock/OS getaddrinfo)
 * 2. DNS-over-HTTPS (DoH) via Cloudflare (1.1.1.1) and Google (8.8.8.8) to bypass ISP blockages/glitches
 * 3. Static Anycast IP Fallback table for essential YouTube and Google API domains
 */
object DesktopDns : Dns {

    private val systemDns = Dns.SYSTEM

    private val staticFallbackIps = mapOf(
        "www.youtube.com" to listOf("142.251.150.4", "142.251.154.4", "142.251.153.4", "142.251.155.4"),
        "music.youtube.com" to listOf("142.250.70.46", "142.250.70.78", "192.178.174.93", "192.178.174.91"),
        "youtubei.googleapis.com" to listOf("172.217.118.4", "172.217.113.4", "172.217.112.4", "172.217.116.4"),
        "i.ytimg.com" to listOf("172.253.118.119", "64.233.170.119", "142.251.175.119"),
        "firestore.googleapis.com" to listOf("142.250.70.42", "142.250.70.74", "192.178.174.74")
    )

    override fun lookup(hostname: String): List<InetAddress> {
        // 1. Try Standard System DNS
        try {
            val systemAddresses = systemDns.lookup(hostname)
            if (systemAddresses.isNotEmpty()) {
                return systemAddresses
            }
        } catch (_: Exception) {
            // Fall through to DoH
        }

        // 2. Try DNS-over-HTTPS (Cloudflare / Google)
        try {
            val dohAddresses = resolveViaDoh(hostname)
            if (dohAddresses.isNotEmpty()) {
                println("DesktopDns: Resolved $hostname via DoH -> ${dohAddresses.map { it.hostAddress }}")
                return dohAddresses
            }
        } catch (e: Exception) {
            System.err.println("DesktopDns: DoH resolution failed for $hostname: ${e.message}")
        }

        // 3. Static Anycast IP Fallback
        val fallback = staticFallbackIps[hostname.lowercase()]
        if (fallback != null) {
            val addresses = fallback.mapNotNull { ip ->
                try {
                    InetAddress.getByName(ip)
                } catch (_: Exception) {
                    null
                }
            }
            if (addresses.isNotEmpty()) {
                println("DesktopDns: Using static fallback IPs for $hostname -> $fallback")
                return addresses
            }
        }

        throw UnknownHostException("Unable to resolve host '$hostname' via System DNS, DoH, or Static Fallbacks")
    }

    private fun resolveViaDoh(hostname: String): List<InetAddress> {
        val providers = listOf(
            "https://1.1.1.1/dns-query?name=${URLEncoder.encode(hostname, "UTF-8")}&type=A",
            "https://dns.google/resolve?name=${URLEncoder.encode(hostname, "UTF-8")}&type=A"
        )

        for (endpoint in providers) {
            try {
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Accept", "application/dns-json")
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.connectTimeout = 3500
                conn.readTimeout = 3500

                if (conn.responseCode in 200..299) {
                    val responseText = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(responseText)
                    val answers = json.optJSONArray("Answer") ?: continue
                    val result = mutableListOf<InetAddress>()
                    for (i in 0 until answers.length()) {
                        val ans = answers.optJSONObject(i) ?: continue
                        val ip = ans.optString("data")
                        if (ip.isNotBlank() && !ip.contains(":")) { // IPv4
                            try {
                                result.add(InetAddress.getByName(ip))
                            } catch (_: Exception) {}
                        }
                    }
                    if (result.isNotEmpty()) return result
                }
            } catch (_: Exception) {}
        }
        return emptyList()
    }
}
