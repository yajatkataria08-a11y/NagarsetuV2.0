package com.nagarsetu.predictive.di

import com.nagarsetu.predictive.data.api.PredictiveApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Fix R1: PredictiveModule was an empty object, so PredictiveApiService was
 * never injectable — any call to the forecast or RAG endpoints would have
 * failed with a Hilt "cannot be provided" compile error.
 *
 * Now provides a Retrofit-backed [PredictiveApiService] singleton, sharing the
 * app-wide [OkHttpClient] supplied by CoreModule so we don't spin up a second
 * HTTP stack.
 *
 * [PredictiveRepository] and [RiskApiRepository] continue to use
 * constructor-injection and need no explicit @Provides here.
 */
@Module
@InstallIn(SingletonComponent::class)
object PredictiveModule {

    @Provides
    @Singleton
    fun providePredictiveApiService(okHttpClient: OkHttpClient): PredictiveApiService =
        Retrofit.Builder()
            .baseUrl(PredictiveApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PredictiveApiService::class.java)
}
