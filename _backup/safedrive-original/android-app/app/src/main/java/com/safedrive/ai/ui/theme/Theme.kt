package com.safedrive.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SafeDriveColorScheme = darkColorScheme()

@Composable
fun SafeDriveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SafeDriveColorScheme,
        content = content,
    )
}
