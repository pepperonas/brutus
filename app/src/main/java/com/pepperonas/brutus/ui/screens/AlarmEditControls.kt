@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.pepperonas.brutus.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pepperonas.brutus.ui.theme.BrutusTheme
import com.pepperonas.brutus.util.ChallengeDifficulty
import com.pepperonas.brutus.util.ChallengeFlags
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.pepperonas.brutus.R

// Building blocks of the alarm editor: mode toggles, difficulty chips, steppers, challenge chips.

/**
 * Mode toggle as a tonal "danger level" card: while OFF it rests quietly on a
 * neutral container; switching it ON tints the whole row — error tones for the
 * hardcore modes, tertiary (warm sunrise orange) for the gentle pre-alarm.
 * Deliberate but calm; no screaming red headline while everything is off.
 */
@Composable
internal fun ModeToggleRow(
    title: String,
    danger: Boolean,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val container by animateColorAsState(
        targetValue = when {
            !checked -> cs.surfaceContainerHigh
            danger -> cs.errorContainer
            else -> cs.tertiaryContainer
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "modeContainer"
    )
    val titleColor = when {
        !checked -> cs.onSurface
        danger -> cs.onErrorContainer
        else -> cs.onTertiaryContainer
    }
    val bodyColor = when {
        !checked -> cs.onSurfaceVariant
        danger -> cs.onErrorContainer.copy(alpha = 0.8f)
        else -> cs.onTertiaryContainer.copy(alpha = 0.8f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(container)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = titleColor)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = bodyColor
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

/** Preset picker as a segmented button row — one visible slot per level. */
@Composable
internal fun DifficultyChips(
    title: String,
    options: List<Int>,
    selected: Int,
    /** Maps a preset level to its label string resource. */
    label: (Int) -> Int,
    /** Description string resource of the selected level. */
    @StringRes description: Int,
    onSelect: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, opt ->
                SegmentedButton(
                    selected = selected == opt,
                    onClick = { onSelect(opt) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size)
                ) {
                    Text(stringResource(label(opt)))
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun CountStepper(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    min: Int,
    max: Int,
    step: Int,
    suffix: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = { if (value - step >= min) onChange(value - step) },
            enabled = value - step >= min
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = stringResource(R.string.action_less),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        com.pepperonas.brutus.ui.theme.RollingNumber(value, modifier = Modifier.width(56.dp)) { v ->
            Text(
                text = if (suffix.isBlank()) "$v" else "$v $suffix",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        IconButton(
            onClick = { if (value + step <= max) onChange(value + step) },
            enabled = value + step <= max
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.action_more),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
internal fun ChallengeChip(
    label: String,
    icon: ImageVector,
    flags: Int,
    mask: Int,
    onToggle: () -> Unit,
) {
    FilterChip(
        selected = ChallengeFlags.has(flags, mask),
        onClick = onToggle,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            )
        },
    )
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Composable
internal fun EditControlsSpecimen() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChallengeChip(
                stringResource(R.string.challenge_math),
                Icons.Default.Calculate, ChallengeFlags.MATH, ChallengeFlags.MATH
            ) {}
            ChallengeChip(
                stringResource(R.string.challenge_shake),
                Icons.Default.Vibration, ChallengeFlags.MATH, ChallengeFlags.SHAKE
            ) {}
            ChallengeChip(
                stringResource(R.string.challenge_qr),
                Icons.Default.QrCodeScanner, ChallengeFlags.MATH, ChallengeFlags.QR
            ) {}
        }
        DifficultyChips(
            title = stringResource(R.string.edit_math_difficulty),
            options = listOf(
                ChallengeDifficulty.MATH_EASY,
                ChallengeDifficulty.MATH_HARD,
                ChallengeDifficulty.MATH_BRUTAL,
            ),
            selected = ChallengeDifficulty.MATH_HARD,
            label = { ChallengeDifficulty.mathLabel(it) },
            description = ChallengeDifficulty.mathDescription(ChallengeDifficulty.MATH_HARD),
            onSelect = {}
        )
        CountStepper(
            label = stringResource(R.string.edit_math_count),
            value = 3, onChange = {}, min = 1, max = 10, step = 1, suffix = ""
        )
        ModeToggleRow(
            title = stringResource(R.string.edit_sunrise_title), danger = false,
            description = stringResource(R.string.edit_sunrise_short_description),
            checked = true, onCheckedChange = {}
        )
        ModeToggleRow(
            title = stringResource(R.string.edit_ultra_hardcore_title), danger = true,
            description = stringResource(R.string.edit_ultra_hardcore_short_description),
            checked = true, onCheckedChange = {}
        )
        ModeToggleRow(
            title = stringResource(R.string.edit_hardcore_title), danger = true,
            description = stringResource(R.string.edit_hardcore_short_description),
            checked = false, onCheckedChange = {}
        )
    }
}

@Preview(name = "Edit controls dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
internal fun EditControlsPreviewDark() {
    BrutusTheme(darkTheme = true) { EditControlsSpecimen() }
}

@Preview(name = "Edit controls light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
internal fun EditControlsPreviewLight() {
    BrutusTheme(darkTheme = false) { EditControlsSpecimen() }
}

@Preview(
    name = "Edit controls dynamic",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    wallpaper = androidx.compose.ui.tooling.preview.Wallpapers.RED_DOMINATED_EXAMPLE,
)

@Composable
internal fun EditControlsPreviewDynamic() {
    BrutusTheme(darkTheme = true) { EditControlsSpecimen() }
}
