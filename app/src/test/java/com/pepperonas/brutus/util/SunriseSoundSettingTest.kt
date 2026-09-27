package com.pepperonas.brutus.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** The Sunrise pre-alarm's sound (v2.5.0) — before, it always played the now-retired chime. */
@RunWith(RobolectricTestRunner::class)
class SunriseSoundSettingTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Storage.prefs(context, "brutus_settings").edit().clear().commit()
    }

    private fun writeRawId(id: Int) = Storage.prefs(context, "brutus_settings")
        .edit().putInt("sunrise_sound_id", id).commit()

    @Test
    fun `a fresh install plays the sunrise pad`() {
        assertEquals(AlarmSound.SUNRISE, AppSettings.sunriseSound(context))
    }

    @Test
    fun `every offered sound round-trips`() {
        AlarmSound.sunriseSounds().forEach { sound ->
            AppSettings.setSunriseSound(context, sound)
            assertEquals(sound, AppSettings.sunriseSound(context))
        }
    }

    @Test
    fun `only gentle sounds and silence are offered — never a harsh one or the system alarm`() {
        val offered = AlarmSound.sunriseSounds()
        assertTrue(AlarmSound.SILENT in offered)
        assertTrue(AlarmSound.SYSTEM !in offered)
        assertTrue(offered.filter { it != AlarmSound.SILENT }.all { it.gentle })
        assertEquals(AlarmSound.entries.count { it.gentle }, offered.size - 1)
    }

    @Test
    fun `a harsh sound cannot be stored, and a stored one falls back to the default`() {
        assertFailsWith<IllegalArgumentException> { AppSettings.setSunriseSound(context, AlarmSound.KLAXON) }
        writeRawId(AlarmSound.KLAXON.id)
        assertEquals(AlarmSound.SUNRISE, AppSettings.sunriseSound(context))
        writeRawId(4711)
        assertEquals(AlarmSound.SUNRISE, AppSettings.sunriseSound(context))
    }

    @Test
    fun `a retired gentle id resolves to its successor`() {
        writeRawId(8) // marimba
        assertEquals(AlarmSound.KALIMBA, AppSettings.sunriseSound(context))
    }
}
