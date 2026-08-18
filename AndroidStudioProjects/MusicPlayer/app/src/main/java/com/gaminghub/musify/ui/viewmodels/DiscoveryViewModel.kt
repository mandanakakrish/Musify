package com.gaminghub.musify.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gaminghub.musify.TrackModel
import com.gaminghub.musify.data.repository.SearchSource
import com.gaminghub.musify.data.repository.YouTubeRepository
import com.gaminghub.musify.util.StreamExtractionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.gaminghub.musify.data.toModel
import com.gaminghub.musify.util.AnalyticsManager

class DiscoveryViewModel(
    application: android.app.Application,
    private val analyticsManager: AnalyticsManager
) : androidx.lifecycle.AndroidViewModel(application) {
    private val youtubeRepository = YouTubeRepository()
    private val dao = com.gaminghub.musicplayer.data.MusicDatabase.getInstance(application).dao

    private val _searchSource = MutableStateFlow(SearchSource.YT_MUSIC)
    val searchSource: StateFlow<SearchSource> = _searchSource

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _trendingTracks = MutableStateFlow<List<TrackModel>>(emptyList())
    val trendingTracks: StateFlow<List<TrackModel>> = _trendingTracks

    val topCharts: StateFlow<List<TrackModel>> = _trendingTracks

    private val _newReleases = MutableStateFlow<List<TrackModel>>(emptyList())
    val newReleases: StateFlow<List<TrackModel>> = _newReleases

    private val _searchTracks = MutableStateFlow<List<TrackModel>>(emptyList())
    val searchTracks: StateFlow<List<TrackModel>> = _searchTracks

    private val _relatedTracks = MutableStateFlow<List<TrackModel>>(emptyList())
    val relatedTracks: StateFlow<List<TrackModel>> = _relatedTracks

    private val _recommendedTracks = MutableStateFlow<List<TrackModel>>(emptyList())
    val recommendedTracks: StateFlow<List<TrackModel>> = _recommendedTracks

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory

    fun loadTrending() {
        if (_trendingTracks.value.isNotEmpty() || _isLoading.value) return
        viewModelScope.launch(Dispatchers.IO) {
            fetchMusic("trending music", _trendingTracks)
            fetchMusic("new songs official", _newReleases, officialOnly = true)
        }
    }

    fun loadNewReleases() {
        if (_newReleases.value.isNotEmpty() || _isLoading.value) return
        viewModelScope.launch(Dispatchers.IO) {
            fetchMusic("new songs official", _newReleases, officialOnly = true)
        }
    }

    fun setSearchSource(source: SearchSource) {
        _searchSource.value = source
        if (_searchQuery.value.isNotEmpty()) {
            searchMusic(_searchQuery.value)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.length > 2) {
            searchMusic(query)
        }
    }

    fun fetchTrendingMusic() {
        analyticsManager.logFeatureClick("fetch_trending")
        if (_trendingTracks.value.isNotEmpty()) return
        fetchMusic("trending music", _trendingTracks)
        fetchMusic("new songs official", _newReleases, officialOnly = true)
    }

    fun searchMusic(query: String) {
        analyticsManager.logFeatureClick("search_query")
        if (query.isBlank()) return

        val currentHistory = _searchHistory.value.toMutableList()
        currentHistory.remove(query)
        currentHistory.add(0, query)
        _searchHistory.value = currentHistory.take(10)

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true

            val ytTracks = youtubeRepository.fetchMusic(query, _searchSource.value, false)
            val localTracks = dao.searchTracks(query).map { it.toModel() }
            _searchTracks.value = (localTracks + ytTracks).distinctBy { it.audioUrl }

            _searchTracks.value.take(5).forEach { track ->
                track.audioUrl?.let { com.gaminghub.musify.util.StreamExtractionManager.preExtract(it) }
            }

            _isLoading.value = false
        }
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
    }

    fun fetchMusic(term: String, targetFlow: MutableStateFlow<List<TrackModel>>, officialOnly: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val source = _searchSource.value
            val tracks = youtubeRepository.fetchMusic(term, source, officialOnly)
            targetFlow.value = tracks

            tracks.take(5).forEach { track ->
                track.audioUrl?.let { url ->
                    StreamExtractionManager.preExtract(url)
                }
            }

            if (tracks.isEmpty() && term == "trending music") {
                fetchMusic("popular songs", _trendingTracks, officialOnly)
            }
            _isLoading.value = false
        }
    }

    fun setRecommendedTracks(tracks: List<TrackModel>) {
        _recommendedTracks.value = tracks
    }

    fun fetchRecommendedFromTrack(track: TrackModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val related = youtubeRepository.fetchRelatedMusic(track.audioUrl ?: "")
            if (related.isNotEmpty()) {
                _recommendedTracks.value = related
            }
        }
    }
}
