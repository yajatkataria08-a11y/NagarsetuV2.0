package com.nagarsetu.drivelegal.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * DriveLegal backend DI module.
 * All classes use @Inject constructors, so no explicit @Provides needed.
 * Hilt auto-binds: TfIdfBot, CalculateFineUseCase, SearchViolationUseCase, DriveLegalRepository.
 */
@Module
@InstallIn(SingletonComponent::class)
object DriveLegalBackendModule
