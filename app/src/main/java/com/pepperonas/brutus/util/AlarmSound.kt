package com.pepperonas.brutus.util

import android.content.Context
import androidx.annotation.StringRes
import com.pepperonas.brutus.R

/**
 * The catalogue of alarm sounds.
 *
 * [id] is **persisted** in `alarms.soundId` (and in the timer and Sunrise preferences) — it is the
 * wire format and must never be renumbered or reused. The visible name and description are
 * localized, so they are resource ids rather than literals.
 */
enum class AlarmSound(
    val id: Int,
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    /** Soft sounds are intended for the timer / Sunrise / gentle wake-ups, harsh ones for hardcore alarms. */
    val gentle: Boolean = false,
) {
    SILENT(6, R.string.sound_silent, R.string.sound_silent_description),
    SYSTEM(0, R.string.sound_system, R.string.sound_system_description),
    KLAXON(1, R.string.sound_klaxon, R.string.sound_klaxon_description),
    NUCLEAR(3, R.string.sound_nuclear, R.string.sound_nuclear_description),
    PIERCING(5, R.string.sound_piercing, R.string.sound_piercing_description),

    // v1.7.0 — five extra extreme sounds for the hardcore crowd.
    AIRHORN(10, R.string.sound_airhorn, R.string.sound_airhorn_description),
    JACKHAMMER(11, R.string.sound_jackhammer, R.string.sound_jackhammer_description),
    FIRE_ALARM(12, R.string.sound_fire_alarm, R.string.sound_fire_alarm_description),
    DENTIST(13, R.string.sound_dentist, R.string.sound_dentist_description),
    BANSHEE(14, R.string.sound_banshee, R.string.sound_banshee_description),

    // v2.5.0 — nine more harsh sounds, chosen by ear from ten candidates.
    AIR_RAID(15, R.string.sound_air_raid, R.string.sound_air_raid_description),
    DIVE(16, R.string.sound_dive, R.string.sound_dive_description),
    CAR_ALARM(17, R.string.sound_car_alarm, R.string.sound_car_alarm_description),
    SCHOOL_BELL(18, R.string.sound_school_bell, R.string.sound_school_bell_description),
    REVERSE_BEEPER(19, R.string.sound_reverse_beeper, R.string.sound_reverse_beeper_description),
    SHEPARD(20, R.string.sound_shepard, R.string.sound_shepard_description),
    STEEL_HAMMER(21, R.string.sound_steel_hammer, R.string.sound_steel_hammer_description),
    STROBE(22, R.string.sound_strobe, R.string.sound_strobe_description),
    WHOOP(23, R.string.sound_whoop, R.string.sound_whoop_description),

    // v2.5.0 — gentle sounds built to loop for the whole Sunrise window.
    SINGING_BOWL(24, R.string.sound_singing_bowl, R.string.sound_singing_bowl_description, gentle = true),
    BIRDS(25, R.string.sound_birds, R.string.sound_birds_description, gentle = true),
    WIND_CHIMES(26, R.string.sound_wind_chimes, R.string.sound_wind_chimes_description, gentle = true),
    KALIMBA(27, R.string.sound_kalimba, R.string.sound_kalimba_description, gentle = true),
    HARP(28, R.string.sound_harp, R.string.sound_harp_description, gentle = true),
    OCEAN(29, R.string.sound_ocean, R.string.sound_ocean_description, gentle = true),
    ELECTRIC_PIANO(30, R.string.sound_electric_piano, R.string.sound_electric_piano_description, gentle = true),
    SUNRISE(31, R.string.sound_sunrise, R.string.sound_sunrise_description, gentle = true);

    fun label(context: Context): String = context.getString(labelRes)

    fun description(context: Context): String = context.getString(descriptionRes)

    companion object {
        /**
         * Ids of sounds removed in v2.5.0, each mapped to its closest successor. Alarms and the timer
         * preference may still store them — they keep ringing with a sound of the same character
         * instead of falling back to the system tone. These ids are never reused.
         */
        val RETIRED: Map<Int, AlarmSound> = mapOf(
            2 to AIR_RAID,     // Siren: a rising and falling sweep → the air-raid siren
            7 to WIND_CHIMES,  // Chime: three soft bells → wind chimes
            8 to KALIMBA,      // Marimba: woody plucks → kalimba
            9 to SUNRISE,      // Morning sun: a slowly swelling chord → the sunrise pad
        )

        fun fromId(id: Int): AlarmSound = entries.firstOrNull { it.id == id } ?: RETIRED[id] ?: SYSTEM

        /** Sounds suitable for the timer / non-brutal contexts. */
        fun gentleSounds(): List<AlarmSound> = entries.filter { it.gentle || it == SYSTEM || it == SILENT }

        /** What the Sunrise pre-alarm can play: the gentle sounds, or silence (light only). */
        fun sunriseSounds(): List<AlarmSound> = entries.filter { it.gentle } + SILENT
    }
}
