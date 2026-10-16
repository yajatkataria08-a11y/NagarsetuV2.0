package com.nagarsetu.raksha.di

import com.nagarsetu.raksha.data.incident.HazardZoneFetcher
import com.nagarsetu.raksha.data.incident.IncidentRepository
import com.nagarsetu.raksha.data.incident.RiskApiRepository
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for the Raksha safety backend.
 *
 * All primary classes ([IncidentRepository], [HazardZoneFetcher],
 * [RiskApiRepository]) are @Singleton and annotated with @Inject, so
 * Hilt provides them automatically without explicit @Provides bindings.
 *
 * The shared [okhttp3.OkHttpClient] is provided by the core module
 * (see CoreModule in :frontend:common-ui).
 */
@Module
@InstallIn(SingletonComponent::class)
object RakshaModule
