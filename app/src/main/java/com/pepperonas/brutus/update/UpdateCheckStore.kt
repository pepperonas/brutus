package com.pepperonas.brutus.update

import android.content.Context
import android.content.SharedPreferences
import com.pepperonas.brutus.util.Storage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * The opt-in switch and what the last check found. SharedPreferences rather than DataStore because the
 * worker reads it synchronously; [changes] lets the alarm list follow it live.
 */
object UpdateCheckStore {
    const val PREFS = "brutus_updates"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_LATEST = "latest_seen"
    private const val KEY_NOTIFIED = "notified_version"

    private fun prefs(c: Context) = Storage.prefs(c, PREFS)

    /** Off by default — also for installs that update from a version without the switch. */
    fun isEnabled(c: Context): Boolean = prefs(c).getBoolean(KEY_ENABLED, false)

    fun setEnabled(c: Context, enabled: Boolean) {
        prefs(c).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun latestSeen(c: Context): String? = prefs(c).getString(KEY_LATEST, null)

    fun recordLatest(c: Context, version: String) {
        prefs(c).edit().putString(KEY_LATEST, version).apply()
    }

    fun notifiedVersion(c: Context): String? = prefs(c).getString(KEY_NOTIFIED, null)

    fun markNotified(c: Context, version: String) {
        prefs(c).edit().putString(KEY_NOTIFIED, version).apply()
    }

    /** Switching off forgets the finding, so a stale banner cannot outlive the switch. */
    fun clearFindings(c: Context) {
        prefs(c).edit().remove(KEY_LATEST).remove(KEY_NOTIFIED).apply()
    }

    /**
     * Emits once on collection and after every change — a counter, not Unit: collectAsState drops a
     * value equal to the previous one, so a constant would never trigger a recomposition.
     */
    fun changes(c: Context): Flow<Int> = callbackFlow {
        val p = prefs(c)
        var n = 0
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(++n) }
        p.registerOnSharedPreferenceChangeListener(listener)
        trySend(n)
        awaitClose { p.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
