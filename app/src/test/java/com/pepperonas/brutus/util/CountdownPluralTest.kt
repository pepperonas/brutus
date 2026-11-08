package com.pepperonas.brutus.util

import android.content.Context
import com.pepperonas.brutus.LocaleContexts
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

/**
 * The list header used a single string for hours and minutes, so it read
 * "Alarm in 1 Stunden, 1 Minuten" / "Alarm in 1 hours, 1 minutes".
 * Each unit now goes through its own plural.
 */
@RunWith(RobolectricTestRunner::class)
class CountdownPluralTest {

    private lateinit var en: Context
    private lateinit var de: Context

    @Before
    fun setUp() {
        en = LocaleContexts.english()
        de = LocaleContexts.german()
    }

    private fun span(h: Int, m: Int) = (h * 60L + m) * 60_000L

    private fun fmt(ctx: Context, h: Int, m: Int) =
        NextAlarmCalculator.formatCountdown(ctx, 0L, span(h, m))

    @Test
    fun `German uses the singular for one hour and one minute`() {
        assertEquals("Alarm in 1 Stunde, 1 Minute", fmt(de, 1, 1))
        assertEquals("Alarm in 1 Stunde, 14 Minuten", fmt(de, 1, 14))
        assertEquals("Alarm in 2 Stunden, 1 Minute", fmt(de, 2, 1))
        assertEquals("Alarm in 1 Minute", fmt(de, 0, 1))
        assertEquals("Alarm in 0 Minuten", fmt(de, 0, 0))
    }

    @Test
    fun `English uses the singular for one hour and one minute`() {
        assertEquals("Alarm in 1 hour, 1 minute", fmt(en, 1, 1))
        assertEquals("Alarm in 7 hours, 12 minutes", fmt(en, 7, 12))
        assertEquals("Alarm in 1 minute", fmt(en, 0, 1))
        assertEquals("Alarm in 45 minutes", fmt(en, 0, 45))
    }

    @Test
    fun `German shake counter writes "25-mal" as one word`() {
        val text = de.resources.getQuantityString(
            com.pepperonas.brutus.R.plurals.shake_remaining, 25, 25
        )
        assertEquals("Noch 25-mal!", text)
    }
}
