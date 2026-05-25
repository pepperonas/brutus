package com.pepperonas.brutus.util

/**
 * Difficulty / sensitivity presets for the configurable challenge types.
 * Values match the stored integer fields on AlarmEntity.
 */
object ChallengeDifficulty {

    // Math
    const val MATH_EASY = 0
    const val MATH_HARD = 1
    const val MATH_BRUTAL = 2

    fun mathLabel(level: Int): String = when (level) {
        MATH_EASY -> "Einfach"
        MATH_BRUTAL -> "Brutal"
        else -> "Hart"
    }

    fun mathDescription(level: Int): String = when (level) {
        MATH_EASY -> "Addition und Subtraktion bis 20"
        MATH_BRUTAL -> "Zweistellige Multiplikation und große Zahlen"
        else -> "Mix mit Multiplikation bis 50 × 20"
    }

    // Shake
    const val SHAKE_LIGHT = 0
    const val SHAKE_NORMAL = 1
    const val SHAKE_HARD = 2

    fun shakeLabel(level: Int): String = when (level) {
        SHAKE_LIGHT -> "Empfindlich"
        SHAKE_HARD -> "Stark"
        else -> "Normal"
    }

    fun shakeDescription(level: Int): String = when (level) {
        SHAKE_LIGHT -> "Schon leichte Bewegungen zählen"
        SHAKE_HARD -> "Nur kräftiges Schütteln zählt"
        else -> "Standard-Schwellwert (delta ≥ 12)"
    }

    /** Acceleration delta threshold (m/s²) — anything above counts as one shake. */
    fun shakeThreshold(level: Int): Float = when (level) {
        SHAKE_LIGHT -> 9f
        SHAKE_HARD -> 16f
        else -> 12f
    }
}
