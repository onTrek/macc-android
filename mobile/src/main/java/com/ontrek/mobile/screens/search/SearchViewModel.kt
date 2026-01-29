package com.ontrek.mobile.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ontrek.shared.api.search.searchTracks
import com.ontrek.shared.api.search.searchUsers
import com.ontrek.shared.data.Track
import com.ontrek.shared.data.UserMinimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import com.ontrek.shared.api.profile.getProfile
import com.ontrek.shared.data.Profile

import com.ontrek.shared.api.track.getSavedTracks

class SearchViewModel : ViewModel() {
    private val _msgToast = MutableStateFlow("")
    val msgToast: StateFlow<String> = _msgToast

    private val _currentUser = MutableStateFlow<Profile?>(null)
    val currentUser: StateFlow<Profile?> = _currentUser

    private val savedTrackIds = mutableSetOf<Int>()

    init {
        fetchCurrentUser()
        fetchSavedTracks()
    }

    private fun fetchCurrentUser() {
        viewModelScope.launch {
            getProfile(
                onSuccess = { profile ->
                    _currentUser.value = profile
                },
                onError = { /* Ignore error for now */ }
            )
        }
    }

    private fun fetchSavedTracks() {
        viewModelScope.launch {
            getSavedTracks(
                onSuccess = { tracks ->
                    tracks?.forEach { savedTrackIds.add(it.id) }
                },
                onError = { /* Ignore error */ }
            )
        }
    }

    private val _userSearchState = MutableStateFlow<UserSearchState>(UserSearchState.Empty)
    val userSearchState: StateFlow<UserSearchState> = _userSearchState

    private val _trackSearchState = MutableStateFlow<TrackSearchState>(TrackSearchState.Empty)
    val trackSearchState: StateFlow<TrackSearchState> = _trackSearchState

    // Search users (all users, not filtering by friend status)
    fun searchUsers(query: String) {
        viewModelScope.launch {
            _userSearchState.value = UserSearchState.Loading
            if (query.length < 3) {
                _userSearchState.value = UserSearchState.Empty
                return@launch
            }

            searchUsers(
                username = query,
                onSuccess = { users ->
                    _userSearchState.value = if (users.isNullOrEmpty()) {
                        UserSearchState.Empty
                    } else {
                        UserSearchState.Success(users)
                    }
                },
                onError = { error ->
                    _userSearchState.value = UserSearchState.Error(error)
                }
            )
        }
    }

    // Search tracks by title
    fun searchTracks(query: String) {
        viewModelScope.launch {
            _trackSearchState.value = TrackSearchState.Loading
            if (query.length < 3) {
                _trackSearchState.value = TrackSearchState.Empty
                return@launch
            }

            searchTracks(
                query = query,
                onSuccess = { tracks ->
                    if (tracks.isNullOrEmpty()) {
                        _trackSearchState.value = TrackSearchState.Empty
                    } else {
                        val syncedTracks = tracks.map { track ->
                            track.copy(saved = savedTrackIds.contains(track.id))
                        }
                        _trackSearchState.value = TrackSearchState.Success(syncedTracks)
                    }
                },
                onError = { error ->
                    _trackSearchState.value = TrackSearchState.Error(error)
                }
            )
        }
    }

    // User search states
    sealed class UserSearchState {
        object Loading : UserSearchState()
        object Empty : UserSearchState()
        data class Success(val users: List<UserMinimal>) : UserSearchState()
        data class Error(val message: String) : UserSearchState()
    }

    // Track search states
    sealed class TrackSearchState {
        object Loading : TrackSearchState()
        object Empty : TrackSearchState()
        data class Success(val tracks: List<Track>) : TrackSearchState()
        data class Error(val message: String) : TrackSearchState()
    }

    fun toggleTrackSave(track: Track) {
        viewModelScope.launch {
            val currentTracks = (_trackSearchState.value as? TrackSearchState.Success)?.tracks?.toMutableList() ?: return@launch
            val index = currentTracks.indexOfFirst { it.id == track.id }
            if (index == -1) return@launch

            if (track.saved) {
                com.ontrek.shared.api.track.unsaveTrack(
                    id = track.id,
                    onSuccess = {
                        savedTrackIds.remove(track.id)
                        currentTracks[index] = track.copy(saved = false)
                        _trackSearchState.value = TrackSearchState.Success(currentTracks)
                    },
                    onError = { error ->
                        _msgToast.value = error
                    }
                )
            } else {
                com.ontrek.shared.api.track.saveTrack(
                    id = track.id,
                    onSuccess = {
                        savedTrackIds.add(track.id)
                        currentTracks[index] = track.copy(saved = true)
                        _trackSearchState.value = TrackSearchState.Success(currentTracks)
                    },
                    onError = { error ->
                        _msgToast.value = error
                    }
                )
            }
        }
    }

    fun clearMsgToast() {
        _msgToast.value = ""
    }
}