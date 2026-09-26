package com.pepperonas.brutus.util

import android.content.Context

/**
 * State of the ringing path that must outlive the process: pending snoozes (so a reboot during
 * a snooze does not lose it) and the alarm volume from before an alarm forced it to maximum
 * (so a killed service does not leave the phone at full volume forever).
 */
object RingingStore {
    private const val PREFS = "brutus_ringing"
    private const val KEY_SNOOZE = "snooze_"          // snooze_{alarmId} = "triggerAt;followup;seq"
    private const val KEY_PREVIOUS_VOLUME = "previous_alarm_volume"

    data class Snooze(val alarmId: Long, val triggerAt: Long, val isFollowup: Boolean, val followupSeq: Int)

    private fun prefs(context: Context) = Storage.prefs(context, PREFS)

    fun recordSnooze(context: Context, snooze: Snooze) {
        prefs(context).edit()
            .putString("$KEY_SNOOZE${snooze.alarmId}", "${snooze.triggerAt};${snooze.isFollowup};${snooze.followupSeq}")
            .commit()
    }

    fun clearSnooze(context: Context, alarmId: Long) {
        prefs(context).edit().remove("$KEY_SNOOZE$alarmId").commit()
    }

    fun snoozes(context: Context): List<Snooze> =
        prefs(context).all.mapNotNull { (key, value) ->
            if (!key.startsWith(KEY_SNOOZE)) return@mapNotNull null
            val id = key.removePrefix(KEY_SNOOZE).toLongOrNull() ?: return@mapNotNull null
            val parts = (value as? String)?.split(';') ?: return@mapNotNull null
            if (parts.size != 3) return@mapNotNull null
            val at = parts[0].toLongOrNull() ?: return@mapNotNull null
            Snooze(id, at, parts[1].toBoolean(), parts[2].toIntOrNull() ?: 0)
        }

    /** Only the first alarm of a session records it — a takeover must not store "maximum". */
    fun rememberVolume(context: Context, volume: Int) {
        val p = prefs(context)
        if (!p.contains(KEY_PREVIOUS_VOLUME)) p.edit().putInt(KEY_PREVIOUS_VOLUME, volume).commit()
    }

    fun previousVolume(context: Context): Int? =
        prefs(context).getInt(KEY_PREVIOUS_VOLUME, -1).takeIf { it >= 0 }

    fun forgetVolume(context: Context) {
        prefs(context).edit().remove(KEY_PREVIOUS_VOLUME).commit()
    }
}
