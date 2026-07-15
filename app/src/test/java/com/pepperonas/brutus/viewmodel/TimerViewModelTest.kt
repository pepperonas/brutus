package com.pepperonas.brutus.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pepperonas.brutus.util.AlarmSound
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Countdown + cancel/undo state machine, driven by the injected clock.
 * Robolectric supplies the Application/SharedPreferences the ViewModel needs;
 * the ticker coroutine stays queued on an unadvanced test dispatcher except
 * where a test explicitly runs it (finish detection).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TimerViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private var nowMs = 0L
    private lateinit var vm: TimerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        nowMs = 0L
        vm = TimerViewModel(ApplicationProvider.getApplicationContext<Application>()) { nowMs }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun startOneMinute() {
        vm.hours = 0; vm.minutes = 1; vm.seconds = 0
        vm.start()
    }

    @Test
    fun `start arms a running countdown with the configured duration`() {
        startOneMinute()
        assertEquals(TimerState.RUNNING, vm.state)
        assertEquals(60_000, vm.liveRemaining)
    }

    @Test
    fun `start with zero duration stays idle`() {
        vm.hours = 0; vm.minutes = 0; vm.seconds = 0
        vm.start()
        assertEquals(TimerState.IDLE, vm.state)
    }

    @Test
    fun `pause freezes the remaining time`() {
        startOneMinute()
        nowMs = 20_000
        vm.pause()
        assertEquals(TimerState.PAUSED, vm.state)
        assertEquals(40_000, vm.liveRemaining)
    }

    @Test
    fun `cancel while running is undoable and undo resumes with the leftover time`() {
        startOneMinute()
        nowMs = 10_000
        vm.cancel()
        assertEquals(TimerState.IDLE, vm.state)
        assertEquals(0, vm.liveRemaining)
        assertTrue(vm.lastCancelUndoable)

        nowMs = 15_000
        vm.undoCancel()
        assertEquals(TimerState.RUNNING, vm.state)
        assertEquals(50_000, vm.liveRemaining)   // 60s minus the 10s that had elapsed
    }

    @Test
    fun `cancel while paused undoes back into the paused state`() {
        startOneMinute()
        nowMs = 20_000
        vm.pause()                                // 40s left
        vm.cancel()
        assertTrue(vm.lastCancelUndoable)

        vm.undoCancel()
        assertEquals(TimerState.PAUSED, vm.state)
        assertEquals(40_000, vm.liveRemaining)
    }

    @Test
    fun `cancel from idle is not undoable`() {
        vm.cancel()
        assertFalse(vm.lastCancelUndoable)
        vm.undoCancel()
        assertEquals(TimerState.IDLE, vm.state)
    }

    @Test
    fun `undoCancel is single-shot — a consumed snapshot cannot revive a newer cancel`() {
        startOneMinute()
        nowMs = 10_000
        vm.cancel()
        vm.undoCancel()                           // running again, 50s left
        vm.undoCancel()                           // must be a no-op
        assertEquals(TimerState.RUNNING, vm.state)
        assertEquals(50_000, vm.liveRemaining)
    }

    @Test
    fun `stopping a finished timer is deliberately not undoable`() {
        vm.selectSound(AlarmSound.SILENT)         // keep the finish path free of audio
        startOneMinute()
        nowMs = 61_000                            // clock passes the end...
        dispatcher.scheduler.runCurrent()         // ...ticker observes it and finishes
        assertEquals(TimerState.FINISHED, vm.state)

        vm.cancel()                               // "Stop" on the ringing timer
        assertEquals(TimerState.IDLE, vm.state)
        assertFalse(vm.lastCancelUndoable)        // undo would resurrect the alarm
    }
}
