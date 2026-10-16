package com.nagarsetu.healthwatch.di

import com.nagarsetu.core.data.network.ApiClient
import com.nagarsetu.healthwatch.data.network.HealthApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HealthNetworkModule {

    /**
     * HealthApiService hosts two distinct base URLs:
     *  - data.gov.in    (epidemic data)
     *  - overpass-api.de (hospital/clinic discovery)
     *
     * Both use absolute @GET URLs so the Retrofit baseUrl here is a
     * placeholder; Retrofit will follow the full URL in each annotation.
     */
    @Provides
    @Singleton
    fun provideHealthApiService(apiClient: ApiClient): HealthApiService =
        apiClient.retrofit.newBuilder()
            .baseUrl("https://api.data.gov.in/")  // default; overpass calls use absolute URLs
            .build()
            .create(HealthApiService::class.java)
}
