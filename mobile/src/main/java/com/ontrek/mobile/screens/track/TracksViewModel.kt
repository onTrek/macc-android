package com.ontrek.mobile.screens.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ontrek.shared.api.track.getTracks
import com.ontrek.shared.data.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import com.ontrek.shared.api.track.getSavedTracks

import com.ontrek.shared.api.track.saveTrack
import com.ontrek.shared.api.track.unsaveTrack

data class UiTrack(
    val track: Track,
    val isCreatedByMe: Boolean,
    val isSavedByMe: Boolean
)

class TrackViewModel : ViewModel() {
    private val _tracksState = MutableStateFlow<TracksState>(TracksState.Loading)
    val tracksState: StateFlow<TracksState> = _tracksState
    private val _msgToast = MutableStateFlow("")
    val msgToast: StateFlow<String> = _msgToast

    fun loadTracks() {
        _tracksState.value = TracksState.Loading
        viewModelScope.launch {
            getTracks(
                onSuccess = { createdTracks ->
                    val safelyCreated = createdTracks ?: emptyList()
                    fetchSavedTracks(safelyCreated)
                },
                onError = { errorMsg ->
                    _msgToast.value = errorMsg
                    _tracksState.value = TracksState.Error(errorMsg)
                },
            )
        }
    }

    private fun fetchSavedTracks(createdTracks: List<Track>) {
        getSavedTracks(
            onSuccess = { savedTracks ->
                val safelySaved = savedTracks ?: emptyList()
                processTracks(createdTracks, safelySaved)
            },
            onError = { errorMsg ->
                _msgToast.value = errorMsg
                _tracksState.value = TracksState.Error(errorMsg)
            }
        )
    }

    private fun processTracks(created: List<Track>, saved: List<Track>) {
        val trackMap = mutableMapOf<Int, UiTrack>()

        // Add created tracks
        created.forEach { track ->
            trackMap[track.id] = UiTrack(track, isCreatedByMe = true, isSavedByMe = track.saved)
        }

        // Merge saved tracks
        saved.forEach { track ->
            val existing = trackMap[track.id]
            if (existing != null) {
                trackMap[track.id] = existing.copy(isSavedByMe = true)
            } else {
                trackMap[track.id] = UiTrack(track, isCreatedByMe = false, isSavedByMe = true)
            }
        }

        val combinedList = trackMap.values.toList().sortedByDescending { it.track.upload_date }
        _tracksState.value = TracksState.Success(combinedList)
    }

    fun toggleTrackSave(uiTrack: UiTrack) {
        val currentList = (_tracksState.value as? TracksState.Success)?.tracks?.toMutableList() ?: return
        val index = currentList.indexOfFirst { it.track.id == uiTrack.track.id }
        if (index == -1) return

        if (uiTrack.isSavedByMe) {
            unsaveTrack(
                id = uiTrack.track.id,
                onSuccess = {
                    currentList[index] = uiTrack.copy(isSavedByMe = false)
                    _tracksState.value = TracksState.Success(currentList)
                },
                onError = { errorMsg ->
                    _msgToast.value = errorMsg
                }
            )
        } else {
            saveTrack(
                id = uiTrack.track.id,
                onSuccess = {
                    currentList[index] = uiTrack.copy(isSavedByMe = true)
                    _tracksState.value = TracksState.Success(currentList)
                },
                onError = { errorMsg ->
                    _msgToast.value = errorMsg
                }
            )
        }
    }

    fun clearMsgToast() {
        _msgToast.value = ""
    }

    sealed class TracksState {
        data class Success(val tracks: List<UiTrack>) : TracksState()
        data class Error(val message: String) : TracksState()
        object Loading : TracksState()
    }
}