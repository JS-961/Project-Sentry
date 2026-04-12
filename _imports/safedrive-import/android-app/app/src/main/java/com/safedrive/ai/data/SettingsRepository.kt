package com.safedrive.ai.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun refresh() {
        _settings.value = loadSettings()
    }

    fun save(newSettings: AppSettings) {
        val normalizedContacts = newSettings.emergencyContacts
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(DEFAULT_PLACEHOLDER) }

        val normalized = newSettings.copy(
            emergencyContacts = normalizedContacts,
            demoCallNumber = newSettings.demoCallNumber.trim().ifBlank { DEFAULT_PLACEHOLDER },
            ttsTemplate = newSettings.ttsTemplate.trim().ifBlank { DEFAULT_TTS },
        )

        prefs.edit()
            .putString(KEY_CONTACTS, normalized.emergencyContacts.joinToString(separator = ","))
            .putString(KEY_DEMO_NUMBER, normalized.demoCallNumber)
            .putString(KEY_TTS, normalized.ttsTemplate)
            .apply()

        _settings.value = normalized
    }

    private fun loadSettings(): AppSettings {
        val rawContacts = prefs.getString(KEY_CONTACTS, DEFAULT_PLACEHOLDER).orEmpty()
        val contacts = rawContacts
            .split(",", "\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(DEFAULT_PLACEHOLDER) }

        return AppSettings(
            emergencyContacts = contacts,
            demoCallNumber = prefs.getString(KEY_DEMO_NUMBER, DEFAULT_PLACEHOLDER).orEmpty(),
            ttsTemplate = prefs.getString(KEY_TTS, DEFAULT_TTS).orEmpty(),
        )
    }

    companion object {
        private const val PREFS_NAME = "safedrive_settings"
        private const val KEY_CONTACTS = "contacts"
        private const val KEY_DEMO_NUMBER = "demo_number"
        private const val KEY_TTS = "tts_template"
        private const val DEFAULT_PLACEHOLDER = "+10000000000"
        private const val DEFAULT_TTS = "This is a SafeDrive AI demo alert. Potential crash detected. Please respond."
    }
}
