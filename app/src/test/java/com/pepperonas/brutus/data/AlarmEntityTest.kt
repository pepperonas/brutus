package com.pepperonas.brutus.data

import android.content.Context
import com.pepperonas.brutus.LocaleContexts
import com.pepperonas.brutus.util.AlarmSound
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Formatting/derivation helpers the alarm cards render from. They take a Context
 * because their output is localized — so every one of them is asserted in both
 * shipped languages.
 */
@RunWith(RobolectricTestRunner::class)
class AlarmEntityTest {

    private lateinit var en: Context
    private lateinit var de: Context

    @Before
    fun setUp() {
        en = LocaleContexts.english()
        de = LocaleContexts.german()
    }

    private fun alarm(
        hour: Int = 8,
        minute: Int = 30,
        repeatDays: Int = 0,
        hardcore: Boolean = false,
        ultra: Boolean = false,
    ) = AlarmEntity(
        hour = hour,
        minute = minute,
        repeatDays = repeatDays,
        hardcoreMode = hardcore,
        ultraHardcoreMode = ultra,
    )

    @Test
    fun `timeString zero-pads hour and minute`() {
        // Digits only — the one string on the card that is not translated.
        assertEquals("08:05", alarm(hour = 8, minute = 5).timeString())
        assertEquals("23:59", alarm(hour = 23, minute = 59).timeString())
        assertEquals("00:00", alarm(hour = 0, minute = 0).timeString())
    }

    @Test
    fun `repeatDaysString names the special cases in both languages`() {
        assertEquals("Once", alarm(repeatDays = 0).repeatDaysString(en))
        assertEquals("Every day", alarm(repeatDays = 0x7F).repeatDaysString(en))

        assertEquals("Einmalig", alarm(repeatDays = 0).repeatDaysString(de))
        assertEquals("Jeden Tag", alarm(repeatDays = 0x7F).repeatDaysString(de))
    }

    @Test
    fun `repeatDaysString lists selected days in week order`() {
        // Mon(bit0) + Wed(bit2) + Fri(bit4)
        val midweek = alarm(repeatDays = 0b0010101)
        assertEquals("Mon, Wed, Fri", midweek.repeatDaysString(en))
        assertEquals("Mo, Mi, Fr", midweek.repeatDaysString(de))

        val weekend = alarm(repeatDays = 0b1100000)
        assertEquals("Sat, Sun", weekend.repeatDaysString(en))
        assertEquals("Sa, So", weekend.repeatDaysString(de))
    }

    @Test
    fun `isDayEnabled reads the bitmask per weekday index`() {
        val alarm = alarm(repeatDays = 0b1000001) // Mon + Sun
        assertTrue(alarm.isDayEnabled(0))
        assertFalse(alarm.isDayEnabled(1))
        assertFalse(alarm.isDayEnabled(5))
        assertTrue(alarm.isDayEnabled(6))
    }

    @Test
    fun `ultra hardcore implies the effective hardcore behavior`() {
        assertFalse(alarm().hardcoreEffective)
        assertTrue(alarm(hardcore = true).hardcoreEffective)
        assertTrue(alarm(ultra = true).hardcoreEffective)
        assertTrue(alarm(hardcore = true, ultra = true).hardcoreEffective)
    }

    @Test
    fun `soundName resolves the stored sound id in the active language`() {
        val alarm = AlarmEntity(hour = 7, minute = 0, soundId = AlarmSound.KLAXON.id)
        assertEquals("Klaxon", alarm.soundName(en))
        assertEquals("Klaxon", alarm.soundName(de))

        val chimes = alarm.copy(soundId = AlarmSound.WIND_CHIMES.id)
        assertEquals("Wind chimes", chimes.soundName(en))
        assertEquals("Windspiel", chimes.soundName(de))

        // An alarm saved with the retired chime (id 7) shows — and rings — its successor.
        val retired = alarm.copy(soundId = 7)
        assertEquals("Wind chimes", retired.soundName(en))
    }

    @Test
    fun `challengeName describes the stored combination in both languages`() {
        val all = AlarmEntity(
            hour = 7,
            minute = 0,
            challengeFlags = 0b111,
        )
        assertEquals("Math + Shake + QR code", all.challengeName(en))
        assertEquals("Mathe + Schütteln + QR-Code", all.challengeName(de))
    }
}
