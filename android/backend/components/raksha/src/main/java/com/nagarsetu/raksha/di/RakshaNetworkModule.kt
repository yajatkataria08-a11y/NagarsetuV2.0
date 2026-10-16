package com.nagarsetu.raksha.di

import com.nagarsetu.firebase.realtime.GpsTrackingRepository
import com.nagarsetu.raksha.data.DisasterAlertRepository
import com.nagarsetu.raksha.data.DisasterAlertRepositoryImpl
import com.nagarsetu.raksha.data.LiveTrackManager
import com.nagarsetu.raksha.data.network.NdmaApiService
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RakshaNetworkModule {

    @Binds
    @Singleton
    abstract fun bindDisasterAlertRepository(
        impl: DisasterAlertRepositoryImpl
    ): DisasterAlertRepository

    companion object {

        @Provides
        @Singleton
        fun provideNdmaApiService(okHttpClient: OkHttpClient): NdmaApiService =
            NdmaApiService(okHttpClient)

        /**
         * Provides [LiveTrackManager] — the Raksha ↔ Firebase RTDB bridge.
         *
         * [GpsTrackingRepository] is provided by FirebaseModule in
         * :backend:components:firebase and injected transitively via Hilt.
         */
        @Provides
        @Singleton
        fun provideLiveTrackManager(
            @ApplicationContext context: Context,
            gpsTrackingRepository: GpsTrackingRepository
        ): LiveTrackManager = LiveTrackManager(context, gpsTrackingRepository)
    }
}
