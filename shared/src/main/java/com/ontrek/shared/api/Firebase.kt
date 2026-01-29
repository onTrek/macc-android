package com.ontrek.shared.api

import retrofit2.Retrofit
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import retrofit2.Call

object Firebase {

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl("https://storage.googleapis.com/")
            .build()
    }

    val api: FirebaseService by lazy { retrofit.create(FirebaseService::class.java) }
}

interface FirebaseService {

    @Streaming
    @GET
    fun downloadFile(@Url fileUrl: String): Call<ResponseBody>
}