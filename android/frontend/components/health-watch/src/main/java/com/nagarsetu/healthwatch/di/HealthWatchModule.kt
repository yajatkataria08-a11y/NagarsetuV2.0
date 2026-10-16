package com.nagarsetu.healthwatch.di

import com.nagarsetu.healthwatch.data.HealthDataRepository
import com.nagarsetu.healthwatch.data.HealthDataRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HealthWatchModule {
    @Binds
    @Singleton
    abstract fun bindHealthDataRepository(impl: HealthDataRepositoryImpl): HealthDataRepository
}
