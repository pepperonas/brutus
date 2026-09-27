package com.pepperonas.brutus.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Persistence of the timer-finish tone (v1.5.0). */
@RunWith(RobolectricTestRunner::class)
class TimerSoundStoreTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun writeRawId(id: Int) = Storage.prefs(context, "brutus_timer")
        .edit().putInt("timer_sound_id", id).commit()

    @Test
    fun `a fresh install gets the gentle default, not the harsh system alarm`() {
        val sound = TimerSoundStore.get(context)
        assertEquals(AlarmSound.WIND_CHIMES, sound)
        assertTrue(sound.gentle, "the kitchen timer must not ring like a hardcore alarm")
    }

    @Test
    fun `a chosen gentle sound round-trips`() {
        TimerSoundStore.set(context, AlarmSound.KALIMBA)
        assertEquals(AlarmSound.KALIMBA, TimerSoundStore.get(context))

        TimerSoundStore.set(context, AlarmSound.HARP)
        assertEquals(AlarmSound.HARP, TimerSoundStore.get(context))
    }

    @Test
    fun `the system ringtone can be selected even though its id is zero`() {
        // id 0 is the classic "is it stored or is it the default?" trap.
        TimerSoundStore.set(context, AlarmSound.SYSTEM)
        assertEquals(AlarmSound.SYSTEM, TimerSoundStore.get(context))
    }

    @Test
    fun `silence can be selected for the timer`() {
        TimerSoundStore.set(context, AlarmSound.SILENT)
        assertEquals(AlarmSound.SILENT, TimerSoundStore.get(context))
    }

    @Test
    fun `a corrupt persisted id degrades to the system tone instead of crashing`() {
        writeRawId(4711)
        assertEquals(AlarmSound.SYSTEM, TimerSoundStore.get(context))
    }

    @Test
    fun `every sound offered by the picker survives a round-trip`() {
        AlarmSound.gentleSounds().forEach { sound ->
            TimerSoundStore.set(context, sound)
            assertEquals(sound, TimerSoundStore.get(context), "round-trip failed for $sound")
        }
    }

    @Test
    fun `a timer saved with a retired sound keeps ringing with its successor`() {
        writeRawId(7) // the chime, removed in v2.5.0
        assertEquals(AlarmSound.WIND_CHIMES, TimerSoundStore.get(context))
        writeRawId(8) // marimba
        assertEquals(AlarmSound.KALIMBA, TimerSoundStore.get(context))
    }
}
