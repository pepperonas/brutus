package com.pepperonas.brutus.data

import com.pepperonas.brutus.util.AlarmSound
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Pure formatting/derivation helpers the alarm cards render from. */
class AlarmEntityTest {

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
        assertEquals("08:05", alarm(hour = 8, minute = 5).timeString())
        assertEquals("23:59", alarm(hour = 23, minute = 59).timeString())
        assertEquals("00:00", alarm(hour = 0, minute = 0).timeString())
    }

    @Test
    fun `repeatDaysString names the special cases`() {
        assertEquals("Einmalig", alarm(repeatDays = 0).repeatDaysString())
        assertEquals("Jeden Tag", alarm(repeatDays = 0x7F).repeatDaysString())
    }

    @Test
    fun `repeatDaysString lists selected days in week order`() {
        // Mon(bit0) + Wed(bit2) + Fri(bit4)
        val alarm = alarm(repeatDays = 0b0010101)
        assertEquals("Mo, Mi, Fr", alarm.repeatDaysString())
        // weekend only
        assertEquals("Sa, So", alarm(repeatDays = 0b1100000).repeatDaysString())
    }

    @Test
    fun `isDayEnabled reads the bitmask per weekday index`() {
        val alarm = alarm(repeatDays = 0b1000001) // Mon + Sun
        assertTrue(alarm.isDayEnabled(0))
        assertFalse(alarm.isDayEnabled(1))
        assertTrue(alarm.isDayEnabled(6))
    }

    @Test
    fun `ultra hardcore implies the effective hardcore behavior`() {
        assertFalse(alarm().hardcoreEffective)
        assertTrue(alarm(hardcore = true).hardcoreEffective)
        assertTrue(alarm(ultra = true).hardcoreEffective)          // even without the flag
        assertTrue(alarm(hardcore = true, ultra = true).hardcoreEffective)
    }

    @Test
    fun `soundName resolves the stored sound id`() {
        val alarm = alarm().copy(soundId = AlarmSound.KLAXON.id)
        assertEquals(AlarmSound.KLAXON.displayName, alarm.soundName())
    }
}
