package com.pepperonas.brutus.widget

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The home-screen widget is read at a glance, half asleep, without the app open.
 * Its two strings are pure functions of (trigger, now) and the repeat bitmask,
 * so they are worth pinning exactly.
 *
 * Robolectric only because the enclosing class extends AppWidgetProvider.
 */
@RunWith(RobolectricTestRunner::class)
class NextAlarmWidgetFormatTest {

    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour

    private fun relative(offset: Long): String {
        val now = 1_700_000_000_000L
        return NextAlarmWidget.formatRelative(now + offset, now)
    }

    // ---- countdown -------------------------------------------------------

    @Test
    fun `an alarm less than a minute out reads as gleich`() {
        assertEquals("gleich", relative(0))
        assertEquals("gleich", relative(30_000))
        assertEquals("gleich", relative(59_999))
    }

    @Test
    fun `a trigger in the past never renders a negative countdown`() {
        assertEquals("gleich", relative(-1))
        assertEquals("gleich", relative(-5 * day))
    }

    @Test
    fun `minutes are shown alone below the hour`() {
        assertEquals("in 1 Min", relative(minute))
        assertEquals("in 59 Min", relative(59 * minute))
    }

    @Test
    fun `hours carry the remaining minutes`() {
        assertEquals("in 1 Std 0 Min", relative(hour))
        assertEquals("in 7 Std 12 Min", relative(7 * hour + 12 * minute))
        assertEquals("in 23 Std 59 Min", relative(23 * hour + 59 * minute))
    }

    @Test
    fun `exactly one day is singular — 'in 1 Tagen' is wrong German`() {
        assertEquals("in 1 Tag", relative(day))
        assertEquals("in 1 Tag", relative(day + 23 * hour))
    }

    @Test
    fun `more than one day is plural`() {
        assertEquals("in 2 Tagen", relative(2 * day))
        assertEquals("in 6 Tagen", relative(6 * day + 5 * hour))
    }

    @Test
    fun `the countdown never rounds up into a unit that has not been reached`() {
        // One millisecond short of each boundary must still show the smaller unit.
        assertEquals("in 59 Min", relative(hour - 1))
        assertEquals("in 23 Std 59 Min", relative(day - 1))
    }

    // ---- repeat-day strip ------------------------------------------------

    @Test
    fun `a full week collapses to taeglich`() {
        assertEquals("täglich", NextAlarmWidget.formatDays(0x7F, 0L))
    }

    @Test
    fun `selected days are listed in week order, space separated`() {
        val monWedFri = (1 shl 0) or (1 shl 2) or (1 shl 4)
        assertEquals("Mo Mi Fr", NextAlarmWidget.formatDays(monWedFri, 0L))
        // Bit order must not leak into the output.
        val friWedMon = (1 shl 4) or (1 shl 2) or (1 shl 0)
        assertEquals("Mo Mi Fr", NextAlarmWidget.formatDays(friWedMon, 0L))
    }

    @Test
    fun `a single weekday renders on its own`() {
        assertEquals("So", NextAlarmWidget.formatDays(1 shl 6, 0L))
        assertEquals("Sa", NextAlarmWidget.formatDays(1 shl 5, 0L))
    }

    @Test
    fun `a weekend selection is not mistaken for the full week`() {
        val weekend = (1 shl 5) or (1 shl 6)
        assertEquals("Sa So", NextAlarmWidget.formatDays(weekend, 0L))
    }

    @Test
    fun `a one-shot alarm shows the weekday it will actually fire on`() {
        val c = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 17, 6, 30, 0)   // a Monday
            set(Calendar.MILLISECOND, 0)
        }
        val label = NextAlarmWidget.formatDays(0, c.timeInMillis)
        // Locale data renders German abbreviations as "Mo" or "Mo." depending on
        // the CLDR version — the prefix is the contract, the period is not.
        assertTrue(label.startsWith("Mo"), "expected a Monday label, got '$label'")

        c.set(2026, Calendar.AUGUST, 22, 6, 30, 0)     // a Saturday
        assertTrue(NextAlarmWidget.formatDays(0, c.timeInMillis).startsWith("Sa"))
    }
}
