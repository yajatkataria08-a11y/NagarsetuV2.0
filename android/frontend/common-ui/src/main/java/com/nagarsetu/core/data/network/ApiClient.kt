package com.nagarsetu.core.data.network

import com.nagarsetu.core.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClient @Inject constructor() {
    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
    }

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.nagarsetu.com/") // Placeholder, rebased by components
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val sendGridService: SendGridService by lazy {
        retrofit.newBuilder()
            .baseUrl("https://api.sendgrid.com/")
            .build()
            .create(SendGridService::class.java)
    }
}
