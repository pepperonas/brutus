package com.pepperonas.brutus.util

import androidx.annotation.StringRes
import com.pepperonas.brutus.R

/**
 * Difficulty / sensitivity presets for the configurable challenge types.
 * Values match the stored integer fields on AlarmEntity.
 *
 * The label/description accessors return **resource ids**: the levels are
 * persisted numbers, their names are localized text.
 */
object ChallengeDifficulty {

    // Math
    const val MATH_EASY = 0
    const val MATH_HARD = 1
    const val MATH_BRUTAL = 2

    @StringRes
    fun mathLabel(level: Int): Int = when (level) {
        MATH_EASY -> R.string.math_difficulty_easy
        MATH_BRUTAL -> R.string.math_difficulty_brutal
        else -> R.string.math_difficulty_hard
    }

    @StringRes
    fun mathDescription(level: Int): Int = when (level) {
        MATH_EASY -> R.string.math_difficulty_easy_description
        MATH_BRUTAL -> R.string.math_difficulty_brutal_description
        else -> R.string.math_difficulty_hard_description
    }

    // Shake
    const val SHAKE_LIGHT = 0
    const val SHAKE_NORMAL = 1
    const val SHAKE_HARD = 2

    @StringRes
    fun shakeLabel(level: Int): Int = when (level) {
        SHAKE_LIGHT -> R.string.shake_sensitivity_light
        SHAKE_HARD -> R.string.shake_sensitivity_hard
        else -> R.string.shake_sensitivity_normal
    }

    @StringRes
    fun shakeDescription(level: Int): Int = when (level) {
        SHAKE_LIGHT -> R.string.shake_sensitivity_light_description
        SHAKE_HARD -> R.string.shake_sensitivity_hard_description
        else -> R.string.shake_sensitivity_normal_description
    }

    /** Acceleration delta threshold (m/s²) — anything above counts as one shake. */
    fun shakeThreshold(level: Int): Float = when (level) {
        SHAKE_LIGHT -> 9f
        SHAKE_HARD -> 16f
        else -> 12f
    }
}
