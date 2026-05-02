package com.safedrive.ai.data

data class AppSettings(
    val emergencyContacts: List<String> = emptyList(),
    val demoCallNumber: String = "",
    val ttsTemplate: String = "This is a Sentry alert. Potential crash detected. Please respond.",
)
