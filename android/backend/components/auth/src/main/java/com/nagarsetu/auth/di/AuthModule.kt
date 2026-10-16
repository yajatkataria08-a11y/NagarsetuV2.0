package com.nagarsetu.auth.di

import com.nagarsetu.auth.data.SupabaseTokenUploaderImpl
import com.nagarsetu.auth.data.TwilioConfig
import com.nagarsetu.auth.data.TwilioHttpClientFactory
import com.nagarsetu.auth.data.TwilioOtpService
import com.nagarsetu.firebase.fcm.SupabaseTokenUploader
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    /**
     * Binds the Supabase-backed [SupabaseTokenUploaderImpl] as the
     * implementation of [SupabaseTokenUploader].
     *
     * This is the hybrid-model binding: Firebase FCM calls this interface
     * to persist the device token into Supabase profiles.fcm_token.
     */
    @Binds
    @Singleton
    abstract fun bindSupabaseTokenUploader(
        impl: SupabaseTokenUploaderImpl
    ): SupabaseTokenUploader

    companion object {
        @Provides
        @Singleton
        fun provideTwilioOtpService(config: TwilioConfig, httpClient: OkHttpClient): TwilioOtpService =
            TwilioOtpService(config, httpClient)
    }
}
