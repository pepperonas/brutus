package com.pepperonas.brutus.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.pepperonas.brutus.BrutusApplication
import com.pepperonas.brutus.data.AlarmDatabase
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.util.Storage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowLooper
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * On an unlocked phone Android shows the alarm notification — it carries a full-screen intent — as a
 * pinned heads-up, right over the alarm screen's clock. While that screen is in front the service runs
 * on a quiet notification instead, and switches back the moment the screen leaves.
 */
@RunWith(RobolectricTestRunner::class)
class AlarmServiceScreenNotificationTest {

    private lateinit var context: Context
    private lateinit var db: AlarmDatabase
    private lateinit var nm: NotificationManager
    private val started = mutableListOf<AlarmService>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        AlarmDatabase.resetInstanceForTests()
        db = AlarmDatabase.getInstance(context)
        listOf("brutus_ultra_hardcore", "brutus_ringing").forEach { Storage.prefs(context, it).edit().clear().commit() }
        nm = context.getSystemService(NotificationManager::class.java)
    }

    @After
    fun tearDown() {
        started.forEach { svc ->
            svc.onStartCommand(Intent(context, AlarmService::class.java).setAction(AlarmService.ACTION_STOP), 0, 99)
            waitFor("service stopped") { Shadows.shadowOf(svc).isStoppedBySelf }
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

    private fun ring(): AlarmService {
        val alarm = runBlocking {
            val a = AlarmEntity(hour = 7, minute = 0, enabled = true)
            a.copy(id = db.alarmDao().insert(a))
        }
        val start = Intent(context, AlarmService::class.java)
            .setAction(AlarmService.ACTION_START).putExtra("alarm_id", alarm.id)
        val svc = Robolectric.buildService(AlarmService::class.java, start).create().startCommand(0, 1).get()
        started += svc
        waitFor("alarm loaded") { runBlocking { db.alarmDao().getById(alarm.id)?.enabled == false } }
        return svc
    }

    private fun send(svc: AlarmService, action: String) =
        svc.onStartCommand(Intent(context, AlarmService::class.java).setAction(action), 0, 2)

    private fun posted(id: Int) = Shadows.shadowOf(nm).getNotification(id)

    @Test
    fun `a ringing alarm starts on the loud notification with a full-screen intent`() {
        ring()
        val loud = assertNotNull(posted(AlarmService.NOTIFICATION_ID))
        assertNotNull(loud.fullScreenIntent)
        assertEquals(BrutusApplication.CHANNEL_ALARM, loud.channelId)
    }

    @Test
    fun `while the alarm screen is in front the pinned heads-up gives way to a quiet notification`() {
        val svc = ring()
        send(svc, AlarmService.ACTION_SCREEN_SHOWN)

        assertNull(posted(AlarmService.NOTIFICATION_ID), "the heads-up would still cover the clock")
        val quiet = assertNotNull(posted(AlarmService.NOTIFICATION_ID_QUIET))
        assertNull(quiet.fullScreenIntent)
        assertEquals(BrutusApplication.CHANNEL_SERVICE, quiet.channelId)
        assertNotNull(quiet.contentIntent, "tapping it still leads back to the alarm screen")
        assertEquals(AlarmService.NOTIFICATION_ID_QUIET, Shadows.shadowOf(svc).lastForegroundNotificationId)
    }

    @Test
    fun `when the alarm screen leaves, the loud notification comes back`() {
        val svc = ring()
        send(svc, AlarmService.ACTION_SCREEN_SHOWN)
        send(svc, AlarmService.ACTION_SCREEN_HIDDEN)

        assertNull(posted(AlarmService.NOTIFICATION_ID_QUIET))
        assertNotNull(assertNotNull(posted(AlarmService.NOTIFICATION_ID)).fullScreenIntent)
        assertEquals(AlarmService.NOTIFICATION_ID, Shadows.shadowOf(svc).lastForegroundNotificationId)
    }

    @Test
    fun `dismissing removes both notifications`() {
        val svc = ring()
        send(svc, AlarmService.ACTION_SCREEN_SHOWN)
        svc.onStartCommand(Intent(context, AlarmService::class.java).setAction(AlarmService.ACTION_STOP), 0, 3)
        waitFor("stopped") { Shadows.shadowOf(svc).isStoppedBySelf }
        started.remove(svc)
        assertNull(posted(AlarmService.NOTIFICATION_ID))
        assertNull(posted(AlarmService.NOTIFICATION_ID_QUIET))
    }

    @Test
    fun `a screen report without a ringing session does not leave a started service behind`() {
        val svc = Robolectric.buildService(AlarmService::class.java).create().get()
        send(svc, AlarmService.ACTION_SCREEN_SHOWN)
        assertTrue(Shadows.shadowOf(svc).isStoppedBySelf)
        assertNull(posted(AlarmService.NOTIFICATION_ID_QUIET))
    }
}
