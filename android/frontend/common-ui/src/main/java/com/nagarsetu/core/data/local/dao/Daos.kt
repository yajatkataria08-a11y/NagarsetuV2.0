package com.nagarsetu.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nagarsetu.core.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RoadReportDao {
    @Query("SELECT * FROM road_reports ORDER BY id DESC")
    fun observeAll(): Flow<List<RoadReportEntity>>

    @Query("SELECT * FROM road_reports ORDER BY id DESC")
    suspend fun getAll(): List<RoadReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RoadReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<RoadReportEntity>)

    @Query("SELECT COUNT(*) FROM road_reports")
    suspend fun count(): Int
}

@Dao
interface ParkingDao {
    @Query("SELECT * FROM parking_bookings WHERE status = 'ACTIVE' LIMIT 1")
    fun observeActive(): Flow<ParkingBookingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ParkingBookingEntity)

    @Query("DELETE FROM parking_bookings WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ReportItDao {
    @Query("SELECT * FROM civic_issues ORDER BY id DESC")
    fun observeAll(): Flow<List<CivicIssueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CivicIssueEntity)

    @Query("SELECT COUNT(*) FROM civic_issues")
    suspend fun count(): Int
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}

@Dao
interface ChallanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ChallanEntity)
}
