package com.pepperonas.brutus.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.ui.theme.BrutusRed
import com.pepperonas.brutus.ui.theme.BrutusRedBright
import com.pepperonas.brutus.ui.theme.BrutusTextSecondary
import com.pepperonas.brutus.util.ExactAlarmPermission
import com.pepperonas.brutus.util.NextAlarmCalculator
import com.pepperonas.brutus.util.SoundPreviewPlayer
import com.pepperonas.brutus.viewmodel.AlarmViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlarmListScreen(viewModel: AlarmViewModel) {
    val alarms by viewModel.alarms.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<AlarmEntity?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val previewPlayer = remember { SoundPreviewPlayer(context) }
    DisposableEffect(Unit) {
        onDispose { previewPlayer.stop() }
    }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(30_000L)
        }
    }

    // Re-check exact-alarm permission whenever the user returns from settings
    var exactGranted by remember { mutableStateOf(ExactAlarmPermission.isGranted(context)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                exactGranted = ExactAlarmPermission.isGranted(context)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

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
                        onClick = {
                            menuOpen = false
                            alarms.forEach { viewModel.deleteAlarm(it) }
                        }
                    )
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
                        onToggle = { viewModel.toggleAlarm(alarm) },
                        onDelete = { viewModel.deleteAlarm(alarm) },
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

    if (showDialog) {
        AlarmEditDialog(
            existingAlarm = editingAlarm,
            onDismiss = { showDialog = false },
            onPreviewSound = { snd -> previewPlayer.play(snd) },
            onStopPreview = { previewPlayer.stop() },
            onSave = { hour, minute, label, repeatDays, challengeFlags, snooze, soundId, math, shake, hardcore ->
                if (editingAlarm != null) {
                    viewModel.updateAlarm(
                        editingAlarm!!.copy(
                            hour = hour,
                            minute = minute,
                            label = label,
                            repeatDays = repeatDays,
                            challengeFlags = challengeFlags,
                            snoozeDuration = snooze,
                            soundId = soundId,
                            mathProblemCount = math,
                            shakeCount = shake,
                            hardcoreMode = hardcore,
                        )
                    )
                } else {
                    viewModel.addAlarm(
                        hour, minute, label, repeatDays, challengeFlags,
                        snooze, soundId, math, shake, hardcore
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

@Composable
private fun AlarmCard(
    alarm: AlarmEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surfaceVariant,
        label = "cardColor"
    )

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alarm.timeString(),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Light,
                    color = if (alarm.enabled) MaterialTheme.colorScheme.onSurface
                    else BrutusTextSecondary
                )
                if (alarm.label.isNotBlank()) {
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrutusTextSecondary
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeekdayStrip(repeatDays = alarm.repeatDays, enabled = alarm.enabled)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Löschen",
                            tint = BrutusTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.size(4.dp))
                    Switch(
                        checked = alarm.enabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = BrutusRed,
                            checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                        )
                    )
                }
                // Flags row (sound / challenges / hardcore / snooze)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (alarm.hardcoreMode) {
                        TagChip("HARDCORE", BrutusRed)
                    }
                    TagChip(alarm.soundName(), BrutusTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun WeekdayStrip(repeatDays: Int, enabled: Boolean) {
    val labels = listOf("M", "D", "M", "D", "F", "S", "S")
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        labels.forEachIndexed { index, label ->
            val isOn = (repeatDays and (1 shl index)) != 0
            val color = when {
                !enabled -> BrutusTextSecondary.copy(alpha = 0.35f)
                isOn -> BrutusRedBright
                else -> BrutusTextSecondary.copy(alpha = 0.5f)
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isOn) FontWeight.Bold else FontWeight.Normal,
                color = color
            )
        }
    }
}

@Composable
private fun TagChip(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
}

@Composable
private fun ExactAlarmBanner(onFix: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BrutusRed.copy(alpha = 0.18f)),
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
                tint = BrutusRedBright,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Exakte Alarme deaktiviert",
                    fontWeight = FontWeight.SemiBold,
                    color = BrutusRedBright,
                )
                Text(
                    text = "Brutus kann ohne diese Berechtigung nicht zur exakten Minute klingeln.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            TextButton(onClick = onFix) {
                Text("Aktivieren", color = BrutusRedBright, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
