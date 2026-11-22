// OWNER: A — TODO
package com.example.mos.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.mos.data.RiskLevel

// Colours from DESIGN.md. Keep the two in sync.

// Brand
val Primary = Color(0xFF14532D)
val PrimaryDark = Color(0xFF0B3B20)
val PrimaryContainer = Color(0xFFE4F0E8)
val OnPrimary = Color(0xFFFFFFFF)

// Risk (only ever used to show risk)
val Safe = Color(0xFF2DB864)
val SafeContainer = Color(0xFFD7F5E2)
val Caution = Color(0xFFF9AB00)
val CautionContainer = Color(0xFFFEF3D6)
val Risk = Color(0xFFD93025)
val RiskContainer = Color(0xFFFCE8E6)
val Info = Color(0xFF1A73E8)
val InfoContainer = Color(0xFFE8F0FE)

// Neutrals
val Background = Color(0xFFF5F7FB)
val Surface = Color(0xFFFFFFFF)
val SurfaceVariant = Color(0xFFEEF1F6)
val Outline = Color(0xFFE1E5EB)
val TextPrimary = Color(0xFF1F1F1F)
val TextSecondary = Color(0xFF5F6368)

// Green / amber / red for a risk level.
fun RiskLevel.color(): Color = when (this) {
    RiskLevel.LOW -> Safe
    RiskLevel.MODERATE -> Caution
    RiskLevel.HIGH -> Risk
}
