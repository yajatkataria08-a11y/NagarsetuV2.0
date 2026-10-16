package com.nagarsetu.chargeup.di

import com.nagarsetu.core.data.network.ApiClient
import com.nagarsetu.chargeup.data.network.ChargingApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChargingNetworkModule {
    
    @Provides
    @Singleton
    fun provideChargingApiService(apiClient: com.nagarsetu.core.data.network.ApiClient): ChargingApiService {
        return apiClient.retrofit.newBuilder()
            .baseUrl("https://api.openchargemap.io/")
            .build()
            .create(ChargingApiService::class.java)
    }
}
