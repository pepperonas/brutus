package com.pepperonas.brutus.ui.alarm

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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pepperonas.brutus.ui.theme.BrutusOrange
import com.pepperonas.brutus.ui.theme.BrutusOrangeBright
import kotlinx.coroutines.launch

/**
 * Slide-to-unlock style snooze button. User must drag the thumb from the left
 * to past ~85% of the track width to trigger [onSnooze]. Spring-back otherwise.
 */
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
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var triggered by remember { mutableStateOf(false) }

    val maxOffset = (trackWidthPx - thumbSizePx).coerceAtLeast(0f)
    val progress = if (maxOffset > 0f) (offsetX.value / maxOffset).coerceIn(0f, 1f) else 0f

    // Pulsing hint when idle
    val infinite = rememberInfiniteTransition(label = "snoozeHint")
    val hintAlpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "hintAlpha"
    )
    val hintShift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "hintShift"
    )

    // Reset after trigger (for re-use; AlarmScreen finishes anyway)
    LaunchedEffect(triggered) {
        if (triggered) {
            kotlinx.coroutines.delay(400)
            offsetX.snapTo(0f)
            triggered = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(BrutusOrange.copy(alpha = 0.12f))
            .border(
                1.dp,
                BrutusOrange.copy(alpha = 0.35f + 0.4f * progress),
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
                            BrutusOrange.copy(alpha = 0.25f),
                            BrutusOrange.copy(alpha = 0.55f)
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
                text = "Zum Snoozen wischen",
                color = BrutusOrangeBright.copy(alpha = hintAlpha),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.padding(end = 6.dp)
            )
            Icon(
                Icons.Default.KeyboardDoubleArrowRight,
                contentDescription = null,
                tint = BrutusOrangeBright.copy(alpha = hintAlpha),
                modifier = Modifier
                    .size(20.dp)
                    .offset { IntOffset(hintShift.toInt(), 0) }
            )
        }

        // Thumb
        Box(
            modifier = Modifier
                .padding(thumbPadding)
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(BrutusOrange)
                .pointerInput(maxOffset) {
                    if (maxOffset <= 0f) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value >= maxOffset * 0.85f && !triggered) {
                                    offsetX.animateTo(maxOffset, tween(150))
                                    triggered = true
                                    onSnooze()
                                } else {
                                    offsetX.animateTo(
                                        0f,
                                        spring(
                                            dampingRatio = 0.55f,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(
                                    0f,
                                    spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)
                                )
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val next = (offsetX.value + dragAmount).coerceIn(0f, maxOffset)
                                offsetX.snapTo(next)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.KeyboardDoubleArrowRight,
                contentDescription = "Snooze",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

