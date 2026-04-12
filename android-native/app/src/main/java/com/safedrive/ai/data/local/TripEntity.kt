package com.safedrive.ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
    val avgRiskScore: Float = 0f,
    val maxRiskScore: Int = 0,
    val totalEvents: Int = 0,
)
