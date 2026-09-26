package com.pepperonas.brutus.scheduler

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pepperonas.brutus.data.AlarmDatabase
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.util.RingingStore
import com.pepperonas.brutus.util.Storage
import com.pepperonas.brutus.util.UltraHardcoreNotifier
import com.pepperonas.brutus.util.UltraHardcoreStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import java.util.Calendar
import java.util.TimeZone
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The one function every "AlarmManager may be wrong now" event calls: reboot, time-zone or clock
 * change, exact-alarm permission re-granted, app update, returning to the app.
 */
@RunWith(RobolectricTestRunner::class)
class ReschedulerTest {

    private lateinit var context: Context
    private lateinit var db: AlarmDatabase
    private lateinit var alarmManager: AlarmManager
    private val originalZone = TimeZone.getDefault()
    private val minute = 60_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        alarmManager = context.getSystemService(AlarmManager::class.java)
        AlarmDatabase.resetInstanceForTests()
        db = AlarmDatabase.getInstance(context)
        listOf("brutus_ultra_hardcore", "brutus_ringing").forEach { Storage.prefs(context, it).edit().clear().commit() }
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalZone)
    }

    private fun insert(alarm: AlarmEntity): AlarmEntity = runBlocking {
        alarm.copy(id = db.alarmDao().insert(alarm))
    }

    private fun scheduled() = shadowOf(alarmManager).scheduledAlarms
    private fun code(a: ShadowAlarmManager.ScheduledAlarm) = shadowOf(a.operation).requestCode

    @Test
    fun `enabled alarms are registered, disabled ones are not`() {
        val on = insert(AlarmEntity(hour = 7, minute = 0, enabled = true))
        insert(AlarmEntity(hour = 8, minute = 0, enabled = false))

        runBlocking { Rescheduler.rescheduleAll(context) }

        assertEquals(listOf(on.id.toInt()), scheduled().filter { it.alarmClockInfo != null }.map(::code))
    }

    @Test
    fun `a time-zone change moves the registration to the same wall-clock time in the new zone`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
        insert(AlarmEntity(hour = 7, minute = 0, enabled = true))
        runBlocking { Rescheduler.rescheduleAll(context) }

        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"))
        runBlocking { Rescheduler.rescheduleAll(context) }

        val trigger = scheduled().single { it.alarmClockInfo != null }.triggerAtMs
        val london = Calendar.getInstance(TimeZone.getTimeZone("Europe/London")).apply { timeInMillis = trigger }
        assertEquals(7, london.get(Calendar.HOUR_OF_DAY), "07:00 must stay 07:00 after flying to London")
    }

    @Test
    fun `follow-ups of a one-shot alarm survive a reboot although the alarm disabled itself`() {
        // A one-shot alarm is switched off the moment it fires; its follow-ups are exactly what
        // must survive a reboot right after dismissing it.
        val alarm = insert(AlarmEntity(hour = 7, minute = 0, enabled = false, ultraHardcoreMode = true))
        val at = System.currentTimeMillis() + 10 * minute
        UltraHardcoreStore.recordFollowup(context, alarm.id, 1, at)

        runBlocking { Rescheduler.rescheduleAll(context) }

        assertNotNull(scheduled().singleOrNull { it.triggerAtMs == at }, "follow-up re-armed")
        val nm = context.getSystemService(NotificationManager::class.java)
        assertNotNull(
            shadowOf(nm).getNotification(UltraHardcoreNotifier.notificationId(alarm.id)),
            "the reminder — the only way into the anti-snooze task — is back"
        )
    }

    @Test
    fun `expired follow-ups and follow-ups of deleted alarms are dropped`() {
        val alarm = insert(AlarmEntity(hour = 7, minute = 0, ultraHardcoreMode = true))
        UltraHardcoreStore.recordFollowup(context, alarm.id, 1, System.currentTimeMillis() - minute)
        UltraHardcoreStore.recordFollowup(context, 999L, 1, System.currentTimeMillis() + minute)

        runBlocking { Rescheduler.rescheduleAll(context) }

        assertTrue(UltraHardcoreStore.listPending(context).isEmpty())
    }

    @Test
    fun `a remembered snooze is re-armed, an expired one forgotten`() {
        val alarm = insert(AlarmEntity(hour = 7, minute = 0, enabled = false))
        val future = System.currentTimeMillis() + 5 * minute
        RingingStore.recordSnooze(context, RingingStore.Snooze(alarm.id, future, false, 0))
        RingingStore.recordSnooze(context, RingingStore.Snooze(12345L, future, false, 0)) // alarm gone

        runBlocking { Rescheduler.rescheduleAll(context) }

        val snooze = scheduled().single { code(it) == AlarmScheduler.snoozeRequestCode(alarm.id) }
        assertEquals(future, snooze.triggerAtMs)
        assertEquals(listOf(alarm.id), RingingStore.snoozes(context).map { it.alarmId })
    }

    @Test
    fun `running it twice never stacks registrations`() {
        insert(AlarmEntity(hour = 7, minute = 0, enabled = true, sunriseEnabled = true))
        runBlocking { Rescheduler.rescheduleAll(context) }
        val first = scheduled().size
        runBlocking { Rescheduler.rescheduleAll(context) }
        assertEquals(first, scheduled().size)
    }
}
