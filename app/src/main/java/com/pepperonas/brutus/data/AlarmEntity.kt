package com.pepperonas.brutus.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pepperonas.brutus.util.AlarmSound
import com.pepperonas.brutus.util.ChallengeFlags

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
    val label: String = "",
    val repeatDays: Int = 0,              // bitmask: bit 0=Mon .. bit 6=Sun
    val challengeFlags: Int = ChallengeFlags.MATH,  // bitmask: MATH | SHAKE | QR
    val snoozeDuration: Int = 5,          // minutes
    val soundId: Int = AlarmSound.KLAXON.id,
    val mathProblemCount: Int = 3,        // number of math problems to solve
    val shakeCount: Int = 30,             // number of shakes required
    val hardcoreMode: Boolean = false,    // locks STREAM_ALARM on max + blocks volume keys while ringing
) {
    fun isDayEnabled(dayIndex: Int): Boolean = (repeatDays and (1 shl dayIndex)) != 0

    fun timeString(): String = "%02d:%02d".format(hour, minute)

    fun repeatDaysString(): String {
        if (repeatDays == 0) return "Einmalig"
        if (repeatDays == 0x7F) return "Jeden Tag"
        val days = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
        return days.filterIndexed { i, _ -> isDayEnabled(i) }.joinToString(", ")
    }

    fun challengeName(): String = ChallengeFlags.describe(challengeFlags)

    fun soundName(): String = AlarmSound.fromId(soundId).displayName
}
