package com.nagarsetu.main

import android.app.Application
import android.util.Log
import androidx.preference.PreferenceManager
import com.nagarsetu.core.data.SeedInitializer
import com.nagarsetu.backend.core.AdminSocketClient
import com.nagarsetu.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration
import javax.inject.Inject

@HiltAndroidApp
class NagarSetuApp : Application() {

    @Inject lateinit var seedInitializer: SeedInitializer
    @Inject lateinit var adminSocketClient: AdminSocketClient

    override fun onCreate() {
        super.onCreate()
        try {
            Configuration.getInstance().load(
                this,
                PreferenceManager.getDefaultSharedPreferences(this)
            )
        } catch (e: Exception) {
            Log.e("NagarSetuApp", "OSMDroid init failed", e)
        }
        
        try {
            seedInitializer.runOnce()
        } catch (e: Exception) {
            Log.e("NagarSetuApp", "Seed initialization failed", e)
        }

        try {
            val prefs = getSharedPreferences("nagarsetu_prefs", MODE_PRIVATE)
            val adminUrl = prefs.getString("raksha_api_endpoint", BuildConfig.ADMIN_BASE_URL) 
                ?: BuildConfig.ADMIN_BASE_URL
            adminSocketClient.connect(adminUrl)
        } catch (e: Exception) {
            Log.e("NagarSetuApp", "Admin socket init failed", e)
        }
    }
}
