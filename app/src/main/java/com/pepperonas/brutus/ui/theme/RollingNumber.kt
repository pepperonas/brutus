@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.pepperonas.brutus.ui.theme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset

/**
 * A number that rolls like a mechanical counter: counting up, the new value comes in from below
 * and the old one leaves upwards; counting down, the other way round. Springs from the theme.
 */
@Composable
fun RollingNumber(value: Int, modifier: Modifier = Modifier, content: @Composable (Int) -> Unit) {
    val spatial = MaterialTheme.motionScheme.fastSpatialSpec<IntOffset>()
    val effects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    AnimatedContent(
        targetState = value,
        modifier = modifier,
        transitionSpec = {
            val dir = rollDirection(initialState, targetState)
            (slideInVertically(spatial) { dir * it / 2 } + fadeIn(effects)) togetherWith
                (slideOutVertically(spatial) { -dir * it / 2 } + fadeOut(effects))
        },
        label = "rollingNumber",
    ) { content(it) }
}

/** +1: the new value enters from below (counting up); -1: from above (counting down). */
internal fun rollDirection(from: Int, to: Int): Int = if (to >= from) 1 else -1
