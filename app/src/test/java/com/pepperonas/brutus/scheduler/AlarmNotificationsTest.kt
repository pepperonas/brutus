package com.pepperonas.brutus.scheduler

import android.app.AlarmManager
import android.app.Application
import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pepperonas.brutus.BrutusApplication
import com.pepperonas.brutus.TestAlarmActivity
import com.pepperonas.brutus.data.AlarmDatabase
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.receiver.NotificationActionReceiver
import com.pepperonas.brutus.util.AlarmNotifier
import com.pepperonas.brutus.util.AppSettings
import com.pepperonas.brutus.util.RingingStore
import com.pepperonas.brutus.util.Storage
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Snooze countdown, the heads-up before an alarm with "dismiss early", and missed alarms. */
@RunWith(RobolectricTestRunner::class)
class AlarmNotificationsTest {

    private lateinit var context: Context
    private lateinit var db: AlarmDatabase
    private lateinit var alarmManager: AlarmManager
    private lateinit var nm: NotificationManager
    private val minute = 60_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        alarmManager = context.getSystemService(AlarmManager::class.java)
        nm = context.getSystemService(NotificationManager::class.java)
        AlarmDatabase.resetInstanceForTests()
        db = AlarmDatabase.getInstance(context)
        listOf("brutus_ringing", "brutus_settings").forEach { Storage.prefs(context, it).edit().clear().commit() }
    }

    private fun insert(alarm: AlarmEntity) = runBlocking { alarm.copy(id = db.alarmDao().insert(alarm)) }
    private fun code(a: ShadowAlarmManager.ScheduledAlarm) = shadowOf(a.operation).requestCode
    private fun notification(id: Int): Notification? = shadowOf(nm).getNotification(id)
    private fun actionTitles(n: Notification) = n.actions.orEmpty().map { it.title.toString() }

    // ---- snooze countdown ---------------------------------------------------------------------

    @Test
    fun `a snooze shows a countdown to when it rings again`() {
        val alarm = insert(AlarmEntity(hour = 7, minute = 0, snoozeDuration = 5))
        val at = AlarmScheduler.scheduleSnooze(context, alarm)

        val n = assertNotNull(notification(AlarmNotifier.snoozeId(alarm.id)))
        assertEquals(BrutusApplication.CHANNEL_UPCOMING, n.channelId)
        assertEquals(at, n.`when`)
        assertTrue(n.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
    }

    @Test
    fun `only a normal alarm offers to cancel its snooze`() {
        val soft = insert(AlarmEntity(hour = 7, minute = 0))
        val hard = insert(AlarmEntity(hour = 8, minute = 0, hardcoreMode = true))
        AlarmScheduler.scheduleSnooze(context, soft)
        AlarmScheduler.scheduleSnooze(context, hard)

        assertEquals(1, notification(AlarmNotifier.snoozeId(soft.id))!!.actions.size)
        assertTrue(notification(AlarmNotifier.snoozeId(hard.id))!!.actions.isNullOrEmpty(), "Hardcore: no one-tap way out")
    }

    @Test
    fun `cancelling the snooze removes its registration and its countdown`() {
        val alarm = insert(AlarmEntity(hour = 7, minute = 0))
        AlarmScheduler.scheduleSnooze(context, alarm)

        AlarmActions.cancelSnooze(context, alarm.id)

        assertNull(notification(AlarmNotifier.snoozeId(alarm.id)))
        assertTrue(shadowOf(alarmManager).scheduledAlarms.none { code(it) == AlarmScheduler.snoozeRequestCode(alarm.id) })
    }

    // ---- heads-up before the alarm ------------------------------------------------------------

    private fun upcoming() = shadowOf(alarmManager).scheduledAlarms.filter {
        shadowOf(it.operation).savedIntent.getBooleanExtra(AlarmScheduler.EXTRA_IS_UPCOMING, false)
    }

    @Test
    fun `the heads-up is armed the configured lead before the alarm`() {
        val alarm = insert(AlarmEntity(hour = 7, minute = 0, repeatDays = 0b1111111))
        AppSettings.setUpcomingLeadMinutes(context, 30)
        AlarmScheduler.schedule(context, alarm)

        val main = shadowOf(alarmManager).scheduledAlarms.single { it.alarmClockInfo != null }.triggerAtMs
        val heads = upcoming().single()
        assertEquals(main - 30 * minute, heads.triggerAtMs)
        assertEquals(AlarmScheduler.upcomingRequestCode(alarm.id), code(heads))
    }

    @Test
    fun `switched off, no heads-up is armed`() {
        AppSettings.setUpcomingLeadMinutes(context, 0)
        AlarmScheduler.schedule(context, insert(AlarmEntity(hour = 7, minute = 0)))
        assertTrue(upcoming().isEmpty())
    }

    @Test
    fun `a normal alarm's heads-up dismisses it with one tap, a Hardcore one opens its challenge`() {
        val soft = insert(AlarmEntity(hour = 7, minute = 0))
        val hard = insert(AlarmEntity(hour = 8, minute = 0, hardcoreMode = true))
        AlarmNotifier.postUpcoming(context, soft, System.currentTimeMillis() + 60 * minute)
        AlarmNotifier.postUpcoming(context, hard, System.currentTimeMillis() + 60 * minute)

        val softIntent = shadowOf(notification(AlarmNotifier.upcomingId(soft.id))!!.actions.single().actionIntent).savedIntent
        assertEquals(NotificationActionReceiver.ACTION_SKIP_NEXT, softIntent.action)

        val hardIntent = shadowOf(notification(AlarmNotifier.upcomingId(hard.id))!!.actions.single().actionIntent).savedIntent
        assertEquals(TestAlarmActivity::class.java.name, hardIntent.component!!.className)
        assertEquals(hard.id, hardIntent.getLongExtra(TestAlarmActivity.EXTRA_EARLY_DISMISS_ALARM_ID, -1))
    }

    @Test
    fun `dismissing early moves a repeating alarm to its occurrence after — and a reboot keeps it there`() {
        val alarm = insert(AlarmEntity(hour = 7, minute = 0, repeatDays = 0b1111111))
        AlarmScheduler.schedule(context, alarm)
        val first = shadowOf(alarmManager).scheduledAlarms.single { it.alarmClockInfo != null }.triggerAtMs

        runBlocking { assertTrue(AlarmActions.skipNext(context, alarm.id, solvedChallenge = false)) }
        runBlocking { Rescheduler.rescheduleAll(context) }

        val now = shadowOf(alarmManager).scheduledAlarms.single { it.alarmClockInfo != null }.triggerAtMs
        assertEquals(first + 24 * 60 * minute, now, "the day after, not the skipped one")
    }

    @Test
    fun `dismissing a one-shot alarm early switches it off`() {
        val alarm = insert(AlarmEntity(hour = 7, minute = 0, repeatDays = 0))
        AlarmScheduler.schedule(context, alarm)

        runBlocking { AlarmActions.skipNext(context, alarm.id, solvedChallenge = false) }

        assertFalse(runBlocking { db.alarmDao().getById(alarm.id)!!.enabled })
        assertTrue(shadowOf(alarmManager).scheduledAlarms.none { it.alarmClockInfo != null })
    }

    @Test
    fun `a Hardcore alarm cannot be dismissed early without solving its challenge`() {
        val alarm = insert(AlarmEntity(hour = 7, minute = 0, repeatDays = 0b1111111, hardcoreMode = true))
        AlarmScheduler.schedule(context, alarm)
        val first = shadowOf(alarmManager).scheduledAlarms.single { it.alarmClockInfo != null }.triggerAtMs

        runBlocking { assertFalse(AlarmActions.skipNext(context, alarm.id, solvedChallenge = false)) }
        assertEquals(first, shadowOf(alarmManager).scheduledAlarms.single { it.alarmClockInfo != null }.triggerAtMs)

        runBlocking { assertTrue(AlarmActions.skipNext(context, alarm.id, solvedChallenge = true)) }
        assertNotEquals(first, shadowOf(alarmManager).scheduledAlarms.single { it.alarmClockInfo != null }.triggerAtMs)
    }

    // ---- missed alarms ------------------------------------------------------------------------

    @Test
    fun `an occurrence that never rang is reported after the reboot`() {
        val alarm = insert(AlarmEntity(hour = 7, minute = 0, repeatDays = 0b1111111))
        val due = System.currentTimeMillis() - 60 * minute
        RingingStore.setExpected(context, alarm.id, due)

        runBlocking { Rescheduler.rescheduleAll(context) }

        val n = assertNotNull(notification(AlarmNotifier.missedId(alarm.id)))
        assertEquals(BrutusApplication.CHANNEL_MISSED, n.channelId)
        assertEquals(due, n.`when`)
        assertTrue(RingingStore.expected(context).getValue(alarm.id) > System.currentTimeMillis(), "re-armed")
    }

    @Test
    fun `an alarm that is ringing right now is not called missed`() {
        val alarm = insert(AlarmEntity(hour = 7, minute = 0, repeatDays = 0b1111111))
        RingingStore.setExpected(context, alarm.id, System.currentTimeMillis() - 30_000L)

        runBlocking { Rescheduler.rescheduleAll(context) }

        assertNull(notification(AlarmNotifier.missedId(alarm.id)))
    }

    @Test
    fun `the missed notice is given once, not on every app start`() {
        val alarm = insert(AlarmEntity(hour = 7, minute = 0, repeatDays = 0b1111111))
        RingingStore.setExpected(context, alarm.id, System.currentTimeMillis() - 60 * minute)
        runBlocking { Rescheduler.rescheduleAll(context) }
        nm.cancelAll()

        runBlocking { Rescheduler.rescheduleAll(context) }

        assertNull(notification(AlarmNotifier.missedId(alarm.id)))
    }
}
