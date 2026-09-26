package com.pepperonas.brutus.update

import android.content.Context
import com.pepperonas.brutus.util.Storage
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Switching the check on and off is what actually starts and stops network traffic. */
@RunWith(RobolectricTestRunner::class)
class UpdateSchedulerTest {

    private lateinit var context: Context
    private lateinit var wm: WorkManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Storage.prefs(context, UpdateCheckStore.PREFS).edit().clear().commit()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG).setExecutor(SynchronousExecutor()).build(),
        )
        wm = WorkManager.getInstance(context)
    }

    private fun work(name: String) = wm.getWorkInfosForUniqueWork(name).get()
    private fun active(name: String) = work(name).filter { !it.state.isFinished }

    @Test
    fun `switching on stores the choice and schedules a daily check that needs a network`() {
        UpdateScheduler.setEnabled(context, true)

        assertTrue(UpdateCheckStore.isEnabled(context))
        val daily = active(UpdateScheduler.PERIODIC_WORK).single()
        assertEquals(NetworkType.CONNECTED, daily.constraints.requiredNetworkType)
        assertEquals(TimeUnit.HOURS.toMillis(24), daily.periodicityInfo!!.repeatIntervalMillis)
        // The immediate check covers "now"; a periodic run right away too would race it.
        assertTrue(daily.initialDelayMillis >= TimeUnit.HOURS.toMillis(24) - 1000)
    }

    @Test
    fun `switching on also asks once right away, when a network is there`() {
        UpdateScheduler.setEnabled(context, true)

        val now = work(UpdateScheduler.IMMEDIATE_WORK).single()
        assertEquals(NetworkType.CONNECTED, now.constraints.requiredNetworkType)
    }

    @Test
    fun `switching off cancels everything and forgets the banner`() {
        UpdateScheduler.setEnabled(context, true)
        UpdateCheckStore.recordLatest(context, "v9.0.0")
        UpdateScheduler.setEnabled(context, false)

        assertFalse(UpdateCheckStore.isEnabled(context))
        assertTrue(active(UpdateScheduler.PERIODIC_WORK).isEmpty())
        assertTrue(active(UpdateScheduler.IMMEDIATE_WORK).isEmpty())
        assertEquals(null, UpdateCheckStore.latestSeen(context))
    }

    @Test
    fun `switching on twice keeps a single daily check`() {
        UpdateScheduler.setEnabled(context, true)
        UpdateScheduler.setEnabled(context, true)
        assertEquals(1, active(UpdateScheduler.PERIODIC_WORK).size)
        assertTrue(work(UpdateScheduler.PERIODIC_WORK).all { it.state != WorkInfo.State.CANCELLED })
    }

    @Test
    fun `the default is off — existing installs stay offline after the update`() {
        assertFalse(UpdateCheckStore.isEnabled(context))
        UpdateScheduler.ensureScheduled(context)
        assertTrue(active(UpdateScheduler.PERIODIC_WORK).isEmpty())
    }

    @Test
    fun `ensureScheduled restores the daily check when it is switched on`() {
        UpdateCheckStore.setEnabled(context, true)
        UpdateScheduler.ensureScheduled(context)
        assertEquals(1, active(UpdateScheduler.PERIODIC_WORK).size)
    }
}
