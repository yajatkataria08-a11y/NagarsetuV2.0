package com.nagarsetu.core.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.nagarsetu.core.data.local.NagarSetuDatabase
import com.nagarsetu.core.data.network.AuthInterceptor
import com.nagarsetu.core.data.network.NagarSetuApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides @Singleton
    fun provideGson(): Gson = Gson()

    @Provides @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

    @Provides @Singleton
    fun provideNagarSetuApiService(
        okHttpClient: OkHttpClient,
        gson: Gson,
        @ApplicationContext context: Context
    ): NagarSetuApiService {
        val prefs = context.getSharedPreferences("nagarsetu_prefs", Context.MODE_PRIVATE)
        val customBase = prefs.getString("nagarsetu_api_endpoint", null)
        val baseUrl = if (!customBase.isNullOrBlank()) {
            if (customBase.startsWith("http")) customBase else "http://$customBase"
        } else {
            "http://10.0.2.2:4000"
        }.removeSuffix("/") + "/"

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(NagarSetuApiService::class.java)
    }

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
    @Provides fun providePendingQueueDao(db: NagarSetuDatabase) = db.pendingQueueDao()
}
