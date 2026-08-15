package com.pepperonas.brutus.util

import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

/**
 * Properties that must hold for *every* synthesized sound, present and future.
 * The existing suite checks a few named sounds; this one is the net that a
 * newly added enum entry falls into automatically.
 */
class AlarmSoundGeneratorPropertiesTest {

    private val audible = AlarmSound.entries
        .filter { it != AlarmSound.SILENT && it != AlarmSound.SYSTEM }

    @Test
    fun `every loop buffer stays within a sane duration — it is held in memory and looped`() {
        audible.forEach { sound ->
            val seconds = AlarmSoundGenerator.generatePcm(sound).size.toDouble() /
                AlarmSoundGenerator.SAMPLE_RATE
            assertTrue(
                seconds in 0.1..6.0,
                "$sound produced a ${"%.2f".format(seconds)} s loop — outside the 0.1–6 s budget"
            )
        }
    }

    @Test
    fun `generation is deterministic — the same sound always yields the same buffer`() {
        // The buffer is generated once and looped by AudioTrack; a random
        // component would make the loop seam audible and tests flaky.
        audible.forEach { sound ->
            assertContentEquals(
                AlarmSoundGenerator.generatePcm(sound),
                AlarmSoundGenerator.generatePcm(sound),
                "$sound is not deterministic"
            )
        }
    }

    /**
     * PIERCING is knowingly asymmetric: its 3.5 kHz period is 12.6 samples, but
     * the phase index uses `period.toInt()` (12) while the phase *divisor* stays
     * 12.6 — so seven of twelve samples land in the positive half and the duty
     * cycle is ~58 %, worth roughly +10 % DC. It has shipped and been tuned by
     * ear that way (the extra even harmonics are part of why it is so nasty),
     * so it is exempted rather than "fixed" behind the user's back — but it is
     * bounded below, so it cannot silently get worse.
     */
    private val knownDcOffset = mapOf(AlarmSound.PIERCING to 0.12)

    private fun dcRatio(sound: AlarmSound): Double {
        val pcm = AlarmSoundGenerator.generatePcm(sound)
        val peak = pcm.maxOf { abs(it.toInt()) }.coerceAtLeast(1)
        val mean = pcm.sumOf { it.toInt().toLong() }.toDouble() / pcm.size
        return abs(mean) / peak
    }

    @Test
    fun `no sound carries a meaningful DC offset`() {
        // A biased waveform wastes headroom and thumps the speaker on start/stop.
        audible.filterNot { it in knownDcOffset }.forEach { sound ->
            assertTrue(
                dcRatio(sound) < 0.10,
                "$sound has a DC offset of ${"%.1f".format(dcRatio(sound) * 100)} % of full scale"
            )
        }
    }

    @Test
    fun `the known asymmetric sound does not drift further off centre`() {
        knownDcOffset.forEach { (sound, ceiling) ->
            assertTrue(
                dcRatio(sound) < ceiling,
                "$sound drifted to ${"%.1f".format(dcRatio(sound) * 100)} % DC (bound: ${ceiling * 100} %)"
            )
        }
    }

    @Test
    fun `every sound actually oscillates instead of holding one level`() {
        audible.forEach { sound ->
            val pcm = AlarmSoundGenerator.generatePcm(sound)
            // A clean square wave has exactly two levels — two is a waveform, one is silence.
            val distinct = pcm.take(AlarmSoundGenerator.SAMPLE_RATE / 10).toSet().size
            assertTrue(distinct > 1, "$sound looks like a constant, not a waveform")
        }
    }

    @Test
    fun `every sound swings to both sides of the centre line`() {
        audible.forEach { sound ->
            val pcm = AlarmSoundGenerator.generatePcm(sound)
            assertTrue(pcm.any { it > 0 } && pcm.any { it < 0 }, "$sound is single-sided")
        }
    }

    @Test
    fun `gentle sounds keep the documented headroom`() {
        AlarmSound.entries.filter { it.gentle }.forEach { sound ->
            val peak = AlarmSoundGenerator.generatePcm(sound).maxOf { abs(it.toInt()) }
            assertTrue(
                peak <= Short.MAX_VALUE * 0.7,
                "$sound peaks at $peak — gentle sounds are capped at ~50–60 % of full scale"
            )
        }
    }
}
