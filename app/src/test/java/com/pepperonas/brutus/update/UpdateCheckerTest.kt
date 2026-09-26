package com.pepperonas.brutus.update

import android.Manifest
import com.pepperonas.brutus.util.Storage
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.pepperonas.brutus.BrutusApplication
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The whole check without a network: a fake source stands in for the two endpoints.
 * What must hold: off means silent and offline, one notification per version, never for an older one.
 */
@RunWith(RobolectricTestRunner::class)
class UpdateCheckerTest {

    private lateinit var context: Context
    private lateinit var nm: NotificationManager
    private var fetches = 0

    private fun source(version: String?) = object : LatestVersionSource {
        override fun latestVersion(): String? { fetches++; return version }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        nm = context.getSystemService(NotificationManager::class.java)
        Storage.prefs(context, UpdateCheckStore.PREFS).edit().clear().commit()
        fetches = 0
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun posted() = shadowOf(nm).allNotifications

    @Test
    fun `switched off, nothing is fetched and nothing is posted`() {
        val outcome = UpdateChecker.check(context, source("v9.0.0"), installed = "2.2.0")
        assertEquals(UpdateChecker.Outcome.DISABLED, outcome)
        assertEquals(0, fetches, "the switch is the promise: off means no request")
        assertTrue(posted().isEmpty())
    }

    @Test
    fun `a newer release posts one notification and is remembered for the banner`() {
        UpdateCheckStore.setEnabled(context, true)
        val outcome = UpdateChecker.check(context, source("v2.3.0"), installed = "2.2.0")

        assertEquals(UpdateChecker.Outcome.NOTIFIED, outcome)
        assertEquals(1, posted().size)
        assertEquals("v2.3.0", UpdateCheckStore.latestSeen(context))
        val n = posted().single()
        assertEquals(BrutusApplication.CHANNEL_UPDATES, n.channelId)
        assertTrue(n.extras.getString("android.title")!!.contains("2.3.0"))
    }

    @Test
    fun `tapping the notification opens the download page`() {
        UpdateCheckStore.setEnabled(context, true)
        UpdateChecker.check(context, source("v2.3.0"), installed = "2.2.0")

        val intent: Intent = shadowOf(posted().single().contentIntent).savedIntent
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(ReleaseSource.DOWNLOAD_URL, intent.dataString)
    }

    @Test
    fun `the same version is announced only once`() {
        UpdateCheckStore.setEnabled(context, true)
        UpdateChecker.check(context, source("v2.3.0"), installed = "2.2.0")
        nm.cancelAll()
        val second = UpdateChecker.check(context, source("v2.3.0"), installed = "2.2.0")

        assertEquals(UpdateChecker.Outcome.ALREADY_NOTIFIED, second)
        assertTrue(posted().isEmpty(), "a daily check must not nag daily")
    }

    @Test
    fun `a later release is announced again`() {
        UpdateCheckStore.setEnabled(context, true)
        UpdateChecker.check(context, source("v2.3.0"), installed = "2.2.0")
        nm.cancelAll()
        assertEquals(UpdateChecker.Outcome.NOTIFIED, UpdateChecker.check(context, source("v2.4.0"), installed = "2.2.0"))
        assertEquals(1, posted().size)
    }

    @Test
    fun `the installed version or an older one posts nothing`() {
        UpdateCheckStore.setEnabled(context, true)
        assertEquals(UpdateChecker.Outcome.UP_TO_DATE, UpdateChecker.check(context, source("v2.2.0"), installed = "2.2.0"))
        assertEquals(UpdateChecker.Outcome.UP_TO_DATE, UpdateChecker.check(context, source("v2.1.1"), installed = "2.2.0"))
        assertTrue(posted().isEmpty())
    }

    @Test
    fun `an unreachable source is a silent failure that keeps the last finding`() {
        UpdateCheckStore.setEnabled(context, true)
        UpdateChecker.check(context, source("v2.3.0"), installed = "2.2.0")
        nm.cancelAll()

        assertEquals(UpdateChecker.Outcome.FAILED, UpdateChecker.check(context, source(null), installed = "2.2.0"))
        assertTrue(posted().isEmpty())
        assertEquals("v2.3.0", UpdateCheckStore.latestSeen(context))
    }

    @Test
    fun `without the notification permission the finding still reaches the banner`() {
        shadowOf(context as Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        UpdateCheckStore.setEnabled(context, true)
        val outcome = UpdateChecker.check(context, source("v2.3.0"), installed = "2.2.0")

        assertEquals(UpdateChecker.Outcome.NOT_SHOWN, outcome)
        assertTrue(posted().isEmpty())
        assertEquals("2.3.0", UpdateChecker.bannerVersion(context, installed = "2.2.0"))
    }

    @Test
    fun `granting the permission later still announces the version that was found while denied`() {
        shadowOf(context as Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        UpdateCheckStore.setEnabled(context, true)
        UpdateChecker.check(context, source("v2.3.0"), installed = "2.2.0")

        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertEquals(UpdateChecker.Outcome.NOTIFIED, UpdateChecker.check(context, source("v2.3.0"), installed = "2.2.0"))
        assertEquals(1, posted().size)
    }

    @Test
    fun `the banner shows only while switched on and newer than installed`() {
        assertNull(UpdateChecker.bannerVersion(context, installed = "2.2.0"))

        UpdateCheckStore.setEnabled(context, true)
        UpdateChecker.check(context, source("v2.3.0"), installed = "2.2.0")
        assertEquals("2.3.0", UpdateChecker.bannerVersion(context, installed = "2.2.0"))
        assertNull(UpdateChecker.bannerVersion(context, installed = "2.3.0"), "gone after the update")

        UpdateCheckStore.setEnabled(context, false)
        assertNull(UpdateChecker.bannerVersion(context, installed = "2.2.0"))
    }
}
