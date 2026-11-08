package com.pepperonas.brutus.util

import com.pepperonas.brutus.data.AlarmEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import kotlin.test.assertEquals

/** Vacation mode in the next-trigger arithmetic and the date conversions — pinned to Europe/Berlin. */
class VacationTest {

    private lateinit var previousZone: TimeZone
    private val berlin = TimeZone.getTimeZone("Europe/Berlin")

    @Before fun setUp() { previousZone = TimeZone.getDefault(); TimeZone.setDefault(berlin) }
    @After fun tearDown() { TimeZone.setDefault(previousZone) }

    private fun at(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0): Long =
        Calendar.getInstance(berlin).apply { clear(); set(y, m - 1, d, h, min, 0) }.timeInMillis

    private val weekdays = AlarmEntity(id = 1, hour = 7, minute = 0, repeatDays = 0b0011111)
    private val oneShot = AlarmEntity(id = 2, hour = 7, minute = 0)

    // 2026-11-09 is a Monday.
    private val fridayEvening = at(2026, 11, 6, 20)
    private val backOnMonday16 = at(2026, 11, 16) // vacation through Sunday 15th

    @Test
    fun `during vacation the next occurrence is the first one on the day back`() {
        assertEquals(at(2026, 11, 9, 7), NextAlarmCalculator.nextTrigger(weekdays, fridayEvening, null))
        assertEquals(at(2026, 11, 16, 7), NextAlarmCalculator.nextTrigger(weekdays, fridayEvening, null, backOnMonday16))
    }

    @Test
    fun `a one-shot alarm moves to the day back as well`() {
        assertEquals(at(2026, 11, 16, 7), NextAlarmCalculator.nextTrigger(oneShot, fridayEvening, null, backOnMonday16))
    }

    @Test
    fun `an alarm exactly at the end of the pause rings`() {
        val midnight = AlarmEntity(id = 3, hour = 0, minute = 0, repeatDays = 0x7F)
        assertEquals(backOnMonday16, NextAlarmCalculator.nextTrigger(midnight, fridayEvening, null, backOnMonday16))
    }

    @Test
    fun `a pause that is over changes nothing`() {
        val over = at(2026, 11, 1)
        assertEquals(
            NextAlarmCalculator.nextTrigger(weekdays, fridayEvening, null),
            NextAlarmCalculator.nextTrigger(weekdays, fridayEvening, null, over),
        )
    }

    @Test
    fun `findNext honours the pause for every alarm`() {
        val sundayOnly = AlarmEntity(id = 4, hour = 9, minute = 0, repeatDays = 1 shl 6)
        // Without a pause Sunday 8th 09:00 comes first; with it, Monday 16th 07:00.
        assertEquals(sundayOnly, NextAlarmCalculator.findNext(listOf(weekdays, sundayOnly), fridayEvening))
        assertEquals(weekdays, NextAlarmCalculator.findNext(listOf(weekdays, sundayOnly), fridayEvening, pausedUntil = backOnMonday16))
    }

    @Test
    fun `the stored end of a pause is midnight after the last day, also across the DST change`() {
        assertEquals(backOnMonday16, VacationStore.startOfNextDay(at(2026, 11, 15, 12)))
        assertEquals(backOnMonday16, VacationStore.startOfNextDay(at(2026, 11, 15, 23, 59)))
        // 2026-10-25 has 25 hours in Berlin; the next day still starts at 00:00, not 23:00.
        assertEquals(at(2026, 10, 26), VacationStore.startOfNextDay(at(2026, 10, 25, 12)))
    }

    @Test
    fun `the picker's UTC day maps to the same calendar day locally and back`() {
        val utcNov15 = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(2026, 10, 15) }.timeInMillis
        assertEquals(at(2026, 11, 15, 12), VacationDates.fromPickerMillis(utcNov15))
        assertEquals(utcNov15, VacationDates.toPickerMillis(at(2026, 11, 15, 0, 30)))
        assertEquals(utcNov15, VacationDates.toPickerMillis(at(2026, 11, 15, 23, 30)))
    }

    @Test
    fun `west of UTC the day does not slip either`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
        val utcNov15 = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(2026, 10, 15) }.timeInMillis
        val local = Calendar.getInstance().apply { timeInMillis = VacationDates.fromPickerMillis(utcNov15) }
        assertEquals(15, local.get(Calendar.DAY_OF_MONTH))
    }
}
