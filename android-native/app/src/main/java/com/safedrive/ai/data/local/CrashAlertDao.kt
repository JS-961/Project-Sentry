package com.safedrive.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CrashAlertDao {
    @Insert
    suspend fun insert(alert: CrashAlertEntity): Long

    @Query("SELECT * FROM crash_alerts ORDER BY createdAtEpochMs DESC LIMIT :limit")
    fun observeRecentAlerts(limit: Int): Flow<List<CrashAlertEntity>>

    @Query("SELECT COUNT(*) FROM crash_alerts")
    fun observeAlertCount(): Flow<Int>
}
