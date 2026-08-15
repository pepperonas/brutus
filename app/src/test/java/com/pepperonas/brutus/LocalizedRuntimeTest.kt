package com.pepperonas.brutus

import android.content.Context
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.util.AlarmSound
import com.pepperonas.brutus.util.ChallengeFlags
import com.pepperonas.brutus.util.NextAlarmCalculator
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Resolves the shipped strings the way the running app does — through the
 * resource system, once per language.
 *
 * [ResourceParityTest] diffs the XML files; this one proves the ids actually
 * resolve at runtime in both locales, and that a whole alarm card can be
 * rendered end to end in either language.
 */
@RunWith(RobolectricTestRunner::class)
class LocalizedRuntimeTest {

    private lateinit var en: Context
    private lateinit var de: Context

    @Before
    fun setUp() {
        en = LocaleContexts.english()
        de = LocaleContexts.german()
    }

    /** Every `<string name=…>` declared by the app itself. */
    private fun declaredStringKeys(): List<String> {
        val resDir = listOf(File("src/main/res"), File("app/src/main/res"))
            .first { it.isDirectory }
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(resDir, "values/strings.xml"))
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).map {
            (nodes.item(it) as org.w3c.dom.Element).getAttribute("name")
        }
    }

    @Test
    fun `every declared string resolves to non-blank text in both languages`() {
        val keys = declaredStringKeys()
        assertTrue(keys.size > 100, "expected the full catalogue, found ${keys.size}")

        listOf("en" to en, "de" to de).forEach { (tag, ctx) ->
            keys.forEach { key ->
                val id = ctx.resources.getIdentifier(key, "string", ctx.packageName)
                assertTrue(id != 0, "[$tag] resource id missing for '$key'")
                assertTrue(ctx.getString(id).isNotBlank(), "[$tag] '$key' resolves to blank")
            }
        }
    }

    @Test
    fun `the weekday array is complete and ordered Monday-first in both languages`() {
        val enDays = en.resources.getStringArray(R.array.weekday_short)
        val deDays = de.resources.getStringArray(R.array.weekday_short)

        assertEquals(7, enDays.size)
        assertEquals(7, deDays.size)
        assertEquals("Mon", enDays.first())
        assertEquals("Sun", enDays.last())
        assertEquals("Mo", deDays.first())
        assertEquals("So", deDays.last())
        // No duplicates — a copy-paste slip would silently label two columns alike.
        assertEquals(7, enDays.toSet().size)
        assertEquals(7, deDays.toSet().size)
    }

    @Test
    fun `plurals pick the right form in both languages`() {
        listOf(en, de).forEach { ctx ->
            val one = ctx.resources.getQuantityString(R.plurals.alarm_deleted, 1, 1)
            val many = ctx.resources.getQuantityString(R.plurals.alarm_deleted, 3, 3)
            assertTrue(one != many, "singular and plural are identical in ${ctx.resources.configuration.locales[0]}")
            assertTrue(many.contains("3"), "the count is missing from '$many'")
        }
    }

    @Test
    fun `a complete alarm card renders in English`() {
        val alarm = AlarmEntity(
            hour = 6,
            minute = 5,
            repeatDays = 0b0011111,                     // Mon–Fri
            challengeFlags = ChallengeFlags.MATH or ChallengeFlags.QR,
            soundId = AlarmSound.MORNING.id,
        )
        assertEquals("06:05", alarm.timeString())
        assertEquals("Mon, Tue, Wed, Thu, Fri", alarm.repeatDaysString(en))
        assertEquals("Math + QR code", alarm.challengeName(en))
        assertEquals("Morning sun", alarm.soundName(en))
        assertEquals(
            "Alarm in 2 hours, 30 minutes",
            NextAlarmCalculator.formatCountdown(en, 0L, 150 * 60_000L)
        )
    }

    @Test
    fun `the same alarm renders in German`() {
        val alarm = AlarmEntity(
            hour = 6,
            minute = 5,
            repeatDays = 0b0011111,
            challengeFlags = ChallengeFlags.MATH or ChallengeFlags.QR,
            soundId = AlarmSound.MORNING.id,
        )
        assertEquals("06:05", alarm.timeString())
        assertEquals("Mo, Di, Mi, Do, Fr", alarm.repeatDaysString(de))
        assertEquals("Mathe + QR-Code", alarm.challengeName(de))
        assertEquals("Morgensonne", alarm.soundName(de))
        assertEquals(
            "Alarm in 2 Stunden, 30 Minuten",
            NextAlarmCalculator.formatCountdown(de, 0L, 150 * 60_000L)
        )
    }

    @Test
    fun `notification channel names come from resources, not from literals`() {
        // The channels are created by BrutusApplication in the default locale;
        // what matters is that the ids stay stable while the names translate.
        assertEquals("Alarm service", en.getString(R.string.channel_service_name))
        assertEquals("Alarm-Dienst", de.getString(R.string.channel_service_name))
    }

    @Test
    fun `every format string can actually be formatted without crashing`() {
        // Feeding a wrong argument type to getString throws IllegalFormatException;
        // this walks the handful of formatted strings in both languages.
        listOf(en, de).forEach { ctx ->
            ctx.getString(R.string.chip_snooze, 5)
            ctx.getString(R.string.chip_sound, "Klaxon")
            ctx.getString(R.string.edit_sound_hint, "Klaxon")
            ctx.getString(R.string.edit_chain_order, "Math → Shake")
            ctx.getString(R.string.edit_qr_id, "ab12cd34")
            ctx.getString(R.string.edit_snooze_minutes, 10)
            ctx.getString(R.string.alarm_followup, 1)
            ctx.getString(R.string.alarm_challenge_progress, 1, 3)
            ctx.getString(R.string.math_progress, 1, 3, "Hard")
            ctx.getString(R.string.step_instruction, 30)
            ctx.getString(R.string.sunrise_countdown, "9:12")
            ctx.getString(R.string.world_region_offset, "Europe", "+02:00")
            ctx.getString(R.string.stopwatch_lap_number, 3)
            ctx.getString(R.string.countdown_hours, 7, 12)
            ctx.getString(R.string.countdown_minutes, 12)
            ctx.getString(R.string.notification_realarm_text, 2)
            ctx.getString(R.string.widget_in_minutes, 15)
            ctx.getString(R.string.widget_in_hours, 7, 12)
            ctx.resources.getQuantityString(R.plurals.countdown_days, 2, 2, 5)
            ctx.resources.getQuantityString(R.plurals.widget_in_days, 1, 1)
            ctx.resources.getQuantityString(R.plurals.shake_remaining, 4, 4)
            ctx.resources.getQuantityString(R.plurals.step_remaining, 4, 4)
            ctx.resources.getQuantityString(R.plurals.alarm_delete_all_body, 2, 2)
        }
    }
}
