package com.nagarsetu.parkease.di

import com.nagarsetu.core.data.network.ApiClient
import com.nagarsetu.parkease.data.network.OsmOverpassApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ParkEaseNetworkModule {

    /**
     * OSM Overpass API – free, no key.
     * Replaces the paid TomTom API for parking discovery.
     */
    @Provides
    @Singleton
    fun provideOsmOverpassApiService(apiClient: ApiClient): OsmOverpassApiService =
        apiClient.retrofit.newBuilder()
            .baseUrl("https://overpass-api.de/")
            .build()
            .create(OsmOverpassApiService::class.java)
}
