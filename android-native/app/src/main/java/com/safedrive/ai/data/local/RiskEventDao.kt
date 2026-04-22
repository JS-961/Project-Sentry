package com.safedrive.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RiskEventDao {
    @Insert
    suspend fun insert(event: RiskEventEntity): Long

    @Query("SELECT * FROM risk_events ORDER BY createdAtEpochMs DESC LIMIT :limit")
    fun observeRecentEvents(limit: Int): Flow<List<RiskEventEntity>>

    @Query("SELECT COUNT(*) FROM risk_events")
    fun observeEventCount(): Flow<Int>
}
