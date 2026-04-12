package com.safedrive.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: TripEntity): Long

    @Query(
        """
        UPDATE trips
        SET endedAtEpochMs = :endedAt,
            avgRiskScore = :avgRiskScore,
            maxRiskScore = :maxRiskScore,
            totalEvents = :totalEvents
        WHERE id = :tripId
        """,
    )
    suspend fun endTrip(
        tripId: Long,
        endedAt: Long,
        avgRiskScore: Float,
        maxRiskScore: Int,
        totalEvents: Int,
    )
}
