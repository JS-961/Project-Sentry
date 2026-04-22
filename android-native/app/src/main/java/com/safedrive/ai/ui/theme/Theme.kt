package com.safedrive.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val SafeDriveColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF03131D),
    primaryContainer = Color(0xFF0F2A3A),
    onPrimaryContainer = Color(0xFFE0F7FF),
    secondary = Color(0xFF22C55E),
    onSecondary = Color(0xFF03140A),
    tertiary = Color(0xFFFBBF24),
    background = Color(0xFF07111D),
    onBackground = Color(0xFFE5EDF6),
    surface = Color(0xFF101B2A),
    onSurface = Color(0xFFF5F7FA),
    surfaceVariant = Color(0xFF1F2D3D),
    onSurfaceVariant = Color(0xFFB9C5D4),
    error = Color(0xFFEF4444),
    onError = Color.White,
)

private val SafeDriveShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
)

@Composable
fun SafeDriveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SafeDriveColorScheme,
        shapes = SafeDriveShapes,
        content = content,
    )
}
