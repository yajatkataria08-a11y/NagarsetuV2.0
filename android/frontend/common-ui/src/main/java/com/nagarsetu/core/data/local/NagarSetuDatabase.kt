package com.nagarsetu.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nagarsetu.core.data.local.dao.*
import com.nagarsetu.core.data.local.entity.*

@Database(
    entities = [
        RoadReportEntity::class,
        ParkingBookingEntity::class,
        CivicIssueEntity::class,
        ChatMessageEntity::class,
        ChallanEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NagarSetuDatabase : RoomDatabase() {
    abstract fun roadReportDao(): RoadReportDao
    abstract fun parkingDao(): ParkingDao
    abstract fun reportItDao(): ReportItDao
    abstract fun chatDao(): ChatDao
    abstract fun challanDao(): ChallanDao
}
