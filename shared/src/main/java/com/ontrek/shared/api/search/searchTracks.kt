package com.ontrek.shared.api.search

import android.util.Log
import com.ontrek.shared.api.RetrofitClient
import com.ontrek.shared.data.Track

fun searchTracks(query: String, onSuccess: (List<Track>?) -> Unit, onError: (String) -> Unit) {
    RetrofitClient.api.searchTracks(query).enqueue(object : retrofit2.Callback<List<Track>> {
        override fun onResponse(call: retrofit2.Call<List<Track>>, response: retrofit2.Response<List<Track>>) {
            Log.d("API Search Tracks", "Request: ${call.request().url}, Query: $query")
            if (response.isSuccessful) {
                onSuccess(response.body())
            } else {
                onError("API Search Tracks: ${response.code()}")
            }
        }

        override fun onFailure(call: retrofit2.Call<List<Track>>, t: Throwable) {
            Log.e("API Search Tracks", "Error: ${t.message ?: "Unknown error"}")
            onError("API Search Tracks: ${t.message ?: "Unknown error"}")
        }
    })
}
