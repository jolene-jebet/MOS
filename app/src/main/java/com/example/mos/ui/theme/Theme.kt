// OWNER: A — TODO
package com.example.mos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Light mode only for now (see DESIGN.md).
private val MosColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = Primary,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = Outline,
    outlineVariant = Outline,
    error = Risk,
    errorContainer = RiskContainer
)

@Composable
fun MOSTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MosColorScheme,
        typography = Typography,
        content = content
    )
}
