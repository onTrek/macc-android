package com.ontrek.shared.api.firebase

import android.util.Log
import com.ontrek.shared.api.Firebase
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response


fun getGPX(
    url: String,
    onSuccess: (ByteArray) -> Unit,
    onError: (String) -> Unit
) {
    Firebase.api.downloadFile(url).enqueue(object : retrofit2.Callback<ResponseBody> {
        override fun onResponse(
            call: Call<ResponseBody>,
            response: Response<ResponseBody>
        ) {
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()

                Log.d("DownloadTrack", "File found successfully")
                onSuccess(body!!.bytes())
            } else {
                Log.e("DownloadTrack", "Server returned error")
                onError("${response.code()} - ${response.message()}")
            }
        }

        override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
            Log.e("DownloadTrack", "Error: " + t.message)
            onError("API Error: ${t.message ?: "Unknown error"}")
        }
    })
}