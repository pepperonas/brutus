package com.pepperonas.brutus.update

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class UpdateCheckStoreTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(UpdateCheckStore.PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }

    /**
     * The alarm list recomposes from this flow via collectAsState, which drops a value equal to the
     * previous one. Emitting Unit on every change therefore looked fine and did nothing — the switch
     * stayed "off" on screen after tapping it (found on the emulator). Every emission must differ.
     */
    @Test
    fun `every change emits a value different from the one before`() = runTest {
        val seen = mutableListOf<Int>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { UpdateCheckStore.changes(context).toList(seen) }
        UpdateCheckStore.setEnabled(context, true)
        ShadowLooper.idleMainLooper()
        UpdateCheckStore.recordLatest(context, "v2.3.0")
        ShadowLooper.idleMainLooper()
        job.cancel()

        assertEquals(3, seen.size)
        assertEquals(seen.size, seen.toSet().size, "equal consecutive values are swallowed by collectAsState: $seen")
    }

    @Test
    fun `switching off forgets what was found`() {
        UpdateCheckStore.recordLatest(context, "v2.3.0")
        UpdateCheckStore.markNotified(context, "v2.3.0")
        UpdateCheckStore.clearFindings(context)
        assertEquals(null, UpdateCheckStore.latestSeen(context))
        assertEquals(null, UpdateCheckStore.notifiedVersion(context))
    }
}
