package com.pepperonas.brutus.util

enum class AlarmSound(
    val id: Int,
    val displayName: String,
    val description: String,
    /** Soft sounds are intended for the timer / gentle wake-up, harsh ones for hardcore alarms. */
    val gentle: Boolean = false,
) {
    SILENT(6, "Stumm", "Kein Ton — ideal zum Testen der Weckmodi"),
    SYSTEM(0, "System-Alarm", "Standard Android Alarm-Ton"),
    KLAXON(1, "Klaxon", "Pulsierender Zwei-Ton Alarm"),
    SIREN(2, "Sirene", "Auf- und abschwellender Sweep"),
    NUCLEAR(3, "Nuclear Alert", "Schnelles, scharfes Piepen"),
    PIERCING(5, "Durchdringend", "Ultra-hohes Piepen — maximal nervig"),

    // v1.7.0 — five extra extreme sounds for the hardcore crowd.
    AIRHORN(10, "Stadion-Horn", "Brüllendes Air-Horn aus verstimmten Sägezähnen"),
    JACKHAMMER(11, "Presslufthammer", "Pochende Tiefton-Salven wie eine Baustelle"),
    FIRE_ALARM(12, "Feueralarm", "Temporal-3 Rauchmelder-Muster bei 3,1 kHz"),
    DENTIST(13, "Bohrer", "FM-modulierter Zahnarzt-Bohrer — kreischendes Schleifen"),
    BANSHEE(14, "Banshee", "Dissonanter, aufsteigender Schwebungs-Cluster"),

    // v1.5.0 — gentle sounds, designed for the timer and casual wake-ups.
    CHIME(7, "Glockenspiel", "Sanftes 3-Ton-Glockenspiel mit Nachklang", gentle = true),
    MARIMBA(8, "Marimba", "Holziges Anschlag-Pattern", gentle = true),
    MORNING(9, "Morgensonne", "Langsam an- und abschwellender Akkord", gentle = true);

    companion object {
        fun fromId(id: Int): AlarmSound = entries.firstOrNull { it.id == id } ?: SYSTEM

        /** Sounds suitable for the timer / non-brutal contexts. */
        fun gentleSounds(): List<AlarmSound> = entries.filter { it.gentle || it == SYSTEM || it == SILENT }
    }
}
