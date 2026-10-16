package com.nagarsetu.dashboard.di

import com.nagarsetu.core.data.network.ApiClient
import com.nagarsetu.dashboard.data.HybridAlertBridge
import com.nagarsetu.dashboard.data.network.DashboardApiService
import com.nagarsetu.firebase.realtime.LiveAlertFirebaseSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DashboardNetworkModule {

    @Provides
    @Singleton
    fun provideDashboardApiService(apiClient: ApiClient): DashboardApiService {
        return apiClient.retrofit.create(DashboardApiService::class.java)
    }

    /**
     * Provides [HybridAlertBridge] — the merge point for Supabase Realtime
     * and Firebase Firestore alert streams.
     *
     * [LiveAlertFirebaseSource] is provided by FirebaseModule in :backend:components:firebase.
     */
    @Provides
    @Singleton
    fun provideHybridAlertBridge(
        firebaseSource: LiveAlertFirebaseSource
    ): HybridAlertBridge = HybridAlertBridge(firebaseSource)
}
