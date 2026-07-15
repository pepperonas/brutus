package com.pepperonas.brutus.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure-JVM tests for the stopwatch state machine via the injected clock.
 * The Main dispatcher is swapped for a StandardTestDispatcher that is never
 * advanced — the ticker coroutine stays queued, so elapsed time comes purely
 * from the clock, deterministic and instant.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StopwatchViewModelTest {

    // Base offset: the ViewModel uses startAt != 0 as its "running" sentinel,
    // so the fake clock must never hand out 0 (elapsedRealtime never does).
    private val t0 = 1_000_000L
    private var nowMs = 0L
    private lateinit var vm: StopwatchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        nowMs = t0
        vm = StopwatchViewModel(now = { nowMs })
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startStop accumulates wall-clock time across run segments`() {
        vm.startStop()                    // start @ 0
        assertTrue(vm.running)
        nowMs = t0 + 5_000
        vm.startStop()                    // stop @ 5s
        assertFalse(vm.running)
        assertEquals(5_000, vm.elapsed)

        nowMs = t0 + 60_000
        vm.startStop()                    // start again @ 60s — the gap must not count
        nowMs = t0 + 63_000
        vm.startStop()                    // stop @ 63s
        assertEquals(8_000, vm.elapsed)
    }

    @Test
    fun `lap while running records elapsed newest-first`() {
        vm.startStop()                    // start @ 0
        nowMs = t0 + 3_000
        vm.startStop()                    // stop @ 3s → accumulated 3000
        nowMs = t0 + 10_000
        vm.startStop()                    // resume @ 10s
        vm.lapOrReset()                   // lap → 3000 (tick pinned to segment start)
        assertEquals(listOf(3_000L), vm.laps.toList())
        nowMs = t0 + 12_000
        vm.startStop()
        vm.startStop()                    // stop/start cycle to advance accumulated
        vm.lapOrReset()
        assertEquals(2, vm.laps.size)
        assertEquals(5_000L, vm.laps.first()) // newest lap first
    }

    @Test
    fun `reset while stopped clears measurement and undo restores time plus laps`() {
        vm.startStop()                    // start @ 0
        nowMs = t0 + 3_000
        vm.startStop()                    // accumulated 3000
        nowMs = t0 + 4_000
        vm.startStop()
        vm.lapOrReset()                   // lap 3000
        nowMs = t0 + 9_000
        vm.startStop()                    // accumulated 8000

        vm.lapOrReset()                   // reset
        assertEquals(0, vm.elapsed)
        assertTrue(vm.laps.isEmpty())

        vm.undoReset()
        assertEquals(8_000, vm.elapsed)
        assertEquals(listOf(3_000L), vm.laps.toList())
    }

    @Test
    fun `undoReset is single-shot — a consumed snapshot cannot clobber later state`() {
        vm.startStop()
        nowMs = t0 + 2_000
        vm.startStop()                    // accumulated 2000
        vm.lapOrReset()                   // reset
        vm.undoReset()
        assertEquals(2_000, vm.elapsed)

        nowMs = t0 + 3_000
        vm.startStop()
        nowMs = t0 + 4_000
        vm.startStop()                    // accumulated 3000
        vm.undoReset()                    // snapshot consumed → must NOT revert to 2000
        assertEquals(3_000, vm.elapsed)
    }

    @Test
    fun `reset of an empty stopwatch leaves nothing to undo`() {
        vm.lapOrReset()                   // stopped, elapsed 0, no laps
        vm.undoReset()
        assertEquals(0, vm.elapsed)
        assertTrue(vm.laps.isEmpty())
    }

    @Test
    fun `a second reset overwrites the snapshot with the newer measurement`() {
        vm.startStop(); nowMs = t0 + 5_000; vm.startStop()   // measurement A: 5000
        vm.lapOrReset()                                  // reset A
        nowMs = t0 + 10_000
        vm.startStop(); nowMs = t0 + 11_000; vm.startStop()  // measurement B: 1000
        vm.lapOrReset()                                  // reset B
        vm.undoReset()
        assertEquals(1_000, vm.elapsed)                  // restores B, not A
    }
}
