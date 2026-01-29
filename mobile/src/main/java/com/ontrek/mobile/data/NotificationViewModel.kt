package com.ontrek.mobile.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ontrek.shared.api.friends.getFriendRequests
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {

    private val _requestsCount = MutableStateFlow(0)
    val requestsCount: StateFlow<Int> = _requestsCount.asStateFlow()

    fun loadFriendRequests() {
        viewModelScope.launch {
            getFriendRequests(
                onSuccess = { requests ->
                    _requestsCount.value = requests?.size ?: 0
                },
                onError = {
                    _requestsCount.value = 0
                }
            )
        }
    }
}
