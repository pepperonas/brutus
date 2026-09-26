package com.pepperonas.brutus.timer

import android.app.AlarmManager
import android.app.Application
import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.pepperonas.brutus.util.Storage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The timer as a real alarm. Before v2.4.0 it lived only in a ViewModel: Back ended it silently,
 * and in the background it rang late or never.
 */
@RunWith(RobolectricTestRunner::class)
class TimerControllerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var nm: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        alarmManager = context.getSystemService(AlarmManager::class.java)
        nm = context.getSystemService(NotificationManager::class.java)
        Storage.prefs(context, "brutus_timer_state").edit().clear().commit()
    }

    private fun fireAlarm() = shadowOf(alarmManager).scheduledAlarms
        .singleOrNull { shadowOf(it.operation).requestCode == TimerController.REQUEST_FIRE }

    @Test
    fun `starting arms a wake-up at the end, in elapsed time, and shows the countdown`() {
        TimerController.start(context, 90_000L, now = 1_000L)

        val fire = assertNotNull(fireAlarm())
        assertEquals(AlarmManager.ELAPSED_REALTIME_WAKEUP, fire.type)
        assertEquals(91_000L, fire.triggerAtMs)
        val n = assertNotNull(shadowOf(nm).getNotification(TimerController.NOTIFICATION_ID))
        assertTrue(n.extras.getBoolean(android.app.Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
    }

    @Test
    fun `the timer survives the screen going away — its state is persisted`() {
        TimerController.start(context, 60_000L, now = 0L)
        assertEquals(TimerController.Snapshot(TimerController.Phase.RUNNING, 60_000L, 60_000L), TimerController.snapshot(context))
    }

    @Test
    fun `pausing disarms the wake-up and keeps the remaining time, resuming re-arms it`() {
        TimerController.start(context, 60_000L, now = 0L)
        TimerController.pause(context, now = 20_000L)

        assertNull(fireAlarm())
        assertEquals(40_000L, TimerController.snapshot(context).remaining)

        TimerController.resume(context, now = 100_000L)
        assertEquals(140_000L, fireAlarm()!!.triggerAtMs)
    }

    @Test
    fun `firing rings once — AlarmManager and the screen's ticker can both report the end`() {
        TimerController.start(context, 1_000L, now = 0L)
        TimerController.fired(context)
        TimerController.fired(context)

        assertEquals(TimerController.Phase.FINISHED, TimerController.snapshot(context).phase)
        val started = shadowOf(context as Application).nextStartedService
        assertEquals(TimerRingService::class.java.name, started.component!!.className)
        assertNull(shadowOf(context as Application).nextStartedService, "started twice")
    }

    @Test
    fun `cancelling stops everything`() {
        TimerController.start(context, 60_000L, now = 0L)
        TimerController.cancel(context)

        assertEquals(TimerController.Phase.IDLE, TimerController.snapshot(context).phase)
        assertNull(fireAlarm())
        assertNull(shadowOf(nm).getNotification(TimerController.NOTIFICATION_ID))
    }

    @Test
    fun `a timer from before a reboot reads as idle — its elapsed times mean nothing now`() {
        TimerController.start(context, 60_000L, now = 0L)
        val boot = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
        Settings.Global.putInt(context.contentResolver, Settings.Global.BOOT_COUNT, boot + 1)

        assertEquals(TimerController.Phase.IDLE, TimerController.snapshot(context).phase)
    }
}
