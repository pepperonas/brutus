@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.pepperonas.brutus.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.ui.theme.BrutusTheme
import com.pepperonas.brutus.util.rememberBrutusHaptics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource
import com.pepperonas.brutus.R

// The alarm cards of the list: swipe-to-delete wrapper, the card itself, weekday strip and chips.

/** Swipe-to-delete wrapper: drag the card off to the left, undo via snackbar. */
@Composable
internal fun DismissableAlarmCard(
    alarm: AlarmEntity,
    isNext: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onClick: () -> Unit,
) {
    val haptics = rememberBrutusHaptics()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )
    // A tick when releasing would now delete — and again if the finger pulls back out of it.
    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) haptics.tap()
    }
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            // Everything follows the finger: the red deepens and the bin grows with the drag, and
            // jumps once more when letting go would delete. Read in draw/layer lambdas only.
            val errorContainer = MaterialTheme.colorScheme.errorContainer
            fun fraction(): Float =
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                    dismissState.progress.coerceIn(0f, 1f) else 0f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large)
                    .drawBehind { drawRect(errorContainer.copy(alpha = (fraction() * 2.5f).coerceIn(0f, 1f))) },
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .padding(end = 28.dp)
                        .graphicsLayer {
                            val f = fraction()
                            val armed = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
                            val scale = if (armed) 1.25f else 0.6f + 0.4f * (f * 2f).coerceAtMost(1f)
                            scaleX = scale
                            scaleY = scale
                            alpha = (f * 4f).coerceIn(0f, 1f)
                        }
                )
            }
        }
    ) {
        AlarmCard(
            alarm = alarm,
            isNext = isNext,
            onToggle = onToggle,
            onDelete = onDelete,
            onCopy = onCopy,
            onClick = onClick,
        )
    }
}

/**
 * Card color encodes the ENABLED state only: every enabled alarm sits on the
 * same muted red container (primaryContainer blended toward the surface so a
 * list full of alarms stays calm), disabled ones sink to surfaceContainerLow.
 * The next firing alarm keeps its color and is marked by a thin primary
 * outline instead — same state, same color; the header names it anyway.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AlarmCard(
    alarm: AlarmEntity,
    isNext: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val dim = !alarm.enabled

    // Muted red: primaryContainer pulled halfway toward the neutral surface —
    // clearly "armed", but a screen full of enabled alarms doesn't scream.
    val enabledContainer = lerp(cs.surfaceContainerHigh, cs.primaryContainer, 0.45f)
    val containerColor by animateColorAsState(
        targetValue = if (alarm.enabled) enabledContainer else cs.surfaceContainerLow,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "cardColor"
    )
    val timeColor = if (alarm.enabled) cs.onSurface else cs.onSurfaceVariant.copy(alpha = 0.6f)
    val subColor = when {
        alarm.enabled -> lerp(cs.onSurfaceVariant, cs.onPrimaryContainer, 0.5f)
        else -> cs.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.large,
        border = if (isNext && alarm.enabled) BorderStroke(2.dp, cs.primary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alarm.timeString(),
                        style = MaterialTheme.typography.displayMedium,
                        color = timeColor
                    )
                    Text(
                        text = alarm.repeatDaysString(LocalContext.current),
                        style = MaterialTheme.typography.bodyMedium,
                        color = subColor
                    )
                    if (alarm.label.isNotBlank()) {
                        Text(
                            text = alarm.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = timeColor.copy(alpha = if (dim) 0.6f else 0.9f)
                        )
                    }
                }
                IconButton(onClick = onCopy) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.alarm_card_copy),
                        tint = subColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.alarm_card_delete),
                        tint = subColor
                    )
                }
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = { onToggle() },
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            WeekdayStrip(
                repeatDays = alarm.repeatDays,
                enabled = alarm.enabled,
                inactiveColor = subColor,
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Info chips: mode, sunrise, challenge, snooze, sound
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (alarm.ultraHardcoreMode) {
                    InfoChip(
                        stringResource(R.string.chip_ultra_hardcore),
                        cs.error, cs.onError, dim = dim
                    )
                } else if (alarm.hardcoreMode) {
                    InfoChip(
                        stringResource(R.string.chip_hardcore),
                        cs.errorContainer, cs.onErrorContainer, dim = dim
                    )
                }
                if (alarm.sunriseEnabled) {
                    InfoChip(
                        stringResource(R.string.chip_sunrise),
                        cs.tertiaryContainer, cs.onTertiaryContainer, dim = dim
                    )
                }
                val ctx = LocalContext.current
                InfoChip(
                    alarm.challengeName(ctx),
                    cs.secondaryContainer, cs.onSecondaryContainer, dim = dim
                )
                InfoChip(
                    stringResource(R.string.chip_snooze, alarm.snoozeDuration),
                    cs.surfaceContainerHighest, cs.onSurfaceVariant, dim = dim
                )
                InfoChip(
                    stringResource(R.string.chip_sound, alarm.soundName(ctx)),
                    cs.surfaceContainerHighest, cs.onSurfaceVariant, dim = dim
                )
            }
        }
    }
}

/** Full-width weekday strip — always a single row, each day gets an equal slice. */
@Composable
internal fun WeekdayStrip(
    repeatDays: Int,
    enabled: Boolean,
    inactiveColor: Color,
) {
    val cs = MaterialTheme.colorScheme
    val labels = stringArrayResource(R.array.weekday_short)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val isOn = (repeatDays and (1 shl index)) != 0
            val textColor = when {
                !enabled -> inactiveColor.copy(alpha = 0.5f)
                isOn -> cs.onPrimary
                else -> inactiveColor
            }
            val bg = when {
                isOn && enabled -> cs.primary
                isOn -> cs.primary.copy(alpha = 0.25f)
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(bg)
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isOn) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    }
}

@Composable
internal fun InfoChip(
    text: String,
    container: Color,
    content: Color,
    dim: Boolean = false,
) {
    val alpha = if (dim) 0.55f else 1f
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = content.copy(alpha = alpha),
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(container.copy(alpha = alpha))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

internal val previewNextAlarm = AlarmEntity(
    id = 1, hour = 6, minute = 30, label = "Frühschicht",
    repeatDays = 0b0011111, ultraHardcoreMode = true, sunriseEnabled = true,
)

internal val previewAlarm = AlarmEntity(
    id = 2, hour = 9, minute = 15, repeatDays = 0b1100000, hardcoreMode = true,
)

internal val previewDisabledAlarm = AlarmEntity(
    id = 3, hour = 14, minute = 0, label = "Powernap", enabled = false,
)

@Composable
internal fun CardStack() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AlarmCard(previewNextAlarm, isNext = true, {}, {}, {}, {})
        AlarmCard(previewAlarm, isNext = false, {}, {}, {}, {})
        AlarmCard(previewDisabledAlarm, isNext = false, {}, {}, {}, {})
    }
}

@Preview(name = "Cards dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
internal fun AlarmCardsPreviewDark() {
    BrutusTheme(darkTheme = true) { CardStack() }
}

@Preview(name = "Cards light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
internal fun AlarmCardsPreviewLight() {
    BrutusTheme(darkTheme = false) { CardStack() }
}

@Preview(
    name = "Cards dynamic",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    wallpaper = androidx.compose.ui.tooling.preview.Wallpapers.RED_DOMINATED_EXAMPLE,
)

@Composable
internal fun AlarmCardsPreviewDynamic() {
    BrutusTheme(darkTheme = true) { CardStack() }
}
