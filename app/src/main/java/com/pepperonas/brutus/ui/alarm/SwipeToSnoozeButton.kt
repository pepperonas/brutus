package com.pepperonas.brutus.ui.alarm

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.State
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.pepperonas.brutus.ui.theme.rememberReducedMotion
import com.pepperonas.brutus.util.rememberBrutusHaptics
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.pepperonas.brutus.R

/**
 * Slide-to-unlock style snooze button. User must drag the thumb from the left
 * to past ~85% of the track width to trigger [onSnooze]. Spring-back otherwise.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SwipeToSnoozeButton(
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val height = 64.dp
    val thumbSize = 56.dp
    val thumbPadding = 4.dp
    val thumbSizePx = with(density) { thumbSize.toPx() }

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    // One source of truth for the thumb position: the drag writes it directly, the release
    // animation writes it frame by frame — no coroutine per pointer event fighting the fling.
    var offsetX by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    var triggered by remember { mutableStateOf(false) }
    val haptics = rememberBrutusHaptics()
    val releaseSpring = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val snapSpring = MaterialTheme.motionScheme.fastSpatialSpec<Float>()

    val maxOffset = (trackWidthPx - thumbSizePx).coerceAtLeast(0f)
    val progress = if (maxOffset > 0f) (offsetX / maxOffset).coerceIn(0f, 1f) else 0f
    // A tick the moment the thumb crosses the point of no return, so the finger feels it.
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(snoozeArmed(offsetX, maxOffset)) {
        val nowArmed = snoozeArmed(offsetX, maxOffset)
        if (nowArmed && !armed && !triggered) haptics.tap()
        armed = nowArmed
    }
    val accent = MaterialTheme.colorScheme.tertiary
    val onAccent = MaterialTheme.colorScheme.onTertiary

    // Hoisted out of the modifier chain: semantics {} is not a composable scope,
    // so stringResource() cannot be called inside it.
    val swipeHint = stringResource(R.string.snooze_swipe_hint)
    val snoozeAction = stringResource(R.string.snooze_action)

    // Pulsing hint when idle — static when system animations are disabled.
    // Kept as State and read in layer/offset lambdas: the pulse repaints, it does not recompose.
    val reducedMotion = rememberReducedMotion()
    val hintAlpha: State<Float>
    val hintShift: State<Float>
    if (reducedMotion) {
        hintAlpha = remember { mutableFloatStateOf(1f) }
        hintShift = remember { mutableFloatStateOf(0f) }
    } else {
        val infinite = rememberInfiniteTransition(label = "snoozeHint")
        val alphaAnim = infinite.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(1000, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "hintAlpha"
        )
        val shiftAnim = infinite.animateFloat(
            initialValue = 0f,
            targetValue = 8f,
            animationSpec = infiniteRepeatable(
                tween(1200, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "hintShift"
        )
        hintAlpha = alphaAnim
        hintShift = shiftAnim
    }

    // The thumb morphs from circle toward a squircle over the last stretch —
    // the shape itself announces "gleich rastet es ein".
    val thumbCornerPercent = (50 - ((progress - 0.55f).coerceAtLeast(0f) / 0.45f * 22f)).toInt()

    // Reset after trigger (for re-use; AlarmScreen finishes anyway)
    LaunchedEffect(triggered) {
        if (triggered) {
            kotlinx.coroutines.delay(400)
            offsetX = 0f
            triggered = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            // Accessibility fallback: TalkBack / switch-access users can't
            // perform the drag gesture — expose snooze as a custom action.
            .semantics {
                contentDescription = swipeHint
                customActions = listOf(
                    CustomAccessibilityAction(snoozeAction) {
                        if (!triggered) {
                            triggered = true
                            haptics.success()
                            onSnooze()
                        }
                        true
                    }
                )
            }
            .clip(RoundedCornerShape(height / 2))
            .background(accent.copy(alpha = 0.12f))
            .border(
                1.dp,
                accent.copy(alpha = 0.35f + 0.4f * progress),
                RoundedCornerShape(height / 2)
            )
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
    ) {
        // Progress fill
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            accent.copy(alpha = 0.25f),
                            accent.copy(alpha = 0.55f)
                        )
                    )
                )
        )

        // Hint text fades out as user drags
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = thumbSize + thumbPadding * 2)
                .alpha((1f - progress * 1.6f).coerceIn(0f, 1f)),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = swipeHint,
                color = accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .graphicsLayer { alpha = hintAlpha.value }
            )
            Icon(
                Icons.Default.KeyboardDoubleArrowRight,
                contentDescription = null,
                tint = accent,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { alpha = hintAlpha.value }
                    .offset { IntOffset(hintShift.value.toInt(), 0) }
            )
        }

        // Thumb
        Box(
            modifier = Modifier
                .padding(thumbPadding)
                .offset { IntOffset(offsetX.toInt(), 0) }
                .size(thumbSize)
                .clip(RoundedCornerShape(percent = thumbCornerPercent))
                .background(accent)
                .draggable(
                    orientation = Orientation.Horizontal,
                    enabled = maxOffset > 0f && !triggered,
                    state = rememberDraggableState { delta ->
                        offsetX = (offsetX + delta).coerceIn(0f, maxOffset)
                    },
                    onDragStopped = { velocity ->
                        // Only the position decides (see snoozeArmed) — a quick flick must not
                        // snooze by accident. The fling's velocity feeds the spring either way,
                        // so the thumb carries the finger's momentum instead of stopping dead.
                        if (snoozeArmed(offsetX, maxOffset) && !triggered) {
                            triggered = true
                            haptics.success()
                            animate(offsetX, maxOffset, initialVelocity = velocity, animationSpec = snapSpring) { v, _ ->
                                offsetX = v
                            }
                            onSnooze()
                        } else {
                            animate(offsetX, 0f, initialVelocity = velocity, animationSpec = releaseSpring) { v, _ ->
                                offsetX = v.coerceIn(-thumbSizePx / 4, maxOffset)
                            }
                        }
                    },
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.KeyboardDoubleArrowRight,
                contentDescription = snoozeAction,
                tint = onAccent,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// Alarm surfaces are pinned dark by design — no light variant.
@androidx.compose.ui.tooling.preview.Preview(
    name = "Snooze slider",
    showBackground = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun SwipeToSnoozePreview() {
    com.pepperonas.brutus.ui.theme.BrutusTheme(darkTheme = true) {
        SwipeToSnoozeButton(onSnooze = {})
    }
}


/** Past this share of the track, releasing the thumb snoozes. Position only, never velocity. */
internal const val SNOOZE_THRESHOLD = 0.85f

internal fun snoozeArmed(offset: Float, maxOffset: Float): Boolean =
    maxOffset > 0f && offset >= maxOffset * SNOOZE_THRESHOLD
