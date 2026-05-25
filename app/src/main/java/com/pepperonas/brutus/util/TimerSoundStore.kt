package com.pepperonas.brutus.util

import android.content.Context

/**
 * Persists the user's chosen timer-finished sound across launches.
 * Default is [AlarmSound.CHIME] — a soft 3-note bell rather than the harsh system alarm.
 */
object TimerSoundStore {

    private const val PREFS = "brutus_timer"
    private const val KEY_SOUND_ID = "timer_sound_id"
    val DEFAULT_SOUND: AlarmSound = AlarmSound.CHIME

    fun get(context: Context): AlarmSound {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = prefs.getInt(KEY_SOUND_ID, DEFAULT_SOUND.id)
        return AlarmSound.fromId(id)
    }

    fun set(context: Context, sound: AlarmSound) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_SOUND_ID, sound.id).apply()
    }
}
