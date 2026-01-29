package com.ontrek.shared.api.gpx

import android.util.Log
import com.ontrek.shared.api.RetrofitClient
import com.ontrek.shared.data.UrlResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


fun downloadGpx(
    gpxID: Int,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    RetrofitClient.api.downloadGPX(gpxID).enqueue(object : Callback<UrlResponse> {
        override fun onResponse(
            call: Call<UrlResponse>,
            response: Response<UrlResponse>
        ) {
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()

                if (body!!.url.isBlank()) {
                    Log.e("DownloadTrack", "Received URL is blank")
                    return
                }

                Log.d("DownloadTrack", "Received URL: ${body.url}")
                onSuccess(body.url)
            } else {
                Log.e("DownloadTrack", "Server returned error")
                onError("${response.code()} - ${response.message()}")
            }
        }

        override fun onFailure(call: Call<UrlResponse?>, t: Throwable) {
            Log.e("DownloadTrack", "Error: " + t.message)
            onError("API Error: ${t.message ?: "Unknown error"}")
        }
    })
}

