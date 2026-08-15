package com.pepperonas.brutus.util

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The three reliability banners are only useful if their "Aktivieren" button
 * lands on the right settings page. A wrong action string or a missing
 * `package:` URI drops the user into a generic list — which is how these
 * banners quietly stop working after an SDK bump.
 */
@RunWith(RobolectricTestRunner::class)
class PermissionDeepLinkTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun Intent.startsNewTask() =
        (flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0

    // ---- exact alarms ----------------------------------------------------

    @Test
    fun `exact-alarm banner deep-links to this app's own page`() {
        val intent = assertNotNull(ExactAlarmPermission.settingsIntent(context))

        assertEquals(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, intent.action)
        assertEquals("package:${context.packageName}", intent.data.toString())
        // Launched from a Compose banner, i.e. outside an Activity task.
        assertTrue(intent.startsNewTask())
    }

    @Test
    fun `exact-alarm state mirrors what AlarmManager reports`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        assertTrue(ExactAlarmPermission.isGranted(context))

        // Samsung's default for third-party apps.
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        assertFalse(ExactAlarmPermission.isGranted(context))

        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    // ---- battery optimization -------------------------------------------

    @Test
    fun `battery banner asks for this app to be whitelisted`() {
        val intent = BatteryOptimizationPermission.settingsIntent(context)

        assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, intent.action)
        assertEquals("package:${context.packageName}", intent.data.toString())
        assertTrue(intent.startsNewTask())
    }

    @Test
    fun `the battery fallback opens the general list without a package URI`() {
        val fallback = BatteryOptimizationPermission.fallbackSettingsIntent()

        assertEquals(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, fallback.action)
        assertNull(fallback.data, "the list intent must not carry a package: URI")
        assertTrue(fallback.startsNewTask())
    }

    @Test
    fun `battery state mirrors PowerManager`() {
        val pm = context.getSystemService(PowerManager::class.java)
        shadowOf(pm).setIgnoringBatteryOptimizations(context.packageName, false)
        assertFalse(BatteryOptimizationPermission.isIgnoring(context))

        shadowOf(pm).setIgnoringBatteryOptimizations(context.packageName, true)
        assertTrue(BatteryOptimizationPermission.isIgnoring(context))
    }

    // ---- full-screen intent (Android 14+) --------------------------------

    @Test
    fun `full-screen-intent banner deep-links on Android 14 and newer`() {
        val intent = assertNotNull(FullScreenIntentPermission.settingsIntent(context))

        assertEquals(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, intent.action)
        assertEquals("package:${context.packageName}", intent.data.toString())
        assertTrue(intent.startsNewTask())
    }

    @Test
    @Config(sdk = [33])
    fun `below Android 14 the permission is implicit — no banner, no intent`() {
        assertTrue(FullScreenIntentPermission.isGranted(context))
        assertNull(
            FullScreenIntentPermission.settingsIntent(context),
            "there is no settings toggle before API 34; a non-null intent would dead-end"
        )
    }
}
