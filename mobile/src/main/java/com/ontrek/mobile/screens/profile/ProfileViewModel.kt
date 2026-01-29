package com.ontrek.mobile.screens.profile

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.ontrek.mobile.screens.profile.ProfileViewModel.RequestsState.Companion.count
import com.ontrek.shared.api.friends.acceptFriendRequest
import com.ontrek.shared.api.friends.deleteFriend
import com.ontrek.shared.api.friends.deleteFriendRequest
import com.ontrek.shared.api.friends.getFriendRequests
import com.ontrek.shared.api.friends.getFriends
import com.ontrek.shared.api.friends.sendFriendRequest
import com.ontrek.shared.api.profile.deleteProfile
import com.ontrek.shared.api.profile.getImageProfile
import com.ontrek.shared.api.profile.getProfile
import com.ontrek.shared.api.search.searchUsers

import com.ontrek.shared.api.profile.uploadImageProfile
import com.ontrek.shared.data.FriendRequest
import com.ontrek.shared.data.Profile
import com.ontrek.shared.data.Track
import com.ontrek.shared.data.UserMinimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.ontrek.shared.api.track.getTracks
import com.ontrek.shared.api.track.getUserTracks

class ProfileViewModel : ViewModel() {
    private val _userProfile = MutableStateFlow<UserProfileState>(UserProfileState.Loading)
    val userProfile: StateFlow<UserProfileState> = _userProfile.asStateFlow()

    private val _imageProfile = MutableStateFlow<UserImageState>(UserImageState.Loading)
    val imageProfile: StateFlow<UserImageState> = _imageProfile.asStateFlow()

    private var imageVersion: Long = System.currentTimeMillis()

    private val _connectionStatusWaer = MutableStateFlow<ConnectionState>(ConnectionState.Success(false))
    val connectionStatus: StateFlow<ConnectionState> = _connectionStatusWaer.asStateFlow()

    private val _friendsState = MutableStateFlow<FriendsState>(FriendsState.Loading)
    val friendsState: StateFlow<FriendsState> = _friendsState.asStateFlow()

    private val _requestsState = MutableStateFlow<RequestsState>(RequestsState.Loading)
    val requestsState: StateFlow<RequestsState> = _requestsState.asStateFlow()

    private val _msgToast = MutableStateFlow("")
    val msgToast: StateFlow<String> = _msgToast.asStateFlow()

    private val _userTracksState = MutableStateFlow<UserTracksState>(UserTracksState.Loading)
    val userTracksState: StateFlow<UserTracksState> = _userTracksState.asStateFlow()

    private val _relationshipState = MutableStateFlow<RelationshipState>(RelationshipState.Loading)
    val relationshipState: StateFlow<RelationshipState> = _relationshipState.asStateFlow()

    fun fetchUserProfile() {
        viewModelScope.launch {
            _userProfile.value = UserProfileState.Loading
            _imageProfile.value = UserImageState.Loading
            try {
                getProfile(
                    onSuccess = { response ->
                        _userProfile.value = UserProfileState.Success(
                            userProfile = response ?: Profile(
                                username = "",
                                email = "",
                                id = ""
                            )
                        )
                        getImageProfile(
                            id = response?.id ?: "0",
                            onSuccess = { url ->
                                _imageProfile.value = UserImageState.Success(url, imageVersion)
                            },
                            onError = { error ->
                                _imageProfile.value = UserImageState.Error("Error fetching image: $error")
                            }
                        )
                    },
                    onError = { error ->
                        _userProfile.value = UserProfileState.Error("Error fetching profile: $error")
                        _imageProfile.value = UserImageState.Error("Error fetching image: $error")
                    }
                )

            } catch (e: Exception) {
                Log.e("ProfileView", "Error fetching user profile", e)
            }
        }
    }

    fun setOtherUserProfile(userId: String, username: String) {
        _userProfile.value = UserProfileState.Success(
            userProfile = Profile(
                id = userId,
                username = username,
                email = ""
            )
        )
    }

    fun fetchOtherUserImage(userId: String) {
        viewModelScope.launch {
            _imageProfile.value = UserImageState.Loading
            try {
                getImageProfile(
                    id = userId,
                    onSuccess = { url ->
                        _imageProfile.value = UserImageState.Success(url, imageVersion)
                    },
                    onError = { error ->
                        _imageProfile.value = UserImageState.Error("Error fetching image: $error")
                    }
                )
            } catch (e: Exception) {
                Log.e("ProfileView", "Error fetching user image", e)
                _imageProfile.value = UserImageState.Error("Error fetching image: ${e.message}")
            }
        }
    }

    fun deleteProfile(clearToken: () -> Unit) {
        viewModelScope.launch {
            _userProfile.value = UserProfileState.Loading
            _imageProfile.value = UserImageState.Loading
            deleteProfile(
                onSuccess = {
                    _msgToast.value = "Profile deleted successfully"
                    clearToken()
                },
                onError = { error ->
                    _msgToast.value = "Error to delete profile: $error"
                }
            )
        }
    }

    fun sendAuthToWearable(context: Context, token: String, userID: String) {
        viewModelScope.launch {
            _connectionStatusWaer.value = ConnectionState.Loading
            try {
                val putDataMapReq = PutDataMapRequest.create("/auth").apply {
                    dataMap.putString("token", token)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                    dataMap.putString("currentUser", userID)
                }
                val request = putDataMapReq.asPutDataRequest().setUrgent()

                Wearable.getDataClient(context).putDataItem(request)
                    .addOnSuccessListener {
                        _connectionStatusWaer.value = ConnectionState.Success(true)
                        _msgToast.value = "Connected to wearable successfully"
                    }
                    .addOnFailureListener {
                        _connectionStatusWaer.value = ConnectionState.Error("Failed to connect to wearable")
                        _msgToast.value = "Failed to connect to wearable"
                    }
            } catch (e: Exception) {
                _msgToast.value = "Error connecting to wearable: ${e.message}"
            }
        }
    }

    fun updateProfileImage(image: ByteArray, filename: String) {
        try {
            viewModelScope.launch {
                _imageProfile.value = UserImageState.Loading
                uploadImageProfile(
                    imageBytes = image,
                    filename = filename,
                    onSuccess = {
                        _msgToast.value = "Profile image updated successfully"
                        imageVersion = System.currentTimeMillis()
                        fetchUserProfile()
                    },
                    onError = { error ->
                        _msgToast.value = "Error updating profile image: $error"
                        _imageProfile.value = UserImageState.Error(error)
                    }
                )
            }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Error updating profile image", e)
            _msgToast.value = "Error updating profile image: ${e.message}"
        }
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = it.getString(index)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path?.substringAfterLast('/')
        }
        return name
    }

    // -------- Friends Management --------
    fun fetchFriends() {
        viewModelScope.launch {
            _friendsState.value = FriendsState.Loading
            getFriends(
                onSuccess = { friends ->
                    if (friends.isNullOrEmpty()) {
                        _friendsState.value = FriendsState.Empty
                    } else {
                        _friendsState.value = FriendsState.Success(friends)
                    }
                },
                onError = { error ->
                    _friendsState.value = FriendsState.Error(error)
                }
            )
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            deleteFriend(
                id = friendId,
                onSuccess = { message ->
                    _msgToast.value = message
                    removeFriendFromList(friendId)
                },
                onError = { error ->
                    _msgToast.value = error
                }
            )
        }
    }

    private fun removeFriendFromList(friendId: String) {
        viewModelScope.launch {
            _friendsState.value = when (val currentState = _friendsState.value) {
                is FriendsState.Success -> {
                    val updatedFriends = currentState.friends.filter { it.id != friendId }
                    FriendsState.Success(updatedFriends)
                }
                else -> currentState
            }
        }
    }

    private fun addFriendToList(friend: UserMinimal) {
        viewModelScope.launch {
            _friendsState.value = when (val currentState = _friendsState.value) {
                is FriendsState.Success -> {
                    val updatedFriends = currentState.friends.toMutableList().apply { add(friend) }
                    FriendsState.Success(updatedFriends)
                }
                else -> currentState
            }
        }
    }


    // -------- Friend Requests Management --------
    fun loadFriendRequests() {
        viewModelScope.launch {
            _requestsState.value = RequestsState.Loading
            getFriendRequests(
                onSuccess = { requests ->
                    if (requests.isNullOrEmpty()) {
                        _requestsState.value = RequestsState.Empty
                    } else {
                        _requestsState.value = RequestsState.Success(requests)
                    }
                },
                onError = { error ->
                    _requestsState.value = RequestsState.Error(error)
                }
            )
        }
    }

    fun acceptRequest(user: FriendRequest) {
        viewModelScope.launch {
            acceptFriendRequest(
                id = user.id,
                onSuccess = { message ->
                    _msgToast.value = message
                    removeRequestFromList(user.id)
                    val friend = UserMinimal(
                        id = user.id,
                        username = user.username,
                    )
                    addFriendToList(friend)
                },
                onError = { error ->
                    _msgToast.value = error
                }
            )
        }
    }

    fun rejectFriendRequest(requestId: String) {
        viewModelScope.launch {
            deleteFriendRequest(
                id = requestId,
                onSuccess = { message ->
                    _msgToast.value = message
                    removeRequestFromList(requestId)
                },
                onError = { error ->
                    _msgToast.value = error
                }
            )
        }
    }

    private fun removeRequestFromList(requestId: String) {
        viewModelScope.launch {
            _requestsState.value = when (val currentState = _requestsState.value) {
                is RequestsState.Success -> {
                    val updatedRequests = currentState.requests.filter { it.id != requestId }
                    RequestsState.Success(updatedRequests)
                }
                else -> currentState
            }

            if (_requestsState.value is RequestsState.Success) {
                val count = (_requestsState.value as RequestsState.Success).count
                if (count == 0) {
                    _requestsState.value = RequestsState.Empty
                }
            }
        }
    }

    // -------- Relationship Status Management (for viewing other profiles) --------
    fun fetchRelationshipStatus(userId: String, username: String) {
        viewModelScope.launch {
            _relationshipState.value = RelationshipState.Loading
            searchUsers(
                username = username,
                onSuccess = { users ->
                    val targetUser = users?.find { it.id == userId }
                    if (targetUser != null) {
                        _relationshipState.value = when (targetUser.state) {
                            1 -> RelationshipState.AlreadyFriends
                            0 -> RelationshipState.RequestSent
                            else -> RelationshipState.NoRelationship
                        }
                    } else {
                        _relationshipState.value = RelationshipState.NoRelationship
                    }
                },
                onError = { error ->
                    _relationshipState.value = RelationshipState.Error(error)
                }
            )
        }
    }

    fun sendFriendRequestToUser(userId: String) {
        viewModelScope.launch {
            _relationshipState.value = RelationshipState.Loading
            sendFriendRequest(
                id = userId,
                onSuccess = { message ->
                    _msgToast.value = message
                    _relationshipState.value = RelationshipState.RequestSent
                },
                onError = { error ->
                    _msgToast.value = error
                    _relationshipState.value = RelationshipState.NoRelationship
                }
            )
        }
    }



    fun removeFriendFromProfile(friendId: String) {
        viewModelScope.launch {
            _relationshipState.value = RelationshipState.Loading
            deleteFriend(
                id = friendId,
                onSuccess = { message ->
                    _msgToast.value = message
                    _relationshipState.value = RelationshipState.NoRelationship
                    removeFriendFromList(friendId)
                },
                onError = { error ->
                    _msgToast.value = error
                    _relationshipState.value = RelationshipState.AlreadyFriends
                }
            )
        }
    }

    fun clearMsgToast() {
        _msgToast.value = ""
    }

    fun setMsgToast(msg: String) {
        _msgToast.value = msg
    }

    sealed class UserProfileState {
        data class Success(val userProfile: Profile) : UserProfileState()
        data class Error(val message: String) : UserProfileState()
        object Loading : UserProfileState()
    }

    sealed class UserImageState {
        data class Success(val url: String, val version: Long) : UserImageState()
        data class Error(val message: String) : UserImageState()
        object Loading : UserImageState()
    }

    sealed class ConnectionState {
        data class Success(val isConnected: Boolean) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
        object Loading : ConnectionState()
    }

    sealed class FriendsState {
        data class Success(val friends: List<UserMinimal>) : FriendsState()
        data class Error(val message: String) : FriendsState()
        object Loading : FriendsState()
        object Empty : FriendsState()
    }

    sealed class RequestsState {
        data class Success(val requests: List<FriendRequest>) : RequestsState()
        data class Error(val message: String) : RequestsState()
        object Loading : RequestsState()
        object Empty : RequestsState()

        companion object {
            val RequestsState.count: Int
                get() = when (this) {
                    is Success -> requests.size
                    else -> 0
                }
        }
    }

    sealed class RelationshipState {
        object NoRelationship : RelationshipState()
        object RequestSent : RelationshipState()
        object AlreadyFriends : RelationshipState()
        object Loading : RelationshipState()
        data class Error(val message: String) : RelationshipState()
    }

    sealed class UserTracksState {
        data class Success(val tracks: List<Track>) : UserTracksState()
        data class Error(val message: String) : UserTracksState()
        object Loading : UserTracksState()
        object Empty : UserTracksState()
    }

    fun fetchUserTracks(userId: String, isOwner: Boolean) {
        viewModelScope.launch {
            _userTracksState.value = UserTracksState.Loading
            if (isOwner) {
                // Use /gpx/ endpoint for profile owner
                getTracks(
                    onSuccess = { tracks ->
                        if (tracks.isNullOrEmpty()) {
                            _userTracksState.value = UserTracksState.Empty
                        } else {
                            _userTracksState.value = UserTracksState.Success(tracks)
                        }
                    },
                    onError = { error ->
                        _userTracksState.value = UserTracksState.Error(error)
                    }
                )
            } else {
                // Use /users/{id}/tracks/ endpoint for other users
                getUserTracks(
                    userId = userId,
                    onSuccess = { tracks ->
                        if (tracks.isNullOrEmpty()) {
                            _userTracksState.value = UserTracksState.Empty
                        } else {
                            _userTracksState.value = UserTracksState.Success(tracks)
                        }
                    },
                    onError = { error ->
                        _userTracksState.value = UserTracksState.Error(error)
                    }
                )
            }
        }
    }


}