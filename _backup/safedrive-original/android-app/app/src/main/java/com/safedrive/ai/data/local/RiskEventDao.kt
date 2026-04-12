package com.safedrive.ai.data.local

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface RiskEventDao {
    @Insert
    suspend fun insert(event: RiskEventEntity): Long
}
