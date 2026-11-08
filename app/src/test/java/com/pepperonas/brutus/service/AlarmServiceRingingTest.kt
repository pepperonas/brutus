package com.pepperonas.brutus.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pepperonas.brutus.data.AlarmDatabase
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.util.AlarmNotifier
import com.pepperonas.brutus.util.AppSettings
import com.pepperonas.brutus.util.RingPolicy
import com.pepperonas.brutus.util.RingingStore
import com.pepperonas.brutus.util.Storage
import java.time.Duration
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The ringing session's dynamics in the real service: it asks other audio to step aside, and an
 * alarm nobody answers snoozes itself and finally gives up — a Hardcore one never does.
 */
@RunWith(RobolectricTestRunner::class)
class AlarmServiceRingingTest {

    private lateinit var context: Context
    private lateinit var db: AlarmDatabase
    private lateinit var audio: AudioManager
    private val started = mutableListOf<AlarmService>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        AlarmDatabase.resetInstanceForTests()
        db = AlarmDatabase.getInstance(context)
        listOf("brutus_ultra_hardcore", "brutus_ringing", "brutus_settings")
            .forEach { Storage.prefs(context, it).edit().clear().commit() }
        audio = context.getSystemService(AudioManager::class.java)
    }

    @After
    fun tearDown() {
        started.forEach { svc ->
            if (!Shadows.shadowOf(svc).isStoppedBySelf) {
                svc.onStartCommand(Intent(context, AlarmService::class.java).setAction(AlarmService.ACTION_STOP), 0, 99)
                waitFor("service stopped") { Shadows.shadowOf(svc).isStoppedBySelf }
            }
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

    private fun ring(alarm: AlarmEntity = AlarmEntity(hour = 7, minute = 0, repeatDays = 0x7F)): Pair<AlarmService, AlarmEntity> {
        val saved = runBlocking { alarm.copy(id = db.alarmDao().insert(alarm)) }
        val start = Intent(context, AlarmService::class.java)
            .setAction(AlarmService.ACTION_START).putExtra("alarm_id", saved.id)
        val svc = Robolectric.buildService(AlarmService::class.java, start).create().startCommand(0, 1).get()
        started += svc
        waitFor("sound started") { Shadows.shadowOf(audio).lastAudioFocusRequest != null }
        return svc to saved
    }

    private fun advance(minutes: Long) = Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(minutes))

    private fun missedPosted(id: Long) =
        Shadows.shadowOf(context.getSystemService(NotificationManager::class.java))
            .getNotification(AlarmNotifier.missedId(id))

    @Test
    fun `a ringing alarm asks music and podcasts to step aside, and lets go when dismissed`() {
        val (svc, _) = ring()
        val request = assertNotNull(Shadows.shadowOf(audio).lastAudioFocusRequest)
        assertEquals(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT, request.durationHint)
        assertEquals(AudioAttributes.USAGE_ALARM, request.audioFocusRequest.audioAttributes.usage)

        svc.onStartCommand(Intent(context, AlarmService::class.java).setAction(AlarmService.ACTION_STOP), 0, 2)
        waitFor("stopped") { Shadows.shadowOf(svc).isStoppedBySelf }
        assertNotNull(Shadows.shadowOf(audio).lastAbandonedAudioFocusRequest, "focus must be given back")
    }

    @Test
    fun `an unanswered alarm snoozes itself when the ring limit is reached`() {
        AppSettings.setRingLimitMinutes(context, 5)
        val (svc, alarm) = ring()

        advance(4)
        assertTrue(RingingStore.snoozes(context).isEmpty(), "too early")
        advance(1)
        waitFor("snoozed itself") { Shadows.shadowOf(svc).isStoppedBySelf }
        assertEquals(alarm.id, RingingStore.snoozes(context).single().alarmId)
        assertEquals(1, RingingStore.autoSnoozes(context, alarm.id))
    }

    @Test
    fun `after three unanswered rounds it stops and reports itself missed instead of snoozing again`() {
        AppSettings.setRingLimitMinutes(context, 5)
        val (svc, alarm) = ring()
        repeat(RingPolicy.MAX_AUTO_SNOOZES) { RingingStore.recordAutoSnooze(context, alarm.id) }

        advance(5)
        waitFor("gave up") { Shadows.shadowOf(svc).isStoppedBySelf }
        waitFor("missed notice") { missedPosted(alarm.id) != null }
        assertTrue(RingingStore.snoozes(context).isEmpty(), "it must not ring again")
        assertEquals(0, RingingStore.autoSnoozes(context, alarm.id), "the next real alarm starts fresh")
    }

    @Test
    fun `a hardcore alarm keeps ringing past the limit`() {
        AppSettings.setRingLimitMinutes(context, 5)
        val (svc, _) = ring(AlarmEntity(hour = 7, minute = 0, repeatDays = 0x7F, hardcoreMode = true))
        advance(30)
        assertFalse(Shadows.shadowOf(svc).isStoppedBySelf)
        assertTrue(RingingStore.snoozes(context).isEmpty())
    }

    @Test
    fun `with the limit off a normal alarm rings until answered`() {
        AppSettings.setRingLimitMinutes(context, 0)
        val (svc, _) = ring()
        advance(60)
        assertFalse(Shadows.shadowOf(svc).isStoppedBySelf)
    }

    @Test
    fun `dismissing or snoozing by hand resets the count of unanswered rounds`() {
        val (svc, alarm) = ring()
        RingingStore.recordAutoSnooze(context, alarm.id)
        svc.onStartCommand(
            Intent(context, AlarmService::class.java).setAction(AlarmService.ACTION_SNOOZE).putExtra("alarm_id", alarm.id),
            0, 2,
        )
        waitFor("snoozed") { Shadows.shadowOf(svc).isStoppedBySelf }
        assertEquals(0, RingingStore.autoSnoozes(context, alarm.id))

        val (svc2, alarm2) = ring()
        RingingStore.recordAutoSnooze(context, alarm2.id)
        svc2.onStartCommand(Intent(context, AlarmService::class.java).setAction(AlarmService.ACTION_STOP), 0, 3)
        waitFor("dismissed") { Shadows.shadowOf(svc2).isStoppedBySelf }
        assertEquals(0, RingingStore.autoSnoozes(context, alarm2.id))
        assertNull(missedPosted(alarm2.id))
    }

    @Test
    fun `settings fall back to their defaults and refuse values outside the offer`() {
        assertEquals(AppSettings.DEFAULT_CRESCENDO, AppSettings.crescendoSeconds(context))
        assertEquals(AppSettings.DEFAULT_RING_LIMIT, AppSettings.ringLimitMinutes(context))
        Storage.prefs(context, "brutus_settings").edit().putInt("ring_limit_minutes", 7).commit()
        assertEquals(AppSettings.DEFAULT_RING_LIMIT, AppSettings.ringLimitMinutes(context))
        runCatching { AppSettings.setCrescendoSeconds(context, 45) }.onSuccess { fail("45 s is not offered") }
    }
}
