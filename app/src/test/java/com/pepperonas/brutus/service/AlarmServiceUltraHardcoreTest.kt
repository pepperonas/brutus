package com.pepperonas.brutus.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import com.pepperonas.brutus.data.AlarmDatabase
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.scheduler.AlarmScheduler
import com.pepperonas.brutus.util.RingingStore
import com.pepperonas.brutus.util.Storage
import com.pepperonas.brutus.util.UltraHardcoreStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ServiceController
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowLooper
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The real service, driven like AlarmReceiver and AlarmActivity drive it. What must hold:
 * snoozing is not dismissing — only a completed challenge arms the Ultra Hardcore follow-ups.
 * Before v2.3.1 snooze went through the same stop path and armed them, so a snoozed UHC alarm
 * rang three times and demanded the step task although nothing had been dismissed.
 */
@RunWith(RobolectricTestRunner::class)
class AlarmServiceUltraHardcoreTest {

    private lateinit var context: Context
    private lateinit var db: AlarmDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        AlarmDatabase.resetInstanceForTests()
        db = AlarmDatabase.getInstance(context)
        listOf("brutus_ultra_hardcore", "brutus_ringing").forEach { Storage.prefs(context, it).edit().clear().commit() }
    }

    private val started = mutableListOf<AlarmService>()

    /**
     * The service finishes its work on IO before stopping itself. Let every session end, or its
     * coroutine outlives the test and trips over the next test's database.
     */
    @After
    fun tearDown() {
        started.forEach { svc ->
            svc.onStartCommand(Intent(context, AlarmService::class.java).setAction(AlarmService.ACTION_STOP), 0, 99)
            waitFor("service stopped") { org.robolectric.Shadows.shadowOf(svc).isStoppedBySelf }
        }
        Thread.sleep(100)
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        val until = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < until) {
            ShadowLooper.idleMainLooper()
            if (condition()) return
            Thread.sleep(20)
        }
        fail("timed out waiting for: $what")
    }

    private fun ring(alarm: AlarmEntity): ServiceController<AlarmService> {
        val start = Intent(context, AlarmService::class.java)
            .setAction(AlarmService.ACTION_START)
            .putExtra("alarm_id", alarm.id)
        val controller = Robolectric.buildService(AlarmService::class.java, start).create().startCommand(0, 1)
        started += controller.get()
        // A one-shot alarm disables itself once the service has loaded it: the session is live.
        waitFor("alarm loaded") { runBlocking { db.alarmDao().getById(alarm.id)?.enabled == false } }
        return controller
    }

    private fun uhcAlarm(): AlarmEntity = runBlocking {
        val a = AlarmEntity(hour = 7, minute = 0, enabled = true, ultraHardcoreMode = true, snoozeDuration = 5)
        a.copy(id = db.alarmDao().insert(a))
    }

    @Test
    fun `snoozing an Ultra Hardcore alarm does not arm the follow-ups`() {
        val alarm = uhcAlarm()
        val service = ring(alarm).get()

        service.onStartCommand(
            Intent(context, AlarmService::class.java).setAction(AlarmService.ACTION_SNOOZE).putExtra("alarm_id", alarm.id),
            0, 2,
        )
        waitFor("snooze registered") { RingingStore.snoozes(context).isNotEmpty() }

        assertTrue(UltraHardcoreStore.listPending(context).isEmpty(), "a snooze is not a dismissal")
    }

    @Test
    fun `dismissing an Ultra Hardcore alarm arms both follow-ups`() {
        val alarm = uhcAlarm()
        val service = ring(alarm).get()

        service.onStartCommand(Intent(context, AlarmService::class.java).setAction(AlarmService.ACTION_STOP), 0, 2)
        waitFor("follow-ups armed") { UltraHardcoreStore.listPending(context).size == 2 }

        assertEquals(setOf(1, 2), UltraHardcoreStore.listPending(context).map { it.seq }.toSet())
        assertTrue(RingingStore.snoozes(context).isEmpty())
    }

    @Test
    fun `the alarm volume is restored — also by a restart after the process was killed mid-ring`() {
        val audio = context.getSystemService(AudioManager::class.java)
        audio.setStreamVolume(AudioManager.STREAM_ALARM, 2, 0)
        val alarm = uhcAlarm().copy(ultraHardcoreMode = false)
        runBlocking { db.alarmDao().update(alarm) }
        ring(alarm)
        assertEquals(audio.getStreamMaxVolume(AudioManager.STREAM_ALARM), audio.getStreamVolume(AudioManager.STREAM_ALARM))

        // The process dies; the system restarts the sticky service with a null intent.
        val fresh = Robolectric.buildService(AlarmService::class.java).create().get()
        fresh.onStartCommand(null, 0, 1)

        assertEquals(2, audio.getStreamVolume(AudioManager.STREAM_ALARM))
    }

    @Test
    fun `a fired snooze clears its reboot record`() {
        val alarm = uhcAlarm()
        RingingStore.recordSnooze(context, RingingStore.Snooze(alarm.id, 1L, false, 0))
        val start = Intent(context, AlarmService::class.java)
            .setAction(AlarmService.ACTION_START)
            .putExtra("alarm_id", alarm.id)
            .putExtra(AlarmScheduler.EXTRA_IS_SNOOZE, true)
        started += Robolectric.buildService(AlarmService::class.java, start).create().startCommand(0, 1).get()
        waitFor("alarm loaded") { runBlocking { db.alarmDao().getById(alarm.id)?.enabled == false } }

        assertTrue(RingingStore.snoozes(context).isEmpty())
    }
}
