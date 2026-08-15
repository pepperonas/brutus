package com.pepperonas.brutus.util

import android.content.Context
import androidx.annotation.StringRes
import com.pepperonas.brutus.R

/**
 * The catalogue of alarm sounds.
 *
 * [id] is **persisted** in `alarms.soundId` — it is the wire format and must
 * never be renumbered. The visible name and description are localized, so they
 * are resource ids rather than literals.
 */
enum class AlarmSound(
    val id: Int,
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    /** Soft sounds are intended for the timer / gentle wake-up, harsh ones for hardcore alarms. */
    val gentle: Boolean = false,
) {
    SILENT(6, R.string.sound_silent, R.string.sound_silent_description),
    SYSTEM(0, R.string.sound_system, R.string.sound_system_description),
    KLAXON(1, R.string.sound_klaxon, R.string.sound_klaxon_description),
    SIREN(2, R.string.sound_siren, R.string.sound_siren_description),
    NUCLEAR(3, R.string.sound_nuclear, R.string.sound_nuclear_description),
    PIERCING(5, R.string.sound_piercing, R.string.sound_piercing_description),

    // v1.7.0 — five extra extreme sounds for the hardcore crowd.
    AIRHORN(10, R.string.sound_airhorn, R.string.sound_airhorn_description),
    JACKHAMMER(11, R.string.sound_jackhammer, R.string.sound_jackhammer_description),
    FIRE_ALARM(12, R.string.sound_fire_alarm, R.string.sound_fire_alarm_description),
    DENTIST(13, R.string.sound_dentist, R.string.sound_dentist_description),
    BANSHEE(14, R.string.sound_banshee, R.string.sound_banshee_description),

    // v1.5.0 — gentle sounds, designed for the timer and casual wake-ups.
    CHIME(7, R.string.sound_chime, R.string.sound_chime_description, gentle = true),
    MARIMBA(8, R.string.sound_marimba, R.string.sound_marimba_description, gentle = true),
    MORNING(9, R.string.sound_morning, R.string.sound_morning_description, gentle = true);

    fun label(context: Context): String = context.getString(labelRes)

    fun description(context: Context): String = context.getString(descriptionRes)

    companion object {
        fun fromId(id: Int): AlarmSound = entries.firstOrNull { it.id == id } ?: SYSTEM

        /** Sounds suitable for the timer / non-brutal contexts. */
        fun gentleSounds(): List<AlarmSound> = entries.filter { it.gentle || it == SYSTEM || it == SILENT }
    }
}
