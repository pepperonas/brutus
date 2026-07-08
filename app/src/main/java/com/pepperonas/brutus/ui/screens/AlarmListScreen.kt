package com.pepperonas.brutus.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AlarmAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.ui.theme.BrutusTheme
import com.pepperonas.brutus.ui.theme.ThemeSettings
import com.pepperonas.brutus.util.BatteryOptimizationPermission
import com.pepperonas.brutus.util.ExactAlarmPermission
import com.pepperonas.brutus.util.FullScreenIntentPermission
import com.pepperonas.brutus.util.NextAlarmCalculator
import com.pepperonas.brutus.util.SoundPreviewPlayer
import com.pepperonas.brutus.util.rememberBrutusHaptics
import com.pepperonas.brutus.viewmodel.AlarmViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlarmListScreen(viewModel: AlarmViewModel) {
    val alarms by viewModel.alarms.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<AlarmEntity?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Deletes with an undo affordance: the removed alarms are held here until
    // the snackbar is dismissed or its "Rückgängig" action restores them.
    val deleteWithUndo: (List<AlarmEntity>) -> Unit = { toDelete ->
        if (toDelete.isNotEmpty()) {
            toDelete.forEach { viewModel.deleteAlarm(it) }
            scope.launch {
                // A newer delete replaces a still-visible undo snackbar.
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = if (toDelete.size == 1) "Alarm gelöscht"
                    else "${toDelete.size} Alarme gelöscht",
                    actionLabel = "Rückgängig",
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.restoreAlarms(toDelete)
                }
            }
        }
    }

    val context = LocalContext.current
    val previewPlayer = remember { SoundPreviewPlayer(context) }
    DisposableEffect(Unit) {
        onDispose { previewPlayer.stop() }
    }
    val haptics = rememberBrutusHaptics()

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(30_000L)
        }
    }
    val nextAlarm = remember(alarms, nowMillis) { NextAlarmCalculator.findNext(alarms, nowMillis) }

    // Re-check exact-alarm + battery + full-screen-intent permissions on resume
    var exactGranted by remember { mutableStateOf(ExactAlarmPermission.isGranted(context)) }
    var batteryIgnoring by remember { mutableStateOf(BatteryOptimizationPermission.isIgnoring(context)) }
    var fsiGranted by remember { mutableStateOf(FullScreenIntentPermission.isGranted(context)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                exactGranted = ExactAlarmPermission.isGranted(context)
                batteryIgnoring = BatteryOptimizationPermission.isIgnoring(context)
                fsiGranted = FullScreenIntentPermission.isGranted(context)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NextAlarmHeader(
                    next = nextAlarm,
                    now = nowMillis,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Mehr",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Alle löschen") },
                            enabled = alarms.isNotEmpty(),
                            onClick = {
                                menuOpen = false
                                confirmDeleteAll = true
                            }
                        )
                        // Material You opt-in (API 31+): wallpaper-based dynamic color
                        // instead of the red brand scheme.
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            val dynamicOn by ThemeSettings.dynamicColorFlow(context)
                                .collectAsState(initial = false)
                            DropdownMenuItem(
                                text = { Text("Material You Farben") },
                                trailingIcon = {
                                    Switch(
                                        checked = dynamicOn,
                                        onCheckedChange = null,
                                        modifier = Modifier.scale(0.8f)
                                    )
                                },
                                onClick = {
                                    haptics.tap()
                                    scope.launch {
                                        ThemeSettings.setDynamicColor(context, !dynamicOn)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (!exactGranted) {
                ExactAlarmBanner(onFix = {
                    ExactAlarmPermission.settingsIntent(context)?.let { context.startActivity(it) }
                })
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!batteryIgnoring) {
                BatteryOptimizationBanner(onFix = {
                    try {
                        context.startActivity(BatteryOptimizationPermission.settingsIntent(context))
                    } catch (_: Exception) {
                        context.startActivity(BatteryOptimizationPermission.fallbackSettingsIntent())
                    }
                })
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!fsiGranted) {
                FullScreenIntentBanner(onFix = {
                    FullScreenIntentPermission.settingsIntent(context)?.let { context.startActivity(it) }
                })
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (alarms.isEmpty()) {
                EmptyState(onCreate = {
                    editingAlarm = null
                    showDialog = true
                })
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        DismissableAlarmCard(
                            alarm = alarm,
                            isNext = nextAlarm?.id == alarm.id,
                            modifier = Modifier.animateItem(),
                            onToggle = {
                                haptics.tap()
                                viewModel.toggleAlarm(alarm)
                            },
                            onDelete = {
                                haptics.warn()
                                deleteWithUndo(listOf(alarm))
                            },
                            onCopy = {
                                haptics.tap()
                                // id = 0 marks the dialog payload as a copy template:
                                // everything prefilled, saving creates a NEW alarm.
                                editingAlarm = alarm.copy(id = 0)
                                showDialog = true
                            },
                            onClick = {
                                editingAlarm = alarm
                                showDialog = true
                            }
                        )
                    }
                    // Keep the FAB from covering the last card's actions.
                    item { Spacer(modifier = Modifier.height(96.dp)) }
                }
            }
        }

        AddAlarmFab(
            onClick = {
                haptics.tap()
                editingAlarm = null
                showDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Alle Alarme löschen?") },
            text = {
                Text(
                    if (alarms.size == 1) "1 Alarm wird gelöscht."
                    else "${alarms.size} Alarme werden gelöscht."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteAll = false
                    haptics.warn()
                    deleteWithUndo(alarms.toList())
                }) {
                    Text(
                        "Löschen",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (showDialog) {
        AlarmEditDialog(
            existingAlarm = editingAlarm,
            onDismiss = { showDialog = false },
            onPreviewSound = { snd -> previewPlayer.play(snd) },
            onStopPreview = { previewPlayer.stop() },
            onSave = { result ->
                // id == 0 means "copy template" — persist as a new alarm.
                if (editingAlarm != null && editingAlarm!!.id != 0L) {
                    viewModel.updateAlarm(
                        editingAlarm!!.copy(
                            hour = result.hour,
                            minute = result.minute,
                            label = result.label,
                            repeatDays = result.repeatDays,
                            challengeFlags = result.challengeFlags,
                            snoozeDuration = result.snoozeDuration,
                            soundId = result.soundId,
                            mathProblemCount = result.mathProblemCount,
                            shakeCount = result.shakeCount,
                            hardcoreMode = result.hardcoreMode,
                            ultraHardcoreMode = result.ultraHardcoreMode,
                            mathDifficulty = result.mathDifficulty,
                            shakeSensitivity = result.shakeSensitivity,
                            sunriseEnabled = result.sunriseEnabled,
                        )
                    )
                } else {
                    viewModel.addAlarm(
                        hour = result.hour,
                        minute = result.minute,
                        label = result.label,
                        repeatDays = result.repeatDays,
                        challengeFlags = result.challengeFlags,
                        snoozeDuration = result.snoozeDuration,
                        soundId = result.soundId,
                        mathProblemCount = result.mathProblemCount,
                        shakeCount = result.shakeCount,
                        hardcoreMode = result.hardcoreMode,
                        ultraHardcoreMode = result.ultraHardcoreMode,
                        mathDifficulty = result.mathDifficulty,
                        shakeSensitivity = result.shakeSensitivity,
                        sunriseEnabled = result.sunriseEnabled,
                    )
                }
                showDialog = false
            }
        )
    }
}

/** Brand FAB with a press shape-morph: pill relaxes toward a squircle under the finger. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddAlarmFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (pressed) 12.dp else 20.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "fabCorner"
    )
    FloatingActionButton(
        onClick = onClick,
        shape = RoundedCornerShape(corner),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        interactionSource = interaction,
        modifier = modifier,
    ) {
        Icon(Icons.Default.Add, contentDescription = "Alarm hinzufügen")
    }
}

@Composable
private fun NextAlarmHeader(next: AlarmEntity?, now: Long, modifier: Modifier = Modifier) {
    val triggerMillis = remember(next, now) { next?.let { NextAlarmCalculator.nextTrigger(it, now) } }

    Column(
        modifier = modifier.padding(top = 28.dp, bottom = 20.dp),
    ) {
        if (triggerMillis != null) {
            Text(
                text = "NÄCHSTER ALARM",
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = NextAlarmCalculator.formatCountdown(now, triggerMillis),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatTriggerDate(triggerMillis),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Kein Alarm aktiv",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTriggerDate(millis: Long): String {
    val fmt = SimpleDateFormat("EEE, d. MMM, HH:mm", Locale.GERMAN)
    return fmt.format(Date(millis))
}

/** Swipe-to-delete wrapper: drag the card off to the left, undo via snackbar. */
@Composable
private fun DismissableAlarmCard(
    alarm: AlarmEntity,
    isNext: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onClick: () -> Unit,
) {
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
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val revealed = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        if (revealed) MaterialTheme.colorScheme.errorContainer
                        else Color.Transparent
                    ),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (revealed) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(end = 28.dp)
                    )
                }
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
 * Tonal card hierarchy: the NEXT firing alarm sits on primaryContainer, other
 * enabled alarms on surfaceContainerHigh, disabled ones sink to
 * surfaceContainerLow with dimmed content.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlarmCard(
    alarm: AlarmEntity,
    isNext: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val dim = !alarm.enabled

    val containerColor by animateColorAsState(
        targetValue = when {
            isNext && alarm.enabled -> cs.primaryContainer
            alarm.enabled -> cs.surfaceContainerHigh
            else -> cs.surfaceContainerLow
        },
        label = "cardColor"
    )
    val timeColor = when {
        isNext && alarm.enabled -> cs.onPrimaryContainer
        alarm.enabled -> cs.onSurface
        else -> cs.onSurfaceVariant.copy(alpha = 0.6f)
    }
    val subColor = when {
        isNext && alarm.enabled -> cs.onPrimaryContainer.copy(alpha = 0.75f)
        alarm.enabled -> cs.onSurfaceVariant
        else -> cs.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.large,
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
                        text = alarm.repeatDaysString(),
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
                        contentDescription = "Kopieren",
                        tint = subColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Löschen",
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
                    InfoChip("ULTRA HC", cs.error, cs.onError, dim = dim)
                } else if (alarm.hardcoreMode) {
                    InfoChip("HARDCORE", cs.errorContainer, cs.onErrorContainer, dim = dim)
                }
                if (alarm.sunriseEnabled) {
                    InfoChip("☀ Sunrise", cs.tertiaryContainer, cs.onTertiaryContainer, dim = dim)
                }
                InfoChip(alarm.challengeName(), cs.secondaryContainer, cs.onSecondaryContainer, dim = dim)
                InfoChip("Snooze ${alarm.snoozeDuration}m", cs.surfaceContainerHighest, cs.onSurfaceVariant, dim = dim)
                InfoChip("♪ ${alarm.soundName()}", cs.surfaceContainerHighest, cs.onSurfaceVariant, dim = dim)
            }
        }
    }
}

/** Full-width weekday strip — always a single row, each day gets an equal slice. */
@Composable
private fun WeekdayStrip(
    repeatDays: Int,
    enabled: Boolean,
    inactiveColor: Color,
) {
    val cs = MaterialTheme.colorScheme
    val labels = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
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
private fun InfoChip(
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

@Composable
private fun EmptyState(onCreate: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.AlarmAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Noch kein Alarm",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Brutus weckt dich — garantiert.\nLeg deinen ersten Alarm an.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            FilledTonalButton(onClick = onCreate) {
                Text("Alarm erstellen")
            }
        }
    }
}

@Composable
private fun ExactAlarmBanner(onFix: () -> Unit) {
    PermissionBanner(
        title = "Exakte Alarme deaktiviert",
        body = "Brutus kann ohne diese Berechtigung nicht zur exakten Minute klingeln.",
        actionLabel = "Aktivieren",
        onFix = onFix,
    )
}

@Composable
private fun BatteryOptimizationBanner(onFix: () -> Unit) {
    PermissionBanner(
        title = "Akku-Optimierung aktiv",
        body = "Aggressive Akkusparmaßnahmen können Alarme verschlucken. Whiteliste Brutus, damit er garantiert klingelt.",
        actionLabel = "Whitelisten",
        warning = true,
        onFix = onFix,
    )
}

@Composable
private fun FullScreenIntentBanner(onFix: () -> Unit) {
    PermissionBanner(
        title = "Vollbild-Alarm blockiert",
        body = "Ohne diese Berechtigung erscheint der Alarm nur als Benachrichtigung — die App poppt nicht in den Vordergrund.",
        actionLabel = "Erlauben",
        onFix = onFix,
    )
}

/** Error container for hard blockers, tertiary (warm orange) for soft warnings. */
@Composable
private fun PermissionBanner(
    title: String,
    body: String,
    actionLabel: String,
    warning: Boolean = false,
    onFix: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val container = if (warning) cs.tertiaryContainer else cs.errorContainer
    val content = if (warning) cs.onTertiaryContainer else cs.onErrorContainer
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.SemiBold, color = content)
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = content.copy(alpha = 0.85f),
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            TextButton(onClick = onFix) {
                Text(actionLabel, color = content, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

private val previewNextAlarm = AlarmEntity(
    id = 1, hour = 6, minute = 30, label = "Frühschicht",
    repeatDays = 0b0011111, ultraHardcoreMode = true, sunriseEnabled = true,
)
private val previewAlarm = AlarmEntity(
    id = 2, hour = 9, minute = 15, repeatDays = 0b1100000, hardcoreMode = true,
)
private val previewDisabledAlarm = AlarmEntity(
    id = 3, hour = 14, minute = 0, label = "Powernap", enabled = false,
)

@Composable
private fun CardStack() {
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
private fun AlarmCardsPreviewDark() {
    BrutusTheme(darkTheme = true) { CardStack() }
}

@Preview(name = "Cards light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun AlarmCardsPreviewLight() {
    BrutusTheme(darkTheme = false) { CardStack() }
}

@Preview(
    name = "Cards dynamic",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    wallpaper = androidx.compose.ui.tooling.preview.Wallpapers.RED_DOMINATED_EXAMPLE,
)
@Composable
private fun AlarmCardsPreviewDynamic() {
    BrutusTheme(darkTheme = true) { CardStack() }
}

@Preview(name = "Empty dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun EmptyStatePreviewDark() {
    BrutusTheme(darkTheme = true) { EmptyState(onCreate = {}) }
}
