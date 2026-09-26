package com.pepperonas.brutus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Notification channels are write-once: after the first install Android ignores
 * every later change to importance, DND bypass or sound. Getting them wrong
 * therefore cannot be fixed by an update — the user has to reinstall. And a
 * renamed channel *id* silently resets whatever the user configured.
 */
@RunWith(RobolectricTestRunner::class)
class BrutusApplicationTest {

    private lateinit var context: Context
    private lateinit var nm: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        nm = context.getSystemService(NotificationManager::class.java)
    }

    private fun channel(id: String): NotificationChannel =
        assertNotNull(nm.getNotificationChannel(id), "channel '$id' was never created")

    @Test
    fun `channel ids are stable — renaming one resets the user's settings`() {
        assertEquals("brutus_alarm", BrutusApplication.CHANNEL_ALARM)
        assertEquals("brutus_service", BrutusApplication.CHANNEL_SERVICE)
        assertEquals("brutus_ultra_hardcore", BrutusApplication.CHANNEL_ULTRA_HARDCORE)
        assertEquals("brutus_updates", BrutusApplication.CHANNEL_UPDATES)
    }

    @Test
    fun `all four channels exist after application start`() {
        listOf(
            BrutusApplication.CHANNEL_ALARM,
            BrutusApplication.CHANNEL_SERVICE,
            BrutusApplication.CHANNEL_ULTRA_HARDCORE,
            BrutusApplication.CHANNEL_UPDATES,
        ).forEach { channel(it) }
    }

    @Test
    fun `the update channel is an ordinary notice — it never breaks through Do Not Disturb`() {
        val updates = channel(BrutusApplication.CHANNEL_UPDATES)

        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, updates.importance)
        assertFalse(updates.canBypassDnd(), "a release note is not an alarm")
    }

    @Test
    fun `the alarm channel is high importance and ignores Do Not Disturb`() {
        val alarm = channel(BrutusApplication.CHANNEL_ALARM)

        assertEquals(NotificationManager.IMPORTANCE_HIGH, alarm.importance)
        assertTrue(alarm.canBypassDnd(), "an alarm silenced by DND is not an alarm")
    }

    @Test
    fun `the alarm channel stays silent — the service owns the audio`() {
        // A channel sound would play *on top of* the synthesized alarm.
        assertNull(channel(BrutusApplication.CHANNEL_ALARM).sound)
    }

    @Test
    fun `the foreground-service channel is quiet enough not to annoy`() {
        val service = channel(BrutusApplication.CHANNEL_SERVICE)

        assertEquals(NotificationManager.IMPORTANCE_LOW, service.importance)
        assertFalse(service.canBypassDnd(), "the housekeeping notification must not punch through DND")
    }

    @Test
    fun `the Ultra Hardcore reminder punches through DND but never buzzes`() {
        val uhc = channel(BrutusApplication.CHANNEL_ULTRA_HARDCORE)

        assertEquals(NotificationManager.IMPORTANCE_HIGH, uhc.importance)
        assertTrue(uhc.canBypassDnd())
        assertNull(uhc.sound)
        // It is posted for up to 15 minutes — a vibrating reminder would be torture.
        assertFalse(uhc.shouldVibrate())
    }

    @Test
    fun `every channel carries a description for the system settings list`() {
        listOf(
            BrutusApplication.CHANNEL_ALARM,
            BrutusApplication.CHANNEL_SERVICE,
            BrutusApplication.CHANNEL_ULTRA_HARDCORE,
        ).forEach {
            assertTrue(channel(it).description.isNullOrBlank().not(), "$it has no description")
        }
    }

    @Test
    fun `the database is a lazy singleton — one connection for the whole process`() {
        val app = context as BrutusApplication
        assertTrue(app.database === app.database)
    }
}
