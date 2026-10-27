package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GamingDarkColorScheme = darkColorScheme(
    primary = NeonBlue,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0369A1),
    onPrimaryContainer = Color.White,
    secondary = NeonGreen,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color.White,
    tertiary = NeonYellow,
    onTertiary = Color.Black,
    background = NavyDarkBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = NavyDarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = NavyDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    error = NeonRed
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GamingDarkColorScheme,
        typography = Typography,
        content = content
    )
}
