package com.gaminghub.musify.util

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

class AnalyticsManager(private val analytics: FirebaseAnalytics) {

    // You only write the Firebase logic once, right here.
    fun logFeatureClick(featureName: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT) {
            param(FirebaseAnalytics.Param.ITEM_ID, featureName)
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "feature_click")
        }
    }
    
    // You can add more specific events later, like error tracking
    fun logError(errorType: String) {
        analytics.logEvent("app_error") {
            param("error_type", errorType)
        }
    }
}