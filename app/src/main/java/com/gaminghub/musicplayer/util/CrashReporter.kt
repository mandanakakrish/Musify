package com.gaminghub.musicplayer.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashReporter {
    private const val TAG = "MusifyCrashReporter"

    private val crashlytics: FirebaseCrashlytics? by lazy {
        try {
            FirebaseCrashlytics.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseCrashlytics not initialized: ${e.message}")
            null
        }
    }

    fun log(message: String) {
        Log.d(TAG, message)
        try {
            crashlytics?.log(message)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging to Crashlytics: ${e.message}")
        }
    }

    fun recordException(throwable: Throwable) {
        Log.e(TAG, "Exception recorded: ${throwable.message}", throwable)
        try {
            crashlytics?.recordException(throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Error recording exception to Crashlytics: ${e.message}")
        }
    }

    fun setUserId(userId: String) {
        try {
            crashlytics?.setUserId(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting user ID: ${e.message}")
        }
    }

    fun setCustomKey(key: String, value: String) {
        try {
            crashlytics?.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting custom key: ${e.message}")
        }
    }

    fun setCustomKey(key: String, value: Boolean) {
        try {
            crashlytics?.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting custom key: ${e.message}")
        }
    }
}
