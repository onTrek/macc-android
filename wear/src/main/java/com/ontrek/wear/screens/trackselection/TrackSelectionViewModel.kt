package com.ontrek.wear.screens.trackselection

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ontrek.shared.api.gpx.downloadGpx
import com.ontrek.shared.api.track.getTracks
import com.ontrek.shared.api.track.getSavedTracks
import com.ontrek.shared.api.firebase.getGPX
import com.ontrek.shared.data.Track
import com.ontrek.wear.data.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class DownloadState {
    object NotStarted : DownloadState()
    object InProgress : DownloadState()
    object Completed : DownloadState()
}

data class TrackUI(
    val id: Int,
    val title: String,
    val filename: String = "$id.gpx",
    val uploadedAt: Long,
    val size: Long,  // size in Bytes
    var state: DownloadState,
) {
    val sizeInKB: Long
        get() = size / 1024

    val sizeInMB: Double
        get() = size / (1024.0 * 1024.0)

    fun getFormattedSize(): String {
        return when {
            sizeInMB >= 1 -> "$sizeInMB MB"
            sizeInKB >= 1 -> "$sizeInKB KB"
            else -> "$size Bytes"
        }
    }
}

class TrackSelectionViewModel(private val db: AppDatabase) : ViewModel() {

    private val _downloadedTrackListState = MutableStateFlow<List<TrackUI>>(listOf())
    val downloadedTrackListState: StateFlow<List<TrackUI>> = _downloadedTrackListState

    private val _availableTrackListState = MutableStateFlow<List<TrackUI>>(listOf())
    val availableTrackListState: StateFlow<List<TrackUI>> = _availableTrackListState

    private val _isLoadingTracks = MutableStateFlow(true)
    val isLoadingTracks: StateFlow<Boolean> = _isLoadingTracks

    private val _fetchError = MutableStateFlow<String?>(null)
    val fetchError: StateFlow<String?> = _fetchError

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess

    private val _downloadSuccess = MutableStateFlow(false)
    val downloadSuccess: StateFlow<Boolean> = _downloadSuccess

    private val _isLoadingDownloads = MutableStateFlow(true)
    val isLoadingDownloads: StateFlow<Boolean> = _isLoadingDownloads

    init {
        loadDownloadedTracks()
    }

    fun resetUpdateSuccess() {
        _updateSuccess.value = false
    }

    fun resetDownloadSuccess() {
        _downloadSuccess.value = false
    }

    private fun loadDownloadedTracks() {
        _isLoadingDownloads.value = true

        viewModelScope.launch {
            try {
                val tracks = db.trackDao().getAllTracks()
                _downloadedTrackListState.value = tracks.map { track ->
                    Log.d(
                        "TrackSelectionViewModel",
                        "Loaded downloaded track: ${track.id} - ${track.title}"
                    )
                    TrackUI(
                        id = track.id,
                        title = track.title,
                        uploadedAt = track.uploadedAt,
                        size = track.size,
                        state = if (track.downloadedAt > 0) DownloadState.Completed else DownloadState.NotStarted
                    )
                }
                _isLoadingDownloads.value = false
            } catch (e: Exception) {
                Log.e("TrackSelectionViewModel", "Error loading downloaded tracks: ${e.message}")
                _fetchError.value = "Failed to load downloaded tracks"
            }
        }
    }

    fun fetchTrackList() {
        _isLoadingTracks.value = true

        viewModelScope.launch {
            // Fetch tracks created by me
            getTracks(
                onSuccess = { createdTracks ->
                    val safelyCreated = createdTracks ?: emptyList()
                    // Fetch tracks saved by me
                    getSavedTracks(
                        onSuccess = { savedTracks ->
                            val safelySaved = savedTracks ?: emptyList()
                            processTracks(safelyCreated, safelySaved)
                        },
                        onError = { error ->
                            // Even if saved tracks fail, we might want to show created tracks,
                            // but for simplicity let's report error or just process created
                            Log.e("WearOS", "Error fetching saved tracks: $error")
                            processTracks(safelyCreated, emptyList())
                            // Optionally set error toast?
                        }
                    )
                },
                onError = ::setError,
            )
        }
    }

    private fun processTracks(created: List<Track>, saved: List<Track>) {
        val trackMap = mutableMapOf<Int, TrackUI>()

        // 1. Process Created Tracks
        created.forEach { track ->
            if (_downloadedTrackListState.value.none { it.id == track.id }) {
                 trackMap[track.id] = TrackUI(
                     id = track.id,
                     title = track.title,
                     uploadedAt = parseDate(track.upload_date),
                     size = track.size,
                     state = DownloadState.NotStarted,
                 )
            }
        }

        // 2. Process Saved Tracks (merge if existing)
        saved.forEach { track ->
             if (_downloadedTrackListState.value.none { it.id == track.id }) {
                 val existing = trackMap[track.id]
                 if (existing == null) {
                     trackMap[track.id] = TrackUI(
                         id = track.id,
                         title = track.title,
                         uploadedAt = parseDate(track.upload_date),
                         size = track.size,
                         state = DownloadState.NotStarted,
                     )
                 }
                 // If not null, it's already there (likely from created list), so ignore or overwrite if needed.
                 // Since we removed flags, we don't need to update existing.
             }
        }

        _availableTrackListState.value = trackMap.values.toList().sortedByDescending { it.uploadedAt }
        _fetchError.value = null
        _updateSuccess.value = true
        _isLoadingTracks.value = false
        Log.d("WearOS", "Tracks updated: ${_availableTrackListState.value.size}")
    }

    private fun parseDate(dateString: String): Long {
        return try {
            java.time.OffsetDateTime.parse(dateString).toInstant().toEpochMilli()
        } catch (e: Exception) {
            0L
        }
    }

    fun setError(error: String?) {
        Log.e("WearOS", "Error occurred: $error")
        _availableTrackListState.value = listOf()  // Clear the available track list on error
        _fetchError.value = error
        _isLoadingTracks.value = false
    }

    private fun updateButtonState(index: Int, newState: DownloadState) {
        _availableTrackListState.value = _availableTrackListState.value.toMutableList().also {
            it[index] = it[index].copy(state = newState)
        }
    }

    fun downloadTrack(index: Int, trackID: Int, context: Context) {
        Log.d("DownloadTrack", "Downloading GPX started")

        updateButtonState(index, DownloadState.InProgress)

        viewModelScope.launch {
            downloadGpx(
                gpxID = trackID,
                onError = { error ->
                    Log.e("DownloadTrack", "Error fetching URL: $error")
                    handleDownloadError(index, "Failed to get track link")
                },
                onSuccess = { signedUrl ->
                    Log.d("DownloadTrack", "Signed URL obtained. Downloading file content...")

                    getGPX(
                        url = signedUrl,
                        onError = { error ->
                            Log.e("DownloadTrack", "Error downloading file bytes: $error")
                            handleDownloadError(index, "Failed to download file content")
                        },
                        onSuccess = { fileBytes ->

                            val downloaded = _availableTrackListState.value[index]

                            viewModelScope.launch {
                                try {
                                    db.trackDao().insertTrack(
                                        com.ontrek.wear.data.Track(
                                            id = downloaded.id,
                                            title = downloaded.title,
                                            filename = downloaded.filename,
                                            uploadedAt = downloaded.uploadedAt,
                                            size = downloaded.size,
                                            downloadedAt = System.currentTimeMillis()
                                        )
                                    )
                                } catch (e: Exception) {
                                    Log.e("DownloadTrack", "DB Error: ${e.message}")
                                }
                            }

                            saveFile(fileBytes, downloaded.filename, context)
                            Log.d("DownloadTrack", "File saved successfully: ${downloaded.filename}")

                            // 3. Aggiorna le liste nell'UI
                            updateButtonState(index, DownloadState.Completed)

                            // Aggiungi alla lista dei scaricati
                            _downloadedTrackListState.value = listOf(
                                downloaded.copy(
                                    state = DownloadState.Completed,
                                    filename = downloaded.filename,
                                )
                            ) + _downloadedTrackListState.value

                            // Rimuovi dalla lista dei disponibili
                            _availableTrackListState.value =
                                _availableTrackListState.value.toMutableList().also {
                                    it.removeAt(index)
                                }

                            Log.d("DownloadTrack", "UI updated successfully")
                            _downloadError.value = null
                            _downloadSuccess.value = true
                        }
                    )
                }
            )
        }
    }

    private fun handleDownloadError(index: Int, message: String) {
        _downloadError.value = message
        updateButtonState(index, DownloadState.NotStarted)
    }

    fun saveFile(fileContent: ByteArray, filename: String, context: Context) {
        context.openFileOutput(filename, Context.MODE_PRIVATE).use {
            it.write(fileContent)
        }
    }

    fun deleteTrack(index: Int, context: Context) {
        val trackToDelete = _downloadedTrackListState.value[index]
        Log.d("DeleteTrack", "Deleting track: ${trackToDelete.title}")

        viewModelScope.launch {
            try {
                // Delete from database
                db.trackDao().deleteTrackById(trackToDelete.id)

                // Delete the file from internal storage
                File(context.filesDir, trackToDelete.filename).delete()

                trackToDelete.state = DownloadState.NotStarted

                // Remove from the list
                _downloadedTrackListState.value =
                    _downloadedTrackListState.value.toMutableList().also {
                        it.removeAt(index)
                    }

                // Add back to available track list with NotStarted state
                _availableTrackListState.value =
                    _availableTrackListState.value.toMutableList().also {
                        it.add(trackToDelete.copy(state = DownloadState.NotStarted))
                    }

                // Reset the button state to NotStarted
                updateButtonState(
                    _availableTrackListState.value.indexOf(trackToDelete),
                    DownloadState.NotStarted
                )
            } catch (e: Exception) {
                Log.e("TrackSelectionViewModel", "Error deleting track: ${e.message}")
            }
        }
    }

    fun resetDownloadError() {
        _downloadError.value = null
    }

    class Factory(private val db: AppDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TrackSelectionViewModel::class.java)) {
                return TrackSelectionViewModel(db) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}