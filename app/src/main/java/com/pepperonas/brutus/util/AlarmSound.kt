package com.pepperonas.brutus.util

enum class AlarmSound(val id: Int, val displayName: String, val description: String) {
    SILENT(6, "Stumm", "Kein Ton — ideal zum Testen der Weckmodi"),
    SYSTEM(0, "System-Alarm", "Standard Android Alarm-Ton"),
    KLAXON(1, "Klaxon", "Pulsierender Zwei-Ton Alarm"),
    SIREN(2, "Sirene", "Auf- und abschwellender Sweep"),
    NUCLEAR(3, "Nuclear Alert", "Schnelles, scharfes Piepen"),
    PIERCING(5, "Durchdringend", "Ultra-hohes Piepen — maximal nervig");

    companion object {
        fun fromId(id: Int): AlarmSound = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}
