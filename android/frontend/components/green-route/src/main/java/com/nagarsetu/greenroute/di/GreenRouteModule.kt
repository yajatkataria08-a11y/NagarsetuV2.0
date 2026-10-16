package com.nagarsetu.greenroute.di

import com.nagarsetu.greenroute.data.GtfsRepository
import com.nagarsetu.greenroute.data.GtfsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GreenRouteModule {
    @Binds
    @Singleton
    abstract fun bindGtfsRepository(impl: GtfsRepositoryImpl): GtfsRepository
}
