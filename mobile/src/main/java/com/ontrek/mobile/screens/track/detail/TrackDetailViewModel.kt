package com.ontrek.mobile.screens.track.detail

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.ontrek.shared.api.track.getMapTrack
import com.ontrek.shared.api.track.getTrack
import com.ontrek.shared.data.Track
import com.ontrek.shared.api.track.deleteTrack
import com.ontrek.shared.api.track.saveTrack
import com.ontrek.shared.api.track.unsaveTrack
import com.ontrek.shared.api.track.setTrackPrivacy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TrackDetailViewModel : ViewModel() {
    private val _trackDetailState = MutableStateFlow<TrackDetailState>(TrackDetailState.Loading)
    val trackDetailState: StateFlow<TrackDetailState> = _trackDetailState

    private val _imageState = MutableStateFlow<ImageState>(ImageState.Loading)
    val imageState: StateFlow<ImageState> = _imageState

    private val _msgToast = MutableStateFlow<String>("")
    val msgToast: StateFlow<String> = _msgToast


    fun loadTrackDetails(trackId: Int) {
        viewModelScope.launch {
            _trackDetailState.value = TrackDetailState.Loading

            getTrack(
                id = trackId,
                onSuccess = { track ->
                    if (track != null) {
                        _trackDetailState.value = TrackDetailState.Success(track)
                    } else {
                        _trackDetailState.value = TrackDetailState.Error("Track not found")
                    }
                },
                onError = { errorMessage ->
                    Log.e("TrackDetailViewModel", "Error loading track: $errorMessage")
                    _trackDetailState.value = TrackDetailState.Error(errorMessage)
                },
            )
        }
    }

    fun loadTrackImage(trackId: Int) {
        viewModelScope.launch {
            _imageState.value = ImageState.Loading

            getMapTrack(
                id = trackId,
                onSuccess = { signedUrl ->
                    Log.d("TrackDetailViewModel", "Image URL received successfully")
                    _imageState.value = ImageState.Success(signedUrl)
                },
                onError = { errorMessage ->
                    _imageState.value = ImageState.Error(errorMessage)
                }
            )
        }
    }

    fun deleteTrack(trackId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _trackDetailState.value = TrackDetailState.Loading
            deleteTrack(
                id = trackId,
                onSuccess = { _ ->
                    _msgToast.value = "Track deleted successfully"
                    onSuccess()
                },
                onError = { errorMsg ->
                    _msgToast.value = errorMsg
                },
            )
        }
    }

    fun sendStartToWearable(context: Context, trackId: Int, trackName: String) {
        viewModelScope.launch {
            try {
                val putDataMapReq = PutDataMapRequest.create("/track-start").apply {
                    dataMap.putInt("trackId", trackId)
                    dataMap.putInt("sessionId", -1)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                    dataMap.putString("trackName", trackName)
                }
                val request = putDataMapReq.asPutDataRequest().setUrgent()

                Wearable.getDataClient(context).putDataItem(request)
                    .addOnSuccessListener {
                        _msgToast.value = "Loading track on wearable"
                    }
                    .addOnFailureListener {
                        _msgToast.value = "Failed to connect to wearable"
                    }
            } catch (e: Exception) {
                _msgToast.value = "Error connecting to wearable: ${e.message}"
            }
        }
    }

    fun saveTrack(trackId: Int) {
        // Optimistic update
        updateTrackState { it.copy(saved = true) }
        viewModelScope.launch {
            saveTrack(
                id = trackId,
                onSuccess = {
                    _msgToast.value = "Track saved successfully"
                },
                onError = { errorMsg ->
                    _msgToast.value = errorMsg
                    // Revert on error
                    updateTrackState { it.copy(saved = false) }
                }
            )
        }
    }

    fun unsaveTrack(trackId: Int) {
        // Optimistic update
        updateTrackState { it.copy(saved = false) }
        viewModelScope.launch {
            unsaveTrack(
                id = trackId,
                onSuccess = {
                    _msgToast.value = "Track unsaved successfully"
                },
                onError = { errorMsg ->
                    _msgToast.value = errorMsg
                    // Revert on error
                    updateTrackState { it.copy(saved = true) }
                }
            )
        }
    }

    fun setPrivacy(trackId: Int, isPublic: Boolean) {
        // Optimistic update
        Log.d("TrackDetailViewModel", "Setting privacy to ${if (isPublic) "public" else "private"}")
        updateTrackState { it.copy(is_public = isPublic) }
        viewModelScope.launch {
            setTrackPrivacy(
                id = trackId,
                isPublic = isPublic,
                onSuccess = {
                    _msgToast.value = "Privacy updated successfully"
                },
                onError = { errorMsg ->
                    _msgToast.value = errorMsg
                    // Revert on error
                    updateTrackState { it.copy(is_public = !isPublic) }
                }
            )
        }
    }

    private fun updateTrackState(update: (Track) -> Track) {
        val currentState = _trackDetailState.value
        if (currentState is TrackDetailState.Success) {
            _trackDetailState.value = TrackDetailState.Success(update(currentState.track))
        }
    }

    // Stati per i dettagli della traccia (carino, così mi consigliava chatGPT e funziona... godo)
    sealed class TrackDetailState {
        object Loading : TrackDetailState()
        data class Success(val track: Track) : TrackDetailState()
        data class Error(val message: String) : TrackDetailState()
    }


    sealed class ImageState {
        object Loading : ImageState()
        data class Success(val url: String?) : ImageState()
        data class Error(val message: String) : ImageState()
    }
}