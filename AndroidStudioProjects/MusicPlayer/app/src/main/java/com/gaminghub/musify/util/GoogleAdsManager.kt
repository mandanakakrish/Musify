package com.gaminghub.musify.util

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*

/**
 * Google Play Services implementation of AdsManager using AdMob.
 */
class GoogleAdsManager : AdsManager {
    override fun initialize(context: Context) {
        try {
            MobileAds.initialize(context) {}
        } catch (_: Exception) {}
    }

    @Composable
    override fun BannerAd(modifier: Modifier) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    // Test Ad Unit ID
                    adUnitId = "ca-app-pub-3940256099942544/6300978111"
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = { it.destroy() }
        )
    }
}
