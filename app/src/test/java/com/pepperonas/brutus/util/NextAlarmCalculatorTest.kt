package com.pepperonas.brutus.util

import com.pepperonas.brutus.data.AlarmEntity
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NextAlarmCalculatorTest {

    private fun atTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val c = Calendar.getInstance()
        c.timeZone = TimeZone.getDefault()
        c.set(year, month - 1, day, hour, minute)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun dayOfWeek(millis: Long): Int {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        return c.get(Calendar.DAY_OF_WEEK)
    }

    private fun hourOf(millis: Long): Int {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        return c.get(Calendar.HOUR_OF_DAY)
    }

    private fun minuteOf(millis: Long): Int {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        return c.get(Calendar.MINUTE)
    }

    // ----- One-shot alarms -----

    @Test
    fun `one-shot alarm later today fires today`() {
        val now = atTime(2026, 5, 1, 8, 0)              // Fri 08:00
        val alarm = AlarmEntity(hour = 22, minute = 30, repeatDays = 0)
        val next = NextAlarmCalculator.nextTrigger(alarm, now)
        assertNotNull(next)
        assertEquals(22, hourOf(next))
        assertEquals(30, minuteOf(next))
        assertTrue(next > now)
        assertTrue(next - now == ((22 - 8) * 60L + 30) * 60_000L)
    }

    @Test
    fun `one-shot alarm earlier today fires tomorrow`() {
        val now = atTime(2026, 5, 1, 8, 0)              // Fri 08:00
        val alarm = AlarmEntity(hour = 7, minute = 30, repeatDays = 0)
        val next = NextAlarmCalculator.nextTrigger(alarm, now)
        assertNotNull(next)
        assertEquals(7, hourOf(next))
        assertEquals(30, minuteOf(next))
        // Should be tomorrow's 7:30, not today's
        assertTrue(next - now > 12 * 60 * 60_000L)
    }

    @Test
    fun `one-shot at exactly current minute fires tomorrow`() {
        val now = atTime(2026, 5, 1, 8, 0)              // 08:00 exactly
        val alarm = AlarmEntity(hour = 8, minute = 0, repeatDays = 0)
        val next = NextAlarmCalculator.nextTrigger(alarm, now)
        assertNotNull(next)
        // Same time means roll to tomorrow per `!target.after(nowCal)`
        val expectedDelta = 24L * 60 * 60_000L
        assertEquals(expectedDelta, next - now)
    }

    // ----- Repeating alarms -----

    @Test
    fun `repeating Monday-only on a Friday fires next Monday`() {
        val now = atTime(2026, 5, 1, 8, 0)              // Fri
        val mondayBit = 1 shl 0
        val alarm = AlarmEntity(hour = 7, minute = 0, repeatDays = mondayBit)
        val next = NextAlarmCalculator.nextTrigger(alarm, now)
        assertNotNull(next)
        assertEquals(Calendar.MONDAY, dayOfWeek(next))
        assertEquals(7, hourOf(next))
        assertEquals(0, minuteOf(next))
    }

    @Test
    fun `repeating today, time still in future, fires today`() {
        val now = atTime(2026, 5, 1, 8, 0)              // Fri 08:00
        val fridayBit = 1 shl 4
        val alarm = AlarmEntity(hour = 22, minute = 0, repeatDays = fridayBit)
        val next = NextAlarmCalculator.nextTrigger(alarm, now)
        assertNotNull(next)
        assertEquals(Calendar.FRIDAY, dayOfWeek(next))
        // Within 24h
        assertTrue(next - now < 24L * 60 * 60_000L)
    }

    @Test
    fun `repeating today, time already passed, fires next week same day`() {
        val now = atTime(2026, 5, 1, 8, 0)              // Fri 08:00
        val fridayBit = 1 shl 4
        val alarm = AlarmEntity(hour = 7, minute = 0, repeatDays = fridayBit)
        val next = NextAlarmCalculator.nextTrigger(alarm, now)
        assertNotNull(next)
        assertEquals(Calendar.FRIDAY, dayOfWeek(next))
        // Should be 7 days later
        assertTrue(next - now in (6L * 24 * 60 * 60_000L)..(8L * 24 * 60 * 60_000L))
    }

    @Test
    fun `every-day alarm picks the soonest available occurrence`() {
        val now = atTime(2026, 5, 1, 23, 30)            // Fri 23:30
        val allDays = 0x7F                               // all 7 bits set
        val alarm = AlarmEntity(hour = 7, minute = 0, repeatDays = allDays)
        val next = NextAlarmCalculator.nextTrigger(alarm, now)
        assertNotNull(next)
        // Should be tomorrow (Sat) 07:00
        assertEquals(Calendar.SATURDAY, dayOfWeek(next))
        assertEquals(7, hourOf(next))
    }

    @Test
    fun `weekend-only alarm on a Wednesday fires next Saturday`() {
        // Wed 2026-05-06, weekend = Sat (bit 5) | Sun (bit 6)
        val now = atTime(2026, 5, 6, 10, 0)
        val weekend = (1 shl 5) or (1 shl 6)
        val alarm = AlarmEntity(hour = 9, minute = 0, repeatDays = weekend)
        val next = NextAlarmCalculator.nextTrigger(alarm, now)
        assertNotNull(next)
        assertEquals(Calendar.SATURDAY, dayOfWeek(next))
    }

    // ----- findNext() -----

    @Test
    fun `findNext picks soonest among multiple enabled alarms`() {
        val now = atTime(2026, 5, 1, 8, 0)              // Fri 08:00
        val a1 = AlarmEntity(id = 1, hour = 22, minute = 0, repeatDays = 0)
        val a2 = AlarmEntity(id = 2, hour = 9, minute = 0, repeatDays = 0)        // earlier
        val a3 = AlarmEntity(id = 3, hour = 6, minute = 0, repeatDays = 0)        // tomorrow
        val pick = NextAlarmCalculator.findNext(listOf(a1, a2, a3), now)
        assertNotNull(pick)
        assertEquals(2L, pick.id)
    }

    @Test
    fun `findNext skips disabled alarms`() {
        val now = atTime(2026, 5, 1, 8, 0)
        val disabled = AlarmEntity(id = 1, hour = 9, minute = 0, repeatDays = 0, enabled = false)
        val later = AlarmEntity(id = 2, hour = 22, minute = 0, repeatDays = 0)
        val pick = NextAlarmCalculator.findNext(listOf(disabled, later), now)
        assertNotNull(pick)
        assertEquals(2L, pick.id)
    }

    @Test
    fun `findNext returns null when all disabled`() {
        val all = listOf(
            AlarmEntity(id = 1, hour = 9, minute = 0, enabled = false),
            AlarmEntity(id = 2, hour = 10, minute = 0, enabled = false),
        )
        assertNull(NextAlarmCalculator.findNext(all, atTime(2026, 5, 1, 8, 0)))
    }

    @Test
    fun `findNext empty list`() {
        assertNull(NextAlarmCalculator.findNext(emptyList(), 0L))
    }

    // ----- formatCountdown -----

    @Test
    fun `formatCountdown minutes only`() {
        assertEquals(
            "Alarm in 5 Minuten",
            NextAlarmCalculator.formatCountdown(0L, 5 * 60_000L)
        )
    }

    @Test
    fun `formatCountdown hours and minutes`() {
        val delta = (3 * 60 + 17) * 60_000L
        assertEquals(
            "Alarm in 3 Stunden, 17 Minuten",
            NextAlarmCalculator.formatCountdown(0L, delta)
        )
    }

    @Test
    fun `formatCountdown days uses singular form for 1 day`() {
        val delta = (24L + 5) * 60 * 60_000L           // 1d 5h
        assertEquals(
            "Alarm in 1 Tag, 5 Std.",
            NextAlarmCalculator.formatCountdown(0L, delta)
        )
    }

    @Test
    fun `formatCountdown plural days`() {
        val delta = (3 * 24L + 7) * 60 * 60_000L
        assertEquals(
            "Alarm in 3 Tagen, 7 Std.",
            NextAlarmCalculator.formatCountdown(0L, delta)
        )
    }

    @Test
    fun `formatCountdown clamps negative delta to zero`() {
        assertEquals(
            "Alarm in 0 Minuten",
            NextAlarmCalculator.formatCountdown(1_000L, 500L)
        )
    }
}
