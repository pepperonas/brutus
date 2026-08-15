package com.pepperonas.brutus.ui.screens

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The stopwatch/timer readouts are the most-watched strings in the app — they
 * update ten times a second in front of a user who is staring at them. The
 * whole point of the tabular-numeral type scale (v2.1.0) is that the glyphs
 * never shift, which only holds if the *string width* is stable too.
 */
class ClockFormattingTest {

    // ---- stopwatch -------------------------------------------------------

    @Test
    fun `stopwatch starts at a zeroed readout`() {
        assertEquals("00:00.00", formatStopwatch(0))
    }

    @Test
    fun `stopwatch truncates to centiseconds instead of rounding up`() {
        // 999 ms must never read as 1.00 s — the readout would run ahead of the clock.
        assertEquals("00:00.99", formatStopwatch(999))
        assertEquals("00:01.00", formatStopwatch(1_000))
    }

    @Test
    fun `stopwatch composes minutes, seconds and centiseconds`() {
        assertEquals("01:05.43", formatStopwatch(65_432))
        assertEquals("59:59.99", formatStopwatch(3_599_999))
    }

    @Test
    fun `stopwatch grows an hour column exactly at the hour mark`() {
        assertEquals("59:59.99", formatStopwatch(3_599_990))
        assertEquals("01:00:00.00", formatStopwatch(3_600_000))
        assertEquals("10:00:00.00", formatStopwatch(36_000_000))
    }

    @Test
    fun `stopwatch clamps a negative elapsed time instead of printing a minus`() {
        assertEquals("00:00.00", formatStopwatch(-1))
        assertEquals("00:00.00", formatStopwatch(-3_600_000))
    }

    @Test
    fun `stopwatch readout keeps a constant width below one hour`() {
        val widths = listOf(0L, 7L, 999L, 60_000L, 599_999L, 3_599_999L)
            .map { formatStopwatch(it).length }
            .toSet()
        assertEquals(setOf(8), widths, "sub-hour readout must stay MM:SS.CC")
    }

    // ---- timer -----------------------------------------------------------

    @Test
    fun `timer counts down in whole seconds`() {
        assertEquals("00:00", formatCountdown(0))
        assertEquals("00:00", formatCountdown(999))
        assertEquals("00:01", formatCountdown(1_999))
        assertEquals("00:59", formatCountdown(59_000))
        assertEquals("01:00", formatCountdown(60_000))
    }

    @Test
    fun `timer grows an hour column exactly at the hour mark`() {
        assertEquals("59:59", formatCountdown(3_599_000))
        assertEquals("01:00:00", formatCountdown(3_600_000))
    }

    @Test
    fun `timer renders the longest configurable duration`() {
        // The HMS picker allows 23:59:59.
        assertEquals("23:59:59", formatCountdown(86_399_000))
    }

    @Test
    fun `timer clamps below zero — an expired timer reads as zero, not negative`() {
        assertEquals("00:00", formatCountdown(-1))
        assertEquals("00:00", formatCountdown(-60_000))
    }

    @Test
    fun `timer readout keeps a constant width below one hour`() {
        val widths = listOf(0L, 1_000L, 59_000L, 600_000L, 3_599_000L)
            .map { formatCountdown(it).length }
            .toSet()
        assertEquals(setOf(5), widths, "sub-hour countdown must stay MM:SS")
    }

    // ---- quick presets ---------------------------------------------------

    @Test
    fun `the quick-preset row renders as whole minutes`() {
        // These six are the presets wired into QuickPresets().
        assertEquals("1m", labelForPreset(60))
        assertEquals("3m", labelForPreset(180))
        assertEquals("5m", labelForPreset(300))
        assertEquals("10m", labelForPreset(600))
        assertEquals("15m", labelForPreset(900))
        assertEquals("30m", labelForPreset(1800))
    }

    @Test
    fun `durations that are not whole minutes stay in seconds`() {
        assertEquals("30s", labelForPreset(30))
        assertEquals("90s", labelForPreset(90))
        assertEquals("59s", labelForPreset(59))
    }

    @Test
    fun `preset labels are short enough for a chip`() {
        listOf(60, 180, 300, 600, 900, 1800, 3600, 5400).forEach {
            assertTrue(labelForPreset(it).length <= 4, "label for $it is too wide")
        }
    }
}
