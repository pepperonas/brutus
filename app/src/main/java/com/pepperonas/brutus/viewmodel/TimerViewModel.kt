package com.pepperonas.brutus.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pepperonas.brutus.util.AlarmSound
import com.pepperonas.brutus.util.SoundPreviewPlayer
import com.pepperonas.brutus.util.TimerSoundStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class TimerState { IDLE, RUNNING, PAUSED, FINISHED }

/**
 * Holds the countdown state and the ticking loop OUTSIDE the composition, so a
 * running timer survives bottom-nav tab switches (the screen composable gets
 * disposed on every tab change) and still fires its finish sound while the user
 * is on another tab.
 */
class TimerViewModel(application: Application) : AndroidViewModel(application) {

    var hours by mutableIntStateOf(0)
    var minutes by mutableIntStateOf(5)
    var seconds by mutableIntStateOf(0)

    var state by mutableStateOf(TimerState.IDLE)
        private set
    var endAt by mutableLongStateOf(0L)      // elapsedRealtime end
        private set
    var remaining by mutableLongStateOf(0L)  // ms left (when paused / idle)
        private set
    var tick by mutableLongStateOf(0L)
        private set

    var selectedSound by mutableStateOf(TimerSoundStore.get(application))
        private set

    val player = SoundPreviewPlayer(application)
    private var tickerJob: Job? = null

    val liveRemaining: Long
        get() = when (state) {
            TimerState.RUNNING -> (endAt - tick).coerceAtLeast(0L)
            else -> remaining
        }

    fun selectSound(sound: AlarmSound) {
        player.stop()
        selectedSound = sound
        TimerSoundStore.set(getApplication(), sound)
        if (sound != AlarmSound.SILENT) player.play(sound)
    }

    fun start() {
        player.stop()
        val totalMs = (hours * 3600L + minutes * 60L + seconds) * 1000L
        if (totalMs <= 0L) return
        tick = SystemClock.elapsedRealtime()
        endAt = tick + totalMs
        remaining = totalMs
        state = TimerState.RUNNING
        startTicker()
    }

    fun pause() {
        remaining = (endAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        state = TimerState.PAUSED
        stopTicker()
    }

    fun resume() {
        tick = SystemClock.elapsedRealtime()
        endAt = tick + remaining
        state = TimerState.RUNNING
        startTicker()
    }

    // Snapshot of a mid-flight timer killed by cancel() — feeds the undo
    // snackbar. Stopping a FINISHED timer is not undoable (nothing left to
    // resume; resurrecting the alarm sound would be hostile).
    private var cancelSnapshot: Pair<Long, Boolean>? = null // remaining ms, wasRunning

    fun cancel() {
        cancelSnapshot = when (state) {
            TimerState.RUNNING ->
                (endAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L) to true
            TimerState.PAUSED -> remaining to false
            else -> null
        }
        player.stop()
        stopTicker()
        state = TimerState.IDLE
        remaining = 0L
    }

    /** True right after cancel() aborted a running/paused countdown. */
    val lastCancelUndoable: Boolean get() = cancelSnapshot != null

    /** Restores the countdown aborted by the last cancel(), resuming if it ran. */
    fun undoCancel() {
        val (rem, wasRunning) = cancelSnapshot ?: return
        cancelSnapshot = null
        if (rem <= 0L) return
        remaining = rem
        if (wasRunning) {
            resume()
        } else {
            state = TimerState.PAUSED
        }
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = viewModelScope.launch {
            while (state == TimerState.RUNNING) {
                tick = SystemClock.elapsedRealtime()
                if (tick >= endAt) {
                    state = TimerState.FINISHED
                    remaining = 0L
                    player.play(selectedSound)
                    break
                }
                delay(100L)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    override fun onCleared() {
        player.stop()
        super.onCleared()
    }
}
