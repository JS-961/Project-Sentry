package com.safedrive.ai.data.local

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface CrashAlertDao {
    @Insert
    suspend fun insert(alert: CrashAlertEntity): Long
}
