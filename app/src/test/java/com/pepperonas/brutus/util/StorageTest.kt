package com.pepperonas.brutus.util

import android.content.Context
import android.os.UserManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The data an alarm needs must be readable before the first unlock after a reboot, so it lives in
 * device-protected storage. Installs older than v2.3.1 kept it in credential storage; the one-time
 * move must carry everything over — above all the QR code, which users have printed and taped up.
 */
@RunWith(RobolectricTestRunner::class)
class StorageTest {

    private lateinit var context: Context
    private val marker get() = Storage.prefs(context, "brutus_storage")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // The Application already migrated at start-up; reset to a pre-v2.3.1 install.
        marker.edit().clear().commit()
        Storage.PREFERENCE_FILES.forEach { Storage.prefs(context, it).edit().clear().commit() }
    }

    private fun credentialPrefs(name: String) = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    @Test
    fun `device storage is a different place than credential storage`() {
        assertTrue(Storage.device(context).isDeviceProtectedStorage)
        assertFalse(context.isDeviceProtectedStorage)
    }

    @Test
    fun `the printed QR code survives the move — it is never regenerated`() {
        credentialPrefs("brutus_global").edit().putString("qr_data", "brutus:printed-on-the-fridge").commit()

        Storage.migrateIfNeeded(context)

        assertEquals("brutus:printed-on-the-fridge", GlobalQrStore.get(context))
    }

    @Test
    fun `pending Ultra Hardcore follow-ups move along`() {
        credentialPrefs("brutus_ultra_hardcore").edit().putLong("uhc_followup_7_1", 123L).commit()

        Storage.migrateIfNeeded(context)

        assertEquals(listOf(UltraHardcoreStore.Pending(7L, 1, 123L)), UltraHardcoreStore.listPending(context))
    }

    @Test
    fun `the database file moves to device storage`() {
        val credentialDb = context.getDatabasePath(Storage.DATABASE)
        credentialDb.parentFile!!.mkdirs()
        credentialDb.writeText("sqlite-bytes")
        File(Storage.device(context).getDatabasePath(Storage.DATABASE).path).delete()

        Storage.migrateIfNeeded(context)

        assertFalse(credentialDb.exists(), "moved, not copied")
        assertEquals("sqlite-bytes", Storage.device(context).getDatabasePath(Storage.DATABASE).readText())
    }

    @Test
    fun `nothing is ready and nothing moves while the phone is still locked`() {
        shadowOf(context.getSystemService(UserManager::class.java)).setUserUnlocked(false)
        credentialPrefs("brutus_global").edit().putString("qr_data", "brutus:x").commit()

        Storage.migrateIfNeeded(context)

        assertFalse(Storage.isReady(context), "the locked boot path must stay out")
        assertNull(Storage.prefs(context, "brutus_global").getString("qr_data", null))
    }

    @Test
    fun `the move happens once — a second run does not wipe what was written since`() {
        Storage.migrateIfNeeded(context)
        assertTrue(Storage.isReady(context))
        Storage.prefs(context, "brutus_global").edit().putString("qr_data", "brutus:new").commit()
        credentialPrefs("brutus_global").edit().putString("qr_data", "brutus:stale").commit()

        Storage.migrateIfNeeded(context)

        assertEquals("brutus:new", GlobalQrStore.get(context))
    }

    @Test
    fun `every store of the app is covered by the move`() {
        // A new store that forgets to register its file here would silently stay behind.
        listOf("brutus_ultra_hardcore", "brutus_global", "brutus_updates", "brutus_timer",
            "brutus_world_clock", "brutus_ringing").forEach {
            assertTrue(it in Storage.PREFERENCE_FILES, "$it is not migrated")
        }
    }
}
