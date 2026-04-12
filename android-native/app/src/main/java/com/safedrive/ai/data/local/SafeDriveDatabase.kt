package com.safedrive.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TripEntity::class,
        RiskEventEntity::class,
        CrashAlertEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class SafeDriveDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun riskEventDao(): RiskEventDao
    abstract fun crashAlertDao(): CrashAlertDao

    companion object {
        @Volatile
        private var INSTANCE: SafeDriveDatabase? = null

        fun getInstance(context: Context): SafeDriveDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SafeDriveDatabase::class.java,
                    "safedrive.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
