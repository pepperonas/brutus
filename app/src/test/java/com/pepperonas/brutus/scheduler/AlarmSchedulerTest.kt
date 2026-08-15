package com.pepperonas.brutus.scheduler

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pepperonas.brutus.data.AlarmEntity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import java.util.Calendar
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What actually lands in AlarmManager. Everything else in the app is decoration
 * around these registrations: if the trigger time, the request code or the
 * cancel path is wrong, the alarm does not ring — and nothing in the UI says so.
 *
 * Robolectric's ShadowAlarmManager records every registration, and PendingIntent
 * equality (request code + Intent.filterEquals, extras excluded) is exactly the
 * mechanism the production code relies on to keep main / sunrise / follow-up
 * registrations apart. That makes the collision behavior observable here.
 */
@RunWith(RobolectricTestRunner::class)
class AlarmSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager

    private val minute = 60_000L
    private val sunriseLead = AlarmScheduler.SUNRISE_LEAD_MIN * minute

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(AlarmManager::class.java)
    }

    private fun scheduled(): List<ShadowAlarmManager.ScheduledAlarm> =
        shadowOf(alarmManager).scheduledAlarms

    /** The main registration is the only one carrying AlarmClockInfo. */
    private fun mainAlarm() = scheduled().singleOrNull { it.alarmClockInfo != null }

    /** The sunrise pre-alarm is the exact-and-allow-while-idle one. */
    private fun sunriseAlarm() = scheduled().singleOrNull { it.alarmClockInfo == null }

    private fun alarmAt(hour: Int, minute: Int, id: Long = 1L) =
        AlarmEntity(id = id, hour = hour, minute = minute)

    /** Wall-clock hour/minute of a timestamp in the default zone. */
    private fun hourMinuteOf(millis: Long): Pair<Int, Int> {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return c.get(Calendar.HOUR_OF_DAY) to c.get(Calendar.MINUTE)
    }

    private fun weekdayOf(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_WEEK)

    private fun clockPlus(offsetMillis: Long): Pair<Int, Int> =
        hourMinuteOf(System.currentTimeMillis() + offsetMillis)

    // ---- the main registration ------------------------------------------

    @Test
    fun `scheduling an alarm registers exactly one alarm clock`() {
        AlarmScheduler.schedule(context, alarmAt(7, 30))

        assertEquals(1, scheduled().size)
        val info = assertNotNull(mainAlarm()?.alarmClockInfo)
        assertEquals(mainAlarm()!!.triggerAtMs, info.triggerTime)
    }

    @Test
    fun `the trigger lands on the configured wall-clock time, in the future`() {
        val (h, m) = clockPlus(90 * minute)
        AlarmScheduler.schedule(context, alarmAt(h, m))

        val trigger = mainAlarm()!!.triggerAtMs
        assertTrue(trigger > System.currentTimeMillis(), "alarm must be scheduled ahead of now")
        assertEquals(h to m, hourMinuteOf(trigger))
    }

    @Test
    fun `an alarm whose time already passed today is pushed to tomorrow`() {
        val (h, m) = clockPlus(-2 * 60 * minute)
        AlarmScheduler.schedule(context, alarmAt(h, m))

        val now = System.currentTimeMillis()
        val trigger = mainAlarm()!!.triggerAtMs
        assertTrue(trigger > now, "a passed time must not schedule in the past")
        assertTrue(
            trigger <= now + 24 * 60 * minute,
            "…and must not skip a whole day either"
        )
        assertEquals(h to m, hourMinuteOf(trigger))
    }

    @Test
    fun `a repeating alarm lands on one of its enabled weekdays`() {
        // Monday + Thursday.
        val alarm = alarmAt(6, 15).copy(repeatDays = (1 shl 0) or (1 shl 3))
        AlarmScheduler.schedule(context, alarm)

        val trigger = mainAlarm()!!.triggerAtMs
        assertTrue(trigger > System.currentTimeMillis())
        assertTrue(
            weekdayOf(trigger) in setOf(Calendar.MONDAY, Calendar.THURSDAY),
            "repeating alarm landed on weekday ${weekdayOf(trigger)}"
        )
        assertEquals(6 to 15, hourMinuteOf(trigger))
    }

    @Test
    fun `a daily alarm is never scheduled more than a day out`() {
        val alarm = alarmAt(6, 15).copy(repeatDays = 0x7F)
        AlarmScheduler.schedule(context, alarm)

        val trigger = mainAlarm()!!.triggerAtMs
        assertTrue(trigger - System.currentTimeMillis() <= 24 * 60 * minute)
    }

    @Test
    fun `two alarms occupy two independent registrations`() {
        AlarmScheduler.schedule(context, alarmAt(7, 0, id = 1L))
        AlarmScheduler.schedule(context, alarmAt(8, 0, id = 2L))

        assertEquals(2, scheduled().size)
    }

    @Test
    fun `re-scheduling the same alarm replaces its registration instead of stacking`() {
        val alarm = alarmAt(7, 0, id = 1L)
        AlarmScheduler.schedule(context, alarm)
        AlarmScheduler.schedule(context, alarm.copy(hour = 9))

        assertEquals(1, scheduled().size)
        assertEquals(9, hourMinuteOf(mainAlarm()!!.triggerAtMs).first)
    }

    // ---- sunrise pre-alarm ----------------------------------------------

    @Test
    fun `sunrise arms a second registration exactly ten minutes earlier`() {
        val (h, m) = clockPlus(3 * 60 * minute)
        AlarmScheduler.schedule(context, alarmAt(h, m).copy(sunriseEnabled = true))

        assertEquals(2, scheduled().size)
        val main = assertNotNull(mainAlarm())
        val sunrise = assertNotNull(sunriseAlarm())
        assertEquals(main.triggerAtMs - sunriseLead, sunrise.triggerAtMs)
    }

    @Test
    fun `sunrise survives Doze — exact and allow-while-idle, wall-clock wakeup`() {
        val (h, m) = clockPlus(3 * 60 * minute)
        AlarmScheduler.schedule(context, alarmAt(h, m).copy(sunriseEnabled = true))

        val sunrise = assertNotNull(sunriseAlarm())
        assertTrue(sunrise.isAllowWhileIdle, "a doze-deferred sunrise is useless")
        assertEquals(AlarmManager.RTC_WAKEUP, sunrise.type)
    }

    @Test
    fun `no sunrise is armed when the alarm is closer than the lead time`() {
        val (h, m) = clockPlus(3 * minute)
        AlarmScheduler.schedule(context, alarmAt(h, m).copy(sunriseEnabled = true))

        assertEquals(1, scheduled().size, "a sunrise in the past must not be armed")
        assertNull(sunriseAlarm())
    }

    @Test
    fun `turning sunrise off clears the previously armed pre-alarm`() {
        // Regression guard for the stale-sunrise bug fixed in v1.8.0.
        val (h, m) = clockPlus(3 * 60 * minute)
        val alarm = alarmAt(h, m).copy(sunriseEnabled = true)
        AlarmScheduler.schedule(context, alarm)
        assertEquals(2, scheduled().size)

        AlarmScheduler.schedule(context, alarm.copy(sunriseEnabled = false))

        assertEquals(1, scheduled().size)
        assertNull(sunriseAlarm())
    }

    @Test
    fun `moving an alarm re-arms its sunrise rather than leaving the old one`() {
        val (h, m) = clockPlus(3 * 60 * minute)
        val alarm = alarmAt(h, m).copy(sunriseEnabled = true)
        AlarmScheduler.schedule(context, alarm)
        val firstSunrise = sunriseAlarm()!!.triggerAtMs

        val (h2, m2) = clockPlus(5 * 60 * minute)
        AlarmScheduler.schedule(context, alarm.copy(hour = h2, minute = m2))

        assertEquals(2, scheduled().size, "exactly one main + one sunrise may remain")
        val moved = assertNotNull(sunriseAlarm())
        assertTrue(moved.triggerAtMs != firstSunrise, "sunrise stayed at the old time")
        assertEquals(mainAlarm()!!.triggerAtMs - sunriseLead, moved.triggerAtMs)
    }

    @Test
    fun `cancel removes the alarm together with its sunrise`() {
        val (h, m) = clockPlus(3 * 60 * minute)
        val alarm = alarmAt(h, m).copy(sunriseEnabled = true)
        AlarmScheduler.schedule(context, alarm)

        AlarmScheduler.cancel(context, alarm)

        assertTrue(scheduled().isEmpty(), "leftovers: ${scheduled().size} registration(s)")
    }

    // ---- snooze ----------------------------------------------------------

    @Test
    fun `snooze re-arms the same alarm the configured number of minutes out`() {
        val alarm = alarmAt(7, 0).copy(snoozeDuration = 5)
        val before = System.currentTimeMillis()

        AlarmScheduler.scheduleSnooze(context, alarm)

        val trigger = assertNotNull(mainAlarm()).triggerAtMs
        assertTrue(trigger >= before + 5 * minute, "snooze fired too early")
        assertTrue(trigger <= System.currentTimeMillis() + 5 * minute, "snooze fired too late")
    }

    @Test
    fun `snoozing replaces the pending registration of that alarm`() {
        val alarm = alarmAt(7, 0).copy(snoozeDuration = 2)
        AlarmScheduler.schedule(context, alarm)
        AlarmScheduler.scheduleSnooze(context, alarm)

        assertEquals(1, scheduled().size, "the snooze must not stack onto the original")
        assertTrue(
            mainAlarm()!!.triggerAtMs <= System.currentTimeMillis() + 2 * minute
        )
    }

    @Test
    fun `every offered snooze interval schedules that far out`() {
        listOf(2, 5, 10, 15).forEach { minutes ->
            // Same alarm id ⇒ same PendingIntent ⇒ each round replaces the last.
            val before = System.currentTimeMillis()

            AlarmScheduler.scheduleSnooze(context, alarmAt(7, 0).copy(snoozeDuration = minutes))

            val trigger = assertNotNull(mainAlarm()).triggerAtMs
            assertTrue(
                trigger >= before + minutes * minute,
                "snooze of $minutes min was scheduled too early"
            )
        }
    }

    // ---- Ultra Hardcore follow-ups --------------------------------------

    @Test
    fun `both follow-ups are armed as separate registrations`() {
        val alarm = alarmAt(7, 0, id = 3L).copy(ultraHardcoreMode = true)
        val now = System.currentTimeMillis()

        AlarmScheduler.scheduleFollowup(context, alarm, seq = 1, triggerAt = now + 10 * minute)
        AlarmScheduler.scheduleFollowup(context, alarm, seq = 2, triggerAt = now + 15 * minute)

        val triggers = scheduled().map { it.triggerAtMs }.sorted()
        assertEquals(listOf(now + 10 * minute, now + 15 * minute), triggers)
    }

    @Test
    fun `a follow-up does not overwrite the alarm's own registration`() {
        // The request-code carve-out (0x4F……) exists precisely for this.
        val alarm = alarmAt(7, 0, id = 1L)
        AlarmScheduler.schedule(context, alarm)
        AlarmScheduler.scheduleFollowup(context, alarm, 1, System.currentTimeMillis() + 10 * minute)
        AlarmScheduler.scheduleFollowup(context, alarm, 2, System.currentTimeMillis() + 15 * minute)

        assertEquals(3, scheduled().size, "a request-code collision swallowed a registration")
    }

    @Test
    fun `sunrise, main alarm and both follow-ups coexist for one alarm id`() {
        val (h, m) = clockPlus(3 * 60 * minute)
        val alarm = alarmAt(h, m, id = 1L).copy(sunriseEnabled = true)
        AlarmScheduler.schedule(context, alarm)
        AlarmScheduler.scheduleFollowup(context, alarm, 1, System.currentTimeMillis() + 10 * minute)
        AlarmScheduler.scheduleFollowup(context, alarm, 2, System.currentTimeMillis() + 15 * minute)

        assertEquals(4, scheduled().size)
    }

    @Test
    fun `cancelling one follow-up leaves the other armed`() {
        val alarm = alarmAt(7, 0, id = 5L)
        val now = System.currentTimeMillis()
        AlarmScheduler.scheduleFollowup(context, alarm, 1, now + 10 * minute)
        AlarmScheduler.scheduleFollowup(context, alarm, 2, now + 15 * minute)

        AlarmScheduler.cancelFollowup(context, alarm.id, 1)

        assertEquals(listOf(now + 15 * minute), scheduled().map { it.triggerAtMs })
    }

    @Test
    fun `completing the anti-snooze task cancels every follow-up`() {
        val alarm = alarmAt(7, 0, id = 5L)
        val now = System.currentTimeMillis()
        AlarmScheduler.scheduleFollowup(context, alarm, 1, now + 10 * minute)
        AlarmScheduler.scheduleFollowup(context, alarm, 2, now + 15 * minute)

        AlarmScheduler.cancelAllFollowups(context, alarm.id)

        assertTrue(scheduled().isEmpty())
    }

    @Test
    fun `follow-ups of different alarms never cancel each other`() {
        val now = System.currentTimeMillis()
        AlarmScheduler.scheduleFollowup(context, alarmAt(7, 0, id = 1L), 1, now + 10 * minute)
        AlarmScheduler.scheduleFollowup(context, alarmAt(8, 0, id = 2L), 1, now + 11 * minute)

        AlarmScheduler.cancelAllFollowups(context, 1L)

        assertEquals(listOf(now + 11 * minute), scheduled().map { it.triggerAtMs })
    }

    @Test
    fun `cancelling the alarm leaves its follow-ups untouched — they are dismissed separately`() {
        val alarm = alarmAt(7, 0, id = 1L)
        val now = System.currentTimeMillis()
        AlarmScheduler.schedule(context, alarm)
        AlarmScheduler.scheduleFollowup(context, alarm, 1, now + 10 * minute)

        AlarmScheduler.cancel(context, alarm)

        // Documented behavior: toggling an alarm off does not silently disarm a
        // follow-up that is already chasing the user — AlarmService does that.
        assertEquals(listOf(now + 10 * minute), scheduled().map { it.triggerAtMs })
    }
}
