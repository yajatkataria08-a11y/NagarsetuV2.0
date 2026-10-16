package com.nagarsetu.roadwatch.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * RoadWatch backend DI module.
 * All classes use @Inject constructors; Hilt auto-wires:
 *   RoadWatchRepository → PotholeDetector + CalculateSlaUseCase
 *                        + ClusterReportsUseCase + NearbyReportsUseCase
 */
@Module
@InstallIn(SingletonComponent::class)
object RoadWatchBackendModule
