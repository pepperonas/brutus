package com.pepperonas.brutus.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * True when the user has disabled system animations (developer options or the
 * "remove animations" accessibility setting set ANIMATOR_DURATION_SCALE to 0).
 * Decorative loops (breathing backgrounds, pulse hints) must not run then;
 * state-driven transitions may simply snap.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}
