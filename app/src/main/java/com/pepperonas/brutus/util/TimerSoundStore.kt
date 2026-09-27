package com.pepperonas.brutus.util

import android.content.Context

/**
 * Persists the user's chosen timer-finished sound across launches.
 * Default is [AlarmSound.WIND_CHIMES] — soft bells rather than the harsh system alarm. A stored id of a
 * retired sound resolves to its successor through [AlarmSound.fromId].
 */
object TimerSoundStore {

    private const val PREFS = "brutus_timer"
    private const val KEY_SOUND_ID = "timer_sound_id"
    val DEFAULT_SOUND: AlarmSound = AlarmSound.WIND_CHIMES

    fun get(context: Context): AlarmSound {
        val prefs = Storage.prefs(context, PREFS)
        val id = prefs.getInt(KEY_SOUND_ID, DEFAULT_SOUND.id)
        return AlarmSound.fromId(id)
    }

    fun set(context: Context, sound: AlarmSound) {
        val prefs = Storage.prefs(context, PREFS)
        prefs.edit().putInt(KEY_SOUND_ID, sound.id).apply()
    }
}
