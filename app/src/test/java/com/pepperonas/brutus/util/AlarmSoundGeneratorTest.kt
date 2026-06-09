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
        val gentlePeaks = listOf(AlarmSound.CHIME, AlarmSound.MARIMBA, AlarmSound.MORNING)
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
    fun `gentle sounds start and end near zero to avoid click on loop boundary`() {
        // The harsh sounds (PIERCING etc.) intentionally hard-start at full amplitude.
        // The gentle sounds are meant to loop transparently and must fade in / out.
        listOf(AlarmSound.CHIME, AlarmSound.MARIMBA, AlarmSound.MORNING).forEach { snd ->
            val pcm = AlarmSoundGenerator.generatePcm(snd)
            val tolerance = Short.MAX_VALUE / 20  // 5% of full scale
            assertTrue(
                pcm.first().toInt() in -tolerance..tolerance,
                "$snd first sample ${pcm.first()} is outside ±$tolerance"
            )
            assertTrue(
                pcm.last().toInt() in -tolerance..tolerance,
                "$snd last sample ${pcm.last()} is outside ±$tolerance"
            )
        }
    }

    @Test
    fun `gentleSounds list contains the new soft sounds plus SYSTEM and SILENT`() {
        val gentle = AlarmSound.gentleSounds()
        assertTrue(AlarmSound.CHIME in gentle)
        assertTrue(AlarmSound.MARIMBA in gentle)
        assertTrue(AlarmSound.MORNING in gentle)
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
    fun `TimerSoundStore default is CHIME`() {
        assertEquals(AlarmSound.CHIME, TimerSoundStore.DEFAULT_SOUND)
    }
}
