package com.pepperonas.brutus.util

import com.pepperonas.brutus.data.AlarmEntity
import org.junit.Test
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
class AlarmSoundTest {

    /** Golden map. Changing an id here means migrating the `alarms.soundId` column. */
    private val pinnedIds = mapOf(
        "SYSTEM" to 0,
        "KLAXON" to 1,
        "SIREN" to 2,
        "NUCLEAR" to 3,
        "PIERCING" to 5,
        "SILENT" to 6,
        "CHIME" to 7,
        "MARIMBA" to 8,
        "MORNING" to 9,
        "AIRHORN" to 10,
        "JACKHAMMER" to 11,
        "FIRE_ALARM" to 12,
        "DENTIST" to 13,
        "BANSHEE" to 14,
    )

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
    fun `display names are unique and non-blank — the picker shows them raw`() {
        val names = AlarmSound.entries.map { it.displayName }
        assertTrue(names.none { it.isBlank() }, "blank display name in $names")
        assertEquals(names.size, names.toSet().size, "duplicate display name: $names")
    }

    @Test
    fun `every sound carries a description for the picker subtitle`() {
        AlarmSound.entries.forEach {
            assertTrue(it.description.isNotBlank(), "${it.name} has no description")
        }
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
