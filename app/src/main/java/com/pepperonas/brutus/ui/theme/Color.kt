package com.pepperonas.brutus.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// Brand colors — deliberate, raw brand moments only (alarm-ring gradient,
// hardcore badge, widget). Everything else goes through MaterialTheme roles.
// ============================================================================

// Brand seed. The tonal palettes below are derived from this red
// (Material Theme Builder, chroma-clamped red family).
val BrutusRed = Color(0xFFE53935)           // Material Red 600
val BrutusRedBright = Color(0xFFFF8A80)     // Material Red A100 — text accents on dark
val BrutusDarkRed = Color(0xFFB71C1C)       // Material Red 900 — alarm gradient bg only
val BrutusOrange = Color(0xFFFF9800)        // tertiary seed (Sunrise/Timer accents)
val BrutusOrangeBright = Color(0xFFFFB74D)

// Legacy dark-surface aliases — still referenced by screens; migrated to
// surfaceContainer* roles screen-by-screen during the Expressive redesign.
val BrutusDark = Color(0xFF121212)
val BrutusSurface = Color(0xFF1E1E1E)
val BrutusCard = Color(0xFF2C2C2C)
val BrutusTextPrimary = Color(0xFFEEEEEE)
val BrutusTextSecondary = Color(0xFFB0B0B0)

// ============================================================================
// Red-seed tonal roles — DARK
// Primary keeps the hard brand red on fills (white large-type CTAs, as before);
// the tonal depth comes from containers and the surfaceContainer ladder.
// ============================================================================

val DarkPrimary = BrutusRed
val DarkOnPrimary = Color(0xFFFFFFFF)
val DarkPrimaryContainer = Color(0xFF93000A)     // red tone 30
val DarkOnPrimaryContainer = Color(0xFFFFDAD6)   // red tone 90
val DarkSecondary = Color(0xFFE7BDB8)            // warm red-grey tone 80
val DarkOnSecondary = Color(0xFF442926)
val DarkSecondaryContainer = Color(0xFF5D3F3C)
val DarkOnSecondaryContainer = Color(0xFFFFDAD6)
val DarkTertiary = Color(0xFFFFB77C)             // orange tone 80
val DarkOnTertiary = Color(0xFF4A2800)
val DarkTertiaryContainer = Color(0xFF6A3C00)
val DarkOnTertiaryContainer = Color(0xFFFFDCC2)
val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

// Warm neutrals — noticeably darker than stock M3 to keep the hard night look.
val DarkSurface = Color(0xFF141110)              // neutral tone ~6
val DarkOnSurface = Color(0xFFEDE0DD)
val DarkSurfaceVariant = Color(0xFF534341)
val DarkOnSurfaceVariant = Color(0xFFD8C2BF)
val DarkSurfaceContainerLowest = Color(0xFF0C0908)
val DarkSurfaceContainerLow = Color(0xFF1D1A19)
val DarkSurfaceContainer = Color(0xFF211E1D)
val DarkSurfaceContainerHigh = Color(0xFF2B2827)
val DarkSurfaceContainerHighest = Color(0xFF363231)
val DarkOutline = Color(0xFFA08C8A)
val DarkOutlineVariant = Color(0xFF534341)
val DarkInverseSurface = Color(0xFFEDE0DD)
val DarkInverseOnSurface = Color(0xFF362F2E)
val DarkInversePrimary = Color(0xFFB3261E)

// ============================================================================
// Red-seed tonal roles — LIGHT (tone-40 primary for AA on white)
// ============================================================================

val LightPrimary = Color(0xFFB3261E)             // red tone 40
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFFFDAD6)
val LightOnPrimaryContainer = Color(0xFF410002)
val LightSecondary = Color(0xFF775652)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFFFDAD6)
val LightOnSecondaryContainer = Color(0xFF2C1512)
val LightTertiary = Color(0xFF8B5000)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFDCC2)
val LightOnTertiaryContainer = Color(0xFF2C1600)
val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

val LightSurface = Color(0xFFFFF8F6)
val LightOnSurface = Color(0xFF231918)
val LightSurfaceVariant = Color(0xFFF5DDDA)
val LightOnSurfaceVariant = Color(0xFF534341)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFFFF0EE)
val LightSurfaceContainer = Color(0xFFFCEAE7)
val LightSurfaceContainerHigh = Color(0xFFF7E4E1)
val LightSurfaceContainerHighest = Color(0xFFF1DEDC)
val LightOutline = Color(0xFF857371)
val LightOutlineVariant = Color(0xFFD8C2BF)
val LightInverseSurface = Color(0xFF392E2D)
val LightInverseOnSurface = Color(0xFFFFEDEA)
val LightInversePrimary = Color(0xFFFFB4AB)
