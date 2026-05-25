package com.pepperonas.brutus.scheduler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Lightweight invariants for the AlarmScheduler offsets — full integration scheduling
 * is hard to unit-test without instrumented dependencies on AlarmManager / Context.
 */
class AlarmSchedulerConstantsTest {

    @Test
    fun `ultra hardcore follow-ups are 10 and 15 minutes`() {
        assertEquals(
            listOf(10, 15),
            AlarmScheduler.ULTRA_HARDCORE_FOLLOWUP_OFFSETS_MIN.toList()
        )
    }

    @Test
    fun `sunrise lead is 10 minutes — matches the UI copy in the edit dialog`() {
        assertEquals(10, AlarmScheduler.SUNRISE_LEAD_MIN)
    }

    @Test
    fun `sunrise lead is shorter than the first ultra hardcore follow-up`() {
        // If we ever lengthen the sunrise lead beyond the first follow-up window,
        // the sunrise pre-alarm could collide with the +10 min re-alarm timing.
        assertTrue(
            AlarmScheduler.SUNRISE_LEAD_MIN <= AlarmScheduler.ULTRA_HARDCORE_FOLLOWUP_OFFSETS_MIN.first(),
            "Sunrise lead must stay ≤ the first follow-up offset"
        )
    }

    @Test
    fun `intent extra keys are unique constants`() {
        val keys = listOf(
            AlarmScheduler.EXTRA_IS_FOLLOWUP,
            AlarmScheduler.EXTRA_FOLLOWUP_SEQ,
            AlarmScheduler.EXTRA_IS_SUNRISE,
            AlarmScheduler.EXTRA_MAIN_TRIGGER_AT,
        )
        assertEquals(keys.size, keys.toSet().size, "Intent extra keys must be unique")
    }
}
