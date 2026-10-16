package com.nagarsetu.core.data

import android.content.Context
import android.util.Log
import com.nagarsetu.core.data.local.NagarSetuDatabase
import com.nagarsetu.core.data.local.entity.RoadReportEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: NagarSetuDatabase,
    private val assets: AssetDataRepository
) {
    fun runOnce() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (db.roadReportDao().count() > 0) return@launch
                
                val seedReports = assets.loadSeedReports()
                seedReports.forEach { el ->
                    try {
                        val o = el.asJsonObject
                        db.roadReportDao().insert(
                            RoadReportEntity(
                                id = o["id"].asString,
                                type = o["type"].asString,
                                lat = o["latitude"].asDouble,
                                lng = o["longitude"].asDouble,
                                severity = o["severity"].asString,
                                status = o["status"].asString,
                                ticketId = "RW-SEED-${o["id"].asString}",
                                description = o.get("description")?.asString ?: ""
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("SeedInitializer", "Error inserting seed item", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("SeedInitializer", "Critical error in runOnce", e)
            }
        }
    }
}
