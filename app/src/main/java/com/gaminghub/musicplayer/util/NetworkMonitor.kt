package com.gaminghub.musicplayer.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Lifecycle-aware Network Connectivity Monitor.
 * Detects online/offline transitions and fires callbacks when connection is acquired,
 * allowing pending offline play history and events to sync automatically to Firebase.
 */
class NetworkMonitor private constructor(private val context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isOnline = MutableStateFlow(checkInitialConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val onConnectionRestoredListeners = mutableListOf<suspend () -> Unit>()

    init {
        registerNetworkCallback()
    }

    private fun checkInitialConnectivity(): Boolean {
        return try {
            val network = connectivityManager?.activeNetwork ?: return false
            val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    private fun registerNetworkCallback() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager?.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val wasOffline = !_isOnline.value
                    _isOnline.value = true
                    Log.d(TAG, "Network connection acquired (wasOffline=$wasOffline)")
                    if (wasOffline) {
                        notifyConnectionRestored()
                    }
                }

                override fun onLost(network: Network) {
                    Log.d(TAG, "Network connection lost")
                    _isOnline.value = checkInitialConnectivity()
                }

                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    val wasOffline = !_isOnline.value
                    _isOnline.value = hasInternet
                    if (wasOffline && hasInternet) {
                        notifyConnectionRestored()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback: ${e.message}")
        }
    }

    fun addOnConnectionRestoredListener(listener: suspend () -> Unit) {
        synchronized(onConnectionRestoredListeners) {
            onConnectionRestoredListeners.add(listener)
        }
    }

    private fun notifyConnectionRestored() {
        scope.launch {
            val listeners = synchronized(onConnectionRestoredListeners) {
                onConnectionRestoredListeners.toList()
            }
            for (listener in listeners) {
                try {
                    listener()
                } catch (e: Exception) {
                    Log.w(TAG, "Error running onConnectionRestored listener: ${e.message}")
                }
            }
        }
    }

    companion object {
        private const val TAG = "NetworkMonitor"

        @Volatile
        private var INSTANCE: NetworkMonitor? = null

        fun getInstance(context: Context): NetworkMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkMonitor(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
