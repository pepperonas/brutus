package com.pepperonas.brutus.util

import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.assertTrue

/**
 * What makes each v2.5.0 sound the sound it was chosen as — measured on the buffer, so a later "tidy
 * up" of a generator cannot quietly turn the rising siren flat or the accelerating beeper steady.
 */
class SynthSoundsTest {

    private val sr = AlarmSoundGenerator.SAMPLE_RATE

    private fun pcm(s: AlarmSound) = AlarmSoundGenerator.generatePcm(s)

    private fun window(p: ShortArray, fromSec: Double, toSec: Double) =
        p.copyOfRange((fromSec * sr).toInt(), minOf(p.size, (toSec * sr).toInt()))

    /** Zero crossings per second — a pitch proxy that needs no FFT. */
    private fun crossings(w: ShortArray): Double {
        var n = 0
        for (i in 1 until w.size) if ((w[i - 1] < 0) != (w[i] < 0)) n++
        return n * sr.toDouble() / w.size
    }

    private fun rms(w: ShortArray) = sqrt(w.sumOf { it.toDouble() * it } / w.size)

    /** Separate sounding stretches: runs of samples above 5 % of full scale, split by ≥ 10 ms of quiet. */
    private fun bursts(w: ShortArray): Int {
        val quiet = (0.010 * sr).toInt()
        var count = 0
        var silence = quiet
        for (s in w) {
            if (abs(s.toInt()) > Short.MAX_VALUE / 20) {
                if (silence >= quiet) count++
                silence = 0
            } else silence++
        }
        return count
    }

    private val newHarsh = listOf(
        AlarmSound.AIR_RAID, AlarmSound.DIVE, AlarmSound.CAR_ALARM, AlarmSound.SCHOOL_BELL,
        AlarmSound.REVERSE_BEEPER, AlarmSound.SHEPARD, AlarmSound.STEEL_HAMMER, AlarmSound.STROBE,
        AlarmSound.WHOOP,
    )

    @Test
    fun `the new harsh sounds use the full scale, the gentle ones stay at 60 percent`() {
        newHarsh.forEach { s ->
            val peak = pcm(s).maxOf { abs(it.toInt()) } / Short.MAX_VALUE.toDouble()
            assertTrue(peak > 0.93, "$s peaks at only ${"%.2f".format(peak)}")
        }
        AlarmSound.entries.filter { it.gentle }.forEach { s ->
            val peak = pcm(s).maxOf { abs(it.toInt()) } / Short.MAX_VALUE.toDouble()
            assertTrue(peak <= 0.61, "$s peaks at ${"%.2f".format(peak)}")
        }
    }

    @Test
    fun `gentle loops are six seconds long — long enough not to feel like a loop in Sunrise`() {
        AlarmSound.entries.filter { it.gentle }.forEach { s ->
            assertTrue(pcm(s).size == 6 * sr, "$s loops after ${pcm(s).size.toDouble() / sr} s")
        }
    }

    @Test
    fun `the air-raid siren winds up and back down`() {
        val p = pcm(AlarmSound.AIR_RAID)
        val start = crossings(window(p, 0.1, 0.5))
        val top = crossings(window(p, 2.6, 3.4))
        val end = crossings(window(p, 5.5, 5.9))
        assertTrue(top > start * 2 && top > end * 2, "start $start, top $top, end $end")
    }

    @Test
    fun `the dive alarm climbs through its A-OO`() {
        val p = pcm(AlarmSound.DIVE)
        assertTrue(crossings(window(p, 0.55, 0.75)) > crossings(window(p, 0.02, 0.12)) * 1.5)
    }

    @Test
    fun `the reverse beeper keeps speeding up`() {
        val p = pcm(AlarmSound.REVERSE_BEEPER)
        val first = bursts(window(p, 0.0, 1.0))
        val last = bursts(window(p, 3.0, 4.0))
        assertTrue(last >= first * 3, "first second $first beeps, last $last")
    }

    @Test
    fun `the strobe gets faster and higher`() {
        val p = pcm(AlarmSound.STROBE)
        assertTrue(bursts(window(p, 4.0, 5.0)) > bursts(window(p, 0.0, 1.0)) * 3)
        assertTrue(crossings(window(p, 4.4, 5.0)) > crossings(window(p, 0.0, 0.6)) * 1.5)
    }

    @Test
    fun `the school bell is a fast mechanical clapper, then rings out`() {
        val p = pcm(AlarmSound.SCHOOL_BELL)
        // 22 strikes a second while the clapper runs, the last half second only decays
        assertTrue(rms(window(p, 2.9, 3.0)) < rms(window(p, 1.0, 1.1)) / 2)
    }

    @Test
    fun `the car alarm changes its pattern every one and a half seconds`() {
        val p = pcm(AlarmSound.CAR_ALARM)
        val segments = (0 until 4).map { window(p, it * 1.5 + 0.1, it * 1.5 + 1.4) }
        // the horn segment (third) is gated: it has gaps the wailing segments don't
        assertTrue(bursts(segments[2]) > bursts(segments[0]) + 3, "horn ${bursts(segments[2])}, wail ${bursts(segments[0])}")
    }

    @Test
    fun `the singing bowl rings out over its loop`() {
        val p = pcm(AlarmSound.SINGING_BOWL)
        assertTrue(rms(window(p, 0.2, 1.2)) > rms(window(p, 4.8, 5.8)) * 2)
    }

    @Test
    fun `the ocean swells in the middle and draws back at the edges`() {
        val p = pcm(AlarmSound.OCEAN)
        val middle = rms(window(p, 2.5, 3.5))
        assertTrue(middle > rms(window(p, 0.0, 0.5)) * 2 && middle > rms(window(p, 5.5, 6.0)) * 2)
    }

    @Test
    fun `daybreak brightens towards the middle of its loop`() {
        val p = pcm(AlarmSound.SUNRISE)
        assertTrue(crossings(window(p, 2.5, 3.5)) > crossings(window(p, 0.0, 0.4)) * 1.3)
    }
}
