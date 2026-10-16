package com.nagarsetu.chargeup.di

import com.nagarsetu.chargeup.data.ChargingRepository
import com.nagarsetu.chargeup.data.ChargingRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChargeUpModule {
    @Binds
    @Singleton
    abstract fun bindChargingRepository(impl: ChargingRepositoryImpl): ChargingRepository
}
