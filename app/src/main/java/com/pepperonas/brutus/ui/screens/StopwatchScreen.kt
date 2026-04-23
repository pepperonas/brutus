package com.pepperonas.brutus.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pepperonas.brutus.ui.theme.BrutusRed
import com.pepperonas.brutus.ui.theme.BrutusTextSecondary
import kotlinx.coroutines.delay

@Composable
fun StopwatchScreen() {
    // Running state: when `startAt` > 0 the stopwatch is running.
    // Accumulated holds time from all prior run segments.
    var startAt by remember { mutableLongStateOf(0L) }
    var accumulated by remember { mutableLongStateOf(0L) }
    var tick by remember { mutableLongStateOf(0L) } // forces recomposition
    val laps = remember { mutableStateListOf<Long>() }

    val running = startAt != 0L

    LaunchedEffect(running) {
        while (running) {
            tick = SystemClock.elapsedRealtime()
            delay(25L)
        }
    }

    val elapsed = if (running) accumulated + (tick - startAt) else accumulated

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Stoppuhr",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = formatStopwatch(elapsed),
            fontSize = 64.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Reset / Lap
            CircleActionButton(
                label = if (running) "Runde" else "Reset",
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = {
                    if (running) {
                        laps.add(0, elapsed)
                    } else {
                        accumulated = 0L
                        laps.clear()
                    }
                }
            )
            // Right: Start / Stop
            CircleActionButton(
                label = if (running) "Stopp" else "Start",
                containerColor = BrutusRed,
                contentColor = androidx.compose.ui.graphics.Color.White,
                onClick = {
                    if (running) {
                        accumulated = elapsed
                        startAt = 0L
                    } else {
                        startAt = SystemClock.elapsedRealtime()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (laps.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                itemsIndexed(laps) { index, time ->
                    val number = laps.size - index
                    val prev = if (index + 1 < laps.size) laps[index + 1] else 0L
                    val diff = time - prev
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Runde $number",
                            color = BrutusTextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatStopwatch(diff),
                            color = BrutusTextSecondary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Text(
                            text = formatStopwatch(time),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CircleActionButton(
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor
            )
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}

private fun formatStopwatch(ms: Long): String {
    val totalMs = ms.coerceAtLeast(0)
    val hours = totalMs / 3_600_000L
    val minutes = (totalMs / 60_000L) % 60
    val seconds = (totalMs / 1000L) % 60
    val centis = (totalMs / 10L) % 100
    return if (hours > 0)
        "%02d:%02d:%02d.%02d".format(hours, minutes, seconds, centis)
    else
        "%02d:%02d.%02d".format(minutes, seconds, centis)
}
