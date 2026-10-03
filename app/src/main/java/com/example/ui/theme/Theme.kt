package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MysticColorScheme = darkColorScheme(
    primary = MysticPrimary,
    onPrimary = MysticTextPrimary,
    primaryContainer = MysticContainerElevated,
    onPrimaryContainer = MysticPrimaryHighlight,
    secondary = MysticPrimaryHighlight,
    onSecondary = MysticBackground,
    secondaryContainer = MysticContainer,
    onSecondaryContainer = MysticTextPrimary,
    tertiary = MysticCyan,
    onTertiary = MysticBackground,
    background = MysticBackground,
    onBackground = MysticTextPrimary,
    surface = MysticContainer,
    onSurface = MysticTextPrimary,
    surfaceVariant = MysticContainerElevated,
    onSurfaceVariant = MysticTextSecondary,
    outline = MysticBorder,
    outlineVariant = MysticBorderSubtle,
    error = MysticError,
    onError = MysticTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Mystic Power defaults to dark theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = MysticColorScheme,
        typography = Typography,
        content = content
    )
}
