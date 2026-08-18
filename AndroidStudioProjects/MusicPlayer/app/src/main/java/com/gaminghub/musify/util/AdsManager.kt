package com.gaminghub.musify.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Interface to abstract ad logic.
 */
interface AdsManager {
    fun initialize(context: Context)
    
    @Composable
    fun BannerAd(modifier: Modifier)
}
