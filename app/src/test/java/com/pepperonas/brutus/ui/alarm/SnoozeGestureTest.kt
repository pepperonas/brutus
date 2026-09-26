package com.pepperonas.brutus.ui.alarm

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The swipe-to-snooze rule. Since v2.4.0 the thumb carries the finger's fling into its spring,
 * but only the POSITION decides whether letting go snoozes — a quick flick from the start must
 * never snooze by accident.
 */
class SnoozeGestureTest {

    @Test
    fun `the point of no return stays at 85 percent of the track`() {
        assertEquals(0.85f, SNOOZE_THRESHOLD)
    }

    @Test
    fun `below the threshold nothing happens, at it the snooze is armed`() {
        assertFalse(snoozeArmed(offset = 84.9f, maxOffset = 100f))
        assertTrue(snoozeArmed(offset = 85f, maxOffset = 100f))
        assertTrue(snoozeArmed(offset = 100f, maxOffset = 100f))
    }

    @Test
    fun `a track that has not been measured yet never arms`() {
        assertFalse(snoozeArmed(offset = 0f, maxOffset = 0f))
    }
}
