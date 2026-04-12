package com.safedrive.ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crash_alerts")
data class CrashAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long?,
    val createdAtEpochMs: Long,
    val simulated: Boolean,
    val outcome: String,
    val smsStatus: String,
    val callStatus: String,
    val ttsStatus: String,
    val latitude: Double?,
    val longitude: Double?,
)
