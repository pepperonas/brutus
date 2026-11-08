package com.pepperonas.brutus.util

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Vacation mode: every alarm stays as it is but does not ring before [pausedUntil] — the first
 * occurrence on or after that moment rings normally. Running snoozes and Ultra Hardcore follow-ups
 * are not touched; they belong to an alarm that already rang.
 *
 * Stored is the start (00:00, local time) of the first day back, so "until Sunday" means: the
 * Monday alarm rings.
 */
object VacationStore {
    const val PREFS = "brutus_vacation"
    private const val KEY_UNTIL = "paused_until"

    private fun prefs(context: Context) = Storage.prefs(context, PREFS)

    /** The moment alarms ring again, or null when vacation mode is off or already over. */
    fun pausedUntil(context: Context, now: Long = System.currentTimeMillis()): Long? =
        prefs(context).getLong(KEY_UNTIL, -1L).takeIf { it > now }

    /** Pauses all alarms through the whole of [lastDay] (any moment of that day, local time). */
    fun pauseThrough(context: Context, lastDay: Long) {
        prefs(context).edit().putLong(KEY_UNTIL, startOfNextDay(lastDay)).commit()
    }

    fun resume(context: Context) {
        prefs(context).edit().remove(KEY_UNTIL).commit()
    }

    /** 00:00 of the day after the one [at] falls on — DST-safe via the calendar, not 24 h. */
    fun startOfNextDay(at: Long): Long = Calendar.getInstance().apply {
        timeInMillis = at
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis

    fun changes(context: Context): Flow<Int> = callbackFlow {
        val p = prefs(context)
        var n = 0
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(++n) }
        p.registerOnSharedPreferenceChangeListener(listener)
        trySend(n)
        awaitClose { p.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
