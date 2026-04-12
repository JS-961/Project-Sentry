package com.safedrive.ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "risk_events")
data class RiskEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long?,
    val eventType: String,
    val value: Float,
    val speedMps: Float,
    val latitude: Double?,
    val longitude: Double?,
    val createdAtEpochMs: Long,
)
