package com.pepperonas.brutus.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * This store is the only thing that survives a reboot between "you dismissed the
 * alarm" and "Brutus rings again in 10 minutes". If an entry is lost, the user
 * escapes Ultra Hardcore by rebooting; if a stale entry survives a completed
 * task, Brutus rings for no reason. Both failure modes are silent, so the
 * bookkeeping gets pinned here.
 */
@RunWith(RobolectricTestRunner::class)
class UltraHardcoreStoreTest {

    private lateinit var context: Context
    private val alarmA = 42L
    private val alarmB = 7L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `a fresh install has nothing pending`() {
        assertTrue(UltraHardcoreStore.listPending(context).isEmpty())
        assertTrue(UltraHardcoreStore.pendingAlarmIds(context).isEmpty())
    }

    @Test
    fun `a recorded follow-up comes back with alarm id, sequence and trigger time`() {
        UltraHardcoreStore.recordFollowup(context, alarmA, seq = 1, triggerAt = 1_700_000_000_000L)

        val pending = UltraHardcoreStore.listPending(context)
        assertEquals(1, pending.size)
        assertEquals(
            UltraHardcoreStore.Pending(alarmA, 1, 1_700_000_000_000L),
            pending.single()
        )
    }

    @Test
    fun `both follow-ups of one alarm are tracked independently`() {
        UltraHardcoreStore.recordFollowup(context, alarmA, 1, 1_000L)
        UltraHardcoreStore.recordFollowup(context, alarmA, 2, 2_000L)

        val bySeq = UltraHardcoreStore.listPending(context).associateBy { it.seq }
        assertEquals(setOf(1, 2), bySeq.keys)
        assertEquals(1_000L, bySeq.getValue(1).triggerAt)
        assertEquals(2_000L, bySeq.getValue(2).triggerAt)
    }

    @Test
    fun `re-recording the same sequence overwrites rather than duplicating`() {
        UltraHardcoreStore.recordFollowup(context, alarmA, 1, 1_000L)
        UltraHardcoreStore.recordFollowup(context, alarmA, 1, 9_000L)

        val pending = UltraHardcoreStore.listPending(context)
        assertEquals(1, pending.size)
        assertEquals(9_000L, pending.single().triggerAt)
    }

    @Test
    fun `clearing one sequence leaves the other armed`() {
        UltraHardcoreStore.recordFollowup(context, alarmA, 1, 1_000L)
        UltraHardcoreStore.recordFollowup(context, alarmA, 2, 2_000L)

        UltraHardcoreStore.clearFollowup(context, alarmA, 1)

        val pending = UltraHardcoreStore.listPending(context)
        assertEquals(1, pending.size)
        assertEquals(2, pending.single().seq)
    }

    @Test
    fun `clearAllFor wipes one alarm without touching another`() {
        UltraHardcoreStore.recordFollowup(context, alarmA, 1, 1_000L)
        UltraHardcoreStore.recordFollowup(context, alarmA, 2, 2_000L)
        UltraHardcoreStore.recordFollowup(context, alarmB, 1, 3_000L)
        UltraHardcoreStore.setStepTarget(context, alarmA, 55)
        UltraHardcoreStore.setBaselineSteps(context, alarmA, 1234f)
        UltraHardcoreStore.setStepTarget(context, alarmB, 44)

        UltraHardcoreStore.clearAllFor(context, alarmA)

        assertEquals(listOf(alarmB), UltraHardcoreStore.listPending(context).map { it.alarmId })
        // …and alarm A's task bookkeeping is gone with it.
        assertEquals(
            UltraHardcoreStore.DEFAULT_STEP_TARGET,
            UltraHardcoreStore.stepTarget(context, alarmA)
        )
        assertEquals(-1f, UltraHardcoreStore.baselineSteps(context, alarmA))
        // Alarm B keeps its own target.
        assertEquals(44, UltraHardcoreStore.stepTarget(context, alarmB))
    }

    @Test
    fun `pendingAlarmIds collapses the sequences of one alarm into a single id`() {
        UltraHardcoreStore.recordFollowup(context, alarmA, 1, 1_000L)
        UltraHardcoreStore.recordFollowup(context, alarmA, 2, 2_000L)
        UltraHardcoreStore.recordFollowup(context, alarmB, 1, 3_000L)

        assertEquals(setOf(alarmA, alarmB), UltraHardcoreStore.pendingAlarmIds(context))
    }

    @Test
    fun `step target and baseline keys never leak into the pending list`() {
        // They live in the same preferences file; only the follow-up prefix counts.
        UltraHardcoreStore.setStepTarget(context, alarmA, 30)
        UltraHardcoreStore.setBaselineSteps(context, alarmA, 500f)

        assertTrue(UltraHardcoreStore.listPending(context).isEmpty())
    }

    @Test
    fun `the step target defaults to 30 and round-trips`() {
        assertEquals(30, UltraHardcoreStore.DEFAULT_STEP_TARGET)
        assertEquals(30, UltraHardcoreStore.stepTarget(context, alarmA))

        UltraHardcoreStore.setStepTarget(context, alarmA, 75)
        assertEquals(75, UltraHardcoreStore.stepTarget(context, alarmA))
        // Per alarm, not global.
        assertEquals(30, UltraHardcoreStore.stepTarget(context, alarmB))
    }

    @Test
    fun `the baseline step count uses a negative sentinel until it is captured`() {
        assertEquals(-1f, UltraHardcoreStore.baselineSteps(context, alarmA))

        UltraHardcoreStore.setBaselineSteps(context, alarmA, 12_345f)
        assertEquals(12_345f, UltraHardcoreStore.baselineSteps(context, alarmA))
    }

    @Test
    fun `a negative alarm id round-trips — the key parser must not choke on the minus`() {
        UltraHardcoreStore.recordFollowup(context, -3L, 2, 5_000L)
        assertEquals(
            UltraHardcoreStore.Pending(-3L, 2, 5_000L),
            UltraHardcoreStore.listPending(context).single()
        )
    }

    @Test
    fun `clearing a sequence that was never armed is a no-op`() {
        UltraHardcoreStore.recordFollowup(context, alarmA, 1, 1_000L)
        UltraHardcoreStore.clearFollowup(context, alarmA, 2)
        UltraHardcoreStore.clearAllFor(context, 999L)

        assertEquals(1, UltraHardcoreStore.listPending(context).size)
    }
}
