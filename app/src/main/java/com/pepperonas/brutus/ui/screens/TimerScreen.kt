package com.pepperonas.brutus.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pepperonas.brutus.ui.theme.BrutusTheme
import com.pepperonas.brutus.util.AlarmSound
import com.pepperonas.brutus.viewmodel.TimerState
import com.pepperonas.brutus.viewmodel.TimerViewModel

@Composable
fun TimerScreen(viewModel: TimerViewModel = viewModel()) {
    // All countdown state (incl. the ticking loop and the finish sound) lives in
    // the ViewModel so a running timer survives bottom-nav tab switches.
    val state = viewModel.state
    val liveRemaining = viewModel.liveRemaining
    val selectedSound = viewModel.selectedSound

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Timer",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (state == TimerState.IDLE) {
            TimerConfigurator(
                hours = viewModel.hours, minutes = viewModel.minutes, seconds = viewModel.seconds,
                onHours = { viewModel.hours = it },
                onMinutes = { viewModel.minutes = it },
                onSeconds = { viewModel.seconds = it },
            )
            Spacer(modifier = Modifier.height(32.dp))
            QuickPresets { total ->
                viewModel.hours = (total / 3600)
                viewModel.minutes = ((total / 60) % 60)
                viewModel.seconds = (total % 60)
            }
            Spacer(modifier = Modifier.height(24.dp))
            TimerSoundPicker(
                selected = selectedSound,
                onSelect = { snd -> viewModel.selectSound(snd) },
                onStopPreview = { viewModel.player.stop() }
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { viewModel.start() },
                enabled = (viewModel.hours + viewModel.minutes + viewModel.seconds) > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Text("Start", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        } else {
            val totalMs = (viewModel.hours * 3600L + viewModel.minutes * 60L + viewModel.seconds) * 1000L
            Spacer(modifier = Modifier.height(16.dp))
            TimerRing(
                remainingMs = liveRemaining,
                totalMs = totalMs,
                finished = state == TimerState.FINISHED,
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalButton(
                    onClick = { viewModel.cancel() },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                ) {
                    Text("Abbruch", fontSize = 16.sp)
                }
                val (mainLabel, mainAction) = when (state) {
                    TimerState.RUNNING -> "Pause" to { viewModel.pause() }
                    TimerState.PAUSED -> "Weiter" to { viewModel.resume() }
                    else -> "Stopp" to { viewModel.cancel() }
                }
                Button(
                    onClick = mainAction,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                ) {
                    Text(mainLabel, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Live countdown inside a wavy ring: the wave rolls while time drains, the
 * remaining fraction empties the ring, and the whole thing flips to error
 * tones the moment the timer fires.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimerRing(remainingMs: Long, totalMs: Long, finished: Boolean) {
    val fraction = if (totalMs > 0) (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f
    val ringColor by animateColorAsState(
        targetValue = if (finished) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary,
        label = "timerRingColor"
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
        CircularWavyProgressIndicator(
            progress = { fraction },
            modifier = Modifier.size(280.dp),
            color = ringColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
        Text(
            text = formatCountdown(remainingMs),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp),
            color = if (finished) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimerSoundPicker(
    selected: AlarmSound,
    onSelect: (AlarmSound) -> Unit,
    onStopPreview: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Endton",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AlarmSound.gentleSounds().forEach { snd ->
                FilterChip(
                    selected = selected == snd,
                    onClick = { onSelect(snd) },
                    label = { Text(snd.displayName) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selected.description + " — Tippe zum Vorhören",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onStopPreview) {
                Text("Stopp", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun TimerConfigurator(
    hours: Int, minutes: Int, seconds: Int,
    onHours: (Int) -> Unit,
    onMinutes: (Int) -> Unit,
    onSeconds: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        TimeUnitStepper("Std.", hours, 0, 23) { onHours(it) }
        TimeUnitStepper("Min.", minutes, 0, 59) { onMinutes(it) }
        TimeUnitStepper("Sek.", seconds, 0, 59) { onSeconds(it) }
    }
}

@Composable
private fun TimeUnitStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { if (value + 1 <= max) onChange(value + 1) }) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Mehr",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Box(
            modifier = Modifier.size(width = 88.dp, height = 72.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "%02d".format(value),
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 52.sp),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        IconButton(onClick = { if (value - 1 >= min) onChange(value - 1) }) {
            Icon(
                Icons.Default.Remove,
                contentDescription = "Weniger",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickPresets(onPick: (Int) -> Unit) {
    val presets = listOf(60, 180, 300, 600, 900, 1800)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        presets.forEach { s ->
            AssistChip(
                onClick = { onPick(s) },
                label = { Text(labelForPreset(s)) },
            )
        }
    }
}

private fun labelForPreset(seconds: Int): String = when {
    seconds % 60 != 0 -> "${seconds}s"
    seconds < 60 -> "${seconds}s"
    else -> "${seconds / 60}m"
}

private fun formatCountdown(ms: Long): String {
    val total = ms.coerceAtLeast(0)
    val hours = total / 3_600_000L
    val minutes = (total / 60_000L) % 60
    val seconds = (total / 1000L) % 60
    return if (hours > 0)
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    else
        "%02d:%02d".format(minutes, seconds)
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Ring dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TimerRingPreviewDark() {
    BrutusTheme(darkTheme = true) {
        TimerRing(remainingMs = 154_000L, totalMs = 300_000L, finished = false)
    }
}

@Preview(name = "Ring light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun TimerRingPreviewLight() {
    BrutusTheme(darkTheme = false) {
        TimerRing(remainingMs = 154_000L, totalMs = 300_000L, finished = false)
    }
}

@Preview(
    name = "Ring dynamic",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    wallpaper = androidx.compose.ui.tooling.preview.Wallpapers.RED_DOMINATED_EXAMPLE,
)
@Composable
private fun TimerRingPreviewDynamic() {
    BrutusTheme(darkTheme = true) {
        TimerRing(remainingMs = 30_000L, totalMs = 300_000L, finished = false)
    }
}
