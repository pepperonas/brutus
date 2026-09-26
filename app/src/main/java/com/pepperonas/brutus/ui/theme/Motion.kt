package com.pepperonas.brutus.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.currentStateAsState

/**
 * True when the user has disabled system animations (developer options or the
 * "remove animations" accessibility setting set ANIMATOR_DURATION_SCALE to 0).
 * Decorative loops (breathing backgrounds, pulse hints) must not run then;
 * state-driven transitions may simply snap.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    // Re-read whenever the screen comes back: the user may have switched animations off in the
    // system settings in the meantime.
    val lifecycleState by androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
        .currentStateAsState()
    return remember(lifecycleState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}

/**
 * Press feedback with physics: the element sinks to 94 % on the theme's fast spatial spring and
 * springs back on release; snaps instead when animations are off. Ported from Flipper the Ripper.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun androidx.compose.ui.Modifier.springPressed(
    interactionSource: androidx.compose.foundation.interaction.InteractionSource,
): androidx.compose.ui.Modifier {
    val reduce = rememberReducedMotion()
    val pressed by interactionSource.collectIsPressedAsState()
    val spec = androidx.compose.material3.MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }
    androidx.compose.runtime.LaunchedEffect(pressed, reduce) {
        val target = if (pressed) 0.94f else 1f
        if (reduce) scale.snapTo(target) else scale.animateTo(target, spec)
    }
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
