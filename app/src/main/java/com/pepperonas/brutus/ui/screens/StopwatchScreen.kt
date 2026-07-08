package com.pepperonas.brutus.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pepperonas.brutus.ui.theme.BrutusTheme
import com.pepperonas.brutus.viewmodel.StopwatchViewModel
import kotlinx.coroutines.launch

@Composable
fun StopwatchScreen(viewModel: StopwatchViewModel = viewModel()) {
    // State + ticker live in the ViewModel so a running measurement (incl. laps)
    // survives bottom-nav tab switches.
    val running = viewModel.running
    val laps = viewModel.laps
    val elapsed = viewModel.elapsed

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Reset discards the measurement — undoable via snackbar (restores
    // elapsed time and all laps from the ViewModel snapshot).
    val lapOrResetWithUndo: () -> Unit = {
        val wasReset = !viewModel.running &&
            (viewModel.elapsed > 0L || viewModel.laps.isNotEmpty())
        viewModel.lapOrReset()
        if (wasReset) {
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = "Stoppuhr zurückgesetzt",
                    actionLabel = "Rückgängig",
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.undoReset()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Stoppuhr",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            // Space Grotesk + tabular numerals: the centiseconds tick without
            // the whole readout wobbling.
            text = formatStopwatch(elapsed),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 60.sp),
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Left: Reset / Lap
            FilledTonalButton(
                onClick = lapOrResetWithUndo,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
            ) {
                Text(if (running) "Runde" else "Reset", fontSize = 16.sp)
            }
            // Right: Start / Stop
            Button(
                onClick = { viewModel.startStop() },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
            ) {
                Text(if (running) "Stopp" else "Start", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (laps.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(laps, key = { index, _ -> laps.size - index }) { index, time ->
                    val number = laps.size - index
                    val prev = if (index + 1 < laps.size) laps[index + 1] else 0L
                    LapRow(
                        number = number,
                        diff = time - prev,
                        total = time,
                        modifier = Modifier.animateItem(),
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
}

/** Tonal lap entry — newest lap springs in at the top via animateItem(). */
@Composable
private fun LapRow(number: Int, diff: Long, total: Long, modifier: Modifier = Modifier) {
    // Tabular numerals come with every scale style now (Type.kt).
    val numStyle = MaterialTheme.typography.bodyLarge
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Runde $number",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "+" + formatStopwatch(diff),
            style = numStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(
            text = formatStopwatch(total),
            style = numStyle,
            color = MaterialTheme.colorScheme.onSurface,
        )
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

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Composable
private fun LapStack() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LapRow(number = 3, diff = 31_450L, total = 95_780L)
        LapRow(number = 2, diff = 30_120L, total = 64_330L)
        LapRow(number = 1, diff = 34_210L, total = 34_210L)
    }
}

@Preview(name = "Laps dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun LapsPreviewDark() {
    BrutusTheme(darkTheme = true) { LapStack() }
}

@Preview(name = "Laps light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun LapsPreviewLight() {
    BrutusTheme(darkTheme = false) { LapStack() }
}

@Preview(
    name = "Laps dynamic",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    wallpaper = androidx.compose.ui.tooling.preview.Wallpapers.RED_DOMINATED_EXAMPLE,
)
@Composable
private fun LapsPreviewDynamic() {
    BrutusTheme(darkTheme = true) { LapStack() }
}
