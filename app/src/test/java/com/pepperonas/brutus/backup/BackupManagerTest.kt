package com.pepperonas.brutus.backup

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pepperonas.brutus.data.AlarmDatabase
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.util.AlarmSound
import com.pepperonas.brutus.util.AppSettings
import com.pepperonas.brutus.util.GlobalQrStore
import com.pepperonas.brutus.util.Storage
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAlarmManager
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BackupManagerTest {

    private lateinit var context: Context
    private lateinit var db: AlarmDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        AlarmDatabase.resetInstanceForTests()
        db = AlarmDatabase.getInstance(context)
        Storage.PREFERENCE_FILES.forEach { Storage.prefs(context, it).edit().clear().commit() }
    }

    private fun scheduled() =
        Shadows.shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.size

    private fun wipe() = runBlocking {
        db.alarmDao().getAll().forEach { db.alarmDao().delete(it) }
        Storage.PREFERENCE_FILES.forEach { Storage.prefs(context, it).edit().clear().commit() }
    }

    @Test
    fun `an export brought to an empty phone restores alarms, QR code and settings, and arms them`() = runBlocking {
        db.alarmDao().insert(AlarmEntity(hour = 6, minute = 0, repeatDays = 0x7F, label = "Work"))
        db.alarmDao().insert(AlarmEntity(hour = 9, minute = 30, enabled = false))
        val qr = GlobalQrStore.get(context)
        AppSettings.setCrescendoSeconds(context, 60)
        AppSettings.setRingLimitMinutes(context, 5)
        AppSettings.setSunriseSound(context, AlarmSound.SILENT)
        val file = BackupManager.export(context)

        wipe()
        val result = BackupManager.import(context, file)

        assertEquals(2, result.added)
        assertTrue(result.qrReplaced)
        assertEquals(listOf("Work", ""), db.alarmDao().getAll().map { it.label })
        assertEquals(qr, GlobalQrStore.get(context), "the printed code must keep working")
        assertEquals(60, AppSettings.crescendoSeconds(context))
        assertEquals(5, AppSettings.ringLimitMinutes(context))
        assertEquals(AlarmSound.SILENT, AppSettings.sunriseSound(context))
        assertTrue(scheduled() > 0, "imported alarms must ring without opening the app first")
    }

    @Test
    fun `importing the same file again adds nothing and keeps the QR code`() = runBlocking {
        db.alarmDao().insert(AlarmEntity(hour = 6, minute = 0))
        val file = BackupManager.export(context)
        val result = BackupManager.import(context, file)
        assertEquals(0, result.added)
        assertEquals(1, result.alreadyThere)
        assertFalse(result.qrReplaced)
        assertEquals(1, db.alarmDao().getAll().size)
    }

    @Test
    fun `settings and QR codes the app does not offer are ignored`() = runBlocking {
        val before = GlobalQrStore.get(context)
        BackupManager.import(
            context,
            """{"format":"brutus-backup","version":1,"qr":"evil","settings":{"crescendoSeconds":45,
               "ringLimitMinutes":-1,"sunriseSoundId":${AlarmSound.KLAXON.id}},"alarms":[]}""",
        )
        assertEquals(before, GlobalQrStore.get(context))
        assertEquals(AppSettings.DEFAULT_CRESCENDO, AppSettings.crescendoSeconds(context))
        assertEquals(AppSettings.DEFAULT_RING_LIMIT, AppSettings.ringLimitMinutes(context))
        assertEquals(AppSettings.DEFAULT_SUNRISE_SOUND, AppSettings.sunriseSound(context))
    }

    @Test
    fun `a file that is not a backup changes nothing`() = runBlocking {
        db.alarmDao().insert(AlarmEntity(hour = 6, minute = 0))
        assertFailsWith<AlarmBackup.FormatException> { BackupManager.import(context, "{}") }
        assertEquals(1, db.alarmDao().getAll().size)
    }

    @Test
    fun `after a system restore the alarms are armed without opening the app`() = runBlocking {
        db.alarmDao().insert(AlarmEntity(hour = 6, minute = 0, repeatDays = 0x7F))
        assertEquals(0, scheduled())
        BrutusBackupAgent.afterRestore(context)
        assertTrue(scheduled() > 0)
    }

    @Test
    fun `GlobalQrStore only takes codes Brutus itself makes`() {
        assertTrue(GlobalQrStore.isValid(GlobalQrStore.get(context)))
        listOf("", "brutus:", "brutus:not-a-uuid", "x brutus:123e4567-e89b-12d3-a456-426614174000")
            .forEach { assertFalse(GlobalQrStore.isValid(it), it) }
        assertFailsWith<IllegalArgumentException> { GlobalQrStore.set(context, "evil") }
    }
}
