package com.pepperonas.brutus.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.SystemClock
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pepperonas.brutus.ui.theme.BrutusRed
import com.pepperonas.brutus.ui.theme.BrutusRedBright
import com.pepperonas.brutus.ui.theme.BrutusTextSecondary
import kotlinx.coroutines.delay

private enum class TimerState { IDLE, RUNNING, PAUSED, FINISHED }

@Composable
fun TimerScreen() {
    val context = LocalContext.current

    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(5) }
    var seconds by remember { mutableIntStateOf(0) }

    var state by remember { mutableStateOf(TimerState.IDLE) }
    var endAt by remember { mutableLongStateOf(0L) }      // elapsedRealtime end
    var remaining by remember { mutableLongStateOf(0L) }  // ms left (when paused / idle)
    var tick by remember { mutableLongStateOf(0L) }

    val liveRemaining = when (state) {
        TimerState.RUNNING -> (endAt - tick).coerceAtLeast(0L)
        else -> remaining
    }

    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            player?.runCatching { if (isPlaying) stop() }
            player?.release()
            player = null
        }
    }

    LaunchedEffect(state) {
        while (state == TimerState.RUNNING) {
            tick = SystemClock.elapsedRealtime()
            if (tick >= endAt) {
                state = TimerState.FINISHED
                remaining = 0L
                // Play system alarm until stopped
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(context, uri)
                    isLooping = true
                    prepare()
                    start()
                }
                break
            }
            delay(100L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Timer",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (state == TimerState.IDLE) {
            TimerConfigurator(
                hours = hours, minutes = minutes, seconds = seconds,
                onHours = { hours = it },
                onMinutes = { minutes = it },
                onSeconds = { seconds = it },
            )
            Spacer(modifier = Modifier.height(40.dp))
            QuickPresets { total ->
                hours = (total / 3600)
                minutes = ((total / 60) % 60)
                seconds = (total % 60)
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val totalMs = (hours * 3600L + minutes * 60L + seconds) * 1000L
                    if (totalMs > 0L) {
                        endAt = SystemClock.elapsedRealtime() + totalMs
                        remaining = totalMs
                        state = TimerState.RUNNING
                    }
                },
                enabled = (hours + minutes + seconds) > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrutusRed),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Start", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        } else {
            Box(
                modifier = Modifier
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatCountdown(liveRemaining),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light,
                    color = if (state == TimerState.FINISHED) BrutusRedBright
                    else MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                CircleActionButton(
                    label = "Abbruch",
                    container = MaterialTheme.colorScheme.surfaceVariant,
                    content = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = {
                        player?.runCatching { if (isPlaying) stop() }
                        player?.release()
                        player = null
                        state = TimerState.IDLE
                        remaining = 0L
                    }
                )
                when (state) {
                    TimerState.RUNNING -> CircleActionButton(
                        label = "Pause",
                        container = BrutusRed,
                        content = Color.White,
                        onClick = {
                            remaining = (endAt - SystemClock.elapsedRealtime())
                                .coerceAtLeast(0L)
                            state = TimerState.PAUSED
                        }
                    )
                    TimerState.PAUSED -> CircleActionButton(
                        label = "Weiter",
                        container = BrutusRed,
                        content = Color.White,
                        onClick = {
                            endAt = SystemClock.elapsedRealtime() + remaining
                            state = TimerState.RUNNING
                        }
                    )
                    TimerState.FINISHED -> CircleActionButton(
                        label = "Stopp",
                        container = BrutusRed,
                        content = Color.White,
                        onClick = {
                            player?.runCatching { if (isPlaying) stop() }
                            player?.release()
                            player = null
                            state = TimerState.IDLE
                            remaining = 0L
                        }
                    )
                    else -> {}
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
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
            Icon(Icons.Default.Add, contentDescription = "Mehr", tint = BrutusRedBright)
        }
        Box(
            modifier = Modifier.size(width = 88.dp, height = 72.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "%02d".format(value),
                fontSize = 52.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        IconButton(onClick = { if (value - 1 >= min) onChange(value - 1) }) {
            Icon(Icons.Default.Remove, contentDescription = "Weniger", tint = BrutusRedBright)
        }
        Text(label, color = BrutusTextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun QuickPresets(onPick: (Int) -> Unit) {
    val presets = listOf(60, 180, 300, 600, 900, 1800)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { s ->
            Button(
                onClick = { onPick(s) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = BrutusRedBright
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text(labelForPreset(s), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun labelForPreset(seconds: Int): String = when {
    seconds % 60 != 0 -> "${seconds}s"
    seconds < 60 -> "${seconds}s"
    else -> "${seconds / 60}m"
}

@Composable
private fun CircleActionButton(
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(96.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content)
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
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
