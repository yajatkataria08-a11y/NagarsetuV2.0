package com.nagarsetu.greenroute.di

import com.nagarsetu.core.data.network.ApiClient
import com.nagarsetu.greenroute.data.network.OsrmApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GreenRouteNetworkModule {

    /**
     * OSRM public demo endpoint – free, no API key required.
     * Replace baseUrl with your self-hosted instance for production use.
     */
    @Provides
    @Singleton
    fun provideOsrmApiService(apiClient: ApiClient): OsrmApiService =
        apiClient.retrofit.newBuilder()
            .baseUrl("https://router.project-osrm.org/")
            .build()
            .create(OsrmApiService::class.java)
}
