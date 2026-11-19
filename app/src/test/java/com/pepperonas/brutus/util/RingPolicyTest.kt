package com.pepperonas.brutus.util

import android.media.AudioManager
import kotlin.math.log10
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class RingPolicyTest {

    private fun db(gain: Float) = 20 * log10(gain)

    @Test
    fun `without a crescendo the alarm plays at full gain from the start`() {
        assertEquals(1f, RingPolicy.crescendoGain(0, 0))
        assertEquals(1f, RingPolicy.gain(0, 0, hardcore = false, inCall = false))
    }

    @Test
    fun `a crescendo starts 30 dB down and reaches full gain at its end`() {
        assertEquals(-30f, db(RingPolicy.crescendoGain(0, 30)), 0.01f)
        assertEquals(1f, RingPolicy.crescendoGain(30_000, 30), 0.0001f)
        assertEquals(1f, RingPolicy.crescendoGain(90_000, 30), 0.0001f) // stays there
    }

    @Test
    fun `the crescendo rises evenly in decibels`() {
        assertEquals(-15f, db(RingPolicy.crescendoGain(15_000, 30)), 0.01f)
        assertEquals(-7.5f, db(RingPolicy.crescendoGain(45_000, 60)), 0.01f)
    }

    @Test
    fun `the crescendo never gets quieter over time`() {
        var previous = 0f
        for (ms in 0L..61_000L step 250) {
            val g = RingPolicy.crescendoGain(ms, 60)
            assertTrue(g >= previous, "gain fell at $ms ms")
            previous = g
        }
    }

    @Test
    fun `a hardcore alarm ignores the crescendo and reduced volume`() {
        assertEquals(1f, RingPolicy.gain(0, 60, hardcore = true, inCall = false, volumePercent = 25))
        assertEquals(1f, RingPolicy.gain(60_000, 0, hardcore = true, inCall = false, volumePercent = 5))
    }

    @Test
    fun `reduced volume scales evenly in dB from 5 to 100 percent`() {
        assertEquals(1f, RingPolicy.volumeGain(100), 0.0001f)
        assertEquals(RingPolicy.MIN_VOLUME_DB, db(RingPolicy.volumeGain(5)), 0.01f)
        assertEquals(RingPolicy.MIN_VOLUME_DB / 2f, db(RingPolicy.volumeGain(52)), 0.5f)
        assertTrue(RingPolicy.volumeGain(5) < RingPolicy.volumeGain(50))
        assertTrue(RingPolicy.volumeGain(50) < RingPolicy.volumeGain(100))
    }

    @Test
    fun `a crescendo with reduced volume rises evenly in dB up to the chosen cap`() {
        val targetDb = RingPolicy.volumeDb(50)
        val startDb = targetDb + RingPolicy.CRESCENDO_START_DB
        assertEquals(startDb, db(RingPolicy.crescendoGain(0, 120, 50)), 0.01f)
        val midDb = db(RingPolicy.crescendoGain(60_000, 120, 50))
        assertEquals((startDb + targetDb) / 2f, midDb, 0.01f)
        assertEquals(targetDb, db(RingPolicy.crescendoGain(120_000, 120, 50)), 0.01f)
        assertEquals(targetDb, db(RingPolicy.crescendoGain(300_000, 120, 50)), 0.01f)
    }

    @Test
    fun `a call quiets every alarm, hardcore included, but never makes a quiet one louder`() {
        assertEquals(RingPolicy.IN_CALL_GAIN, RingPolicy.gain(90_000, 30, hardcore = false, inCall = true))
        assertEquals(RingPolicy.IN_CALL_GAIN, RingPolicy.gain(0, 0, hardcore = true, inCall = true))
        val quiet = RingPolicy.crescendoGain(0, 60)
        assertEquals(quiet, RingPolicy.gain(0, 60, hardcore = false, inCall = true))
    }

    @Test
    fun `ringing, calling and screening count as a call - normal and unknown modes do not`() {
        listOf(AudioManager.MODE_RINGTONE, AudioManager.MODE_IN_CALL, AudioManager.MODE_IN_COMMUNICATION, 4, 5, 6)
            .forEach { assertTrue(RingPolicy.isInCall(it), "mode $it") }
        listOf(AudioManager.MODE_NORMAL, AudioManager.MODE_INVALID, AudioManager.MODE_CURRENT, 99)
            .forEach { assertFalse(RingPolicy.isInCall(it), "mode $it") }
    }

    @Test
    fun `the ring limit is reached exactly at its minute, never for hardcore, never when off`() {
        assertFalse(RingPolicy.ringLimitReached(15 * 60_000L - 1, 15, hardcore = false))
        assertTrue(RingPolicy.ringLimitReached(15 * 60_000L, 15, hardcore = false))
        assertFalse(RingPolicy.ringLimitReached(10 * 3_600_000L, 15, hardcore = true))
        assertFalse(RingPolicy.ringLimitReached(10 * 3_600_000L, 0, hardcore = false))
    }

    @Test
    fun `an unanswered alarm snoozes three times, then gives up`() {
        assertEquals(3, RingPolicy.MAX_AUTO_SNOOZES)
        (0 until 3).forEach { assertEquals(RingPolicy.LimitOutcome.SNOOZE, RingPolicy.afterLimit(it)) }
        assertEquals(RingPolicy.LimitOutcome.GIVE_UP, RingPolicy.afterLimit(3))
        assertEquals(RingPolicy.LimitOutcome.GIVE_UP, RingPolicy.afterLimit(7))
    }
}
