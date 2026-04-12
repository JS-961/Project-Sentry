package com.safedrive.ai

import android.app.Application
import com.safedrive.ai.data.SettingsRepository
import com.safedrive.ai.data.local.SafeDriveDatabase

class SafeDriveApplication : Application() {
    val database: SafeDriveDatabase by lazy { SafeDriveDatabase.getInstance(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}
