package com.pepperonas.brutus.util

import com.pepperonas.brutus.LocaleContexts
import com.pepperonas.brutus.data.AlarmEntity
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The enum's numeric ids are **persisted** — every alarm row stores `soundId`.
 * Renumbering an entry therefore silently rewrites what existing users hear in
 * the morning, which is exactly the class of change no compiler catches.
 * These tests pin the wire format; the display strings are covered because a
 * duplicate name makes the picker ambiguous.
 */
@RunWith(RobolectricTestRunner::class)
class AlarmSoundTest {

    /** Golden map. Changing an id here means migrating the `alarms.soundId` column. */
    private val pinnedIds = mapOf(
        "SYSTEM" to 0,
        "KLAXON" to 1,
        "NUCLEAR" to 3,
        "PIERCING" to 5,
        "SILENT" to 6,
        "AIRHORN" to 10,
        "JACKHAMMER" to 11,
        "FIRE_ALARM" to 12,
        "DENTIST" to 13,
        "BANSHEE" to 14,
        "AIR_RAID" to 15,
        "DIVE" to 16,
        "CAR_ALARM" to 17,
        "SCHOOL_BELL" to 18,
        "REVERSE_BEEPER" to 19,
        "SHEPARD" to 20,
        "STEEL_HAMMER" to 21,
        "STROBE" to 22,
        "WHOOP" to 23,
        "SINGING_BOWL" to 24,
        "BIRDS" to 25,
        "WIND_CHIMES" to 26,
        "KALIMBA" to 27,
        "HARP" to 28,
        "OCEAN" to 29,
        "ELECTRIC_PIANO" to 30,
        "SUNRISE" to 31,
    )

    /** Removed in v2.5.0: Siren, Chime, Marimba, Morning sun — alarms may still store these ids. */
    private val retired = mapOf(2 to "AIR_RAID", 7 to "WIND_CHIMES", 8 to "KALIMBA", 9 to "SUNRISE")

    @Test
    fun `retired ids resolve to their successor, never to the system tone`() {
        assertEquals(retired, AlarmSound.RETIRED.mapValues { it.value.name })
        retired.forEach { (id, name) -> assertEquals(name, AlarmSound.fromId(id).name) }
    }

    @Test
    fun `retired ids are never reused by a live sound`() {
        assertTrue(AlarmSound.entries.none { it.id in AlarmSound.RETIRED.keys })
    }

    @Test
    fun `a retired sound is replaced by one of the same character`() {
        // The siren was harsh; chime, marimba and morning sun were gentle.
        assertFalse(AlarmSound.fromId(2).gentle)
        listOf(7, 8, 9).forEach { assertTrue(AlarmSound.fromId(it).gentle, "id $it") }
    }

    @Test
    fun `persisted sound ids are pinned — renumbering would rewrite existing alarms`() {
        val actual = AlarmSound.entries.associate { it.name to it.id }
        assertEquals(pinnedIds, actual)
    }

    @Test
    fun `sound ids are unique — a collision makes fromId ambiguous`() {
        val ids = AlarmSound.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate id in AlarmSound: $ids")
    }

    @Test
    fun `fromId resolves every declared sound`() {
        AlarmSound.entries.forEach { sound ->
            assertEquals(sound, AlarmSound.fromId(sound.id))
        }
    }

    @Test
    fun `fromId falls back to SYSTEM for ids that were never assigned`() {
        // 4 is a real gap in the numbering, not a hypothetical one.
        assertFalse(AlarmSound.entries.any { it.id == 4 })
        assertEquals(AlarmSound.SYSTEM, AlarmSound.fromId(4))
        assertEquals(AlarmSound.SYSTEM, AlarmSound.fromId(99))
        assertEquals(AlarmSound.SYSTEM, AlarmSound.fromId(-1))
        assertEquals(AlarmSound.SYSTEM, AlarmSound.fromId(Int.MAX_VALUE))
    }

    @Test
    fun `display names are unique and non-blank in every language`() {
        listOf("en" to LocaleContexts.english(), "de" to LocaleContexts.german())
            .forEach { (tag, ctx) ->
                val names = AlarmSound.entries.map { it.label(ctx) }
                assertTrue(names.none { it.isBlank() }, "blank display name in $tag: $names")
                assertEquals(names.size, names.toSet().size, "duplicate display name in $tag: $names")
            }
    }

    @Test
    fun `every sound carries a description for the picker subtitle`() {
        listOf(LocaleContexts.english(), LocaleContexts.german()).forEach { ctx ->
            AlarmSound.entries.forEach {
                assertTrue(it.description(ctx).isNotBlank(), "${it.name} has no description")
            }
        }
    }

    @Test
    fun `every sound points at its own label and description resource`() {
        val labels = AlarmSound.entries.map { it.labelRes }
        val descriptions = AlarmSound.entries.map { it.descriptionRes }
        assertEquals(labels.size, labels.toSet().size, "two sounds share one label resource")
        assertEquals(
            descriptions.size, descriptions.toSet().size,
            "two sounds share one description resource"
        )
    }

    @Test
    fun `the translated names differ where the languages differ`() {
        val en = LocaleContexts.english()
        val de = LocaleContexts.german()
        // Proper nouns stay put …
        assertEquals(AlarmSound.KLAXON.label(en), AlarmSound.KLAXON.label(de))
        // … while translated ones actually change.
        assertEquals("Wind chimes", AlarmSound.WIND_CHIMES.label(en))
        assertEquals("Windspiel", AlarmSound.WIND_CHIMES.label(de))
        assertEquals("Silent", AlarmSound.SILENT.label(en))
        assertEquals("Stumm", AlarmSound.SILENT.label(de))
    }

    @Test
    fun `gentleSounds keeps enum declaration order so the timer picker is stable`() {
        val gentle = AlarmSound.gentleSounds()
        assertEquals(gentle.sortedBy { it.ordinal }, gentle)
    }

    @Test
    fun `gentleSounds offers no harsh wake sound`() {
        val harsh = AlarmSound.gentleSounds()
            .filter { !it.gentle && it != AlarmSound.SYSTEM && it != AlarmSound.SILENT }
        assertTrue(harsh.isEmpty(), "harsh sounds leaked into the timer picker: $harsh")
    }

    @Test
    fun `a new alarm defaults to a harsh sound, never a gentle one`() {
        val default = AlarmSound.fromId(AlarmEntity(hour = 7, minute = 0).soundId)
        assertFalse(default.gentle, "a brutal alarm must not default to $default")
        assertEquals(AlarmSound.KLAXON, default)
    }
}
