package com.gaminghub.musicplayer.data.recommendation

import android.util.Log
import com.gaminghub.musicplayer.TrackModel
import com.gaminghub.musicplayer.util.SmartRecommendationEngine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Cloud Synchronization & Event Streaming for Next-Gen Recommendation Pipeline.
 *
 * Implements the behavioral ingestion pipeline required by Graph Neural Networks (LightGCN),
 * Two-Tower Vector Embedding Trainers, and Contextual Bandit algorithms.
 */
data class RecommendationInteractionEvent(
    val eventId: String = "",
    val userId: String = "",
    val trackTitle: String = "",
    val trackArtist: String = "",
    val trackAudioUrl: String = "",
    val vibe: String = "",
    val durationPlayedMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false,
    val isExplorationTrack: Boolean = false,
    val sessionConsecutiveSkips: Int = 0,
    val timeSlot: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

object RecommendationEventSync {
    private const val TAG = "RecEventSync"
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * Dispatches a playback event to Firebase Firestore for downstream ML model training.
     */
    fun logInteraction(
        track: TrackModel,
        durationPlayedMs: Long,
        totalDurationMs: Long,
        isCompleted: Boolean,
        isSkipped: Boolean,
        isExploration: Boolean,
        consecutiveSkips: Int,
        timeSlotName: String
    ) {
        val user = auth.currentUser ?: return
        val vibe = SmartRecommendationEngine.detectVibe(track).name

        val event = RecommendationInteractionEvent(
            eventId = "${System.currentTimeMillis()}_${(1000..9999).random()}",
            userId = user.uid,
            trackTitle = track.title,
            trackArtist = track.artist,
            trackAudioUrl = track.audioUrl ?: "",
            vibe = vibe,
            durationPlayedMs = durationPlayedMs,
            totalDurationMs = totalDurationMs,
            isCompleted = isCompleted,
            isSkipped = isSkipped,
            isExplorationTrack = isExploration,
            sessionConsecutiveSkips = consecutiveSkips,
            timeSlot = timeSlotName,
            timestamp = System.currentTimeMillis()
        )

        coroutineScope.launch {
            try {
                firestore.collection("users")
                    .document(user.uid)
                    .collection("recommendation_events")
                    .document(event.eventId)
                    .set(event)
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Offline/Failed logging recommendation event: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Error logging rec event: ${e.message}")
            }
        }
    }
}
