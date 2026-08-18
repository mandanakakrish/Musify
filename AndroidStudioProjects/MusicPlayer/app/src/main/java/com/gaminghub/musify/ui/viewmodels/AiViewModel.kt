package com.gaminghub.musify.ui.viewmodels

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.gaminghub.musicplayer.BuildConfig
import com.gaminghub.musify.data.repository.SearchSource
import com.gaminghub.musify.data.repository.YouTubeRepository
import com.gaminghub.musify.util.AnalyticsManager
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.FunctionCallPart
import com.google.ai.client.generativeai.type.FunctionResponsePart
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.Tool
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.defineFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false,
    val actionPerformed: String? = null // e.g. "▶ Playing Shape of You"
)


@UnstableApi
class AiViewModel(
    application: Application,
    private val analyticsManager: AnalyticsManager
) : AndroidViewModel(application) {
    private val tag = "AiViewModel"
    private val youtubeRepository = YouTubeRepository()
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("Hey! I'm **Musify AI** ✨\n\nI can control your music, search songs, create playlists, and more. Try:\n• *\"Play Shape of You\"*\n• *\"Set a 30 min sleep timer\"*\n• *\"What's playing right now?\"*", isUser = false))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    // References to other ViewModels — set from the composable
    var playbackViewModel: PlaybackViewModel? = null
    var libraryViewModel: LibraryViewModel? = null

    // --- Gemini Function Declarations ---
    private val searchAndPlayFn = defineFunction(
        name = "search_and_play",
        description = "Search for a song, artist, or album on YouTube Music and play it. Use this when the user wants to listen to something specific.",
        parameters = listOf(
            Schema.str("query", "The search query — song name, artist name, or 'artist - song'"),
        )
    )

    private val controlPlaybackFn = defineFunction(
        name = "control_playback",
        description = "Control music playback. Actions: play, pause, toggle, next, previous, shuffle_on, shuffle_off, repeat_on, repeat_off",
        parameters = listOf(
            Schema.str("action", "The playback action to perform"),
        )
    )

    private val setSleepTimerFn = defineFunction(
        name = "set_sleep_timer",
        description = "Set a sleep timer to stop music after specified minutes. Use 0 to cancel.",
        parameters = listOf(
            Schema.int("minutes", "Number of minutes for the timer. 0 to cancel."),
        )
    )

    private val getNowPlayingFn = defineFunction(
        name = "get_now_playing",
        description = "Get information about the currently playing track, including title, artist, and playback state.",
        parameters = listOf()
    )

    private val setPlaybackSpeedFn = defineFunction(
        name = "set_playback_speed",
        description = "Set the playback speed. Valid range: 0.5 to 2.0",
        parameters = listOf(
            Schema.double("speed", "The playback speed multiplier"),
        )
    )

    private val toggleFavoriteFn = defineFunction(
        name = "toggle_favorite",
        description = "Add or remove the currently playing song from favorites/liked songs.",
        parameters = listOf()
    )

    private val createPlaylistFn = defineFunction(
        name = "create_playlist",
        description = "Create a new empty playlist with the given name.",
        parameters = listOf(
            Schema.str("name", "Name for the new playlist"),
        )
    )

    private val getQueueFn = defineFunction(
        name = "get_queue",
        description = "Get the list of songs in the current playback queue / up next.",
        parameters = listOf()
    )

    // --- Gemini Model ---
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            tools = listOf(
                Tool(listOf(
                    searchAndPlayFn,
                    controlPlaybackFn,
                    setSleepTimerFn,
                    getNowPlayingFn,
                    setPlaybackSpeedFn,
                    toggleFavoriteFn,
                    createPlaylistFn,
                    getQueueFn
                ))
            ),
            systemInstruction = content {
                text("""You are Musify AI, a friendly and helpful music assistant inside the Musify music player app.

Your capabilities:
- Search and play any song, artist, or album
- Control playback (play, pause, skip, shuffle, repeat)
- Set sleep timers
- Change playback speed
- Add songs to favorites
- Create playlists
- Answer questions about music, artists, genres, lyrics
- Provide song recommendations

CRITICAL RULES:
- You must STRICTLY answer ONLY music-related queries.
- If the user asks about ANYTHING else (e.g. coding, cooking, general knowledge, math, weather), POLITELY REFUSE and state that you are exclusively a music assistant.
- Be concise and use emojis sparingly for a friendly tone
- When the user asks to play something, ALWAYS use the search_and_play function
- When asked about the current song, use get_now_playing first
- For general music knowledge questions (artist biography, genre info, etc.), answer directly without calling functions
- Format responses with markdown for readability
- If the API key is missing, tell the user to add their Gemini API key
""")
            }
        )
    }

    private val chat by lazy { generativeModel.startChat() }

    fun sendMessage(userText: String, isVoice: Boolean = false) {
        analyticsManager.logFeatureClick("use_ai_assistant")
        if (userText.isBlank() || _isProcessing.value) return

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            _messages.value = _messages.value + ChatMessage(userText, isUser = true) +
                ChatMessage("⚠️ **API Key Missing!**\n\nAdd your Gemini API key to `local.properties`:\n```\nGEMINI_API_KEY=your_key_here\n```\nGet a free key at [aistudio.google.com/apikey](https://aistudio.google.com/apikey)", isUser = false)
            return
        }

        _messages.value = _messages.value + ChatMessage(userText, isUser = true)
        _isProcessing.value = true

        // Add loading indicator
        _messages.value = _messages.value + ChatMessage("", isUser = false, isLoading = true)

        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    chat.sendMessage(userText)
                }

                // Check if the model wants to call a function
                val functionCall = response.functionCalls.firstOrNull()
                if (functionCall != null) {
                    val result = executeFunctionCall(functionCall)
                    
                    // Send the function result back to Gemini
                    val followUp = withContext(Dispatchers.IO) {
                        chat.sendMessage(
                            content("function") {
                                part(FunctionResponsePart(functionCall.name,
                                    JSONObject(result).toMap() as JSONObject
                                ))
                            }
                        )
                    }

                    // Remove loading, add AI response
                    removeLoadingMessage()
                    val actionLabel = result["_action"] as? String
                    val responseText = followUp.text ?: "Done! ✨"
                    
                    if (isVoice) speakText(responseText)

                    _messages.value = _messages.value + ChatMessage(
                        text = responseText,
                        isUser = false,
                        actionPerformed = actionLabel
                    )
                } else {
                    // Direct text response (knowledge questions etc.)
                    removeLoadingMessage()
                    val responseText = response.text ?: "I'm not sure how to help with that."
                    
                    if (isVoice) speakText(responseText)

                    _messages.value = _messages.value + ChatMessage(
                        text = responseText,
                        isUser = false
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "AI Error: ${e.message}", e)
                removeLoadingMessage()
                _messages.value = _messages.value + ChatMessage(
                    text = "Sorry, something went wrong 😔\n\n`${e.message?.take(100)}`",
                    isUser = false
                )
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun removeLoadingMessage() {
        _messages.value = _messages.value.filter { !it.isLoading }
    }

    private fun speakText(text: String) {
        // Clean markdown for speech
        val cleanText = text.replace(Regex("[*#~_`]"), "")
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private suspend fun executeFunctionCall(call: FunctionCallPart): Map<String, Any?> {
        return when (call.name) {
            "search_and_play" -> executeSearchAndPlay(call.args["query"] as? String ?: "")
            "control_playback" -> executeControlPlayback(call.args["action"] as? String ?: "")
            "set_sleep_timer" -> executeSetSleepTimer(null ?: 0)
            "get_now_playing" -> executeGetNowPlaying()
            "set_playback_speed" -> executeSetPlaybackSpeed(null ?: 1.0f)
            "toggle_favorite" -> executeToggleFavorite()
            "create_playlist" -> executeCreatePlaylist(call.args["name"] as? String ?: "New Playlist")
            "get_queue" -> executeGetQueue()
            else -> mapOf("error" to "Unknown function: ${call.name}")
        }
    }

    private suspend fun executeSearchAndPlay(query: String): Map<String, Any?> {
        return try {
            val tracks = withContext(Dispatchers.IO) {
                youtubeRepository.fetchMusic(query, SearchSource.YT_MUSIC)
            }
            if (tracks.isNotEmpty()) {
                val track = tracks.first()
                withContext(Dispatchers.Main) {
                    playbackViewModel?.playTrack(track, tracks.take(20))
                }
                mapOf(
                    "status" to "playing",
                    "title" to track.title,
                    "artist" to track.artist,
                    "queue_size" to tracks.take(20).size,
                    "_action" to "▶ Playing: ${track.title} — ${track.artist}"
                )
            } else {
                mapOf("status" to "not_found", "message" to "No results found for '$query'")
            }
        } catch (e: Exception) {
            mapOf("error" to e.message)
        }
    }

    private suspend fun executeControlPlayback(action: String): Map<String, Any?> {
        val vm = playbackViewModel ?: return mapOf("error" to "Playback not available")
        return withContext(Dispatchers.Main) {
            when (action.lowercase()) {
                "play", "pause", "toggle" -> { vm.togglePlayPause(); mapOf("status" to "toggled", "_action" to "⏯ Toggled playback") }
                "next", "skip" -> { vm.playNext(); mapOf("status" to "skipped", "_action" to "⏭ Skipped to next") }
                "previous", "prev" -> { vm.playPrevious(); mapOf("status" to "previous", "_action" to "⏮ Playing previous") }
                "shuffle_on" -> { vm.setShuffleModeEnabled(true); mapOf("status" to "shuffle_on", "_action" to "🔀 Shuffle ON") }
                "shuffle_off" -> { vm.setShuffleModeEnabled(false); mapOf("status" to "shuffle_off", "_action" to "🔀 Shuffle OFF") }
                "repeat_on" -> { vm.setRepeatMode(1); mapOf("status" to "repeat_on", "_action" to "🔁 Repeat ON") }
                "repeat_off" -> { vm.setRepeatMode(0); mapOf("status" to "repeat_off", "_action" to "🔁 Repeat OFF") }
                else -> mapOf("error" to "Unknown action: $action")
            }
        }
    }

    private fun executeSetSleepTimer(minutes: Int): Map<String, Any?> {
        val vm = playbackViewModel ?: return mapOf("error" to "Playback not available")
        return if (minutes <= 0) {
            vm.stopSleepTimer()
            mapOf("status" to "cancelled", "_action" to "⏰ Timer cancelled")
        } else {
            vm.startSleepTimer(minutes, stopAtEnd = true)
            mapOf("status" to "set", "minutes" to minutes, "_action" to "⏰ Timer set: ${minutes}min")
        }
    }

    private fun executeGetNowPlaying(): Map<String, Any?> {
        val vm = playbackViewModel ?: return mapOf("error" to "Playback not available")
        val track = vm.currentTrack.value
        return if (track != null) {
            mapOf(
                "title" to track.title,
                "artist" to track.artist,
                "is_playing" to vm.isPlaying.value,
                "position_ms" to vm.currentPosition.value,
                "duration_ms" to vm.duration.value,
                "speed" to vm.playbackSpeed.value
            )
        } else {
            mapOf("status" to "nothing_playing")
        }
    }

    private fun executeSetPlaybackSpeed(speed: Float): Map<String, Any?> {
        val vm = playbackViewModel ?: return mapOf("error" to "Playback not available")
        val clamped = speed.coerceIn(0.5f, 2.0f)
        vm.setPlaybackSpeed(clamped)
        return mapOf("status" to "set", "speed" to clamped, "_action" to "🏃 Speed: ${clamped}x")
    }

    private fun executeToggleFavorite(): Map<String, Any?> {
        val track = playbackViewModel?.currentTrack?.value ?: return mapOf("error" to "Nothing playing")
        libraryViewModel?.toggleFavorite(track)
        return mapOf("status" to "toggled", "title" to track.title, "_action" to "❤️ Toggled favorite: ${track.title}")
    }

    private fun executeCreatePlaylist(name: String): Map<String, Any?> {
        libraryViewModel?.createPlaylist(name) ?: return mapOf("error" to "Library not available")
        return mapOf("status" to "created", "name" to name, "_action" to "📋 Created playlist: $name")
    }

    private fun executeGetQueue(): Map<String, Any?> {
        val vm = playbackViewModel ?: return mapOf("error" to "Playback not available")
        val queue = vm.upNextQueue.value
        return if (queue.isNotEmpty()) {
            mapOf(
                "queue_size" to queue.size,
                "tracks" to queue.take(10).map { "${it.title} — ${it.artist}" }
            )
        } else {
            mapOf("status" to "empty", "message" to "Queue is empty")
        }
    }
}

// Helper to convert JSONObject to Map
private fun JSONObject.toMap(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    keys().forEach { key ->
        map[key] = when (val value = get(key)) {
            is JSONObject -> value.toMap()
            else -> value
        }
    }
    return map
}
