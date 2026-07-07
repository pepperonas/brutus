package com.pepperonas.brutus.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.ui.theme.BrutusOrange
import com.pepperonas.brutus.ui.theme.BrutusRed
import com.pepperonas.brutus.ui.theme.BrutusRedBright
import com.pepperonas.brutus.ui.theme.BrutusTextSecondary
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
        NextAlarmHeader(alarms = alarms, now = nowMillis)

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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                editingAlarm = null
                showDialog = true
            }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Alarm hinzufügen",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Mehr",
                        tint = MaterialTheme.colorScheme.onBackground
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

        Spacer(modifier = Modifier.height(8.dp))

        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Keine Alarme",
                        style = MaterialTheme.typography.headlineMedium,
                        color = BrutusTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tippe + um einen Alarm zu erstellen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrutusTextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
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
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

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
                    Text("Löschen", color = BrutusRedBright, fontWeight = FontWeight.SemiBold)
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

@Composable
private fun NextAlarmHeader(alarms: List<AlarmEntity>, now: Long) {
    val next = remember(alarms, now) { NextAlarmCalculator.findNext(alarms, now) }
    val triggerMillis = remember(next, now) { next?.let { NextAlarmCalculator.nextTrigger(it, now) } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (triggerMillis != null) {
                Text(
                    text = NextAlarmCalculator.formatCountdown(now, triggerMillis),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatTriggerDate(triggerMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrutusTextSecondary
                )
            } else {
                Text(
                    text = "Kein Alarm aktiv",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BrutusTextSecondary
                )
            }
        }
    }
}

private fun formatTriggerDate(millis: Long): String {
    val fmt = SimpleDateFormat("EEE, d. MMM, HH:mm", Locale.GERMAN)
    return fmt.format(Date(millis))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlarmCard(
    alarm: AlarmEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onClick: () -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surfaceVariant,
        label = "cardColor"
    )
    val dim = !alarm.enabled

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(20.dp),
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
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Light,
                        color = if (alarm.enabled) MaterialTheme.colorScheme.onSurface
                        else BrutusTextSecondary
                    )
                    Text(
                        text = alarm.repeatDaysString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrutusTextSecondary
                    )
                    if (alarm.label.isNotBlank()) {
                        Text(
                            text = alarm.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (dim) 0.5f else 0.85f
                            )
                        )
                    }
                }
                IconButton(onClick = onCopy) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Kopieren",
                        tint = BrutusTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Löschen",
                        tint = BrutusTextSecondary
                    )
                }
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = BrutusRed,
                        checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            WeekdayStrip(repeatDays = alarm.repeatDays, enabled = alarm.enabled)
            Spacer(modifier = Modifier.height(14.dp))

            // Info chips: mode, sunrise, challenge, snooze, sound
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (alarm.ultraHardcoreMode) {
                    InfoChip("ULTRA HC", BrutusOrange, filled = true, dim = dim)
                } else if (alarm.hardcoreMode) {
                    InfoChip("HARDCORE", BrutusRed, filled = true, dim = dim)
                }
                if (alarm.sunriseEnabled) {
                    InfoChip("☀ Sunrise", BrutusOrange, dim = dim)
                }
                InfoChip(alarm.challengeName(), BrutusRedBright, dim = dim)
                InfoChip("Snooze ${alarm.snoozeDuration}m", BrutusTextSecondary, dim = dim)
                InfoChip("♪ ${alarm.soundName()}", BrutusTextSecondary, dim = dim)
            }
        }
    }
}

/** Full-width weekday strip — always a single row, each day gets an equal slice. */
@Composable
private fun WeekdayStrip(repeatDays: Int, enabled: Boolean) {
    val labels = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val isOn = (repeatDays and (1 shl index)) != 0
            val textColor = when {
                !enabled -> BrutusTextSecondary.copy(alpha = 0.35f)
                isOn -> androidx.compose.ui.graphics.Color.White
                else -> BrutusTextSecondary.copy(alpha = 0.6f)
            }
            val bg = when {
                isOn && enabled -> BrutusRed
                isOn -> BrutusRed.copy(alpha = 0.3f)
                else -> androidx.compose.ui.graphics.Color.Transparent
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
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
    color: androidx.compose.ui.graphics.Color,
    filled: Boolean = false,
    dim: Boolean = false,
) {
    val alpha = if (dim) 0.5f else 1f
    if (filled) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = alpha),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = alpha))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
    } else {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = color.copy(alpha = alpha),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.14f * alpha))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
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
        accent = BrutusOrange,
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

@Composable
private fun PermissionBanner(
    title: String,
    body: String,
    actionLabel: String,
    accent: androidx.compose.ui.graphics.Color = BrutusRedBright,
    onFix: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(16.dp),
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
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.SemiBold, color = accent)
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            TextButton(onClick = onFix) {
                Text(actionLabel, color = accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
