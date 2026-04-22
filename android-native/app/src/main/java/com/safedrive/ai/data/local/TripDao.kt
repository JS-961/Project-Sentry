package com.safedrive.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: TripEntity): Long

    @Query("SELECT * FROM trips ORDER BY startedAtEpochMs DESC LIMIT :limit")
    fun observeRecentTrips(limit: Int): Flow<List<TripEntity>>

    @Query("SELECT COUNT(*) FROM trips")
    fun observeTripCount(): Flow<Int>

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
