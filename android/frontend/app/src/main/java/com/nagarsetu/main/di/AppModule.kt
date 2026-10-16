package com.nagarsetu.main.di

import com.nagarsetu.BuildConfig
import com.nagarsetu.auth.data.TwilioConfig
import com.nagarsetu.backend.core.assistant.AiAssistantConfig
import com.nagarsetu.backend.core.config.ExternalApiConfig
import com.nagarsetu.core.di.SupabaseConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * App-level Hilt bindings.
 *
 * Provides [SupabaseConfig] from BuildConfig so [SupabaseProvider] can be
 * injected anywhere without needing Context or a manual singleton.
 *
 * All values originate from local.properties → BuildConfig (never hardcoded).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSupabaseConfig(): SupabaseConfig = SupabaseConfig(
        url     = BuildConfig.SUPABASE_URL,
        anonKey = BuildConfig.SUPABASE_ANON_KEY
    )

    @Provides
    @Singleton
    fun provideTwilioConfig(): TwilioConfig = TwilioConfig(
        accountSid = BuildConfig.TWILIO_ACCOUNT_SID,
        authToken  = BuildConfig.TWILIO_AUTH_TOKEN,
        verifySid  = BuildConfig.TWILIO_VERIFY_SID,
        fromNumber = BuildConfig.TWILIO_PHONE_NUMBER
    )

    @Provides
    @Singleton
    fun provideAiAssistantConfig(): AiAssistantConfig = AiAssistantConfig(
        groqApiKey   = BuildConfig.GROQ_API_KEY,
        geminiApiKey = BuildConfig.GEMINI_API_KEY
    )

    @Provides
    @Singleton
    fun provideExternalApiConfig(): ExternalApiConfig = ExternalApiConfig(
        openWeatherApiKey    = BuildConfig.OPENWEATHER_API_KEY,
        dataGovInKey        = BuildConfig.DATA_GOV_IN_KEY,
        openChargeMapKey    = BuildConfig.OPEN_CHARGE_MAP_KEY,
        gNewsApiKey         = BuildConfig.GNEWS_API_KEY,
        waqiToken           = BuildConfig.WAQI_TOKEN
    )
}
