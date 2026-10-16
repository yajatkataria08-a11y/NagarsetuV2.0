package com.nagarsetu.core.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.nagarsetu.core.data.local.NagarSetuDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides @Singleton
    fun provideGson(): Gson = Gson()

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NagarSetuDatabase =
        Room.databaseBuilder(context, NagarSetuDatabase::class.java, "nagarsetu.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideRoadReportDao(db: NagarSetuDatabase) = db.roadReportDao()
    @Provides fun provideParkingDao(db: NagarSetuDatabase) = db.parkingDao()
    @Provides fun provideReportItDao(db: NagarSetuDatabase) = db.reportItDao()
    @Provides fun provideChatDao(db: NagarSetuDatabase) = db.chatDao()
    @Provides fun provideChallanDao(db: NagarSetuDatabase) = db.challanDao()
}
