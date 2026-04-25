package com.pepperonas.brutus.util

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants

/**
 * Lightweight wrapper around Android's [HapticFeedback] that maps Brutus's
 * common interaction kinds to the right tactile primitive.
 *
 * - [tap]      — short crisp tick for a binary toggle / button press
 * - [success]  — slightly heavier hint for completing a step (challenge done,
 *                snooze triggered)
 * - [warn]     — long press equivalent for destructive / cautious actions
 *
 * The newer [HapticFeedbackConstants] are used directly via [LocalView] when
 * available (API 30+) because they provide nicer system-tuned curves than the
 * coarse Compose Haptic wrapper.
 */
class BrutusHaptics(
    private val view: android.view.View,
    private val composeHaptics: HapticFeedback,
) {
    fun tap() {
        if (Build.VERSION.SDK_INT >= 30) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    fun success() {
        if (Build.VERSION.SDK_INT >= 30) {
            view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
        } else {
            composeHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun warn() {
        composeHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

@Composable
fun rememberBrutusHaptics(): BrutusHaptics {
    val view = LocalView.current
    val compose = LocalHapticFeedback.current
    return BrutusHaptics(view, compose)
}
