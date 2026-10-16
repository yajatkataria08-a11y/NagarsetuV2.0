package com.nagarsetu.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

// ── Config data class (provided by AppModule from BuildConfig) ────────────────
data class SupabaseConfig(
    val url: String,
    val anonKey: String
)

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    /**
     * Single SupabaseClient for the whole app.
     * Plugins: Postgrest + Auth + Realtime + Storage.
     */
    @Provides
    @Singleton
    fun provideSupabaseClient(config: SupabaseConfig): SupabaseClient {
        if (config.url.isBlank() || config.anonKey.isBlank()) {
            // Provide a dummy client or throw a more descriptive error that doesn't block the app class
            // if possible. But better to just provide a client that will fail on calls, not on init.
            return createSupabaseClient("https://placeholder.supabase.co", "placeholder") {
                install(Postgrest)
                install(Auth)
            }
        }
        return createSupabaseClient(
            supabaseUrl = config.url,
            supabaseKey = config.anonKey
        ) {
            install(Postgrest)
            install(Auth)
            install(Realtime) {
                reconnectDelay = 3.seconds
            }
            install(Storage)
        }
    }

    // ── Convenience accessors — inject these instead of client directly ────────

    @Provides
    @Singleton
    fun provideSupabaseAuth(client: SupabaseClient): Auth = client.auth

    @Provides
    @Singleton
    fun provideSupabaseStorage(client: SupabaseClient): Storage = client.storage

    @Provides
    @Singleton
    fun provideSupabaseRealtime(client: SupabaseClient): Realtime = client.realtime

    @Provides
    @Singleton
    fun provideSupabasePostgrest(client: SupabaseClient): Postgrest = client.postgrest
}
