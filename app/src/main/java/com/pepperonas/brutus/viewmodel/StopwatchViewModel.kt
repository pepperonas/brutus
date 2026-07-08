package com.pepperonas.brutus.viewmodel

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Holds the stopwatch state and ticking loop outside the composition so an
 * active measurement (including laps) survives bottom-nav tab switches.
 * Time is wall-clock based (elapsedRealtime), so it stays correct even while
 * the ticker is throttled or the screen is elsewhere.
 */
class StopwatchViewModel : ViewModel() {

    // When `startAt` > 0 the stopwatch is running. `accumulated` holds time
    // from all prior run segments.
    var startAt by mutableLongStateOf(0L)
        private set
    var accumulated by mutableLongStateOf(0L)
        private set
    var tick by mutableLongStateOf(0L) // forces recomposition
        private set
    val laps = mutableStateListOf<Long>()

    private var tickerJob: Job? = null

    val running: Boolean get() = startAt != 0L

    /** Reads [tick] so composables observing `elapsed` recompose while running. */
    val elapsed: Long
        get() = if (running) accumulated + (tick - startAt).coerceAtLeast(0L) else accumulated

    fun startStop() {
        if (running) {
            accumulated += SystemClock.elapsedRealtime() - startAt
            startAt = 0L
            stopTicker()
        } else {
            startAt = SystemClock.elapsedRealtime()
            tick = startAt
            startTicker()
        }
    }

    // Snapshot of the measurement discarded by the last reset — feeds the
    // undo snackbar on the screen.
    private var resetSnapshot: Pair<Long, List<Long>>? = null

    fun lapOrReset() {
        if (running) {
            laps.add(0, elapsed)
        } else {
            if (accumulated > 0L || laps.isNotEmpty()) {
                resetSnapshot = accumulated to laps.toList()
            }
            accumulated = 0L
            laps.clear()
        }
    }

    /** Restores the measurement (elapsed time + laps) discarded by the last reset. */
    fun undoReset() {
        val (acc, savedLaps) = resetSnapshot ?: return
        resetSnapshot = null
        accumulated = acc
        laps.clear()
        laps.addAll(savedLaps)
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = viewModelScope.launch {
            while (running) {
                tick = SystemClock.elapsedRealtime()
                delay(25L)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }
}
