package com.pepperonas.brutus.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pepperonas.brutus.TestAlarmActivity
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.ui.theme.BrutusTheme
import com.pepperonas.brutus.util.AlarmSound
import com.pepperonas.brutus.util.ChallengeDifficulty
import com.pepperonas.brutus.util.ChallengeFlags
import com.pepperonas.brutus.util.GlobalQrStore
import com.pepperonas.brutus.util.QrGenerator
import com.pepperonas.brutus.util.rememberBrutusHaptics
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource
import com.pepperonas.brutus.R

data class AlarmEditResult(
    val hour: Int,
    val minute: Int,
    val label: String,
    val repeatDays: Int,
    val challengeFlags: Int,
    val snoozeDuration: Int,
    val soundId: Int,
    val mathProblemCount: Int,
    val shakeCount: Int,
    val hardcoreMode: Boolean,
    val ultraHardcoreMode: Boolean,
    val mathDifficulty: Int,
    val shakeSensitivity: Int,
    val sunriseEnabled: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditDialog(
    existingAlarm: AlarmEntity?,
    onDismiss: () -> Unit,
    onSave: (AlarmEditResult) -> Unit,
    onPreviewSound: (AlarmSound) -> Unit,
    onStopPreview: () -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = existingAlarm?.hour ?: 7,
        initialMinute = existingAlarm?.minute ?: 0,
        is24Hour = true
    )
    var label by remember { mutableStateOf(existingAlarm?.label ?: "") }
    var repeatDays by remember { mutableIntStateOf(existingAlarm?.repeatDays ?: 0) }
    var challengeFlags by remember {
        mutableIntStateOf(existingAlarm?.challengeFlags ?: ChallengeFlags.MATH)
    }
    var snoozeDuration by remember { mutableIntStateOf(existingAlarm?.snoozeDuration ?: 5) }
    var soundId by remember { mutableIntStateOf(existingAlarm?.soundId ?: AlarmSound.KLAXON.id) }
    var mathProblemCount by remember { mutableIntStateOf(existingAlarm?.mathProblemCount ?: 3) }
    var shakeCount by remember { mutableIntStateOf(existingAlarm?.shakeCount ?: 30) }
    var hardcoreMode by remember { mutableStateOf(existingAlarm?.hardcoreMode ?: false) }
    var ultraHardcoreMode by remember { mutableStateOf(existingAlarm?.ultraHardcoreMode ?: false) }
    var mathDifficulty by remember {
        mutableIntStateOf(existingAlarm?.mathDifficulty ?: ChallengeDifficulty.MATH_HARD)
    }
    var shakeSensitivity by remember {
        mutableIntStateOf(existingAlarm?.shakeSensitivity ?: ChallengeDifficulty.SHAKE_NORMAL)
    }
    var sunriseEnabled by remember { mutableStateOf(existingAlarm?.sunriseEnabled ?: false) }

    val ctxForQr = LocalContext.current
    val qrCodeData = remember { GlobalQrStore.get(ctxForQr) }
    val qrBitmap = remember(qrCodeData) { QrGenerator.generateBitmap(qrCodeData) }

    val days = stringArrayResource(R.array.weekday_short)
    val snoozeOptions = listOf(0, 2, 5, 10, 15)
    val qrEnabled = ChallengeFlags.has(challengeFlags, ChallengeFlags.QR)
    val mathEnabled = ChallengeFlags.has(challengeFlags, ChallengeFlags.MATH)
    val shakeEnabled = ChallengeFlags.has(challengeFlags, ChallengeFlags.SHAKE)

    val ctx = LocalContext.current
    val haptics = rememberBrutusHaptics()

    val activityRecognitionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* runtime answer handled when the task screen opens; we only ask up-front to smooth the path */ }

    val shareQr: () -> Unit = {
        if (!QrGenerator.shareQr(ctx, qrCodeData)) {
            Toast.makeText(ctx, R.string.toast_share_failed, Toast.LENGTH_SHORT).show()
        }
    }
    val launchTest: () -> Unit = {
        onStopPreview()
        val flags = if (challengeFlags == 0) ChallengeFlags.MATH else challengeFlags
        val i = Intent(ctx, TestAlarmActivity::class.java).apply {
            putExtra(TestAlarmActivity.EXTRA_CHALLENGE_FLAGS, flags)
            putExtra(TestAlarmActivity.EXTRA_QR_DATA, qrCodeData)
            putExtra(TestAlarmActivity.EXTRA_SOUND_ID, soundId)
            putExtra(TestAlarmActivity.EXTRA_MATH_COUNT, mathProblemCount)
            putExtra(TestAlarmActivity.EXTRA_SHAKE_COUNT, shakeCount)
            putExtra(TestAlarmActivity.EXTRA_SNOOZE_ENABLED, snoozeDuration > 0)
            putExtra(TestAlarmActivity.EXTRA_HARDCORE, hardcoreMode)
            putExtra(TestAlarmActivity.EXTRA_ULTRA_HARDCORE, ultraHardcoreMode)
            putExtra(TestAlarmActivity.EXTRA_MATH_DIFFICULTY, mathDifficulty)
            putExtra(TestAlarmActivity.EXTRA_SHAKE_SENSITIVITY, shakeSensitivity)
        }
        ctx.startActivity(i)
    }
    val saveQr: () -> Unit = {
        val uri = QrGenerator.savePng(ctx, qrCodeData)
        if (uri != null) {
            Toast.makeText(ctx, R.string.toast_qr_saved, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(ctx, R.string.toast_save_failed, Toast.LENGTH_SHORT).show()
        }
    }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) saveQr() else
        Toast.makeText(ctx, R.string.toast_storage_permission, Toast.LENGTH_SHORT).show()
    }
    val onSaveQrClick: () -> Unit = {
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            saveQr()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            onStopPreview()
            onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
        // Scrollable content; the save CTA below stays pinned so the primary
        // action never requires scrolling through the whole sheet.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 12.dp)
        ) {
            Text(
                // id == 0 with a non-null alarm means "prefilled copy template".
                text = stringResource(
                    when {
                        existingAlarm == null -> R.string.edit_title_new
                        existingAlarm.id == 0L -> R.string.edit_title_copy
                        else -> R.string.edit_title_edit
                    }
                ),
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            TimePicker(
                state = timePickerState,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(R.string.edit_label)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Repeat days — equal-weight pills so all seven always fit one row,
            // even on ~360 dp screens (fixed 44 dp circles used to wrap "So").
            Text(stringResource(R.string.edit_repeat), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                days.forEachIndexed { index, day ->
                    val selected = (repeatDays and (1 shl index)) != 0
                    val bg by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        label = "dayPill"
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(bg)
                            .clickable { repeatDays = repeatDays xor (1 shl index) }
                    ) {
                        Text(
                            text = day,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sound picker
            Text(stringResource(R.string.edit_sound), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AlarmSound.entries.forEach { snd ->
                    val selected = soundId == snd.id
                    FilterChip(
                        selected = selected,
                        onClick = {
                            soundId = snd.id
                            onPreviewSound(snd)
                        },
                        label = { Text(stringResource(snd.labelRes)) },
                    )
                }
            }
            Text(
                text = stringResource(
                    R.string.edit_sound_hint,
                    stringResource(AlarmSound.fromId(soundId).descriptionRes)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            TextButton(
                onClick = { onStopPreview() },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.edit_stop_preview))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Challenges (multi-select)
            Text(stringResource(R.string.edit_modes), style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.edit_modes_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChallengeChip(stringResource(R.string.challenge_math), Icons.Default.Calculate, challengeFlags, ChallengeFlags.MATH) {
                    challengeFlags = challengeFlags xor ChallengeFlags.MATH
                }
                ChallengeChip(stringResource(R.string.challenge_shake), Icons.Default.Vibration, challengeFlags, ChallengeFlags.SHAKE) {
                    challengeFlags = challengeFlags xor ChallengeFlags.SHAKE
                }
                ChallengeChip(stringResource(R.string.challenge_qr), Icons.Default.QrCodeScanner, challengeFlags, ChallengeFlags.QR) {
                    challengeFlags = challengeFlags xor ChallengeFlags.QR
                }
            }
            // The "chain": combined challenges run strictly in this order.
            val chainCtx = LocalContext.current
            val chainNames = remember(challengeFlags, chainCtx) {
                ChallengeFlags.activeList(challengeFlags).map { flag ->
                    chainCtx.getString(ChallengeFlags.labelOf(flag))
                }
            }
            if (chainNames.size > 1) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        R.string.edit_chain_order,
                        chainNames.joinToString("  →  ")
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (mathEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                CountStepper(
                    label = stringResource(R.string.edit_math_count),
                    value = mathProblemCount,
                    onChange = { mathProblemCount = it },
                    min = 1,
                    max = 10,
                    step = 1,
                    suffix = "",
                )
                Spacer(modifier = Modifier.height(8.dp))
                DifficultyChips(
                    title = stringResource(R.string.edit_math_difficulty),
                    options = listOf(
                        ChallengeDifficulty.MATH_EASY,
                        ChallengeDifficulty.MATH_HARD,
                        ChallengeDifficulty.MATH_BRUTAL,
                    ),
                    selected = mathDifficulty,
                    label = { ChallengeDifficulty.mathLabel(it) },
                    description = ChallengeDifficulty.mathDescription(mathDifficulty),
                    onSelect = { mathDifficulty = it }
                )
            }

            if (shakeEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                CountStepper(
                    label = stringResource(R.string.edit_shake_count),
                    value = shakeCount,
                    onChange = { shakeCount = it },
                    min = 10,
                    max = 100,
                    step = 5,
                    suffix = "",
                )
                Spacer(modifier = Modifier.height(8.dp))
                DifficultyChips(
                    title = stringResource(R.string.edit_shake_sensitivity),
                    options = listOf(
                        ChallengeDifficulty.SHAKE_LIGHT,
                        ChallengeDifficulty.SHAKE_NORMAL,
                        ChallengeDifficulty.SHAKE_HARD,
                    ),
                    selected = shakeSensitivity,
                    label = { ChallengeDifficulty.shakeLabel(it) },
                    description = ChallengeDifficulty.shakeDescription(shakeSensitivity),
                    onSelect = { shakeSensitivity = it }
                )
            }

            if (qrEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.edit_qr_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.edit_qr_content_description),
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.edit_qr_id, qrCodeData.takeLast(8)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(onClick = onSaveQrClick) { Text(stringResource(R.string.edit_qr_save)) }
                    FilledTonalButton(onClick = shareQr) { Text(stringResource(R.string.action_share)) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(R.string.edit_snooze), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                snoozeOptions.forEachIndexed { index, minutes ->
                    SegmentedButton(
                        selected = snoozeDuration == minutes,
                        onClick = { snoozeDuration = minutes },
                        shape = SegmentedButtonDefaults.itemShape(index, snoozeOptions.size)
                    ) {
                        Text(
                            if (minutes == 0) stringResource(R.string.edit_snooze_off)
                            else stringResource(R.string.edit_snooze_minutes, minutes)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sunrise pre-alarm toggle
            ModeToggleRow(
                title = stringResource(R.string.edit_sunrise_title),
                danger = false,
                description = stringResource(R.string.edit_sunrise_description),
                checked = sunriseEnabled,
                onCheckedChange = {
                    haptics.tap()
                    sunriseEnabled = it
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Hardcore Mode toggle
            ModeToggleRow(
                title = stringResource(R.string.edit_hardcore_title),
                danger = true,
                description = stringResource(R.string.edit_hardcore_description),
                checked = hardcoreMode || ultraHardcoreMode,
                enabled = !ultraHardcoreMode, // Ultra forces this on
                onCheckedChange = {
                    haptics.tap()
                    hardcoreMode = it
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Ultra Hardcore Mode toggle
            ModeToggleRow(
                title = stringResource(R.string.edit_ultra_hardcore_title),
                danger = true,
                description = stringResource(R.string.edit_ultra_hardcore_description),
                checked = ultraHardcoreMode,
                onCheckedChange = {
                    haptics.warn()
                    ultraHardcoreMode = it
                    if (it) {
                        hardcoreMode = true
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                            ContextCompat.checkSelfPermission(
                                ctx, Manifest.permission.ACTIVITY_RECOGNITION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            FilledTonalButton(
                onClick = launchTest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(stringResource(R.string.edit_test_modes))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Pinned save CTA (outside the scroll container)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 32.dp)
        ) {
            Button(
                onClick = {
                    haptics.success()
                    onStopPreview()
                    onSave(
                        AlarmEditResult(
                            hour = timePickerState.hour,
                            minute = timePickerState.minute,
                            label = label,
                            repeatDays = repeatDays,
                            challengeFlags = challengeFlags,
                            snoozeDuration = snoozeDuration,
                            soundId = soundId,
                            mathProblemCount = mathProblemCount,
                            shakeCount = shakeCount,
                            hardcoreMode = hardcoreMode || ultraHardcoreMode,
                            ultraHardcoreMode = ultraHardcoreMode,
                            mathDifficulty = mathDifficulty,
                            shakeSensitivity = shakeSensitivity,
                            sunriseEnabled = sunriseEnabled,
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    text = stringResource(
                        if (existingAlarm != null && existingAlarm.id != 0L) R.string.action_save
                        else R.string.edit_create_alarm
                    ),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
        }
    }
}

/**
 * Mode toggle as a tonal "danger level" card: while OFF it rests quietly on a
 * neutral container; switching it ON tints the whole row — error tones for the
 * hardcore modes, tertiary (warm sunrise orange) for the gentle pre-alarm.
 * Deliberate but calm; no screaming red headline while everything is off.
 */
@Composable
private fun ModeToggleRow(
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
private fun DifficultyChips(
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
private fun CountStepper(
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
        Text(
            text = if (suffix.isBlank()) "$value" else "$value $suffix",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.width(56.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
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
private fun ChallengeChip(
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
private fun EditControlsSpecimen() {
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
private fun EditControlsPreviewDark() {
    BrutusTheme(darkTheme = true) { EditControlsSpecimen() }
}

@Preview(name = "Edit controls light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun EditControlsPreviewLight() {
    BrutusTheme(darkTheme = false) { EditControlsSpecimen() }
}

@Preview(
    name = "Edit controls dynamic",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    wallpaper = androidx.compose.ui.tooling.preview.Wallpapers.RED_DOMINATED_EXAMPLE,
)
@Composable
private fun EditControlsPreviewDynamic() {
    BrutusTheme(darkTheme = true) { EditControlsSpecimen() }
}
