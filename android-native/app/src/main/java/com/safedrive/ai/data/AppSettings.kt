package com.safedrive.ai.data

data class AppSettings(
    val emergencyContacts: List<String> = listOf("+10000000000"),
    val demoCallNumber: String = "+10000000000",
    val ttsTemplate: String = "This is a Project Sentry demo alert. Potential crash detected. Please respond.",
)
