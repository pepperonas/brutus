package com.pepperonas.brutus.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
    val label: String = "",
    val repeatDays: Int = 0,       // bitmask: bit 0=Mon, bit 6=Sun
    val challengeType: Int = 0,    // 0=Math, 1=Shake, 2=QR
    val snoozeDuration: Int = 5,   // minutes
    val qrCodeData: String = "",   // stored QR data for verification
) {
    fun isDayEnabled(dayIndex: Int): Boolean = (repeatDays and (1 shl dayIndex)) != 0

    fun toggleDay(dayIndex: Int): AlarmEntity {
        return copy(repeatDays = repeatDays xor (1 shl dayIndex))
    }

    fun timeString(): String = "%02d:%02d".format(hour, minute)

    fun repeatDaysString(): String {
        if (repeatDays == 0) return "Einmalig"
        if (repeatDays == 0x7F) return "Jeden Tag"
        val days = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
        return days.filterIndexed { i, _ -> isDayEnabled(i) }.joinToString(", ")
    }

    fun challengeName(): String = when (challengeType) {
        0 -> "Mathe"
        1 -> "Schütteln"
        2 -> "QR-Code"
        else -> "Mathe"
    }
}
