package com.pepperonas.brutus.util

import com.pepperonas.brutus.data.AlarmEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Calendar arithmetic around the dates that break alarm clocks: the two DST
 * switches, month/year rollovers and February 29th. All of it is pinned in
 * Europe/Berlin, because "it works on my machine in UTC" is precisely how
 * these bugs ship.
 *
 * The user-visible contract is: **the alarm keeps its wall-clock time**. On the
 * short night that means 23 real hours between two triggers, on the long one 25.
 */
class NextAlarmCalendarEdgeTest {

    private lateinit var previousZone: TimeZone
    private val berlin = TimeZone.getTimeZone("Europe/Berlin")
    private val hourMs = 3_600_000L

    @Before
    fun setUp() {
        previousZone = TimeZone.getDefault()
        TimeZone.setDefault(berlin)
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(previousZone)
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(berlin).apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
        }.timeInMillis

    private fun fieldsOf(millis: Long): Calendar =
        Calendar.getInstance(berlin).apply { timeInMillis = millis }

    private fun daily(hour: Int, minute: Int = 0) =
        AlarmEntity(id = 1, hour = hour, minute = minute, repeatDays = 0x7F)

    private fun oneShot(hour: Int, minute: Int) =
        AlarmEntity(id = 1, hour = hour, minute = minute, repeatDays = 0)

    // ---- daylight saving -------------------------------------------------

    @Test
    fun `spring forward — consecutive daily triggers are 23 real hours apart`() {
        // Night of 2026-03-29: 02:00 CET jumps to 03:00 CEST.
        val alarm = daily(6)
        val saturday = assertNotNull(nextOf(alarm, at(2026, 3, 28, 5, 0)))
        val sunday = assertNotNull(nextOf(alarm, at(2026, 3, 28, 6, 30)))

        assertEquals(23 * hourMs, sunday - saturday)
        // …and both still ring at six in the morning.
        assertEquals(6, fieldsOf(saturday).get(Calendar.HOUR_OF_DAY))
        assertEquals(6, fieldsOf(sunday).get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `fall back — consecutive daily triggers are 25 real hours apart`() {
        // Night of 2026-10-25: 03:00 CEST falls back to 02:00 CET.
        val alarm = daily(6)
        val saturday = assertNotNull(nextOf(alarm, at(2026, 10, 24, 5, 0)))
        val sunday = assertNotNull(nextOf(alarm, at(2026, 10, 24, 6, 30)))

        assertEquals(25 * hourMs, sunday - saturday)
        assertEquals(6, fieldsOf(sunday).get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `an alarm set inside the skipped hour still fires, shifted forward`() {
        // 02:30 does not exist on 2026-03-29. It must not silently vanish.
        val trigger = assertNotNull(nextOf(oneShot(2, 30), at(2026, 3, 29, 0, 30)))

        assertTrue(trigger > at(2026, 3, 29, 0, 30))
        val c = fieldsOf(trigger)
        assertEquals(29, c.get(Calendar.DAY_OF_MONTH), "must stay on the same night")
        assertEquals(3, c.get(Calendar.HOUR_OF_DAY), "02:30 CET resolves to 03:30 CEST")
    }

    @Test
    fun `an alarm inside the repeated hour fires once, on the first pass`() {
        // 02:30 happens twice on 2026-10-25 — the earlier (CEST) one wins.
        val trigger = assertNotNull(nextOf(oneShot(2, 30), at(2026, 10, 25, 0, 30)))

        val c = fieldsOf(trigger)
        assertEquals(25, c.get(Calendar.DAY_OF_MONTH))
        assertEquals(2, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, c.get(Calendar.MINUTE))
        // Java resolves the ambiguous wall time to standard time, i.e. the
        // *second* pass of 02:30 — three hours after 00:30 CEST, not two.
        // Pinned so a JDK/CLDR change that flips this becomes visible instead
        // of quietly moving the alarm by an hour.
        assertEquals(3 * hourMs, trigger - at(2026, 10, 25, 0, 30))
    }

    // ---- calendar rollovers ---------------------------------------------

    @Test
    fun `a one-shot alarm rolls into the next month`() {
        val trigger = assertNotNull(nextOf(oneShot(0, 30), at(2026, 1, 31, 23, 0)))

        val c = fieldsOf(trigger)
        assertEquals(2026, c.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, c.get(Calendar.MONTH))
        assertEquals(1, c.get(Calendar.DAY_OF_MONTH))
        assertEquals(0 to 30, c.get(Calendar.HOUR_OF_DAY) to c.get(Calendar.MINUTE))
    }

    @Test
    fun `a one-shot alarm rolls into the next year`() {
        val trigger = assertNotNull(nextOf(oneShot(0, 5), at(2026, 12, 31, 23, 50)))

        val c = fieldsOf(trigger)
        assertEquals(2027, c.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, c.get(Calendar.MONTH))
        assertEquals(1, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `a daily alarm lands on February 29th in a leap year`() {
        val trigger = assertNotNull(nextOf(daily(6), at(2028, 2, 28, 7, 0)))

        val c = fieldsOf(trigger)
        assertEquals(Calendar.FEBRUARY, c.get(Calendar.MONTH))
        assertEquals(29, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `a weekly alarm crosses the month boundary to its weekday`() {
        // Monday-only, evaluated on Tuesday 2026-06-30.
        val mondayOnly = AlarmEntity(id = 1, hour = 6, minute = 0, repeatDays = 1 shl 0)
        val trigger = assertNotNull(nextOf(mondayOnly, at(2026, 6, 30, 12, 0)))

        val c = fieldsOf(trigger)
        assertEquals(Calendar.MONDAY, c.get(Calendar.DAY_OF_WEEK))
        assertEquals(Calendar.JULY, c.get(Calendar.MONTH))
        assertEquals(6, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `a weekly alarm is never more than seven days out`() {
        val everyWeekday = (0..4).fold(0) { acc, d -> acc or (1 shl d) }
        val alarm = AlarmEntity(id = 1, hour = 6, minute = 0, repeatDays = everyWeekday)

        // Every day of one week, from Monday through Sunday.
        (1..7).forEach { day ->
            val now = at(2026, 6, day, 12, 0)
            val trigger = assertNotNull(nextOf(alarm, now), "no trigger found from day $day")
            assertTrue(trigger > now)
            assertTrue(
                trigger - now <= 7 * 24 * hourMs,
                "workday alarm scheduled more than a week out from day $day"
            )
            val weekday = fieldsOf(trigger).get(Calendar.DAY_OF_WEEK)
            assertTrue(
                weekday !in setOf(Calendar.SATURDAY, Calendar.SUNDAY),
                "workday alarm landed on the weekend"
            )
        }
    }

    @Test
    fun `findNext picks the soonest alarm across a DST night`() {
        val early = daily(6).copy(id = 1)
        val late = daily(7).copy(id = 2)
        val now = at(2026, 3, 28, 6, 30)   // after today's 06:00

        assertEquals(late.id, NextAlarmCalculator.findNext(listOf(early, late), now)?.id)
    }

    @Test
    fun `disabled alarms are ignored even when they would be sooner`() {
        val disabledEarly = daily(6).copy(id = 1, enabled = false)
        val enabledLate = daily(9).copy(id = 2)
        val now = at(2026, 6, 1, 5, 0)

        assertEquals(enabledLate.id, NextAlarmCalculator.findNext(listOf(disabledEarly, enabledLate), now)?.id)
    }

    private fun nextOf(alarm: AlarmEntity, now: Long): Long? =
        NextAlarmCalculator.nextTrigger(alarm, now)
}
