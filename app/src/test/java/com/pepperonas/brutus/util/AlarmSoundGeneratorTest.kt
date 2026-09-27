package com.pepperonas.brutus.util

import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AlarmSoundGeneratorTest {

    @Test
    fun `SILENT and SYSTEM produce empty buffers because they're handled elsewhere`() {
        assertEquals(0, AlarmSoundGenerator.generatePcm(AlarmSound.SILENT).size)
        assertEquals(0, AlarmSoundGenerator.generatePcm(AlarmSound.SYSTEM).size)
    }

    @Test
    fun `every audible sound returns a non-trivial PCM buffer`() {
        AlarmSound.entries
            .filter { it != AlarmSound.SILENT && it != AlarmSound.SYSTEM }
            .forEach { snd ->
                val pcm = AlarmSoundGenerator.generatePcm(snd)
                assertTrue(
                    pcm.size >= AlarmSoundGenerator.SAMPLE_RATE / 10,
                    "$snd produced only ${pcm.size} samples (< 100ms)"
                )
                val peak = pcm.maxOf { max(it.toInt(), -it.toInt()) }
                assertTrue(peak > 1000, "$snd never rises above $peak — likely silent")
            }
    }

    @Test
    fun `gentle sounds are noticeably quieter than the harsh ones`() {
        val gentlePeaks = AlarmSound.entries.filter { it.gentle }
            .map { AlarmSoundGenerator.generatePcm(it).maxOf { s -> max(s.toInt(), -s.toInt()) } }
        val harshPeaks = listOf(AlarmSound.KLAXON, AlarmSound.NUCLEAR, AlarmSound.PIERCING)
            .map { AlarmSoundGenerator.generatePcm(it).maxOf { s -> max(s.toInt(), -s.toInt()) } }
        val gentleMax = gentlePeaks.max()
        val harshMin = harshPeaks.min()
        assertTrue(
            gentleMax < harshMin || gentleMax < Short.MAX_VALUE * 0.75,
            "Gentle peak $gentleMax should be ≤ ~75% of full-scale (harsh min: $harshMin)"
        )
    }

    @Test
    fun `gentle sounds loop without a click`() {
        // Sunrise loops one for ten minutes. A click is a break in the waveform, not a steep slope:
        // the first sample must follow the trend of the last two as closely as any sample inside
        // the buffer follows its predecessors.
        AlarmSound.entries.filter { it.gentle }.forEach { snd ->
            val p = AlarmSoundGenerator.generatePcm(snd).map { it.toInt() }
            val inside = (2 until p.size).map { kotlin.math.abs(p[it] - 2 * p[it - 1] + p[it - 2]) }.sorted()
            val typical = inside[(inside.size * 0.999).toInt()].coerceAtLeast(1)
            val wrap = kotlin.math.abs(p[0] - 2 * p[p.size - 1] + p[p.size - 2])
            assertTrue(wrap <= typical, "$snd: wrap break $wrap vs 99.9th percentile $typical")
        }
    }

    @Test
    fun `gentleSounds list contains the new soft sounds plus SYSTEM and SILENT`() {
        val gentle = AlarmSound.gentleSounds()
        AlarmSound.entries.filter { it.gentle }.forEach { assertTrue(it in gentle, "$it") }
        assertEquals(8, AlarmSound.entries.count { it.gentle })
        assertTrue(AlarmSound.SILENT in gentle)
        assertTrue(AlarmSound.SYSTEM in gentle)
        // Harsh sounds should NOT be in the gentle list
        assertTrue(AlarmSound.KLAXON !in gentle)
        assertTrue(AlarmSound.PIERCING !in gentle)
    }

    @Test
    fun `the v1_7 extreme sounds are harsh, not gentle`() {
        val extreme = listOf(
            AlarmSound.AIRHORN, AlarmSound.JACKHAMMER, AlarmSound.FIRE_ALARM,
            AlarmSound.DENTIST, AlarmSound.BANSHEE,
            // v2.5.0
            AlarmSound.AIR_RAID, AlarmSound.DIVE, AlarmSound.CAR_ALARM, AlarmSound.SCHOOL_BELL,
            AlarmSound.REVERSE_BEEPER, AlarmSound.SHEPARD, AlarmSound.STEEL_HAMMER, AlarmSound.STROBE,
            AlarmSound.WHOOP,
        )
        val gentle = AlarmSound.gentleSounds()
        extreme.forEach { snd ->
            assertTrue(snd !in gentle, "$snd must not be offered as a gentle sound")
            assertTrue(!snd.gentle, "$snd.gentle should be false")
            val peak = AlarmSoundGenerator.generatePcm(snd).maxOf { max(it.toInt(), -it.toInt()) }
            assertTrue(peak > Short.MAX_VALUE * 0.3, "$snd peak $peak is too quiet for an extreme sound")
        }
    }

    @Test
    fun `TimerSoundStore default is the successor of the retired chime`() {
        assertEquals(AlarmSound.WIND_CHIMES, TimerSoundStore.DEFAULT_SOUND)
        assertEquals(AlarmSound.fromId(7), TimerSoundStore.DEFAULT_SOUND)
    }
}
