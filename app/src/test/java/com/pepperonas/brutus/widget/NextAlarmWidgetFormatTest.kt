package com.pepperonas.brutus.widget

import android.content.Context
import com.pepperonas.brutus.LocaleContexts
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The home-screen widget is read at a glance, half asleep, without the app open.
 * Its two strings are pure functions of (trigger, now), the repeat bitmask — and
 * of the language, so both are asserted in English and German.
 */
@RunWith(RobolectricTestRunner::class)
class NextAlarmWidgetFormatTest {

    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour
    private val now = 1_700_000_000_000L

    private lateinit var en: Context
    private lateinit var de: Context

    @Before
    fun setUp() {
        en = LocaleContexts.english()
        de = LocaleContexts.german()
    }

    private fun relative(ctx: Context, offset: Long): String =
        NextAlarmWidget.formatRelative(ctx, now + offset, now)

    // ---- countdown -------------------------------------------------------

    @Test
    fun `an alarm less than a minute out reads as now`() {
        listOf(0L, 30_000L, 59_999L).forEach {
            assertEquals("now", relative(en, it))
            assertEquals("gleich", relative(de, it))
        }
    }

    @Test
    fun `a trigger in the past never renders a negative countdown`() {
        assertEquals("now", relative(en, -1))
        assertEquals("gleich", relative(de, -5 * day))
    }

    @Test
    fun `minutes are shown alone below the hour`() {
        assertEquals("in 1 min", relative(en, minute))
        assertEquals("in 59 min", relative(en, 59 * minute))
        assertEquals("in 1 Min", relative(de, minute))
        assertEquals("in 59 Min", relative(de, 59 * minute))
    }

    @Test
    fun `hours carry the remaining minutes`() {
        assertEquals("in 1h 0m", relative(en, hour))
        assertEquals("in 7h 12m", relative(en, 7 * hour + 12 * minute))
        assertEquals("in 1 Std 0 Min", relative(de, hour))
        assertEquals("in 7 Std 12 Min", relative(de, 7 * hour + 12 * minute))
    }

    @Test
    fun `exactly one day is singular in both languages`() {
        // "in 1 Tagen" / "in 1 days" is what a missing plural rule looks like.
        assertEquals("in 1 day", relative(en, day))
        assertEquals("in 1 day", relative(en, day + 23 * hour))
        assertEquals("in 1 Tag", relative(de, day))
        assertEquals("in 1 Tag", relative(de, day + 23 * hour))
    }

    @Test
    fun `more than one day is plural in both languages`() {
        assertEquals("in 2 days", relative(en, 2 * day))
        assertEquals("in 6 days", relative(en, 6 * day + 5 * hour))
        assertEquals("in 2 Tagen", relative(de, 2 * day))
        assertEquals("in 6 Tagen", relative(de, 6 * day + 5 * hour))
    }

    @Test
    fun `the countdown never rounds up into a unit that has not been reached`() {
        // One millisecond short of each boundary must still show the smaller unit.
        assertEquals("in 59 min", relative(en, hour - 1))
        assertEquals("in 23h 59m", relative(en, day - 1))
        assertEquals("in 59 Min", relative(de, hour - 1))
        assertEquals("in 23 Std 59 Min", relative(de, day - 1))
    }

    // ---- repeat-day strip ------------------------------------------------

    @Test
    fun `a full week collapses to a single word`() {
        assertEquals("daily", NextAlarmWidget.formatDays(en, 0x7F, 0L))
        assertEquals("täglich", NextAlarmWidget.formatDays(de, 0x7F, 0L))
    }

    @Test
    fun `selected days are listed in week order, space separated`() {
        val monWedFri = (1 shl 0) or (1 shl 2) or (1 shl 4)
        assertEquals("Mon Wed Fri", NextAlarmWidget.formatDays(en, monWedFri, 0L))
        assertEquals("Mo Mi Fr", NextAlarmWidget.formatDays(de, monWedFri, 0L))
        // Bit order must not leak into the output.
        val friWedMon = (1 shl 4) or (1 shl 2) or (1 shl 0)
        assertEquals("Mon Wed Fri", NextAlarmWidget.formatDays(en, friWedMon, 0L))
    }

    @Test
    fun `a single weekday renders on its own`() {
        assertEquals("Sun", NextAlarmWidget.formatDays(en, 1 shl 6, 0L))
        assertEquals("So", NextAlarmWidget.formatDays(de, 1 shl 6, 0L))
        assertEquals("Sat", NextAlarmWidget.formatDays(en, 1 shl 5, 0L))
        assertEquals("Sa", NextAlarmWidget.formatDays(de, 1 shl 5, 0L))
    }

    @Test
    fun `a weekend selection is not mistaken for the full week`() {
        val weekend = (1 shl 5) or (1 shl 6)
        assertEquals("Sat Sun", NextAlarmWidget.formatDays(en, weekend, 0L))
        assertEquals("Sa So", NextAlarmWidget.formatDays(de, weekend, 0L))
    }

    @Test
    fun `a one-shot alarm shows the weekday it will actually fire on`() {
        val c = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 17, 6, 30, 0)   // a Monday
            set(Calendar.MILLISECOND, 0)
        }
        // Locale data renders the abbreviation as "Mon"/"Mo" or with a trailing
        // period depending on the CLDR version — the prefix is the contract.
        assertTrue(
            NextAlarmWidget.formatDays(en, 0, c.timeInMillis).startsWith("Mon"),
            "expected an English Monday label"
        )
        assertTrue(
            NextAlarmWidget.formatDays(de, 0, c.timeInMillis).startsWith("Mo"),
            "expected a German Monday label"
        )

        c.set(2026, Calendar.AUGUST, 22, 6, 30, 0)     // a Saturday
        assertTrue(NextAlarmWidget.formatDays(en, 0, c.timeInMillis).startsWith("Sat"))
        assertTrue(NextAlarmWidget.formatDays(de, 0, c.timeInMillis).startsWith("Sa"))
    }
}
