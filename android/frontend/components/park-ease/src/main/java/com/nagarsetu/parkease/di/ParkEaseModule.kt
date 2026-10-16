package com.nagarsetu.parkease.di

import com.nagarsetu.parkease.data.ParkingRepository
import com.nagarsetu.parkease.data.ParkingRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ParkEaseModule {
    @Binds
    @Singleton
    abstract fun bindParkingRepository(impl: ParkingRepositoryImpl): ParkingRepository
}
